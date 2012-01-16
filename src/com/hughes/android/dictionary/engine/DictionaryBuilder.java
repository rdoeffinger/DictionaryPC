// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hughes.android.dictionary.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.hughes.android.dictionary.parser.DictFileParser;
import com.hughes.android.dictionary.parser.enwiktionary.EnWiktionaryXmlParser;
import com.hughes.util.Args;
import com.hughes.util.FileUtil;


public class DictionaryBuilder {
  
  public final Dictionary dictionary;
  public final List<IndexBuilder> indexBuilders = new ArrayList<IndexBuilder>();
  
  public DictionaryBuilder(final String dictInfo, final Language lang0, final Language lang1, final String normalizerRules1, final String normalizerRules2, final Set<String> lang1Stoplist, final Set<String> lang2Stoplist) {
    dictionary = new Dictionary(dictInfo);
    indexBuilders.add(new IndexBuilder(this, lang0.getIsoCode(), lang0.getIsoCode() + "->" + lang1.getIsoCode(), lang0, normalizerRules1, lang1Stoplist, false));
    indexBuilders.add(new IndexBuilder(this, lang1.getIsoCode(), lang1.getIsoCode() + "->" + lang0.getIsoCode(), lang1, normalizerRules2, lang2Stoplist, true));
  }
  
  void build() {
    for (final IndexBuilder indexBuilder : indexBuilders) {
      indexBuilder.build();
      dictionary.indices.add(indexBuilder.index);
    }
  }
  
  public static void main(final String[] args) throws IOException, ParserConfigurationException, SAXException {
    System.out.println("Running with arguments:");
    for (final String arg : args) {
      System.out.println(arg);
    }
    
    final Map<String,String> keyValueArgs = Args.keyValueArgs(args);
    
    if (!keyValueArgs.containsKey("lang1") || !keyValueArgs.containsKey("lang2")) {
      fatalError("--lang1= and --lang2= must both be specified.");
    }
    final Language lang1 = Language.lookup(keyValueArgs.remove("lang1"));
    final Language lang2 = Language.lookup(keyValueArgs.remove("lang2"));

    final Set<String> lang1Stoplist = new LinkedHashSet<String>();
    final Set<String> lang2Stoplist = new LinkedHashSet<String>();
    final String lang1StoplistFile = keyValueArgs.remove("lang1Stoplist");
    final String lang2StoplistFile = keyValueArgs.remove("lang2Stoplist");
    if (lang1StoplistFile != null) {
      lang1Stoplist.addAll(FileUtil.readLines(new File(lang1StoplistFile)));
    }
    if (lang2StoplistFile != null) {
      lang2Stoplist.addAll(FileUtil.readLines(new File(lang2StoplistFile)));
    }

    String normalizerRules1 = keyValueArgs.remove("normalizerRules1");
    String normalizerRules2 = keyValueArgs.remove("normalizerRules2");
    if (normalizerRules1 == null) {
      normalizerRules1 = lang1.getDefaultNormalizerRules();
    }
    if (normalizerRules2 == null) {
      normalizerRules2 = lang2.getDefaultNormalizerRules();
    }
    
    final String dictOutFilename = keyValueArgs.remove("dictOut");
    if (dictOutFilename == null) {
      fatalError("--dictOut= must be specified.");
    }
    
    String dictInfo = keyValueArgs.remove("dictInfo");
    if (dictInfo == null) {
      fatalError("--dictInfo= must be specified.");
    }
    if (dictInfo.startsWith("@")) {
      dictInfo = FileUtil.readToString(new File(dictInfo.substring(1)));
    }
    
    final String printFile = keyValueArgs.remove("print");
    
    System.out.println("lang1=" + lang1);
    System.out.println("lang2=" + lang2);
    System.out.println("normalizerRules1=" + normalizerRules1);
    System.out.println("normalizerRules2=" + normalizerRules2);
    System.out.println("dictInfo=" + dictInfo);
    System.out.println("dictOut=" + dictOutFilename);    
    
    final DictionaryBuilder dictionaryBuilder = new DictionaryBuilder(dictInfo, lang1, lang2, normalizerRules1, normalizerRules2, lang1Stoplist, lang2Stoplist);
    
    for (int i = 0; i < 100; ++i) {
      final String prefix = "input" + i;
      if (keyValueArgs.containsKey(prefix)) {
        final File file = new File(keyValueArgs.remove(prefix));
        System.out.println("Processing: " + file);
        String charsetName = keyValueArgs.remove(prefix + "Charset");
        if (charsetName == null) {
          charsetName = "UTF8";
        }
        final Charset charset = Charset.forName(charsetName);
        String inputName = keyValueArgs.remove(prefix + "Name");
        if (inputName == null) {
          fatalError("Must specify human readable name for: " + prefix + "Name");
        }

        final EntrySource entrySource = new EntrySource(dictionaryBuilder.dictionary.sources.size(), inputName, dictionaryBuilder.dictionary.pairEntries.size());
        System.out.println("");
        
        String inputFormat = keyValueArgs.remove(prefix + "Format");
        if ("tab_separated".equals(inputFormat)) {
          final boolean flipColumns = "true".equals(keyValueArgs.remove(prefix + "FlipColumns"));
          new DictFileParser(charset, flipColumns, DictFileParser.TAB, null, dictionaryBuilder, dictionaryBuilder.indexBuilders.toArray(new IndexBuilder[0]), null).parseFile(file);
        } else if ("chemnitz".equals(inputFormat)) {
          final boolean flipColumns = "true".equals(keyValueArgs.remove(prefix + "FlipColumns"));
          new DictFileParser(charset, flipColumns, DictFileParser.DOUBLE_COLON, DictFileParser.PIPE, dictionaryBuilder, dictionaryBuilder.indexBuilders.toArray(new IndexBuilder[0]), null).parseFile(file);
        } else if ("enwiktionary".equals(inputFormat)) {
          final Pattern langPattern = Pattern.compile(keyValueArgs.remove(prefix + "LangPattern"), Pattern.CASE_INSENSITIVE);
          final Pattern langCodePattern = Pattern.compile(keyValueArgs.remove(prefix + "LangCodePattern"));
          final int enIndex = Integer.parseInt(keyValueArgs.remove(prefix + "EnIndex")) - 1;
          String pageLimit = keyValueArgs.remove(prefix + "PageLimit");
          if (pageLimit == null) {
            pageLimit = "-1";
          }
            
          if (enIndex < 0 || enIndex >= 2) {
            fatalError("Must be 1 or 2: " + prefix + "EnIndex");
          }
          new EnWiktionaryXmlParser(dictionaryBuilder.indexBuilders.get(enIndex), dictionaryBuilder.indexBuilders.get(1-enIndex),
              langPattern, langCodePattern, enIndex != 0).parse(file, Integer.parseInt(pageLimit));
        } else {
          fatalError("Invalid or missing input format: " + inputFormat);
        }
        
        dictionaryBuilder.dictionary.sources.add(entrySource);
        System.out.println("Done: " + file + "\n\n");
      }
    }
   
    dictionaryBuilder.build();
    
    if (printFile != null) {
      final PrintStream out = new PrintStream(new File(printFile));
      dictionaryBuilder.dictionary.print(out);
      out.close();
    }
    
    System.out.println("Writing dictionary to: " + dictOutFilename);
    final RandomAccessFile dictOut = new RandomAccessFile(dictOutFilename, "rw");
    dictOut.setLength(0);
    dictionaryBuilder.dictionary.write(dictOut);
    dictOut.close();
    
    if (!keyValueArgs.isEmpty()) {
      System.err.println("WARNING: couldn't parse arguments: " + keyValueArgs);
      System.exit(1);
    }
  
  }
  
  private static void fatalError(String string) {
    System.err.println(string);
    
    
    System.exit(1);
  }
  
}

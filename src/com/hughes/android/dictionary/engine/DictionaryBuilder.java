package com.hughes.android.dictionary.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hughes.util.Args;
import com.hughes.util.FileUtil;

/*

--maxEntries=100
--dictOut=de-en.dict
--lang1=DE
--lang2=EN
--dictInfo=@dictInfo.txt

--input0=/Users/thadh/personal/quickDic/de-en-chemnitz.txt
--input0Name=chemnitz
--input0Charset=UTF8
--input0Format=chemnitz

--input1=/Users/thadh/personal/quickDic/dewiktionary-20100326-pages-articles.xml
--input1Name=wiktionary
--input1Format=wiktionary

--input2=/Users/thadh/personal/quickDic/de-en-dictcc.txt
--input2Name=dictcc
--input2Charset=Cp1252
--input2Format=dictcc
 */

public class DictionaryBuilder {
  
  final Dictionary dictionary;
  
  final List<IndexBuilder> indexBuilders = new ArrayList<IndexBuilder>();
  
  public DictionaryBuilder(final String dictInfo, final Language lang0, final Language lang1) {
    dictionary = new Dictionary(dictInfo);
    indexBuilders.add(new IndexBuilder(this, lang0.getSymbol(), lang0.getSymbol() + "->" + lang1.getSymbol(), lang0, false));
    indexBuilders.add(new IndexBuilder(this, lang1.getSymbol(), lang1.getSymbol() + "->" + lang0.getSymbol(), lang1, true));
  }
  
  void build() {
    for (final IndexBuilder indexBuilder : indexBuilders) {
      indexBuilder.build();
      dictionary.indices.add(indexBuilder.index);
    }
  }
  
  public static void main(final String[] args) throws IOException {
    final Map<String,String> keyValueArgs = Args.keyValueArgs(args);
    
    final Language lang1 = Language.lookup(keyValueArgs.remove("lang1"));
    final Language lang2 = Language.lookup(keyValueArgs.remove("lang2"));
    if (lang1 == null || lang2 == null) {
      fatalError("--lang1= and --lang2= must both be specified.");
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
    System.out.println("dictInfo=" + dictInfo);
    System.out.println("dictOut=" + dictOutFilename);    
    
    final DictionaryBuilder dictionaryBuilder = new DictionaryBuilder(dictInfo, lang1, lang2);
    
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

        String inputFormat = keyValueArgs.remove(prefix + "Format");
        if ("dictcc".equals(inputFormat)) {
          new DictFileParser(charset, false, DictFileParser.TAB, null, dictionaryBuilder, dictionaryBuilder.indexBuilders.toArray(new IndexBuilder[0]), null).parseFile(file);
        } else if ("chemnitz".equals(inputFormat)) {
          new DictFileParser(charset, false, DictFileParser.DOUBLE_COLON, DictFileParser.PIPE, dictionaryBuilder, dictionaryBuilder.indexBuilders.toArray(new IndexBuilder[0]), null).parseFile(file);
        } else if ("wiktionary".equals(inputFormat)) {
          throw new RuntimeException();
//          new WiktionaryXmlParser(dict).parse(file);
        } else {
          fatalError("Invalid or missing input format: " + inputFormat);
        }
        
        final EntrySource entrySource = new EntrySource(dictionaryBuilder.dictionary.sources.size(), inputName);
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

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

import com.hughes.android.dictionary.parser.wiktionary.EnTranslationToTranslationParser;
import com.hughes.android.dictionary.parser.wiktionary.WholeSectionToHtmlParser;
import com.hughes.android.dictionary.parser.wiktionary.WiktionaryLangs;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DictionaryBuilderMain extends TestCase {
  
  static final String INPUTS = "data/inputs/";
  static final String STOPLISTS = "data/inputs/stoplists/";
  static final String OUTPUTS = "data/outputs/";  
  
  // Build the non EN ones.
  static final String[][] nonEnPairs = new String[][] {
      {"EN"},
      {"DE"},
      {"IT"},
      // This one takes a really long time, and the result is too big for code.google.com
      //{"FR"},
          
      // The 3 I use most:
      {"IT", "EN" },
      {"DE", "EN" },
      {"DE", "IT" },

      {"AR", "DE" },
      {"AR", "ES" },
      {"AR", "FR" },
      {"AR", "HE" },
      {"AR", "IT" },
      {"AR", "JA" },
      {"AR", "RU" },
      {"AR", "TR" },  // Turkish
      {"AR", "cmn" },
      
      {"DE", "AR" },
      {"DE", "FR" },
      {"DE", "CA" },  // Catalan
      {"DE", "CS" },  // Czech
      {"DE", "EO" },  // Esperanto
      {"DE", "ES" },
      {"DE", "FR" },
      {"DE", "HE" },
      {"DE", "HU" },  // Hungarian
      {"DE", "IT" },
      {"DE", "JA" },
      {"DE", "LA" },  // Latin
      {"DE", "NL" },  // Dutch
      {"DE", "PL" },  // Polish
      {"DE", "RU" },
      {"DE", "SV" },  // Swedish
      {"DE", "TR" },  // Turkish
      {"DE", "cmn" },
      {"DE", "TA" },  // Tamil
      
      {"ES", "RU" },  // Spanish-Russian
      
      {"FR", "BG" },  // Bulgarian
      {"FR", "CS" },  // Czech
      {"FR", "DE" },
      {"FR", "ES" },
      {"FR", "IT" },
      {"FR", "JA" },
      {"FR", "LA" },
      {"FR", "NL" },  // Dutch
      {"FR", "RU" },
      {"FR", "TR" },  // Turkish
      {"FR", "cmn" },
      {"FR", "EL" },  

      {"IT", "DE" },
      {"IT", "EL" },  // Greek
      {"IT", "ES" },
      {"IT", "FR" },
      {"IT", "HU" },
      {"IT", "JA" },
      {"IT", "LA" },  // Latin
      {"IT", "LV" },  // Latvian
      {"IT", "NL" },
      {"IT", "PL" },
      {"IT", "RU" },
      {"IT", "SV" },
      {"IT", "TR" },  // Turkish
      {"IT", "cmn" },

      {"JA", "cmn" },
      {"JA", "AR" },
      {"JA", "KO" },

      {"cmn", "AR" },
      {"cmn", "DE" },
      {"cmn", "ES" },
      {"cmn", "FR" },
      {"cmn", "IT" },
      {"cmn", "KO" },

      {"NO", "SV" },
      {"NO", "FI" },
      {"FI", "SV" },
      
      {"PL", "FR" },  // Polish
      {"PL", "RU" },  // Polish
      {"PL", "HU" },  // Polish
      {"PL", "ES" },  // Polish
      
      {"TR", "EL" },  // Turkish, Greek

      {"FA", "HY" },  // Persian, Armenian, by request.
      {"FA", "SV" },  // Persian, Swedish, by request.
      {"NL", "PL" },  // Dutch, Polish, by request.
      
  };


  
  static final Map<String,String>  isoToDedication = new LinkedHashMap<String, String>();
  static {
  isoToDedication.put("AF", "Wiktionary-based Afrikaans dictionary dedicated to Heiko and Mariëtte Horn.");
  isoToDedication.put("HR", "Wiktionary-based Croatian dictionary dedicated to Ines Viskic and Miro Kresonja.");
  isoToDedication.put("NL", "Wiktionary-based Dutch dictionary dedicated to Mike LeBeau.");
  isoToDedication.put("DE", "@data/inputs/de-en_dedication.txt");
  isoToDedication.put("EL", "Wiktionary-based Greek dictionary dedicated to Noah Egge.");
  isoToDedication.put("IT", "Wiktionary-based Italian dictionary dedicated to Carolina Tropini, my favorite stardust in the whole universe!  Ti amo!");
  isoToDedication.put("KO", "Wiktionary-based Korean dictionary dedicated to Ande Elwood--fall fashion und Fernsehturms!");
  isoToDedication.put("PT", "Wiktionary-based Portuguese dictionary dedicated to Carlos Melo, one Tough Mudder.");
  isoToDedication.put("RO", "Wiktionary-based Romanian dictionary dedicated to Radu Teodorescu.");
  isoToDedication.put("RU", "Wiktionary-based Russian dictionary dedicated to Maxim Aronin--best friend always!.");
  isoToDedication.put("SR", "Wiktionary-based Serbian dictionary dedicated to Filip Crnogorac--thanks for the honey.");
  isoToDedication.put("ES", "Wiktionary-based Spanish dictionary made especially for Carolina Tropini! <3 XoXoXXXXX!");
  isoToDedication.put("SV", "Wiktionary-based Swedish dictionary dedicated to Kajsa Palmblad--björn kramar!");
  }
  private static String getEnDictionaryInfo(String iso) {
    return isoToDedication.containsKey(iso) ? isoToDedication.get(iso) : String.format("Wiktionary-based %s dictionary.", iso);
  }
  
  static final Map<String,String>  isoToStoplist = new LinkedHashMap<String, String>();
  static {
  isoToStoplist.put("DE", "de.txt");
  isoToStoplist.put("EN", "en.txt");
  isoToStoplist.put("ES", "es.txt");
  isoToStoplist.put("IT", "it.txt");
  isoToStoplist.put("FR", "fr.txt");
  }
  private static String getStoplist(String iso) {
    return isoToStoplist.containsKey(iso) ? isoToStoplist.get(iso) : "empty.txt";
  }
  
  static String getOtherLang(final String[] pair, final String first) {
      assert Arrays.asList(pair).contains(first);
      assert pair.length == 2;
      return pair[0].equals(first) ? pair[1] : pair[0];
  }
  
  static List<String> getMainArgs(final String[] pair) {
      final List<String> result = new ArrayList<String>();
      
    int i = 1;
    
    if (pair.length == 1) {
        final String lang1 = pair[0];
        final String dictFile = String.format("%s/%s.quickdic", OUTPUTS, lang1);
        result.add(String.format("--dictOut=%s", dictFile));
        result.add(String.format("--lang1=%s", lang1));
        result.add(String.format("--lang1Stoplist=%s", STOPLISTS + getStoplist(lang1)));
        result.add(String.format("--dictInfo=Wikitionary-based %s dictionary.", lang1));

        
        final String wikiSplitFile = String.format("%s/wikiSplit/%s/%s.data", INPUTS, lang1.toLowerCase(), lang1);
        if (new File(wikiSplitFile).canRead()) {
            result.add(String.format("--input%d=%s", i, wikiSplitFile));
            result.add(String.format("--input%dName=%s.wiktionary.org", i, lang1.toLowerCase()));
            result.add(String.format("--input%dFormat=%s", i, WholeSectionToHtmlParser.NAME));
            result.add(String.format("--input%dTitleIndex=%d", i, 1));
            result.add(String.format("--input%dWiktionaryLang=%s", i, lang1));
            result.add(String.format("--input%dSkipLang=%s", i, lang1));
            result.add(String.format("--input%dWebUrlTemplate=http://%s.wiktionary.org/wiki/%%s", i, lang1.toLowerCase()));
            //result.add(String.format("--input%dPageLimit=100", i));
            ++i;
        } else {
            System.err.println("Can't read file: " + wikiSplitFile);
        }
        
        if (lang1.equals("EN") && !lang1.equals("EN")) {
            // Add a parser that tries to use the definitions.  This is
            // not very pretty yet.
            result.add(String.format("--input%d=%s/wikiSplit/en/%s.data", i, INPUTS, lang1));
            result.add(String.format("--input%dName=ENWiktionary.%s", i, lang1)) ;
            result.add(String.format("--input%dFormat=enwiktionary", i));
            result.add(String.format("--input%dWiktionaryType=EnEnglish", i));
            result.add(String.format("--input%dLangPattern=%s", i, "English"));
            result.add(String.format("--input%dLangCodePattern=%s", i, lang1.toLowerCase()));
            result.add(String.format("--input%dEnIndex=%d", i, 1));
            //result.add(String.format("--input%dPageLimit=100", i));
            ++i;
        }
        
        return result;
    }  // Single-lang dictionaries.
    
    final String lang1 = pair[0];
    final String lang2 = pair[1];
    
    final String dictFile = String.format("%s/%s-%s.quickdic", 
        OUTPUTS, lang1, lang2);
    
    result.add(String.format("--dictOut=%s", dictFile));
    result.add(String.format("--lang1Stoplist=%s", STOPLISTS + getStoplist(lang1)));
    result.add(String.format("--lang2Stoplist=%s", STOPLISTS + getStoplist(lang2)));

    // For a few langs, put the defs of the other language in DE/IT/FR using WholeSection.
    for (final String wikitionaryLang : Arrays.asList("EN", "DE", "IT", "FR")) {
        if (!Arrays.asList(pair).contains(wikitionaryLang)) {
            continue;
        }
        final String foreignIso = getOtherLang(pair, wikitionaryLang);
        final String wikiSplitFile = String.format("%s/wikiSplit/%s/%s.data", INPUTS, wikitionaryLang.toLowerCase(), foreignIso);
        if (!new File(wikiSplitFile).canRead()) {
            System.err.println("WARNING: Can't read file: " + wikiSplitFile);
            continue;
        }
        result.add(String.format("--input%d=%s", i, wikiSplitFile));
        result.add(String.format("--input%dName=%s.wiktionary.org", i, wikitionaryLang.toLowerCase()));
        result.add(String.format("--input%dFormat=%s", i, WholeSectionToHtmlParser.NAME));
        result.add(String.format("--input%dTitleIndex=%d", i, Arrays.asList(pair).indexOf(foreignIso) + 1));
        result.add(String.format("--input%dWiktionaryLang=%s", i, wikitionaryLang));
        result.add(String.format("--input%dSkipLang=%s", i, foreignIso));
        result.add(String.format("--input%dWebUrlTemplate=http://%s.wiktionary.org/wiki/%%s", i, wikitionaryLang.toLowerCase()));
        ++i;
    }
    
    // Deal with the pairs where one is English.
    if (Arrays.asList(pair).contains("EN")) {
      final String foreignIso = getOtherLang(pair, "EN");
      String foreignRegex = WiktionaryLangs.isoCodeToEnWikiName.get(foreignIso);
      
      result.add(String.format("--lang1=%s", lang1));
      result.add(String.format("--lang2=%s",  lang2));
      result.add(String.format("--dictInfo=%s", getEnDictionaryInfo(foreignIso)));
      
      // Foreign section.
      result.add(String.format("--input%d=%s/wikiSplit/en/%s.data", i, INPUTS, foreignIso));
      result.add(String.format("--input%dName=ENWiktionary.%s", i, foreignIso)) ;
      result.add(String.format("--input%dFormat=enwiktionary", i));
      result.add(String.format("--input%dWiktionaryType=EnForeign", i));
      result.add(String.format("--input%dLangPattern=%s", i, foreignRegex));
      result.add(String.format("--input%dLangCodePattern=%s", i, foreignIso.toLowerCase()));
      result.add(String.format("--input%dEnIndex=%d", i, Arrays.asList(pair).indexOf("EN") + 1));
      ++i;

      // Translation section.
      result.add(String.format("--input%d=%swikiSplit/en/EN.data", i, INPUTS));
      result.add(String.format("--input%dName=enwiktionary.english", i));
      result.add(String.format("--input%dFormat=enwiktionary", i));
      result.add(String.format("--input%dWiktionaryType=EnToTranslation", i));
      result.add(String.format("--input%dLangPattern=%s", i, foreignRegex));
      result.add(String.format("--input%dLangCodePattern=%s", i, foreignIso.toLowerCase()));
      result.add(String.format("--input%dEnIndex=%d", i, Arrays.asList(pair).indexOf("EN") + 1));
      ++i;
      
      if (foreignIso.equals("DE")) {
        result.add(String.format("--input%d=%sde-en_chemnitz.txt", i, INPUTS));
        result.add(String.format("--input%dName=chemnitz", i));
        result.add(String.format("--input%dCharset=UTF8", i));
        result.add(String.format("--input%dFormat=chemnitz", i));
        ++i;
      }
      
    } else {
      // Pairs without English.
      result.add(String.format("--lang1=%s", lang1));
      result.add(String.format("--lang2=%s", lang2));
      result.add(String.format("--dictInfo=Wikitionary-based %s-%s dictionary.", lang1, lang2));
 
      result.add(String.format("--input%d=%swikiSplit/en/EN.data", i, INPUTS));
      result.add(String.format("--input%dName=BETA!enwiktionary.%s-%s", i, lang1, lang2));
      result.add(String.format("--input%dFormat=%s", i, EnTranslationToTranslationParser.NAME));
      result.add(String.format("--input%dLangPattern1=%s", i, lang1));
      result.add(String.format("--input%dLangPattern2=%s", i, lang2));
      ++i;
      
      // TODO: Could use FR translation section here too.
    }
    
    return result;
  }

  public static void main(final String[] args) throws Exception {
    
    final List<String[]> allPairs = new ArrayList<String[]>();
    
    allPairs.addAll(Arrays.asList(nonEnPairs));
    // Add all the EN-XX pairs.
    for (final String isoCode : WiktionaryLangs.isoCodeToEnWikiName.keySet()) {
      if (!isoCode.equals("EN")) {
          allPairs.add(new String[] {"EN", isoCode});
      }
    }
    
        
    final Set<List<String>> done = new LinkedHashSet<List<String>>();
    boolean go = true;
    for (final String[] pair : allPairs) {
      Arrays.sort(pair);
      final List<String> pairList = Arrays.asList(pair);
      if (done.contains(pairList)) {
        continue;
      }
      done.add(pairList);
      
      if (pairList.contains("EN") && pairList.contains("DE")) {
          go = true;
      } else {
          go = false;
      }
      
      if (!go) {
          continue;
      }
      
      DictionaryBuilder.main(getMainArgs(pair).toArray(new String[0]));
    }
    
  }    
}

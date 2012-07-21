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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.hughes.android.dictionary.parser.wiktionary.EnTranslationToTranslationParser;
import com.hughes.android.dictionary.parser.wiktionary.WiktionaryLangs;

public class DictionaryBuilderMain extends TestCase {
  
  static final String INPUTS = "data/inputs/";
  static final String STOPLISTS = "data/inputs/stoplists/";
  static final String OUTPUTS = "data/outputs/";  
    
  public static void main(final String[] args) throws Exception {
    
    // Builds all the dictionaries it can, outputs list to a text file.
    
    final Map<String,String> isoToWikiName = new LinkedHashMap<String, String>(WiktionaryLangs.isoCodeToWikiName);
    isoToWikiName.remove("EN");
    isoToWikiName.remove("DE");

    final Map<String,String>  isoToDedication = new LinkedHashMap<String, String>();
    isoToDedication.put("AF", "Afrikaans dictionary dedicated to Heiko and Mariëtte Horn.");
    isoToDedication.put("HR", "Croatian dictionary dedicated to Ines Viskic and Miro Kresonja.");
    isoToDedication.put("NL", "Dutch dictionary dedicated to Mike LeBeau.");
    // German handled in file.
    isoToDedication.put("EL", "Greek dictionary dedicated to Noah Egge.");
    isoToDedication.put("IT", "Italian dictionary dedicated to Carolina Tropini, my favorite stardust in the whole universe!  Ti amo!");
    isoToDedication.put("KO", "Korean dictionary dedicated to Ande Elwood--fall fashion und Fernsehturms!");
    isoToDedication.put("PT", "Portuguese dictionary dedicated to Carlos Melo, one Tough Mudder.");
    isoToDedication.put("RO", "Romanian dictionary dedicated to Radu Teodorescu.");
    isoToDedication.put("RU", "Russian dictionary dedicated to Maxim Aronin--best friend always!.");
    isoToDedication.put("SR", "Serbian dictionary dedicated to Filip Crnogorac--thanks for the honey.");
    isoToDedication.put("ES", "Spanish dictionary made especially for Carolina Tropini! <3 XoXoXXXXX!");
    isoToDedication.put("SV", "Swedish dictionary dedicated to Kajsa Palmblad--björn kramar!");

    final Map<String,String>  isoToStoplist = new LinkedHashMap<String, String>();
    isoToStoplist.put("DE", "de.txt");
    isoToStoplist.put("EN", "en.txt");
    isoToStoplist.put("ES", "es.txt");
    isoToStoplist.put("IT", "it.txt");
    isoToStoplist.put("FR", "fr.txt");

    final Map<String,String>  isoToRegex = new LinkedHashMap<String, String>();
    // HACK: The missing "e" prevents a full match, causing "Cantonese" to be appended to the entries.
    isoToRegex.put("ZH", "Chinese|Mandarin|Cantones");
    
    
    // Build the non EN ones.
    
    final String[][] nonEnPairs = new String[][] {
        
        /*
        {"AR", "DE" },
        {"AR", "ES" },
        {"AR", "FR" },
        {"AR", "HE" },
        {"AR", "IT" },
        {"AR", "JA" },
        {"AR", "RU" },
        {"AR", "TR" },  // Turkish
        {"AR", "ZH" },
        
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
        {"DE", "PL" },  // Polish
        {"DE", "RU" },
        {"DE", "SV" },  // Swedish
        {"DE", "TR" },  // Turkish
        {"DE", "ZH" },

        
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
        {"FR", "ZH" },

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
        {"IT", "ZH" },

        {"JA", "ZH" },
        {"JA", "AR" },
        {"JA", "KO" },

        {"ZH", "AR" },
        {"ZH", "DE" },
        {"ZH", "ES" },
        {"ZH", "FR" },
        {"ZH", "IT" },
        {"ZH", "KO" },

        
        {"NO", "SV" },
        {"NO", "FI" },
        {"FI", "SV" },
        
        {"PL", "FR" },  // Polish
        {"PL", "RU" },  // Polish
        {"PL", "HU" },  // Polish
        {"PL", "ES" },  // Polish

        */
        

    };
    
    final Set<List<String>> done = new LinkedHashSet<List<String>>();
    for (final String[] pair : nonEnPairs) {
      Arrays.sort(pair);
      final List<String> pairList = Arrays.asList(pair);
      if (done.contains(pairList)) {
        continue;
      }
      done.add(pairList);
      
      final String lang1 = pair[0];
      final String lang2 = pair[1];
      
      final String dictFile = String.format("%s/%s-%s_enwiktionary_BETA.quickdic", 
          OUTPUTS, lang1, lang2);
      System.out.println("building dictFile: " + dictFile);

      if (!isoToStoplist.containsKey(lang1)) {
        isoToStoplist.put(lang1, "empty.txt");
      }
      if (!isoToStoplist.containsKey(lang2)) {
        isoToStoplist.put(lang2, "empty.txt");
      }
      
      DictionaryBuilder.main(new String[] {
          String.format("--dictOut=%s", dictFile),
          String.format("--lang1=%s", lang1),
          String.format("--lang2=%s", lang2),
          String.format("--lang1Stoplist=%s", STOPLISTS + isoToStoplist.get(lang1)),
          String.format("--lang2Stoplist=%s", STOPLISTS + isoToStoplist.get(lang2)),
          String.format("--dictInfo=(EN)Wikitionary-based %s-%s dictionary.", lang1, lang2),

          String.format("--input2=%swikiSplit/en/EN.data", INPUTS),
          String.format("--input2Name=BETA!enwiktionary.%s-%s", lang1, lang2),
          String.format("--input2Format=%s", EnTranslationToTranslationParser.NAME),
          String.format("--input2LangPattern1=%s", lang1),
          String.format("--input2LangPattern2=%s", lang2),
      });
    }
    if (1==1) {
      //return;
    }


    // Now build the EN ones.
    
//    isoToWikiName.keySet().retainAll(Arrays.asList("UK", "HR", "FI"));
    //isoToWikiName.clear();
    boolean go = false;
    for (final String foreignIso : isoToWikiName.keySet()) {
      if (foreignIso.equals("SL")) {
        go = true;
      }
      if (!go) {
        continue;
      }

        final String dictFile = String.format("%s/EN-%s_enwiktionary.quickdic", OUTPUTS, foreignIso);
        System.out.println("building dictFile: " + dictFile);
        
        if (!isoToStoplist.containsKey(foreignIso)) {
          isoToStoplist.put(foreignIso, "empty.txt");
        }
        if (!isoToDedication.containsKey(foreignIso)) {
          isoToDedication.put(foreignIso, "");
        }
        if (!isoToRegex.containsKey(foreignIso)) {
          isoToRegex.put(foreignIso, isoToWikiName.get(foreignIso));
        }
  
        DictionaryBuilder.main(new String[] {
            String.format("--dictOut=%s", dictFile),
            String.format("--lang1=EN"),
            String.format("--lang2=%s", foreignIso),
            String.format("--lang1Stoplist=%s", STOPLISTS + isoToStoplist.get("EN")),
            String.format("--lang2Stoplist=%s", STOPLISTS + isoToStoplist.get(foreignIso)),
            String.format("--dictInfo=(EN)Wikitionary-based EN-%s dictionary.\n\n%s", foreignIso, isoToDedication.get(foreignIso)),

            "--input2=" + INPUTS + "wikiSplit/en/" + foreignIso + ".data",
            "--input2Name=enwiktionary." + foreignIso,
            "--input2Format=enwiktionary",
            "--input2WiktionaryType=EnForeign",
            "--input2LangPattern=" + isoToRegex.get(foreignIso),
            "--input2LangCodePattern=" + foreignIso.toLowerCase(),
            "--input2EnIndex=1",

            "--input3=" + INPUTS + "wikiSplit/en/EN.data",
            "--input3Name=enwiktionary.english",
            "--input3Format=enwiktionary",
            "--input3WiktionaryType=EnToTranslation",
            "--input3LangPattern=" + isoToRegex.get(foreignIso),
            "--input3LangCodePattern=" + foreignIso.toLowerCase(),
            "--input3EnIndex=1",

        });
        
    }  // foreignIso

    // Now special case German-English.

    final String dictFile = String.format("%s/DE-EN_chemnitz_enwiktionary.quickdic", OUTPUTS);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + dictFile,
        "--lang1=DE",
        "--lang2=EN",
        String.format("--lang1Stoplist=%s", STOPLISTS + "de.txt"),
        String.format("--lang2Stoplist=%s", STOPLISTS + "en.txt"),
        "--dictInfo=@" + INPUTS + "de-en_chemnitz_enwiktionary.info",

        "--input4=" + INPUTS + "de-en_chemnitz.txt",
        "--input4Name=chemnitz",
        "--input4Charset=UTF8",
        "--input4Format=chemnitz",
        
        "--input2=" + INPUTS + "wikiSplit/en/DE.data",
        "--input2Name=enwiktionary.DE",
        "--input2Format=enwiktionary",
        "--input2WiktionaryType=EnForeign",
        "--input2LangPattern=German",
        "--input2LangCodePattern=de",
        "--input2EnIndex=2",

        "--input3=" + INPUTS + "wikiSplit/en/EN.data",
        "--input3Name=enwiktionary.english",
        "--input3Format=enwiktionary",
        "--input3WiktionaryType=EnToTranslation",
        "--input3LangPattern=German",
        "--input3LangCodePattern=de",
        "--input3EnIndex=2",
    });
    
  }
    
}

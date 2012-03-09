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

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

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
    
//    isoToWikiName.keySet().retainAll(Arrays.asList("UK", "HR", "FI"));
    //isoToWikiName.clear();
    boolean go = true;
    for (final String foreignIso : isoToWikiName.keySet()) {
      if (foreignIso.equals("GD")) {
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

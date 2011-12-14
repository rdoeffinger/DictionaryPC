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
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class DictionaryBuilderMain extends TestCase {
  
  static final String INPUTS = "../DictionaryData/inputs/";
  static final String OUTPUTS = "../DictionaryData/outputs/";
  
  static class Lang {
    final String nameRegex;
    final String isoCode;
    final String wikiSplit;
    public Lang(String nameRegex, String code, final String wikiSplit) {
      this.nameRegex = nameRegex;
      this.isoCode = code;
      this.wikiSplit = wikiSplit;
    }
  }
  
  
  public static void main(final String[] args) throws Exception {

    Lang[] langs1 = new Lang[] { 
        new Lang("^English$", "EN", null),
    };
    Lang[] langs2 = new Lang[] { 
        new Lang("^.*Italian.*$", "IT", "italian.data"),
        new Lang("^.*Greek.*$", "EL", "greek.data"),
        new Lang("^.*Spanish.*$", "ES", "spanish.data"),
        /*
        new Lang("^German$", "DE"),
        new Lang("^Afrikaans$", "AF"),
        new Lang("^Armenian$", "HY"),
        new Lang("^Arabic$", "AR"),
        new Lang("^Chinese$|^Mandarin$", "ZH"),
        new Lang("^Croation$", "HR"),
        new Lang("^Czech$", "CS"),
        new Lang("^Dutch$", "NL"),
        new Lang("^English$", "EN"),
        new Lang("^Finnish$", "FI"),
        new Lang("^French$", "FR"),
        new Lang("^Hebrew$", "HE"),
        new Lang("^Hindi$", "HI"),
        new Lang("^Icelandic$", "IS"),
        new Lang("^Irish$", "GA"),
        new Lang("^Japanese$", "JA"),
        new Lang("^Korean$", "KO"),
        new Lang("^Kurdish$", "KU"),
        new Lang("^Lithuanian$", "LT"),
        new Lang("^Malay$", "MS"),
        new Lang("^Maori$", "MI"),
        new Lang("^Mongolian$", "MN"),
        new Lang("^Norwegian$", "NO"),
        new Lang("^Persian$", "FA"),
        new Lang("^Portuguese$", "PT"),
        new Lang("^Romanian$", "RO"),
        new Lang("^Russian$", "RU"),
        new Lang("^Sanskrit$", "SA"),
        new Lang("^Serbian$", "SR"),
        new Lang("^Somali$", "SO"),
        new Lang("^Sudanese$", "SU"),
        new Lang("^Swedish$", "SV"),
        new Lang("^Tajik$", "TG"),
        new Lang("^Thai$", "TH"),
        new Lang("^Tibetan$", "BO"),
        new Lang("^Turkish$", "TR"),
        new Lang("^Ukranian$", "UK"),
        new Lang("^Vietnamese$", "VI"),
        new Lang("^Welsh$", "CY"),
        new Lang("^Yiddish$", "YI"),
        new Lang("^Zulu$", "ZU"),*/
    };
    
    for (final Lang lang1 : langs1) {
      for (final Lang lang2 : langs2) {
        if (lang1.nameRegex.equals(lang2.nameRegex)) {
          continue;
        }
        
        int enIndex = -1;
        Lang nonEnglish = null;
        if (lang2.isoCode.equals("EN")) {
          enIndex = 2;
          nonEnglish = lang1;
        }
        if (lang1.isoCode.equals("EN")) {
          enIndex = 1;
          nonEnglish = lang2;
        }
        assert nonEnglish != null;

        final String dictFile = String.format(OUTPUTS + "/%s-%s_enwiktionary.quickdic", lang1.isoCode, lang2.isoCode);
        System.out.println("building dictFile: " + dictFile);
        DictionaryBuilder.main(new String[] {
            String.format("--dictOut=%s", dictFile),
            String.format("--lang1=%s", lang1.isoCode),
            String.format("--lang2=%s", lang2.isoCode),
            String.format("--dictInfo=(EN)Wikitionary-based %s-%s dictionary", lang1.isoCode, lang2.isoCode),

            "--input2=" + INPUTS + "wikiSplit/" + nonEnglish.wikiSplit,
            "--input2Name=enwiktionary." + nonEnglish.wikiSplit,
            "--input2Format=enwiktionary",
            "--input2LangPattern=" + nonEnglish.nameRegex,
            "--input2LangCodePattern=" + nonEnglish.isoCode.toLowerCase(),
            "--input2EnIndex=" + enIndex,

            "--input3=" + INPUTS + "wikiSplit/english.data",
            "--input3Name=enwiktionary.english",
            "--input3Format=enwiktionary",
            "--input3LangPattern=" + nonEnglish.nameRegex,
            "--input3LangCodePattern=" + (enIndex == 1 ? lang2.isoCode : lang1.isoCode).toLowerCase(),
            "--input3EnIndex=" + enIndex,

        });
        
        // Print the entries for diffing.
        final RandomAccessFile raf = new RandomAccessFile(new File(dictFile), "r");
        final Dictionary dict = new Dictionary(raf);
        final PrintWriter textOut = new PrintWriter(new File(dictFile + ".text"));
        final List<PairEntry> sorted = new ArrayList<PairEntry>(dict.pairEntries);
        Collections.sort(sorted);
        for (final PairEntry pairEntry : sorted) {
          textOut.println(pairEntry.getRawText(false));
        }
        textOut.close();
        raf.close();

      }  // langs2
    }  // langs1

    DictionaryBuilder.main(new String[] {
        "--dictOut=" + OUTPUTS + "DE-EN_chemnitz.quickdic",
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=@" + INPUTS + "de-en_chemnitz.info",

        "--input1=" + INPUTS + "de-en_chemnitz.txt",
        "--input1Name=chemnitz",
        "--input1Charset=UTF8",
        "--input1Format=chemnitz",
    });

    DictionaryBuilder.main(new String[] {
        "--dictOut=" + OUTPUTS + "de-en_all.quickdic",
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=@" + INPUTS + "de-en_all.info",

        "--input2=" + INPUTS + "de-en_chemnitz.txt",
        "--input2Name=dictcc",
        "--input2Charset=UTF8",
        "--input2Format=chemnitz",

        "--input3=" + INPUTS + "/copyrighted/de-en_dictcc.txt",
        "--input3Name=dictcc",
        "--input3Charset=UTF8",
        "--input3Format=dictcc",
        
        // TODO: wiktionary
    });

  }
  
}

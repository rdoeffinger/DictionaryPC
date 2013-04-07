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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Collections;

import com.hughes.android.dictionary.parser.wiktionary.EnTranslationToTranslationParser;
import com.hughes.android.dictionary.parser.wiktionary.WholeSectionToHtmlParser;
import com.hughes.util.FileUtil;

import junit.framework.TestCase;

public class DictionaryBuilderTest extends TestCase {
  
  public static final String TEST_INPUTS = "testdata/inputs/";
  public static final String WIKISPLIT = "data/inputs/wikiSplit/";
  public static final String WIKISPLIT_EN = "data/inputs/wikiSplit/en/";
  public static final String STOPLISTS = "data/inputs/stoplists/";
  public static final String GOLDENS = "testdata/goldens/";

  public static final String TEST_OUTPUTS = "testdata/outputs/";

  public void testItConj() throws Exception {
      final String toParse = "{{it-conj-are|d|avere|pres2s=dai|pres3s=dà|pres3p=danno|prem1s=diedi|prem1s2=detti|prem2s=desti|prem3s=diede|prem3s2=dette|prem1p=demmo|prem2p=deste|prem3p=diedero|prem3p2=dettero|fut1s=darò|fut2s=darai|fut3s=darà|fut1p=daremo|fut2p=darete|fut3p=daranno|cond1s=darei|cond2s=daresti|cond3s=darebbe|cond1p=daremmo|cond2p=dareste|cond3p=darebbero|sub123s=dia|sub3p=diano|impsub12s=dessi|impsub3s=desse|impsub1p=dessimo|impsub2p=deste|impsub3p=dessero|imp2s=dà|imp2s2=dai|imp2s3=da'|imp3s=dia|imp3p=diano}}\n" +
              "{{it-conj-are|accus|avere}}\n" +
              "{{it-conj-care|pag|avere or essere}}\n" +
              "{{it-conj-iare|studi|avere}}\n" +
              "{{it-conj-iare-b|avvi|avere}}\n" +
              "{{it-conj-ciare|pronunc|avere}}\n" +
              "{{it-conj-ere|sed|essere|pres1s=siedo|pres1s2=seggo|pres2s=siedi|pres3s=siede|pres3p=siedono|pres3p2=seggono|fut1s2=siederò|fut2s2=siederai|fut3s2=siederà|fut1p2=siederemo|fut2p2=siederete|fut3p2=siederanno|cond1s2=siederei|cond2s2=siederesti|cond3s2=siederebbe|cond1p2=siederemmo|cond2p2=siedereste|cond3p2=siederebbero|sub123s=sieda|sub3p=siedano|imp2s=siedi|imp3s=sieda|imp3s2=segga|imp3p=siedano|imp3p2=seggano}}\n" +
              "{{it-conj-ere|persuad|avere|pastp=persuaso|prem1s=persuasi|prem3s=persuase|prem3s2=''|prem3p=persuasero|prem3p2=''}}\n" +
              "{{it-conj-ere|abbatt|avere}}\n" +
              "{{it-conj-ire|copr|avere|pastp=coperto|prem1s2=copersi|prem3s2=coperse|prem3p2=copersero}}\n" +
              "{{it-conj-ire-b|prefer|avere}}\n" +
              "{{it-conj-urre|prod|avere}}\n" +
              "{{it-conj-arsi|lav}}\n" +
              "{{it-conj-ersi|abbatt}}\n" +
              "{{it-conj-iarsi|annoi}}\n" +
              "{{it-conj-carsi|coniug}}\n" +
              "{{it-conj-ciarsi|affacc}}\n" +
              "{{it-conj-irsi|vest}}\n" +
              "{{it-conj-irsi-b|fer}}\n" +
              "{{it-conj-ursi|rid|essere}}\n" +
              "{{it-conj-cire|ricuc|avere}}\n" +
              "{{it-conj-iarsi-b|riavvi|essere}}" +
              "{{it-conj-fare|putre|avere}}\n" + 
              "{{it-conj-cirsi|cuc|essere}}\n" +
              "{{it-conj-ere|smett|avere|pastp=smesso|prem1s=smisi|prem3s=smise|prem3s2=''|prem3p=smisero|prem3p2=''}}\n" +
              "{{term||[[cor#Latin|Cor]] [[Carolus#Latin|Carolī]]|Charles' heart}}\n" +
              "{{term|sc=Grek|λόγος|tr=lógos||word}}\n" +
              "{{term|verbo|verbō|for the word}}\n"
              ;
      final DictionaryBuilder db = new DictionaryBuilder("", Language.en, Language.it,  "", "", Collections.singleton("X"), Collections.singleton("X"));
      WholeSectionToHtmlParser parser = new WholeSectionToHtmlParser(db.indexBuilders.get(0), null, "EN", "IT", "http://en.wiktionary.org/wiki/%s");
      parser.title = "dummyTitle";
      parser.entrySource = new EntrySource(0, "dummySource", 0);
      parser.parseSection("dummyHeading", toParse);
      db.build();
      
      final String dictName = "testItConj.html";
      final PrintStream out = new PrintStream(new File(TEST_OUTPUTS, dictName));
      db.dictionary.print(out);
      out.close();
      
      assertFilesEqual(GOLDENS + dictName, TEST_OUTPUTS + dictName);
  }
  
  public void doTestCustomDict(final String name, final String lang1,
      final String lang2, final String inputFile) throws Exception {
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=" + lang1,
        "--lang2=" + lang2,
        "--lang1Stoplist=" + STOPLISTS + "empty.txt",
        "--lang2Stoplist=" + STOPLISTS + "empty.txt",
        "--dictInfo=bleh.",
        
        "--input1=testdata/inputs/" + inputFile,
        "--input1Name=my_input_" + name,
        "--input1Charset=ISO-8859-1",
        "--input1Format=tab_separated",

        "--print=" + result.getPath() + ".text",
    });
    
    checkGolden(name, result); 
  }
  
  public void test_FR_NL() throws Exception {
    doTestCustomDict("QuickDic-FR-NL.quickdic", "FR", "NL", "QuickDic-FR-NL.txt");
  }
  
  public void testWiktionary_en_de2fr() throws Exception {
    wiktionaryTestWithEnTrans2Trans("wiktionary.de_fr.quickdic", "DE", "FR");
  }

  public void wiktionaryTestWithEnTrans2Trans(final String name, final String lang1,
      final String lang2) throws Exception {
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=" + lang1,
        "--lang2=" + lang2,
        "--lang1Stoplist=" + STOPLISTS + "empty.txt",
        "--lang2Stoplist=" + STOPLISTS + "empty.txt",
        "--dictInfo=SomeWikiDataTrans2Trans",

        "--input4=" + WIKISPLIT_EN + "EN.data",
        "--input4Name=" + name,
        "--input4Format=" + EnTranslationToTranslationParser.NAME,
        "--input4LangPattern1=" + lang1,
        "--input4LangPattern2=" + lang2,
        "--input4PageLimit=1000",

        "--print=" + result.getPath() + ".text",
    });
    
    checkGolden(name, result); 
  }

  public void testWiktionary_WholeSection_DE() throws Exception {
    enWiktionaryTestWithWholeSectionToHtml("enwiktionary.WholeSection.DE.quickdic", "DE", 100);
  }

  public void testWiktionary_WholeSection_EN() throws Exception {
    enWiktionaryTestWithWholeSectionToHtml("enwiktionary.WholeSection.EN.quickdic", "EN", 100);
  }

  public void testWiktionary_WholeSection_IT() throws Exception {
    // Have to run to 800 to get a few verb conjugations (including essere!)
    enWiktionaryTestWithWholeSectionToHtml("enwiktionary.WholeSection.IT.quickdic", "IT", 800);
  }

  public void enWiktionaryTestWithWholeSectionToHtml(final String name, final String langCode, final int pageLimit) throws Exception {
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=" + langCode,
        "--lang2=" + "EN",
        "--lang1Stoplist=" + STOPLISTS + "empty.txt",
        "--lang2Stoplist=" + STOPLISTS + "empty.txt",
        "--dictInfo=SomeWikiDataWholeSection",

        "--input4=" + WIKISPLIT_EN + langCode + ".data",
        "--input4Name=" + name,
        "--input4Format=" + WholeSectionToHtmlParser.NAME,
        "--input4WiktionaryLang=EN",
        "--input4SkipLang=" + langCode,
        "--input4TitleIndex=" + "1",
        "--input4PageLimit=" + pageLimit,

        "--print=" + result.getPath() + ".text",
    });
    checkGolden(name, result); 
  }
  
  //-----------------------------------------------------------------

  public void testSingleLang_EN() throws Exception {
      wiktionaryTestSingleLang("SingleLang_EN.quickdic", "EN", 100);
  }

  public void testSingleLang_DE() throws Exception {
      wiktionaryTestSingleLang("SingleLang_DE.quickdic", "DE", 100);
  }

  public void testSingleLang_IT() throws Exception {
      wiktionaryTestSingleLang("SingleLang_IT.quickdic", "IT", 100);
  }

  public void testSingleLang_FR() throws Exception {
      wiktionaryTestSingleLang("SingleLang_FR.quickdic", "FR", 100);
  }

  public void wiktionaryTestSingleLang(final String name, final String langCode, final int pageLimit) throws Exception {
      final File result = new File(TEST_OUTPUTS + name);
      System.out.println("Writing to: " + result);
      DictionaryBuilder.main(new String[] {
          "--dictOut=" + result.getAbsolutePath(),
          "--lang1=" + langCode,
          "--lang1Stoplist=" + STOPLISTS + "empty.txt",
          "--dictInfo=SomeWikiDataWholeSection",
          "--input4=" + WIKISPLIT + langCode.toLowerCase() + "/" + langCode + ".data",
          "--input4Name=" + name,
          "--input4Format=" + WholeSectionToHtmlParser.NAME,
          "--input4WiktionaryLang=" + langCode,
          "--input4SkipLang=" + langCode,
          "--input4TitleIndex=" + "1",
          "--input4PageLimit=" + pageLimit,
          "--print=" + result.getPath() + ".text",
      });
      checkGolden(name, result); 
    }

  //-----------------------------------------------------------------

  public void testWiktionary_IT_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.it_en.quickdic", "IT", "it.txt",
        "EN.data", "enwiktionary.english", "Italian", "it", 1000);
  }

  public void testWiktionary_cmn_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.cmn_en.quickdic", "cmn", "empty.txt",
        // These missing "e" prevents a complete match, forcing the name to be printed
        "EN.data", "enwiktionary.english", "Chinese|Mandarin", "cmn", 1000);
  }

  public void testWiktionary_DE_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.de_en.quickdic", "DE", "de.txt",
        "EN.data", "enwiktionary.english", "German", "de", 1000);
  }

  public void testWiktionary_IT_IT() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.it_it.quickdic", "IT", "it.txt",
        "IT.data", "enwiktionary.italian", "Italian", "it", 1000);
  }

  // French
  public void testWiktionary_FR_FR() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.fr_fr.quickdic", "FR", "fr.txt",
        "FR.data", "enwiktionary.french", "French", "fr", 1000);
  }

  
  // Arabic
  public void testWiktionary_AR_AR() throws Exception {
      // Arabic is really big for some reason, use fewer pages.
    wiktionaryTestWithLangToEn("wiktionary.ar_ar.quickdic", "AR", "empty.txt",
        "AR.data", "enwiktionary.arabic", "Arabic", "ar", 200);
  }

  // Chinese
  public void testWiktionary_cmn_cmn() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.cmn_cmn.quickdic", "cmn", "empty.txt",
        // These missing "e" prevents a complete match, forcing the name to be printed.
        "cmn.data", "enwiktionary.chinese", "Chinese|Mandarin", "cmn", 1000);
  }

  // German
  public void testWiktionary_DE_DE() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.de_de.quickdic", "DE", "de.txt",
        "DE.data", "enwiktionary.german", "German", "de", 1000);
  }

  // Thai
  public void testWiktionary_TH_TH() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.th_th.quickdic", "TH", "empty.txt",
        // These missing "e" prevents a complete match, forcing the name to be printed.
        "TH.data", "enwiktionary.thai", "Thai", "th", 1000);
  }

  public void wiktionaryTestWithLangToEn(final String name, final String lang1,
      final String stoplist, final String data, final String dictName,
      final String langPattern, final String langCode, int pageLimit) throws Exception {
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    final String type = data.equals("EN.data") ? "EnToTranslation" : "EnForeign";
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=" + lang1,
        "--lang2=EN",
        "--lang1Stoplist=" + STOPLISTS + stoplist,
        "--lang2Stoplist=" + STOPLISTS + "en.txt",
        "--dictInfo=SomeWikiData",

        "--input4=" + WIKISPLIT_EN + data,
        "--input4Name=" + dictName,
        "--input4Format=enwiktionary",
        "--input4WiktionaryType=" + type,
        "--input4LangPattern=" + langPattern,
        "--input4LangCodePattern=" + langCode,
        "--input4EnIndex=2",
        "--input4PageLimit=" + pageLimit,

        "--print=" + result.getPath() + ".text",
    });
    
    checkGolden(name, result); 
  }

  public void testGermanCombined() throws Exception {
    final String name = "de-en.quickdic";
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=@" + TEST_INPUTS + "de-en_dictInfo.txt",

        "--input1=" + TEST_INPUTS + "de-en_chemnitz_100",
        "--input1Name=chemnitz",
        "--input1Charset=UTF8",
        "--input1Format=chemnitz",

        "--input2=" + TEST_INPUTS + "de-en_dictcc_simulated",
        "--input2Name=dictcc",
        "--input2Charset=UTF8",
        "--input2Format=tab_separated",

        "--print=" + result.getPath() + ".text",
    });
    
    checkGolden(name, result); 
  }

  public void testItalianTurkish() throws Exception {
      final String name = "it-tr_dictcc.quickdic";
      final File result = new File(TEST_OUTPUTS + name);
      System.out.println("Writing to: " + result);
      DictionaryBuilder.main(new String[] {
          "--dictOut=" + result.getAbsolutePath(),
          "--lang1=IT",
          "--lang2=TR",
          "--dictInfo=it-tr_dictcc_simulated",

          "--input1=" + TEST_INPUTS + "it-tr_dictcc_simulated.txt",
          "--input1Name=dictcc",
          "--input1Charset=UTF8",
          "--input1Format=tab_separated",

          "--print=" + result.getPath() + ".text",
      });
      
      checkGolden(name, result); 
    }

  private void checkGolden(final String dictName, final File dictFile)
      throws IOException, FileNotFoundException {
    // Check it once:
    assertFilesEqual(GOLDENS + dictName + ".text", dictFile.getPath() + ".text");

    // Check it again.
    final Dictionary dict = new Dictionary(new RandomAccessFile(dictFile.getAbsolutePath(), "r"));
    final PrintStream out = new PrintStream(new File(dictFile.getPath() + ".text"));
    dict.print(out);
    out.close();
    assertFilesEqual(GOLDENS + dictName + ".text", dictFile.getPath() + ".text");
  }


  void assertFilesEqual(final String expected, final String actual) throws IOException {
    final String expectedString = FileUtil.readToString(new File(expected));
    final String actualString = FileUtil.readToString(new File(actual));
    assertEquals(expectedString, actualString);
  }

  
}

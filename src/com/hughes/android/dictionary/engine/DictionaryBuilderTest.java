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

import com.hughes.android.dictionary.parser.wiktionary.EnTranslationToTranslationParser;
import com.hughes.android.dictionary.parser.wiktionary.WholeSectionToHtmlParser;
import com.hughes.util.FileUtil;

import junit.framework.TestCase;

public class DictionaryBuilderTest extends TestCase {
  
  public static final String TEST_INPUTS = "testdata/inputs/";
  public static final String WIKISPLIT_EN = "data/inputs/wikiSplit/en/";
  public static final String STOPLISTS = "data/inputs/stoplists/";
  public static final String GOLDENS = "testdata/goldens/";

  public static final String TEST_OUTPUTS = "testdata/outputs/";

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
    wiktionaryTestWithWholeSectionToHtml("wiktionary.WholeSection.DE.quickdic", "DE", 100);
  }

  public void testWiktionary_WholeSection_EN() throws Exception {
    wiktionaryTestWithWholeSectionToHtml("wiktionary.WholeSection.EN.quickdic", "EN", 100);
  }

  public void testWiktionary_WholeSection_IT() throws Exception {
    // Have to run to 800 to get a few verb conjugations (including essere!)
    wiktionaryTestWithWholeSectionToHtml("wiktionary.WholeSection.IT.quickdic", "IT", 800);
  }

  public void wiktionaryTestWithWholeSectionToHtml(final String name, final String langCode, final int pageLimit) throws Exception {
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

  
  public void testWiktionary_IT_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.it_en.quickdic", "IT", "it.txt",
        "EN.data", "enwiktionary.english", "Italian", "it");
  }

  public void testWiktionary_ZH_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.zh_en.quickdic", "ZH", "empty.txt",
        // These missing "e" prevents a complete match, forcing the name to be printed
        "EN.data", "enwiktionary.english", "Chinese|Mandarin|Cantones", "zh");
  }

  public void testWiktionary_DE_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.de_en.quickdic", "DE", "de.txt",
        "EN.data", "enwiktionary.english", "German", "it");
  }

  public void testWiktionary_IT_IT() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.it_it.quickdic", "IT", "it.txt",
        "IT.data", "enwiktionary.italian", "Italian", "it");
  }

  // French
  public void testWiktionary_FR_FR() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.fr_fr.quickdic", "FR", "fr.txt",
        "FR.data", "enwiktionary.french", "French", "fr");
  }

  
  // Arabic
  public void testWiktionary_AR_AR() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.ar_ar.quickdic", "AR", "empty.txt",
        "AR.data", "enwiktionary.arabic", "Arabic", "ar");
  }

  // Chinese
  public void testWiktionary_ZH_ZH() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.zh_zh.quickdic", "ZH", "empty.txt",
        // These missing "e" prevents a complete match, forcing the name to be printed.
        "ZH.data", "enwiktionary.chinese", "Chinese|Mandarin|Cantones", "zh");
  }

  // German
  public void testWiktionary_DE_DE() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.de_de.quickdic", "DE", "de.txt",
        "DE.data", "enwiktionary.german", "German", "it");
  }

  // Thai
  public void testWiktionary_TH_TH() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.th_th.quickdic", "TH", "empty.txt",
        // These missing "e" prevents a complete match, forcing the name to be printed.
        "TH.data", "enwiktionary.thai", "Thai", "th");
  }

  public void wiktionaryTestWithLangToEn(final String name, final String lang1,
      final String stoplist, final String data, final String dictName,
      final String langPattern, final String langCode) throws Exception {
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
        "--input4PageLimit=1000",

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

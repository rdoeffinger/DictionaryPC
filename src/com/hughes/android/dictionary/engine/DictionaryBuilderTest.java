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

import com.hughes.util.FileUtil;

import junit.framework.TestCase;

public class DictionaryBuilderTest extends TestCase {
  
  public static final String TEST_INPUTS = "testdata/inputs/";
  public static final String WIKISPLIT = "../DictionaryData/inputs/enWikiSplit/";
  public static final String STOPLISTS = "../DictionaryData/inputs/stoplists/";
  public static final String GOLDENS = "testdata/goldens/";

  public static final String TEST_OUTPUTS = "testdata/outputs/";

  // Chinese
  public void testWiktionary_ZH_ZH() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.zh_zh.quickdic", "ZH", "empty.txt",
        "ZH.data", "enwiktionary.chinese", "Chinese|Mandarin|Cantonese", "zh");
  }

  public void testWiktionary_ZH_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.zh_en.quickdic", "ZH", "empty.txt",
        "EN.data", "enwiktionary.english", "Chinese|Mandarin|Cantonese", "zh");
  }
  
  // German
  public void testWiktionary_DE_DE() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.de_de.quickdic", "DE", "de.txt",
        "DE.data", "enwiktionary.german", "German", "it");
  }

  public void testWiktionary_DE_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.de_en.quickdic", "DE", "de.txt",
        "EN.data", "enwiktionary.english", "German", "it");
  }

  // Italian
  public void testWiktionary_IT_IT() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.it_it.quickdic", "IT", "it.txt",
        "IT.data", "enwiktionary.italian", "Italian", "it");
  }

  public void testWiktionary_IT_EN() throws Exception {
    wiktionaryTestWithLangToEn("wiktionary.it_en.quickdic", "IT", "it.txt",
        "EN.data", "enwiktionary.english", "Italian", "it");
  }

  public void wiktionaryTestWithLangToEn(final String name, final String lang1,
      final String stoplist, final String data, final String dictName,
      final String langPattern, final String langCode) throws Exception {
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=" + lang1,
        "--lang2=EN",
        "--lang1Stoplist=" + STOPLISTS + stoplist,
        "--lang2Stoplist=" + STOPLISTS + "en.txt",
        "--dictInfo=SomeWikiData",

        "--input4=" + WIKISPLIT + data,
        "--input4Name=" + dictName,
        "--input4Format=enwiktionary",
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
        "--input2Format=dictcc",

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

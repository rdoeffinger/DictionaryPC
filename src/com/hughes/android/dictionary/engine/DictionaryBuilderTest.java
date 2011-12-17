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
  
  public static final String TEST_INPUTS = "../DictionaryData/testdata/inputs/";
  public static final String WIKISPLIT = "../DictionaryData/inputs/enWikiSplit/";
  public static final String STOPLISTS = "../DictionaryData/inputs/stoplists/";
  public static final String GOLDENS = "../DictionaryData/testdata/goldens/";

  public static final String TEST_OUTPUTS = "../DictionaryData/testdata/outputs/";

  public void testWiktionaryItalianFromItalian() throws Exception {
    final String name = "wiktionary.it_it.quickdic";
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=IT",
        "--lang2=EN",
        "--lang1Stoplist=" + STOPLISTS + "it.txt",
        "--lang2Stoplist=" + STOPLISTS + "en.txt",
        "--dictInfo=SomeWikiData",

        "--input4=" + WIKISPLIT + "italian.data",
        "--input4Name=enwiktionary.italian",
        "--input4Format=enwiktionary",
        "--input4LangPattern=Italian",
        "--input4LangCodePattern=it",
        "--input4EnIndex=2",
        "--input4PageLimit=1000",

        "--print=" + result.getPath() + ".text",
    });
    
    checkGolden(name, result); 
  }

  public void testWiktionaryItalianFromEnglish() throws Exception {
    final String name = "wiktionary.it_en.quickdic";
    final File result = new File(TEST_OUTPUTS + name);
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=IT",
        "--lang2=EN",
        "--lang1Stoplist=" + STOPLISTS + "it.txt",
        "--lang2Stoplist=" + STOPLISTS + "en.txt",
        "--dictInfo=SomeWikiData",

        "--input3=" + WIKISPLIT + "english.data",
        "--input3Name=enwiktionary.english",
        "--input3Format=enwiktionary",
        "--input3LangPattern=Italian",
        "--input3LangCodePattern=it",
        "--input3EnIndex=2",
        "--input3PageLimit=1000",

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

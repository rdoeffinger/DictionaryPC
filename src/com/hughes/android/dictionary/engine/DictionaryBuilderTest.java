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

import com.hughes.util.FileUtil;

import junit.framework.TestCase;

public class DictionaryBuilderTest extends TestCase {
  
  public void testWiktionaryItalian() throws Exception {
    final File result = new File("testdata/wiktionary.it.quickdic");
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=IT",
        "--lang2=EN",
        "--dictInfo=SomeWikiData",

        /*
        "--input3=wikiSplit/english.data",
        "--input3Name=enwiktionary.english",
        "--input3Format=enwiktionary",
        "--input3LangPattern=Italian",
        "--input3LangCodePattern=it",
        "--input3EnIndex=2",
        "--input3PageLimit=1000",
*/
        "--input4=wikiSplit/italian.data",
        "--input4Name=enwiktionary.italian",
        "--input4Format=enwiktionary",
        "--input4LangPattern=Italian",
        "--input4LangCodePattern=it",
        "--input4EnIndex=2",
        "--input4PageLimit=1000",

        "--print=testdata/wiktionary.it.test",
    });
    
    // Check it once:
    assertFilesEqual("testdata/wiktionary.it.golden2", "testdata/wiktionary.it.test"); 
    
    
    // Check it again.
    final Dictionary dict = new Dictionary(new RandomAccessFile(result.getAbsolutePath(), "r"));
    final PrintStream out = new PrintStream(new File("testdata/wiktionary.it.test"));
    dict.print(out);
    out.close();
    
    assertFilesEqual("testdata/wiktionary.it.golden", "testdata/wiktionary.it.test");
  }

  
  public void testGermanCombined() throws Exception {
    final File result = new File("testdata/de-en.quickdic");
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=@testdata/de-en_dictInfo.txt",

        "--input1=testdata/de-en_chemnitz_100",
        "--input1Name=chemnitz",
        "--input1Charset=UTF8",
        "--input1Format=chemnitz",

        "--input2=testdata/de-en_dictcc_100",
        "--input2Name=dictcc",
        "--input2Charset=UTF8",
        "--input2Format=dictcc",

        "--print=testdata/de-en.test",
    });
    
    // Check it once:
    assertFilesEqual("testdata/de-en.golden", "testdata/de-en.test"); 
    
    
    // Check it again.
    final Dictionary dict = new Dictionary(new RandomAccessFile(result.getAbsolutePath(), "r"));
    final PrintStream out = new PrintStream(new File("testdata/de-en.test"));
    dict.print(out);
    out.close();
    
    assertFilesEqual("testdata/de-en.golden", "testdata/de-en.test");
  }



  void assertFilesEqual(final String expected, final String actual) throws IOException {
    final String expectedString = FileUtil.readToString(new File(expected));
    final String actualString = FileUtil.readToString(new File(actual));
    assertEquals(expectedString, actualString);
  }

  
}

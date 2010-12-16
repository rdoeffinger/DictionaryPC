package com.hughes.android.dictionary.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import com.hughes.util.FileUtil;

import junit.framework.TestCase;

public class DictionaryBuilderTest extends TestCase {
  
  public void testWiktionaryCombined() throws Exception {
    final File result = new File("testdata/wiktionary.quickdic");
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=SomeWikiData",

        "--input3=testdata/enwiktionary_small.xml",
        "--input3Name=enwiktionary",
        "--input3Format=enwiktionary",
        "--input3TranslationPattern1=German|Italian|Spanish|French|Japanese|Arabic|Mandarin|Korean|Latin|Swedish|Croation|Serbian|Dutch|Afrikaans",
        "--input3TranslationPattern2=English",
        "--input3EnIndex=2",

        "--print=testdata/wiktionary.test",
    });
    
    // Check it once:
    assertFilesEqual("testdata/wiktionary.golden", "testdata/wiktionary.test"); 
    
    
    // Check it again.
    final Dictionary dict = new Dictionary(new RandomAccessFile(result.getAbsolutePath(), "r"));
    final PrintStream out = new PrintStream(new File("testdata/wiktionary.test"));
    dict.print(out);
    out.close();
    
    assertFilesEqual("testdata/wiktionary.golden", "testdata/wiktionary.test");
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
        "--input1Name=dictcc",
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

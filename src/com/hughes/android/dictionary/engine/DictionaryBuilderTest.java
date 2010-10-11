package com.hughes.android.dictionary.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import com.hughes.util.FileUtil;

import junit.framework.TestCase;

public class DictionaryBuilderTest extends TestCase {
  
  public void testGermanCombined() throws IOException {
    final File result = new File("testdata/de_en.dict");
    System.out.println("Writing to: " + result);
    DictionaryBuilder.main(new String[] {
        "--dictOut=" + result.getAbsolutePath(),
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=@testdata/de_en_dictInfo.txt",

        "--input1=testdata/de-en-chemnitz_100",
        "--input1Name=dictcc",
        "--input1Charset=UTF8",
        "--input1Format=chemnitz",

        "--input2=testdata/de-en-dictcc_100",
        "--input2Name=dictcc",
        "--input2Charset=UTF8",
        "--input2Format=dictcc",
        
        "--print=testdata/de_en.test",
    });
    
    // Check it once:
    assertFilesEqual("testdata/de_en.golden", "testdata/de_en.test"); 
    
    
    // Check it again.
    final Dictionary dict = new Dictionary(new RandomAccessFile(result.getAbsolutePath(), "r"));
    final PrintStream out = new PrintStream(new File("testdata/de_en.test"));
    dict.print(out);
    out.close();
    
    assertFilesEqual("testdata/de_en.golden", "testdata/de_en.test");
  }
  

  void assertFilesEqual(final String expected, final String actual) throws IOException {
    final String expectedString = FileUtil.readToString(new File(expected));
    final String actualString = FileUtil.readToString(new File(actual));
    assertEquals(expectedString, actualString);
  }

}

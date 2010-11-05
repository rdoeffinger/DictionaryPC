package com.hughes.android.dictionary.engine;

import junit.framework.TestCase;

public class DictionaryBuilder_DE extends TestCase {
  
  public static void main(final String[] args) throws Exception {
    
    DictionaryBuilder.main(new String[] {
        "--dictOut=dictOutputs/de-en_chemnitz.quickdic",
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=@dictInputs/de-en_chemnitz.info",

        "--input1=dictInputs/de-en_chemnitz.txt",
        "--input1Name=dictcc",
        "--input1Charset=UTF8",
        "--input1Format=chemnitz",
    });

    DictionaryBuilder.main(new String[] {
        "--dictOut=dictOutputs/de-en_all.quickdic",
        "--lang1=DE",
        "--lang2=EN",
        "--dictInfo=@dictInputs/de-en_all.info",

        "--input1=dictInputs/de-en_chemnitz.txt",
        "--input1Name=dictcc",
        "--input1Charset=UTF8",
        "--input1Format=chemnitz",

        "--input2=dictInputs/de-en_dictcc.txt",
        "--input2Name=dictcc",
        "--input2Charset=UTF8",
        "--input2Format=dictcc",
    });

  }
  
}

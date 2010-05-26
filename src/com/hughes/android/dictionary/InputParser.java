package com.hughes.android.dictionary;

import java.io.File;

public interface InputParser {
  
  void parse(final File file, final Dictionary dest);
  
  class LineParser implements InputParser {
    @Override
    public void parse(File file, Dictionary dest) {
    }
  }

}

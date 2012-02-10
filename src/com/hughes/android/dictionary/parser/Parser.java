package com.hughes.android.dictionary.parser;

import java.io.File;
import java.io.IOException;

import com.hughes.android.dictionary.engine.EntrySource;

public interface Parser {
  
  void parse(final File file, final EntrySource entrySource, final int pageLimit) throws IOException;

}

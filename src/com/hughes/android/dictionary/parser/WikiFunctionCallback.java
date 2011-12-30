package com.hughes.android.dictionary.parser;

import java.util.List;
import java.util.Map;

public interface WikiFunctionCallback {
  
  void onWikiFunction(final String name, final List<String> args, final Map<String,String> namedArgs);

}

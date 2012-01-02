package com.hughes.android.dictionary.parser.enwiktionary;

import java.util.List;
import java.util.Map;

import com.hughes.android.dictionary.parser.WikiTokenizer;

public interface FunctionCallback {
  
  boolean onWikiFunction(
      final WikiTokenizer tokenizer, 
      final String name,
      final List<String> args, 
      final Map<String,String> namedArgs,
      final EnWiktionaryXmlParser parser,
      final AppendAndIndexWikiCallback appendAndIndexWikiCallback);

}

package com.hughes.android.dictionary.parser.wiktionary;

import java.util.Map;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.IndexBuilder;

public class WholeSectionToHtmlParser extends AbstractWiktionaryParser {
  
  final IndexBuilder thisIndexBuilder;
  final IndexBuilder foreignIndexBuilder;
  final Pattern langPattern;
  final Pattern langCodePattern;


  @Override
  void parseSection(String heading, String text) {
    
  }

  @Override
  void removeUselessArgs(Map<String, String> namedArgs) {
  }

}

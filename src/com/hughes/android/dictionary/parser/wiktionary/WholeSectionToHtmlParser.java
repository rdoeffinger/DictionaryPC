package com.hughes.android.dictionary.parser.wiktionary;

import java.util.Map;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.HtmlEntry;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;

public class WholeSectionToHtmlParser extends AbstractWiktionaryParser {
  
  public static final String NAME = "WholeSectionToHtmlParser";
  
  final IndexBuilder titleIndexBuilder;

  
  public  WholeSectionToHtmlParser(final IndexBuilder titleIndexBuilder) {
    this.titleIndexBuilder = titleIndexBuilder;
  }

  @Override
  void parseSection(String heading, String text) {
    HtmlEntry htmlEntry = new HtmlEntry(entrySource, title, text);
    IndexedEntry indexedEntry = new IndexedEntry(htmlEntry);
    indexedEntry.isValid = true;
    titleIndexBuilder.addEntryWithString(indexedEntry, title, EntryTypeName.WIKTIONARY_TITLE_MULTI_DETAIL);
  }

  @Override
  void removeUselessArgs(Map<String, String> namedArgs) {
  }

}

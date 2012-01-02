package com.hughes.android.dictionary.parser.enwiktionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.parser.WikiTokenizer;

final class AppendAndIndexWikiCallback implements WikiTokenizer.Callback {

  final EnWiktionaryXmlParser parser;
  StringBuilder builder;
  IndexedEntry indexedEntry;
  IndexBuilder indexBuilder;
  final Map<String,FunctionCallback> functionCallbacks = new LinkedHashMap<String, FunctionCallback>();
  
  EntryTypeName entryTypeName = null;
  
  public AppendAndIndexWikiCallback(final EnWiktionaryXmlParser parser) {
    this.parser = parser;
  }
  
  public void reset(final StringBuilder builder, final IndexedEntry indexedEntry) {
    this.builder = builder;
    this.indexedEntry = indexedEntry;
    this.indexBuilder = null;
    entryTypeName = null;
  }
  
  public void dispatch(final String wikiText, final IndexBuilder indexBuilder, final EntryTypeName entryTypeName) {
    final IndexBuilder oldIndexBuilder = this.indexBuilder;
    final EntryTypeName oldEntryTypeName = this.entryTypeName;
    this.indexBuilder = indexBuilder;
    this.entryTypeName = entryTypeName;
    WikiTokenizer.dispatch(wikiText, false, this);
    this.indexBuilder = oldIndexBuilder;
    this.entryTypeName = oldEntryTypeName;
  }
  
  public void dispatch(final String wikiText, final EntryTypeName entryTypeName) {
    dispatch(wikiText, this.indexBuilder, entryTypeName);
  }

  
  @Override
  public void onPlainText(final String plainText) {
    // The only non-recursive callback.  Just appends to the builder, and indexes.
    builder.append(plainText);
    if (indexBuilder != null && entryTypeName != null) {
      indexBuilder.addEntryWithString(indexedEntry, plainText, entryTypeName);
    }
  }

  @Override
  public void onWikiLink(WikiTokenizer wikiTokenizer) {
    final String wikiText = wikiTokenizer.wikiLinkText();

    final String linkDest = wikiTokenizer.wikiLinkDest();
    if (linkDest != null) {
      System.out.println("linkDest: " + linkDest);
      // TODO: Check for English before appending.
      // TODO: Could also index under link dest, too.
    }
    dispatch(wikiText, EntryTypeName.WIKTIONARY_TRANSLATION_WIKI_TEXT);
  }

  @Override
  public void onFunction(
      final WikiTokenizer wikiTokenizer,
      final String name,
      final List<String> args, 
      final Map<String, String> namedArgs) {
    
    final FunctionCallback functionCallback = functionCallbacks.get(name);
    if (functionCallback == null || !functionCallback.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, this)) {
      // Default function handling:
      final IndexBuilder oldIndexBuilder = indexBuilder;
      indexBuilder = null;
      builder.append("{{").append(name);
      for (int i = 0; i < args.size(); ++i) {
        builder.append("|");
        WikiTokenizer.dispatch(args.get(i), false, this);
      }
      for (final Map.Entry<String, String> entry : namedArgs.entrySet()) {
        builder.append("|");
        WikiTokenizer.dispatch(entry.getKey(), false, this);
        builder.append("=");
        WikiTokenizer.dispatch(entry.getValue(), false, this);
      }
      builder.append("}}");
      indexBuilder = oldIndexBuilder;
    }
  }

  @Override
  public void onMarkup(WikiTokenizer wikiTokenizer) {
    // Do nothing.
  }

  @Override
  public void onComment(WikiTokenizer wikiTokenizer) {
    // Do nothing.
  }

  @Override
  public void onNewline(WikiTokenizer wikiTokenizer) {
    assert false;
  }

  @Override
  public void onHeading(WikiTokenizer wikiTokenizer) {
    assert false;
  }

  @Override
  public void onListItem(WikiTokenizer wikiTokenizer) {
    assert false;
  }

}
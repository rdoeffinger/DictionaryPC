package com.hughes.android.dictionary.parser.enwiktionary;

import java.util.List;
import java.util.Map;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.parser.WikiTokenizer;

final class AppendAndIndexWikiCallback implements WikiTokenizer.Callback {

  final EnWiktionaryXmlParser parser;
  final StringBuilder builder;
  final IndexedEntry indexedEntry;
  IndexBuilder defaultIndexBuilder;
  final Map<String,FunctionCallback> functionCallbacks;
  
  // TODO: the classes of text are wrong....
  
  public AppendAndIndexWikiCallback(
      final EnWiktionaryXmlParser parser,
      final String title,
      final StringBuilder builder, 
      final IndexedEntry indexedEntry,
      final IndexBuilder defaultIndexBuilder,
      final Map<String, FunctionCallback> functionCallbacks) {
    this.parser = parser;
    this.indexedEntry = indexedEntry;
    this.defaultIndexBuilder = defaultIndexBuilder;
    this.builder = builder;
    this.functionCallbacks = functionCallbacks;
  }

  @Override
  public void onPlainText(WikiTokenizer wikiTokenizer) {
    // The only non-recursive callback.  Just appends to the builder, and indexes.
    final String plainText = wikiTokenizer.token(); 
    builder.append(plainText);
    defaultIndexBuilder.addEntryWithString(indexedEntry, plainText, EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
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
    // TODO: save, set, restore text type...
    new WikiTokenizer(wikiText, false).dispatch(this);
  }

  @Override
  public void onFunction(
      final String name,
      final List<String> args, 
      final Map<String, String> namedArgs) {
    
    final FunctionCallback functionCallback = functionCallbacks.get(name);
    if (functionCallback == null || !functionCallback.onWikiFunction(name, args, namedArgs, parser, title)) {
      // Default function handling:
      builder.append("{{").append(name);
      for (int i = 0; i < args.size(); ++i) {
        builder.append("|");
        new WikiTokenizer(args.get(i), false).dispatch(this);
      }
      for (final Map.Entry<String, String> entry : namedArgs.entrySet()) {
        builder.append("|");
        new WikiTokenizer(entry.getKey(), false).dispatch(this);
        builder.append("=");
        new WikiTokenizer(entry.getValue(), false).dispatch(this);
      }
      builder.append("}}");
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
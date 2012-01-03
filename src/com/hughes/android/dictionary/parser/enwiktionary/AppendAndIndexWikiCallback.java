package com.hughes.android.dictionary.parser.enwiktionary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.util.EnumUtil;

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
    this.entryTypeName = EnumUtil.min(entryTypeName, this.entryTypeName);
    if (entryTypeName == null) this.entryTypeName = null;
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
    if (indexBuilder != null && entryTypeName != null && indexedEntry != null) {
      indexBuilder.addEntryWithString(indexedEntry, plainText, entryTypeName);
    }
  }

  @Override
  public void onWikiLink(WikiTokenizer wikiTokenizer) {
    final String text = wikiTokenizer.wikiLinkText();
    final String link = wikiTokenizer.wikiLinkDest();
    if (link != null) {
      if (link.contains("#English")) {
        dispatch(text, parser.enIndexBuilder, EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK);
      } else if (link.contains("#") && parser.langPattern.matcher(link).find()) {
        dispatch(text, parser.foreignIndexBuilder, EntryTypeName.WIKTIONARY_ENGLISH_DEF_OTHER_LANG);
      } else if (link.equals("plural")) {
        builder.append(text);
      } else {
        //LOG.warning("Special link: " + englishTokenizer.token());
        dispatch(text, EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK);
      }
    } else {
      // link == null
      final EntryTypeName entryTypeName;
      switch (parser.state) {
      case TRANSLATION_LINE:
        entryTypeName = EntryTypeName.WIKTIONARY_TRANSLATION_WIKI_TEXT;
        break;
      case ENGLISH_DEF_OF_FOREIGN:
        entryTypeName = EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK;
        break;
        default:
          throw new IllegalStateException("Invalid enum value: " + parser.state);
      }
      dispatch(text, entryTypeName);
    }
  }

  @Override
  public void onFunction(
      final WikiTokenizer wikiTokenizer,
      final String name,
      final List<String> args, 
      final Map<String, String> namedArgs) {
    
    FunctionCallback functionCallback = functionCallbacks.get(name);
    if (functionCallback == null) {
      if (
          name.equals("form of") || // TODO: switch to contains
          name.contains("conjugation of") || 
          name.contains("participle of") || 
          name.contains("gerund of") || 
          name.contains("feminine of") || 
          name.contains("plural of")) {
        functionCallback = functionCallbacks.get("form of");
      }
    }
    if (functionCallback == null || !functionCallback.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, this)) {
      // Default function handling:
      namedArgs.keySet().removeAll(EnWiktionaryXmlParser.USELESS_WIKI_ARGS);
      final boolean single = args.isEmpty() && namedArgs.isEmpty();
      builder.append(single ? "{" : "{{");

      final IndexBuilder oldIndexBuilder = indexBuilder;
      indexBuilder = null;
      FunctionCallbacksDefault.NAME_AND_ARGS.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, this);
      indexBuilder = oldIndexBuilder;

      builder.append(single ? "}" : "}}");
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
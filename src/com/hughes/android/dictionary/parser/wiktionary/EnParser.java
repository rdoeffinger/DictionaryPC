// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hughes.android.dictionary.parser.wiktionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.parser.WikiTokenizer;

public abstract class EnParser extends AbstractWiktionaryParser {

  // TODO: process {{ttbc}} lines
  
  public static final Pattern partOfSpeechHeader = Pattern.compile(
      "Noun|Verb|Adjective|Adverb|Pronoun|Conjunction|Interjection|" +
      "Preposition|Proper noun|Article|Prepositional phrase|Acronym|" +
      "Abbreviation|Initialism|Contraction|Prefix|Suffix|Symbol|Letter|" +
      "Ligature|Idiom|Phrase|\\{\\{acronym\\}\\}|\\{\\{initialism\\}\\}|" +
      "\\{\\{abbreviation\\}\\}|" +
      // These are @deprecated:
      "Noun form|Verb form|Adjective form|Nominal phrase|Noun phrase|" +
      "Verb phrase|Transitive verb|Intransitive verb|Reflexive verb|" +
      // These are extras I found:
      "Determiner|Numeral|Number|Cardinal number|Ordinal number|Proverb|" +
      "Particle|Interjection|Pronominal adverb" +
      "Han character|Hanzi|Hanja|Kanji|Katakana character|Syllable");
  
  static final Set<String> USELESS_WIKI_ARGS = new LinkedHashSet<String>(
      Arrays.asList(
          "lang",
          "sc",
          "sort",
          "cat",
          "cat2",
          "xs",
          "nodot"));

  static boolean isIgnorableTitle(final String title) {
    return title.startsWith("Wiktionary:") ||
        title.startsWith("Template:") ||
        title.startsWith("Appendix:") ||
        title.startsWith("Category:") ||
        title.startsWith("Index:") ||
        title.startsWith("MediaWiki:") ||
        title.startsWith("TransWiki:") ||
        title.startsWith("Citations:") ||
        title.startsWith("Concordance:") ||
        title.startsWith("Help:");
  }
  
  final IndexBuilder enIndexBuilder;
  final IndexBuilder foreignIndexBuilder;
  final Pattern langPattern;
  final Pattern langCodePattern;
  final boolean swap;
  
  // State used while parsing.
  enum State {
    TRANSLATION_LINE,
    ENGLISH_DEF_OF_FOREIGN,
    ENGLISH_EXAMPLE,
    FOREIGN_EXAMPLE,
  }
  State state = null;

  public boolean entryIsFormOfSomething = false;
  final Collection<String> wordForms = new ArrayList<String>();
  boolean titleAppended = false;


  final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback = new AppendAndIndexCallback(this);
  {
    appendAndIndexWikiCallback.functionCallbacks.putAll(EnFunctionCallbacks.DEFAULT);
    for (final String key : new ArrayList<String>(appendAndIndexWikiCallback.functionCallbacks.keySet())) {
        // Don't handle the it-conj functions here.
        if (key.startsWith("it-conj")) {
            appendAndIndexWikiCallback.functionCallbacks.remove(key);
        }
    }
  }
  
  EnParser(final IndexBuilder enIndexBuilder, final IndexBuilder otherIndexBuilder, final Pattern langPattern, final Pattern langCodePattern, final boolean swap) {
    this.enIndexBuilder = enIndexBuilder;
    this.foreignIndexBuilder = otherIndexBuilder;
    this.langPattern = langPattern;
    this.langCodePattern = langCodePattern;
    this.swap = swap;
  }

  @Override
  void removeUselessArgs(Map<String, String> namedArgs) {
    namedArgs.keySet().removeAll(USELESS_WIKI_ARGS);
  }
  
  static class AppendAndIndexCallback extends AppendAndIndexWikiCallback<EnParser> {

    public AppendAndIndexCallback(EnParser parser) {
      super(parser);
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
    
  }

}

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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.AppendAndIndexWikiCallback;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.NameAndArgs;
import com.hughes.util.ListUtil;

class EnFunctionCallbacks {
  
  static final Map<String,FunctionCallback<EnParser>> DEFAULT = new LinkedHashMap<String, FunctionCallback<EnParser>>();

  static final Map<String,FunctionCallback<AbstractWiktionaryParser>> DEFAULT_GENERIC = new LinkedHashMap<String, FunctionCallback<AbstractWiktionaryParser>>();
  static {
      FunctionCallback<AbstractWiktionaryParser> callback = new TranslationCallback<AbstractWiktionaryParser>();
      DEFAULT_GENERIC.put("t", callback);
  }
  
  static <T extends AbstractWiktionaryParser> void addGenericCallbacks(Map<String, FunctionCallback<T>> callbacks) {
      FunctionCallback<T> callback = new Gender<T>();
      callbacks.put("m", callback);
      callbacks.put("f", callback);
      callbacks.put("n", callback);
      callbacks.put("p", callback);
      callbacks.put("g", callback);
      
      callback = new EncodingCallback<T>();
      Set<String> encodings = new LinkedHashSet<String>(Arrays.asList(
          "zh-ts", "zh-tsp",
          "sd-Arab", "ku-Arab", "Arab", "unicode", "Laoo", "ur-Arab", "Thai", 
          "fa-Arab", "Khmr", "Cyrl", "IPAchar", "ug-Arab", "ko-inline", 
          "Jpan", "Kore", "Hebr", "rfscript", "Beng", "Mong", "Knda", "Cyrs",
          "yue-tsj", "Mlym", "Tfng", "Grek", "yue-yue-j"));
      for (final String encoding : encodings) {
          callbacks.put(encoding, callback);
      }
      
      callback = new Ignore<T>();
      callbacks.put("trreq", callback);
      callbacks.put("t-image", callback);
      callbacks.put("defn", callback);
      callbacks.put("rfdef", callback);
      callbacks.put("rfdate", callback);
      callbacks.put("rfex", callback);
      callbacks.put("rfquote", callback);
      callbacks.put("attention", callback);
      callbacks.put("zh-attention", callback);
      
      callback = new AppendName<T>();
      callbacks.put("...", callback);
      
      callbacks.put("qualifier", new QualifierCallback<T>());
      callbacks.put("italbrac", new italbrac<T>());
      callbacks.put("gloss", new gloss<T>());
      callbacks.put("not used", new not_used<T>());
      callbacks.put("wikipedia", new wikipedia<T>());
  }

  static {
    addGenericCallbacks(DEFAULT);
      
    FunctionCallback<EnParser> callback = new TranslationCallback<EnParser>();
    DEFAULT.put("t", callback);
    DEFAULT.put("t+", callback);
    DEFAULT.put("t-", callback);
    DEFAULT.put("tø", callback);
    DEFAULT.put("apdx-t", callback);
    
    callback = new l_term();
    DEFAULT.put("l", callback);
    DEFAULT.put("term", callback);

    //callback = new AppendArg0();

    callback = new FormOf();
    DEFAULT.put("form of", callback);
    DEFAULT.put("conjugation of", callback);
    DEFAULT.put("participle of", callback);
    DEFAULT.put("present participle of", callback);
    DEFAULT.put("past participle of", callback);
    DEFAULT.put("feminine past participle of", callback);
    DEFAULT.put("gerund of", callback);
    DEFAULT.put("feminine of", callback);
    DEFAULT.put("plural of", callback);
    DEFAULT.put("feminine plural of", callback);
    DEFAULT.put("inflected form of", callback);
    DEFAULT.put("alternative form of", callback);
    DEFAULT.put("dated form of", callback);
    DEFAULT.put("apocopic form of", callback);
    
    callback = new InflOrHead();
    DEFAULT.put("infl", callback);
    DEFAULT.put("head", callback);
  }
  
  static final NameAndArgs<EnParser> NAME_AND_ARGS = new NameAndArgs<EnParser>();

  // ------------------------------------------------------------------

  static final class TranslationCallback<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs, final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {

      final String transliteration = namedArgs.remove("tr");
      final String alt = namedArgs.remove("alt");
      namedArgs.keySet().removeAll(EnParser.USELESS_WIKI_ARGS);
      if (args.size() < 2) {
        if (!name.equals("ttbc")) {
          EnParser.LOG.warning("{{t...}} with wrong args: title=" + parser.title + ", " + wikiTokenizer.token());
        }
        return false;
      }
      final String langCode = ListUtil.get(args, 0);
      if (!appendAndIndexWikiCallback.langCodeToTCount.containsKey(langCode)) {
        appendAndIndexWikiCallback.langCodeToTCount.put(langCode, new AtomicInteger());
      }
      appendAndIndexWikiCallback.langCodeToTCount.get(langCode).incrementAndGet();
      final String word = ListUtil.get(args, 1);
      appendAndIndexWikiCallback.dispatch(alt != null ? alt : word, EntryTypeName.WIKTIONARY_TITLE_MULTI);

      // Genders...
      if (args.size() > 2) {
        appendAndIndexWikiCallback.builder.append(" {");
        for (int i = 2; i < args.size(); ++i) {
          if (i > 2) {
            appendAndIndexWikiCallback.builder.append("|");
          }
          appendAndIndexWikiCallback.builder.append(args.get(i));
        }
        appendAndIndexWikiCallback.builder.append("}");
      }

      if (transliteration != null) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(transliteration, EntryTypeName.WIKTIONARY_TRANSLITERATION);
        appendAndIndexWikiCallback.builder.append(")");
      }
      
      if (alt != null) {
        // If alt wasn't null, we appended alt instead of the actual word
        // we're filing under..
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(word, EntryTypeName.WIKTIONARY_TITLE_MULTI);
        appendAndIndexWikiCallback.builder.append(")");
      }

      // Catch-all for anything else...
      if (!namedArgs.isEmpty()) {
        appendAndIndexWikiCallback.builder.append(" {");
        EnParser.appendNamedArgs(namedArgs, appendAndIndexWikiCallback);
        appendAndIndexWikiCallback.builder.append("}");
      }
      
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class QualifierCallback<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        EnParser.LOG.warning("weird qualifier: ");
        return false;
      }
      String qualifier = args.get(0);
      appendAndIndexWikiCallback.builder.append("(");
      appendAndIndexWikiCallback.dispatch(qualifier, null);
      appendAndIndexWikiCallback.builder.append(")");
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class EncodingCallback<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      if (!namedArgs.isEmpty()) {
        EnParser.LOG.warning("weird encoding: " + wikiTokenizer.token());
      }
      if (args.size() == 0) {
        // Things like "{{Jpan}}" exist.
        return true;
      }
      
      for (int i = 0; i < args.size(); ++i) {
        if (i > 0) {
          appendAndIndexWikiCallback.builder.append(", ");
        }
        final String arg = args.get(i);
//        if (arg.equals(parser.title)) {
//          parser.titleAppended = true;
//        }
        appendAndIndexWikiCallback.dispatch(arg, appendAndIndexWikiCallback.entryTypeName);
      }
      
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class Gender<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      if (!namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append("{");
      appendAndIndexWikiCallback.builder.append(name);
      for (int i = 0; i < args.size(); ++i) {
        appendAndIndexWikiCallback.builder.append("|").append(args.get(i));
      }
      appendAndIndexWikiCallback.builder.append("}");
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class l_term implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      
      // for {{l}}, lang is arg 0, but not for {{term}}
      if (name.equals("term")) {
        args.add(0, "");
      }
      
      final EntryTypeName entryTypeName;
      switch (parser.state) {
      case TRANSLATION_LINE: entryTypeName = EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT; break;
      case ENGLISH_DEF_OF_FOREIGN: entryTypeName = EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK; break;
      default: throw new IllegalStateException("Invalid enum value: " + parser.state);
      }
      
      final String langCode = args.get(0);
      final IndexBuilder indexBuilder;
      if ("".equals(langCode)) {
        indexBuilder = parser.foreignIndexBuilder;
      } else if ("en".equals(langCode)) {
        indexBuilder = parser.enIndexBuilder;
      } else {
        indexBuilder = parser.foreignIndexBuilder;
      }
      
      String displayText = ListUtil.get(args, 2, "");
      if (displayText.equals("")) {
        displayText = ListUtil.get(args, 1, null);
      }
      
      if (displayText != null) {
        appendAndIndexWikiCallback.dispatch(displayText, indexBuilder, entryTypeName);
      } else {
        EnParser.LOG.warning("no display text: " + wikiTokenizer.token());
      }
      
      final String tr = namedArgs.remove("tr");
      if (tr != null) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(tr, indexBuilder, EntryTypeName.WIKTIONARY_TRANSLITERATION);
        appendAndIndexWikiCallback.builder.append(")");
      }
      
      final String gloss = ListUtil.get(args, 3, "");
      if (!gloss.equals("")) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(gloss, parser.enIndexBuilder, EntryTypeName.WIKTIONARY_ENGLISH_DEF);
        appendAndIndexWikiCallback.builder.append(")");
      }
      
      namedArgs.keySet().removeAll(EnParser.USELESS_WIKI_ARGS);
      if (!namedArgs.isEmpty()) {
        appendAndIndexWikiCallback.builder.append(" {").append(name);
        EnParser.appendNamedArgs(namedArgs, appendAndIndexWikiCallback);
        appendAndIndexWikiCallback.builder.append("}");
      }

      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class AppendArg0<T extends AbstractWiktionaryParser> implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);

      final String tr = namedArgs.remove("tr");
      if (tr != null) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(tr, EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
        appendAndIndexWikiCallback.builder.append(")");
        parser.wordForms.add(tr);
      }

      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class italbrac<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append("(");
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      appendAndIndexWikiCallback.builder.append(")");
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class gloss<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append("(");
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      appendAndIndexWikiCallback.builder.append(")");
      return true;
    }
  }
  
  // ------------------------------------------------------------------
  
  static final class Ignore<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class not_used<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      appendAndIndexWikiCallback.builder.append("(not used)");
      return true;
    }
  }


  // ------------------------------------------------------------------
  
  static final class AppendName<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      if (!args.isEmpty() || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append(name);
      return true;
    }
  }

  // --------------------------------------------------------------------
  // --------------------------------------------------------------------
  

  static final class FormOf implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      parser.entryIsFormOfSomething = true;
      String formName = name;
      if (name.equals("form of")) {
        formName = ListUtil.remove(args, 0, null);
      }
      if (formName == null) {
        EnParser.LOG.warning("Missing form name: " + parser.title);
        formName = "form of";
      }
      String baseForm = ListUtil.get(args, 1, "");
      if ("".equals(baseForm)) {
        baseForm = ListUtil.get(args, 0, null);
        ListUtil.remove(args, 1, "");
      } else {
        ListUtil.remove(args, 0, null);
      }
      namedArgs.keySet().removeAll(EnParser.USELESS_WIKI_ARGS);
      
      appendAndIndexWikiCallback.builder.append("{");
      NAME_AND_ARGS.onWikiFunction(wikiTokenizer, formName, args, namedArgs, parser, appendAndIndexWikiCallback);
      appendAndIndexWikiCallback.builder.append("}");
      if (baseForm != null && appendAndIndexWikiCallback.indexedEntry != null) {
        parser.foreignIndexBuilder.addEntryWithString(appendAndIndexWikiCallback.indexedEntry, baseForm, EntryTypeName.WIKTIONARY_BASE_FORM_MULTI);
      } else {
        // null baseForm happens in Danish.
        EnParser.LOG.warning("Null baseform: " + parser.title);
      }
      return true;
    }
  }
  
  static final EnFunctionCallbacks.FormOf FORM_OF = new FormOf();
  

  // --------------------------------------------------------------------
  // --------------------------------------------------------------------
  
  static final class wikipedia<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      namedArgs.remove("lang");
      if (args.size() > 1 || !namedArgs.isEmpty()) {
        // Unindexed!
        return false;
      } else if (args.size() == 1) {
        return false;
      } else {
        return true;
      }
    }
  }

  static final class InflOrHead implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      // See: http://en.wiktionary.org/wiki/Template:infl
      // TODO: Actually these functions should start a new WordPOS:
      // See: http://en.wiktionary.org/wiki/quattro
      final String langCode = ListUtil.get(args, 0);
      String head = namedArgs.remove("head");
      if (head == null) {
        head = namedArgs.remove("title"); // Bug
      }
      if (head == null) {
        head = parser.title;
      }
      
      namedArgs.keySet().removeAll(EnParser.USELESS_WIKI_ARGS);

      final String tr = namedArgs.remove("tr");
      String g = namedArgs.remove("g");
      if (g == null) {
        g = namedArgs.remove("gender");
      }
      final String g2 = namedArgs.remove("g2");
      final String g3 = namedArgs.remove("g3");

      // We might have already taken care of this in a generic way...
      if (!parser.titleAppended) {
        appendAndIndexWikiCallback.dispatch(head, EntryTypeName.WIKTIONARY_TITLE_MULTI);
        parser.titleAppended = true;
      }

      if (g != null) {
        appendAndIndexWikiCallback.builder.append(" {").append(g);
        if (g2 != null) {
          appendAndIndexWikiCallback.builder.append("|").append(g2);
        }
        if (g3 != null) {
          appendAndIndexWikiCallback.builder.append("|").append(g3);
        }
        appendAndIndexWikiCallback.builder.append("}");
      }

      if (tr != null) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(tr, EntryTypeName.WIKTIONARY_TITLE_MULTI);
        appendAndIndexWikiCallback.builder.append(")");
        parser.wordForms.add(tr);
      }

      final String pos = ListUtil.get(args, 1);
      if (pos != null) {
        appendAndIndexWikiCallback.builder.append(" (").append(pos).append(")");
      }
      for (int i = 2; i < args.size(); i += 2) {
        final String inflName = ListUtil.get(args, i);
        final String inflValue = ListUtil.get(args, i + 1);
        appendAndIndexWikiCallback.builder.append(", ");
        appendAndIndexWikiCallback.dispatch(inflName, null, null);
        if (inflValue != null && inflValue.length() > 0) {
          appendAndIndexWikiCallback.builder.append(": ");
          appendAndIndexWikiCallback.dispatch(inflValue, null, null);
          parser.wordForms.add(inflValue);
        }
      }
      for (final String key : namedArgs.keySet()) {
        final String value = WikiTokenizer.toPlainText(namedArgs.get(key));
        appendAndIndexWikiCallback.builder.append(" ");
        appendAndIndexWikiCallback.dispatch(key, null, null);
        appendAndIndexWikiCallback.builder.append("=");
        appendAndIndexWikiCallback.dispatch(value, null, null);
        parser.wordForms.add(value);
      }
      return true;
    }
  }
  

  static {
    DEFAULT.put("it-noun", new it_noun());
  } 
  static final class it_noun implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      parser.titleAppended = true;
      final String base = ListUtil.get(args, 0);
      final String gender = ListUtil.get(args, 1);
      final String singular = base + ListUtil.get(args, 2, null);
      final String plural = base + ListUtil.get(args, 3, null);
      appendAndIndexWikiCallback.builder.append(" ");
      appendAndIndexWikiCallback.dispatch(singular, null, null);
      appendAndIndexWikiCallback.builder.append(" {").append(gender).append("}, ");
      appendAndIndexWikiCallback.dispatch(plural, null, null);
      appendAndIndexWikiCallback.builder.append(" {pl}");
      parser.wordForms.add(singular);
      parser.wordForms.add(plural);
      if (!namedArgs.isEmpty() || args.size() > 4) {
        EnParser.LOG.warning("Invalid it-noun: " + wikiTokenizer.token());
      }
      return true;
    }
  }

  static {
    DEFAULT.put("it-proper noun", new it_proper_noun());
  } 
  static final class it_proper_noun implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      return false;
    }
  }
  
  // -----------------------------------------------------------------------
  // Italian stuff
  // -----------------------------------------------------------------------

  }
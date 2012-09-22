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

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.AppendAndIndexWikiCallback;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.NameAndArgs;
import com.hughes.util.ListUtil;
import com.hughes.util.MapUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class EnFunctionCallbacks {
  
  static final Map<String,FunctionCallback<EnParser>> DEFAULT = new LinkedHashMap<String, FunctionCallback<EnParser>>();

  static <T extends AbstractWiktionaryParser> void addGenericCallbacks(Map<String, FunctionCallback<T>> callbacks) {
      FunctionCallback<T> callback = new Gender<T>();
      callbacks.put("m", callback);
      callbacks.put("f", callback);
      callbacks.put("n", callback);
      callbacks.put("p", callback);
      callbacks.put("g", callback);
      
      callback = new EncodingCallback<T>();
      Set<String> encodings = new LinkedHashSet<String>(Arrays.asList(
          "IPA", "IPAchar",  // Not really encodings, but it works.
          "zh-ts", "zh-tsp",
          "sd-Arab", "ku-Arab", "Arab", "unicode", "Laoo", "ur-Arab", "Thai", 
          "fa-Arab", "Khmr", "Cyrl", "ug-Arab", "ko-inline", 
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
      
      final it_conj<T> it_conj_cb = new it_conj<T>();
      callbacks.put("it-conj", it_conj_cb);
      callbacks.put("it-conj-are", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-care", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-iare", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-ciare", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-ere", new it_conj_ere<T>(it_conj_cb));
      callbacks.put("it-conj-ire", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-ire-b", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-urre", new it_conj_urre<T>(it_conj_cb));
      
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
        return false;
      }
      if (args.size() == 0) {
        // Things like "{{Jpan}}" exist.
        return true;
      }
      
      if (name.equals("IPA")) {
          appendAndIndexWikiCallback.dispatch("IPA: ", null);
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
    DEFAULT.put("it-proper noun", new it_proper_noun<EnParser>());
  } 
  static final class it_proper_noun<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final T parser,
        final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
      return false;
    }
  }
  
  // -----------------------------------------------------------------------
  // Italian stuff
  // -----------------------------------------------------------------------
  
  static void passThroughOrFillIn(final Map<String,String> namedArgs, final String key, final String fillIn, final boolean quoteToEmpty) {
      final String value = namedArgs.get(key);
      if (quoteToEmpty && "''".equals(value)) {
          namedArgs.put(key, "");
          return;
      }
      if (value == null || value.equals("")) {
          namedArgs.put(key, fillIn);
      }
  }
  
  static final List<String> it_number_s_p = Arrays.asList("s", "p");
  static final List<String> it_person_1_2_3 = Arrays.asList("1", "2", "3");
  
  static void it_conj_passMood(final Map<String,String> namedArgs, final String moodName, final boolean quoteToEmpty, final String root, final List<String> suffixes) {
      assert suffixes.size() == 6;
      int i = 0;
      for (final String number : it_number_s_p) {
          for (final String person : it_person_1_2_3) {
              passThroughOrFillIn(namedArgs, String.format("%s%s%s", moodName, person, number), root + suffixes.get(i), quoteToEmpty);
              ++i;
          }
      }
  }

  static final class it_conj_are<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    final it_conj<T> dest;
    it_conj_are(it_conj<T> dest) {
      this.dest = dest;
    }
    @Override
      public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
          final Map<String, String> namedArgs,
          final T parser,
          final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
        final String h = name.equals("it-conj-care") ? "h" : "";
        final String i = name.equals("it-conj-ciare") ? "i" : "";
        final String i2 = name.equals("it-conj-iare") ? "" : "i";
        final String root = args.get(0);
        passThroughOrFillIn(namedArgs, "inf", root + i + "are", false);
        namedArgs.put("aux", args.get(1));
        passThroughOrFillIn(namedArgs, "ger", root + i + "ando", true);
        passThroughOrFillIn(namedArgs, "presp", root + i + "ante", true);
        passThroughOrFillIn(namedArgs, "pastp", root + i + "ato", true);
        it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList(i + "o", h + i2, i + "a", h + i2 + "amo", i + "ate", i + "ano"));
        it_conj_passMood(namedArgs, "imperf", false, root, Arrays.asList(i + "avo", i + "avi", i + "ava", i + "avamo", i + "avate", i + "avano"));
        it_conj_passMood(namedArgs, "prem", false, root, Arrays.asList(i + "ai", i + "asti", i + "ò", i + "ammo", i + "aste", i + "arono"));
        it_conj_passMood(namedArgs, "fut", true, root, Arrays.asList(h + "erò", h + "erai", h + "erà", h + "eremo", h + "erete", h + "eranno"));
        it_conj_passMood(namedArgs, "cond", true, root, Arrays.asList(h + "erei", h + "eresti", h + "erebbe", h + "eremmo", h + "ereste", h + "erebbero"));
        
        passThroughOrFillIn(namedArgs, "sub123s", root + h + i2, false);
        passThroughOrFillIn(namedArgs, "sub1p", root + h + i2 + "amo", false);
        passThroughOrFillIn(namedArgs, "sub2p", root + h + i2 + "ate", false);
        passThroughOrFillIn(namedArgs, "sub3p", root + h + i2 + "no", false);

        passThroughOrFillIn(namedArgs, "impsub12s", root + i + "assi", false);
        passThroughOrFillIn(namedArgs, "impsub3s", root + i + "asse", false);
        passThroughOrFillIn(namedArgs, "impsub1p", root + i + "assimo", false);
        passThroughOrFillIn(namedArgs, "impsub2p", root + i + "aste", false);
        passThroughOrFillIn(namedArgs, "impsub3p", root + i + "assero", false);

        passThroughOrFillIn(namedArgs, "imp2s", root + i + "a", true);
        passThroughOrFillIn(namedArgs, "imp3s", root + h + i2, true);
        passThroughOrFillIn(namedArgs, "imp1p", root + h + i2 + "amo", true);
        passThroughOrFillIn(namedArgs, "imp2p", root + i + "ate", true);
        passThroughOrFillIn(namedArgs, "imp3p", root + h + i2 + "no", true);

        return dest.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, appendAndIndexWikiCallback);
      }
    }
  
  static final class it_conj_ere<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
      final it_conj<T> dest;
      it_conj_ere(it_conj<T> dest) {
        this.dest = dest;
      }
      @Override
        public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
            final Map<String, String> namedArgs,
            final T parser,
            final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
          final String root = args.get(0);
          passThroughOrFillIn(namedArgs, "inf", root + "ere", false);
          namedArgs.put("aux", args.get(1));
          passThroughOrFillIn(namedArgs, "ger", root + "endo", true);
          passThroughOrFillIn(namedArgs, "presp", root + "ente", true);
          passThroughOrFillIn(namedArgs, "pastp", root + "uto", true);
          it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList("o", "i", "e", "iamo", "ete", "ono"));
          it_conj_passMood(namedArgs, "imperf", false, root, Arrays.asList("evo", "evi", "eva", "evamo", "evate", "evano"));
          it_conj_passMood(namedArgs, "prem", false, root, Arrays.asList("ei", "esti", "ette", "emmo", "este", "ettero"));
          // Regular past historic synonyms:
          passThroughOrFillIn(namedArgs, "prem3s2", root + "é", true);
          passThroughOrFillIn(namedArgs, "prem3p2", root + "erono", true);
          it_conj_passMood(namedArgs, "fut", true, root, Arrays.asList("erò", "erai", "erà", "eremo", "erete", "eranno"));
          it_conj_passMood(namedArgs, "cond", true, root, Arrays.asList("erei", "eresti", "erebbe", "eremmo", "ereste", "erebbero"));

          passThroughOrFillIn(namedArgs, "sub123s", root + "a", false);
          passThroughOrFillIn(namedArgs, "sub1p", root + "iamo", false);
          passThroughOrFillIn(namedArgs, "sub2p", root + "iate", false);
          passThroughOrFillIn(namedArgs, "sub3p", root + "ano", false);

          passThroughOrFillIn(namedArgs, "impsub12s", root + "essi", false);
          passThroughOrFillIn(namedArgs, "impsub3s", root + "esse", false);
          passThroughOrFillIn(namedArgs, "impsub1p", root + "essimo", false);
          passThroughOrFillIn(namedArgs, "impsub2p", root + "este", false);
          passThroughOrFillIn(namedArgs, "impsub3p", root + "essero", false);

          passThroughOrFillIn(namedArgs, "imp2s", root + "i", true);
          passThroughOrFillIn(namedArgs, "imp3s", root + "a", true);
          passThroughOrFillIn(namedArgs, "imp1p", root + "iamo", true);
          passThroughOrFillIn(namedArgs, "imp2p", root + "ete", true);
          passThroughOrFillIn(namedArgs, "imp3p", root + "ano", true);

          return dest.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, appendAndIndexWikiCallback);
        }
      }

  static final class it_conj_ire<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
      final it_conj<T> dest;
      it_conj_ire(it_conj<T> dest) {
        this.dest = dest;
      }
      @Override
        public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
            final Map<String, String> namedArgs,
            final T parser,
            final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
          final String root = args.get(0);
          passThroughOrFillIn(namedArgs, "inf", root + "ire", false);
          namedArgs.put("aux", args.get(1));
          passThroughOrFillIn(namedArgs, "ger", root + "endo", true);
          passThroughOrFillIn(namedArgs, "presp", root + "ente", true);
          passThroughOrFillIn(namedArgs, "pastp", root + "ito", true);
          if (name.equals("it-conj-ire")) {
              it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList("o", "i", "e", "iamo", "ite", "ono"));
          } else if (name.equals("it-conj-ire-b")) {
              it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList("isco", "isci", "isce", "iamo", "ite", "iscono"));
          } else {
              assert false;
          }
          it_conj_passMood(namedArgs, "imperf", false, root, Arrays.asList("ivo", "ivi", "iva", "ivamo", "ivate", "ivano"));
          it_conj_passMood(namedArgs, "prem", false, root, Arrays.asList("ii", "isti", "ì", "immo", "iste", "irono"));
          // Regular past historic synonyms:
          passThroughOrFillIn(namedArgs, "prem3s2", root + "é", true);
          passThroughOrFillIn(namedArgs, "prem3p2", root + "erono", true);
          it_conj_passMood(namedArgs, "fut", true, root, Arrays.asList("irò", "irai", "irà", "iremo", "irete", "iranno"));
          it_conj_passMood(namedArgs, "cond", true, root, Arrays.asList("irei", "iresti", "irebbe", "iremmo", "ireste", "irebbero"));

          if (name.equals("it-conj-ire")) {
              passThroughOrFillIn(namedArgs, "sub123s", root + "a", false);
              passThroughOrFillIn(namedArgs, "sub3p", root + "ano", false);
          } else if (name.equals("it-conj-ire-b")) {
              passThroughOrFillIn(namedArgs, "sub123s", root + "isca", false);
              passThroughOrFillIn(namedArgs, "sub3p", root + "iscano", false);
          } else {
              assert false;
          }
          passThroughOrFillIn(namedArgs, "sub1p", root + "iamo", false);
          passThroughOrFillIn(namedArgs, "sub2p", root + "iate", false);

          passThroughOrFillIn(namedArgs, "impsub12s", root + "issi", false);
          passThroughOrFillIn(namedArgs, "impsub3s", root + "isse", false);
          passThroughOrFillIn(namedArgs, "impsub1p", root + "issimo", false);
          passThroughOrFillIn(namedArgs, "impsub2p", root + "iste", false);
          passThroughOrFillIn(namedArgs, "impsub3p", root + "issero", false);

          if (name.equals("it-conj-ire")) {
              passThroughOrFillIn(namedArgs, "imp2s", root + "i", true);
              passThroughOrFillIn(namedArgs, "imp3s", root + "a", true);
              passThroughOrFillIn(namedArgs, "imp3p", root + "ano", true);
          } else if (name.equals("it-conj-ire-b")) {
              passThroughOrFillIn(namedArgs, "imp2s", root + "isci", true);
              passThroughOrFillIn(namedArgs, "imp3s", root + "isca", true);
              passThroughOrFillIn(namedArgs, "imp3p", root + "iscano", true);
          } else {
              assert false;
          }
          passThroughOrFillIn(namedArgs, "imp1p", root + "iamo", true);
          passThroughOrFillIn(namedArgs, "imp2p", root + "ite", true);

          return dest.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, appendAndIndexWikiCallback);
        }
      }

  
  static final class it_conj_urre<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
      final it_conj<T> dest;
      it_conj_urre(it_conj<T> dest) {
        this.dest = dest;
      }
      @Override
        public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
            final Map<String, String> namedArgs,
            final T parser,
            final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
          final String root = args.get(0);
          passThroughOrFillIn(namedArgs, "inf", root + "urre", false);
          namedArgs.put("aux", args.get(1));
          passThroughOrFillIn(namedArgs, "ger", root + "ucendo", true);
          passThroughOrFillIn(namedArgs, "presp", root + "ucente", true);
          passThroughOrFillIn(namedArgs, "pastp", root + "otto", true);
          it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList("uco", "uci", "uce", "uciamo", "ucete", "ucono"));
          it_conj_passMood(namedArgs, "imperf", false, root, Arrays.asList("ucevo", "ucevi", "uceva", "ucevamo", "ucevate", "ucevano"));
          it_conj_passMood(namedArgs, "prem", false, root, Arrays.asList("ussi", "ucesti", "usse", "ucemmo", "uceste", "ussero"));
          it_conj_passMood(namedArgs, "fut", true, root, Arrays.asList("urrò", "urrai", "urrà", "urremo", "urrete", "urranno"));
          it_conj_passMood(namedArgs, "cond", true, root, Arrays.asList("urrei", "urresti", "urrebbe", "urremmo", "urreste", "urrebbero"));

          passThroughOrFillIn(namedArgs, "sub123s", root + "uca", false);
          passThroughOrFillIn(namedArgs, "sub1p", root + "uciamo", false);
          passThroughOrFillIn(namedArgs, "sub2p", root + "uciate", false);
          passThroughOrFillIn(namedArgs, "sub3p", root + "ucano", false);

          passThroughOrFillIn(namedArgs, "impsub12s", root + "ucessi", false);
          passThroughOrFillIn(namedArgs, "impsub3s", root + "ucesse", false);
          passThroughOrFillIn(namedArgs, "impsub1p", root + "ucessimo", false);
          passThroughOrFillIn(namedArgs, "impsub2p", root + "uceste", false);
          passThroughOrFillIn(namedArgs, "impsub3p", root + "ucessero", false);

          passThroughOrFillIn(namedArgs, "imp2s", root + "uci", true);
          passThroughOrFillIn(namedArgs, "imp3s", root + "uca", true);
          passThroughOrFillIn(namedArgs, "imp1p", root + "uciamo", true);
          passThroughOrFillIn(namedArgs, "imp2p", root + "ucete", true);
          passThroughOrFillIn(namedArgs, "imp3p", root + "ucano", true);

          return dest.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, appendAndIndexWikiCallback);
        }
      }

  static final Map<String,String> it_indicativePronouns = new LinkedHashMap<String, String>();
  static {
      it_indicativePronouns.put("1s", "io");
      it_indicativePronouns.put("2s", "tu");
      it_indicativePronouns.put("3s", "lui/lei");
      it_indicativePronouns.put("1p", "noi");
      it_indicativePronouns.put("2p", "voi");
      it_indicativePronouns.put("3p", "essi/esse");
  }

  static final Map<String,String> it_subjunctivePronouns = new LinkedHashMap<String, String>();
  static {
      it_subjunctivePronouns.put("1s", "che io");
      it_subjunctivePronouns.put("2s", "che tu");
      it_subjunctivePronouns.put("3s", "che lui/lei");
      it_subjunctivePronouns.put("1p", "che noi");
      it_subjunctivePronouns.put("2p", "che voi");
      it_subjunctivePronouns.put("3p", "che essi/esse");
  }

  static final Map<String,String> it_imperativePronouns = new LinkedHashMap<String, String>();
  static {
      it_imperativePronouns.put("1s", "-");
      it_imperativePronouns.put("2s", "tu");
      it_imperativePronouns.put("3s", "lui/lei");
      it_imperativePronouns.put("1p", "noi");
      it_imperativePronouns.put("2p", "voi");
      it_imperativePronouns.put("3p", "essi/esse");
  }


  static final class it_conj<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
      @Override
      public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
          final Map<String, String> namedArgs,
          final T parser,
          final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
        
        final StringBuilder builder = appendAndIndexWikiCallback.builder;
        
        // TODO: center everything horizontally.
        builder.append("<table style=\"background:#F0F0F0;border-collapse:separate;border-spacing:2px\">");
        
        builder.append("<tr>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">infinitive</th>");
        builder.append("<td colspan=\"1\">");
        appendAndIndexWikiCallback.dispatch(MapUtil.safeRemove(namedArgs, "inf", "-"), null);
        builder.append("</td>");
        builder.append("</tr>\n");

        builder.append("<tr>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">auxiliary verb</th>");
        builder.append("<td colspan=\"1\">");
        appendAndIndexWikiCallback.dispatch(MapUtil.safeRemove(namedArgs, "aux", "-"), null);
        builder.append("</td>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">gerund</th>");
        builder.append("<td colspan=\"1\">");
        appendAndIndexWikiCallback.dispatch(MapUtil.safeRemove(namedArgs, "ger", "-"), null);
        builder.append("</td>");
        builder.append("</tr>\n");

        builder.append("<tr>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">present participle</th>");
        builder.append("<td colspan=\"1\">");
        appendAndIndexWikiCallback.dispatch(MapUtil.safeRemove(namedArgs, "presp", "-"), null);
        builder.append("</td>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">past participle</th>");
        builder.append("<td colspan=\"1\">");
        appendAndIndexWikiCallback.dispatch(MapUtil.safeRemove(namedArgs, "pastp", "-"), null);
        builder.append("</td>");
        builder.append("</tr>\n");

        String style = " style=\"background:#c0cfe4\"";
        outputDataRow(appendAndIndexWikiCallback, style, "indicative", style, "th", "", new LinkedHashMap<String, String>(it_indicativePronouns));
        outputDataRow(appendAndIndexWikiCallback, style, "present", "", "td", "pres", namedArgs);
        outputDataRow(appendAndIndexWikiCallback, style, "imperfect", "", "td", "imperf", namedArgs);
        outputDataRow(appendAndIndexWikiCallback, style, "past historic", "", "td", "prem", namedArgs);
        outputDataRow(appendAndIndexWikiCallback, style, "future", "", "td", "fut", namedArgs);

        style = " style=\"background:#c0d8e4\"";
        outputDataRow(appendAndIndexWikiCallback, style, "conditional", style, "th", "", new LinkedHashMap<String, String>(it_indicativePronouns));
        outputDataRow(appendAndIndexWikiCallback, style, "present", "", "td", "cond", namedArgs);

        style = " style=\"background:#c0e4c0\"";
        outputDataRow(appendAndIndexWikiCallback, style, "subjuntive", style, "th", "", new LinkedHashMap<String, String>(it_subjunctivePronouns));
        namedArgs.put("sub3s2", namedArgs.remove("sub3s"));
        namedArgs.put("sub1s", namedArgs.get("sub123s"));
        namedArgs.put("sub2s", namedArgs.get("sub123s"));
        namedArgs.put("sub3s", namedArgs.remove("sub123s"));
        namedArgs.put("sub1s2", namedArgs.get("sub123s2"));
        namedArgs.put("sub2s2", namedArgs.get("sub123s2"));
        namedArgs.put("sub3s2", namedArgs.remove("sub123s2"));
        outputDataRow(appendAndIndexWikiCallback, style, "present", "", "td", "sub", namedArgs);
        namedArgs.put("impsub1s", namedArgs.get("impsub12s"));
        namedArgs.put("impsub2s", namedArgs.remove("impsub12s"));
        namedArgs.put("impsub1s2", namedArgs.get("impsub12s2"));
        namedArgs.put("impsub2s2", namedArgs.remove("impsub12s2"));
        outputDataRow(appendAndIndexWikiCallback, style, "imperfect", "", "td", "impsub", namedArgs);

        style = " style=\"background:#e4d4c0\"";
        outputDataRow(appendAndIndexWikiCallback, style, "imperative", style, "th", "", new LinkedHashMap<String, String>(it_imperativePronouns));
        outputDataRow(appendAndIndexWikiCallback, style, "", "", "td", "imp", namedArgs);

        builder.append("</table>");
        
        if (!namedArgs.isEmpty()) {
            System.err.println("NON-EMPTY namedArgs: " + namedArgs);
            assert false;
            return false;
        }

        return true;
      }

        private void outputDataRow(AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback,
                String col1Style, String headerName, 
                String col2Style, final String type2, 
                String moodName, Map<String, String> namedArgs) {
            final StringBuilder builder = appendAndIndexWikiCallback.builder;
            builder.append("<tr>");
            builder.append("<th colspan=\"1\"").append(col1Style).append(">").append(headerName).append("</th>");
            for (final String number : it_number_s_p) {
                for (final String person : it_person_1_2_3) {
                    // Output <td> or <th>
                    builder.append("<").append(type2).append("").append(col2Style).append(">");
                    final String keyBase = String.format("%s%s%s", moodName, person, number);
                    for (int suffix = 0; suffix <= 3; ++suffix) {
                        final String key = suffix == 0 ? keyBase : keyBase + suffix;
                        final String val = namedArgs.remove(key);
                        if (val != null) {
                            if (suffix > 0) {
                                builder.append(", ");
                            }
                            appendAndIndexWikiCallback.dispatch(val, null);
                        }
                    }
                    // Output <td> or <th>
                    builder.append("</").append(type2).append(">");
                }
            }
            builder.append("</tr>\n");
        }
    }
}
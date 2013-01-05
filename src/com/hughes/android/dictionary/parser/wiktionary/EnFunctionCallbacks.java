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
import com.hughes.util.StringUtil;

import java.util.ArrayList;
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
      
      callbacks.put("etyl", new etyl<T>());
      callbacks.put("term", new term<T>());
      
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
      callbacks.put("top2", callback);
      callbacks.put("mid2", callback);
      callbacks.put("top3", callback);
      callbacks.put("mid3", callback);
      callbacks.put("bottom", callback);
      callbacks.put("rel-mid", callback);
      callbacks.put("rel-mid3", callback);
      callbacks.put("rel-mid4", callback);
      callbacks.put("rel-bottom", callback);
      callbacks.put("der-top", callback);
      callbacks.put("der-mid", callback);
      callbacks.put("der-mid3", callback);
      callbacks.put("der-bottom", callback);
      
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
      callbacks.put("it-conj-arsi", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-care", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-carsi", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-ciare", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-ciarsi", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-iare", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-iarsi", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-iare-b", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-iarsi-b", new it_conj_are<T>(it_conj_cb));
      callbacks.put("it-conj-ire", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-irsi", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-ire-b", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-irsi-b", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-cire", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-cirsi", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-ire", new it_conj_ire<T>(it_conj_cb));
      callbacks.put("it-conj-ere", new it_conj_ere<T>(it_conj_cb));
      callbacks.put("it-conj-ersi", new it_conj_ere<T>(it_conj_cb));
      callbacks.put("it-conj-urre", new it_conj_urre<T>(it_conj_cb));
      callbacks.put("it-conj-ursi", new it_conj_urre<T>(it_conj_cb));
      callbacks.put("it-conj-fare", new it_conj_fare<T>(it_conj_cb));

      
      //"{{it-conj-fare|putre|avere}}\n" + 

      
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
      namedArgs.remove("lang");
      if (!namedArgs.isEmpty()) {
        EnParser.LOG.warning("weird qualifier: " + wikiTokenizer.token());
        return false;
      }
      appendAndIndexWikiCallback.builder.append("(");
      for (int i = 0; i < args.size(); ++i) {
          appendAndIndexWikiCallback.dispatch(args.get(i), null);
          if (i > 0) {
              appendAndIndexWikiCallback.builder.append(", ");
          }
      }
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
      namedArgs.remove("lang");
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
  
  static final class etyl<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
      @Override
      public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
          final Map<String, String> namedArgs,
          final T parser,
          final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
        final String langCode = ListUtil.get(args, 0);
        if (langCode == null) {
            return false;
        }
        String langName = WiktionaryLangs.getEnglishName(langCode);
        if (langName != null) {
            appendAndIndexWikiCallback.dispatch(langName, null);
        } else {
            appendAndIndexWikiCallback.dispatch("lang:" + langCode, null);
        }
        return true;
      }
  }

  static final class term<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
      @Override
      public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
          final Map<String, String> namedArgs,
          final T parser,
          final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
        namedArgs.remove("sc");
        
        // Main text.
        final String lang = namedArgs.remove("lang");
        String head = ListUtil.get(args, 0);
        String display = ListUtil.get(args, 1);
        if (StringUtil.isNullOrEmpty(head) && StringUtil.isNullOrEmpty(display)) {
            head = display = parser.title;
        }
        if (StringUtil.isNullOrEmpty(head)) {
            // Dispatches formatted wiki text.
            appendAndIndexWikiCallback.dispatch(display, null);
        } else {
            if (StringUtil.isNullOrEmpty(display)) {
                display = head;
            }
            appendAndIndexWikiCallback.dispatch(String.format("[[%s|%s]]", display, head), null);
        }
        
        // Stuff in ()s.
        final String tr = namedArgs.remove("tr");
        final String pos = namedArgs.remove("pos");
        String gloss = ListUtil.get(args, 2);
        String literally = namedArgs.remove("lit");
        if (!StringUtil.isNullOrEmpty(gloss)) {
            gloss = String.format("\"%s\"", gloss);
        }
        if (!StringUtil.isNullOrEmpty(literally)) {
            literally = String.format("literally %s", literally);
        }
        final List<String> inParens = new ArrayList<String>(Arrays.asList(tr, pos, gloss, literally));
        cleanList(inParens);
        appendCommaSeparatedList(appendAndIndexWikiCallback, inParens);
        
        if (tr != null) {
            parser.addLinkToCurrentEntry(tr, lang, EntryTypeName.WIKTIONARY_MENTIONED);
        }
        return namedArgs.isEmpty();
      }

    private void appendCommaSeparatedList(
            final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback,
            final List<String> inParens) {
        if (!inParens.isEmpty()) {
            appendAndIndexWikiCallback.dispatch(" (", null);
            for (int i = 0; i < inParens.size(); ++i) {
                if (i > 0) {
                    appendAndIndexWikiCallback.dispatch(", ", null);
                }
                appendAndIndexWikiCallback.dispatch(inParens.get(i), null);
            }
            appendAndIndexWikiCallback.dispatch(")", null);
        }
    }

  }

  private static void cleanList(List<String> asList) {
      int pos;
      while ((pos = asList.indexOf("")) != -1) {
          asList.remove(pos);
      }
      while ((pos = asList.indexOf(null)) != -1) {
          asList.remove(pos);
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
      final String f = namedArgs.remove("f");
      if (f != null) {
          appendAndIndexWikiCallback.builder.append(", ");
          appendAndIndexWikiCallback.dispatch(f, null, null);
          appendAndIndexWikiCallback.builder.append(" {f}");
      }
      final String m = namedArgs.remove("f");
      if (m != null) {
          appendAndIndexWikiCallback.builder.append(", ");
          appendAndIndexWikiCallback.dispatch(m, null, null);
          appendAndIndexWikiCallback.builder.append(" {m}");
      }
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
        final String h = name.equals("it-conj-care") || name.equals("it-conj-carsi") ? "h" : "";
        final String i = name.equals("it-conj-ciare") || name.equals("it-conj-ciarsi") ? "i" : "";
        final String i2 = name.equals("it-conj-iare") || name.equals("it-conj-iarsi") ? "" : "i";
        final boolean si = name.equals("it-conj-arsi") || name.equals("it-conj-iarsi") || name.equals("it-conj-iarsi-b") || name.equals("it-conj-carsi") || name.equals("it-conj-ciarsi");
        final String root = args.get(0);
        passThroughOrFillIn(namedArgs, "inf", root + i + (si ? "arsi" : "are"), false);
        namedArgs.put("aux", ListUtil.get(args, 1, ""));
        passThroughOrFillIn(namedArgs, "ger", root + i + "ando" + (si ? "si" : ""), true);
        passThroughOrFillIn(namedArgs, "presp", root + i + "ante"+ (si ? "si" : ""), true);
        passThroughOrFillIn(namedArgs, "pastp", root + i + "ato", true);
        if (si) {
            passThroughOrFillIn(namedArgs, "pastp2", root + i + "atosi", true);
        }
        final String i2b = (name.equals("it-conj-iare-b") || name.equals("it-conj-iarsi-b")) ? "" : i2;
        
        it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList(i + "o", h + i2, i + "a", h + i2 + "amo", i + "ate", i + "ano"));
        it_conj_passMood(namedArgs, "imperf", false, root, Arrays.asList(i + "avo", i + "avi", i + "ava", i + "avamo", i + "avate", i + "avano"));
        it_conj_passMood(namedArgs, "prem", false, root, Arrays.asList(i + "ai", i + "asti", i + "ò", i + "ammo", i + "aste", i + "arono"));
        it_conj_passMood(namedArgs, "fut", true, root, Arrays.asList(h + "erò", h + "erai", h + "erà", h + "eremo", h + "erete", h + "eranno"));
        it_conj_passMood(namedArgs, "cond", true, root, Arrays.asList(h + "erei", h + "eresti", h + "erebbe", h + "eremmo", h + "ereste", h + "erebbero"));
        
        passThroughOrFillIn(namedArgs, "sub123s", root + h + i2, false);
        passThroughOrFillIn(namedArgs, "sub1p", root + h + i2b + "amo", false);
        passThroughOrFillIn(namedArgs, "sub2p", root + h + i2b + "ate", false);
        passThroughOrFillIn(namedArgs, "sub3p", root + h + i2 + "no", false);

        passThroughOrFillIn(namedArgs, "impsub12s", root + i + "assi", false);
        passThroughOrFillIn(namedArgs, "impsub3s", root + i + "asse", false);
        passThroughOrFillIn(namedArgs, "impsub1p", root + i + "assimo", false);
        passThroughOrFillIn(namedArgs, "impsub2p", root + i + "aste", false);
        passThroughOrFillIn(namedArgs, "impsub3p", root + i + "assero", false);

        passThroughOrFillIn(namedArgs, "imp2s", root + i + "a" + (si ? "ti" : ""), true);
        passThroughOrFillIn(namedArgs, "imp3s", (si ? "si " : "") + root + h + i2, true);
        passThroughOrFillIn(namedArgs, "imp1p", root + h + i2b + "amo" + (si ? "ci" : ""), true);
        passThroughOrFillIn(namedArgs, "imp2p", root + i + "ate" + (si ? "vi" : ""), true);
        passThroughOrFillIn(namedArgs, "imp3p", (si ? "si " : "") + root + h + i2 + "no", true);

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
        final String i = name.equals("it-conj-cire") || name.equals("it-conj-cirsi") ? "i" : "";
        final boolean si = name.equals("it-conj-irsi") || name.equals("it-conj-irsi-b") || name.equals("it-conj-cirsi");

        passThroughOrFillIn(namedArgs, "inf", root + (si ? "irsi" : "ire"), false);
        namedArgs.put("aux", ListUtil.get(args, 1, ""));
        passThroughOrFillIn(namedArgs, "ger", root + "endo" + (si ? "si" : ""), true);
        passThroughOrFillIn(namedArgs, "presp", root + "ente" + (si ? "si" : ""), true);
        passThroughOrFillIn(namedArgs, "pastp", root + "ito", true);
        if (si) {
            passThroughOrFillIn(namedArgs, "pastp2", root + "itosi", true);
        }
        if (!name.endsWith("-b")) {
            it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList(i + "o", "i", "e", "iamo", "ite", i + "ono"));
        } else {
            it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList("isco", "isci", "isce", "iamo", "ite", "iscono"));
        }
        it_conj_passMood(namedArgs, "imperf", false, root, Arrays.asList("ivo", "ivi", "iva", "ivamo", "ivate", "ivano"));
        it_conj_passMood(namedArgs, "prem", false, root, Arrays.asList("ii", "isti", "ì", "immo", "iste", "irono"));
        // Regular past historic synonyms:
        passThroughOrFillIn(namedArgs, "prem3s2", root + "é", true);
        passThroughOrFillIn(namedArgs, "prem3p2", root + "erono", true);
        it_conj_passMood(namedArgs, "fut", true, root, Arrays.asList("irò", "irai", "irà", "iremo", "irete", "iranno"));
        it_conj_passMood(namedArgs, "cond", true, root, Arrays.asList("irei", "iresti", "irebbe", "iremmo", "ireste", "irebbero"));

        if (!name.endsWith("-b")) {
            passThroughOrFillIn(namedArgs, "sub123s", root + i + "a", false);
            passThroughOrFillIn(namedArgs, "sub3p", root + i + "ano", false);
        } else {
            passThroughOrFillIn(namedArgs, "sub123s", root + "isca", false);
            passThroughOrFillIn(namedArgs, "sub3p", root + "iscano", false);
        }
        passThroughOrFillIn(namedArgs, "sub1p", root + "iamo", false);
        passThroughOrFillIn(namedArgs, "sub2p", root + "iate", false);

        passThroughOrFillIn(namedArgs, "impsub12s", root + "issi", false);
        passThroughOrFillIn(namedArgs, "impsub3s", root + "isse", false);
        passThroughOrFillIn(namedArgs, "impsub1p", root + "issimo", false);
        passThroughOrFillIn(namedArgs, "impsub2p", root + "iste", false);
        passThroughOrFillIn(namedArgs, "impsub3p", root + "issero", false);

        if (!name.endsWith("-b")) {
            passThroughOrFillIn(namedArgs, "imp2s", root + "i" + (si ? "ti" : ""), true);
            passThroughOrFillIn(namedArgs, "imp3s", (si ? "si " : "") + root + i + "a", true);
            passThroughOrFillIn(namedArgs, "imp3p", (si ? "si " : "") + root + i + "ano", true);
        } else {
            passThroughOrFillIn(namedArgs, "imp2s", root + "isci" + (si ? "ti" : ""), true);
            passThroughOrFillIn(namedArgs, "imp3s", (si ? "si " : "") + root + "isca", true);
            passThroughOrFillIn(namedArgs, "imp3p", (si ? "si " : "") + root + "iscano", true);
        }
        passThroughOrFillIn(namedArgs, "imp1p", root + "iamo" + (si ? "ci" : ""), true);
        passThroughOrFillIn(namedArgs, "imp2p", root + "ite" + (si ? "vi" : ""), true);

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
          final boolean si = name.equals("it-conj-ersi");

          passThroughOrFillIn(namedArgs, "inf", root + (si ? "ersi" : "ere"), false);
          namedArgs.put("aux", ListUtil.get(args, 1, ""));
          passThroughOrFillIn(namedArgs, "ger", root + "endo" + (si ? "si" : ""), true);
          passThroughOrFillIn(namedArgs, "presp", root + "ente" + (si ? "si" : ""), true);
          passThroughOrFillIn(namedArgs, "pastp", root + "uto", true);
          if (si) {
              passThroughOrFillIn(namedArgs, "pastp2", root + "utosi", true);
          }
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

          passThroughOrFillIn(namedArgs, "imp2s", root + "i" + (si ? "ti" : ""), true);
          passThroughOrFillIn(namedArgs, "imp3s", (si ? "si " : "") + root + "a", true);
          passThroughOrFillIn(namedArgs, "imp1p", root + "iamo" + (si ? "ci" : ""), true);
          passThroughOrFillIn(namedArgs, "imp2p", root + "ete" + (si ? "vi" : ""), true);
          passThroughOrFillIn(namedArgs, "imp3p", (si ? "si " : "") + root + "ano", true);

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
          final boolean si = name.equals("it-conj-ursi");

          passThroughOrFillIn(namedArgs, "inf", root + (si ? "ursi" : "urre"), false);
          namedArgs.put("aux", ListUtil.get(args, 1, ""));
          passThroughOrFillIn(namedArgs, "ger", root + "ucendo" + (si ? "si" : ""), true);
          passThroughOrFillIn(namedArgs, "presp", root + "ucente" + (si ? "si" : ""), true);
          passThroughOrFillIn(namedArgs, "pastp", root + "otto", true);
          if (si) {
              passThroughOrFillIn(namedArgs, "pastp2", root + "ottosi", true);
          }
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

          passThroughOrFillIn(namedArgs, "imp2s", root + "uci" + (si ? "ti" : ""), true);
          passThroughOrFillIn(namedArgs, "imp3s", (si ? "si" : "") + root + "uca", true);
          passThroughOrFillIn(namedArgs, "imp1p", root + "uciamo" + (si ? "ci" : ""), true);
          passThroughOrFillIn(namedArgs, "imp2p", root + "ucete" + (si ? "vi" : ""), true);
          passThroughOrFillIn(namedArgs, "imp3p", (si ? "si" : "") + root + "ucano", true);

          return dest.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, appendAndIndexWikiCallback);
        }
      }

  static final class it_conj_fare<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
      final it_conj<T> dest;
      it_conj_fare(it_conj<T> dest) {
        this.dest = dest;
      }
      @Override
        public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
            final Map<String, String> namedArgs,
            final T parser,
            final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
          final String root = args.get(0);
          passThroughOrFillIn(namedArgs, "inf", root + "fare", false);
          namedArgs.put("aux", ListUtil.get(args, 1, ""));
          passThroughOrFillIn(namedArgs, "ger", root + "facendo", true);
          passThroughOrFillIn(namedArgs, "presp", root + "facente", true);
          passThroughOrFillIn(namedArgs, "pastp", root + "fatto", true);
          it_conj_passMood(namedArgs, "pres", false, root, Arrays.asList("faccio", "fai", "fà", "facciamo", "fate", "fanno"));
          passThroughOrFillIn(namedArgs, "pres1s2", root + "fò", true);
          it_conj_passMood(namedArgs, "imperf", false, root, Arrays.asList("facevo", "facevi", "faceva", "facevamo", "facevate", "facevano"));
          it_conj_passMood(namedArgs, "prem", false, root, Arrays.asList("feci", "facesti", "fece", "facemmo", "faceste", "fecero"));
          it_conj_passMood(namedArgs, "fut", true, root, Arrays.asList("farò", "farai", "farà", "faremo", "farete", "faranno"));
          it_conj_passMood(namedArgs, "cond", true, root, Arrays.asList("farei", "faresti", "farebbe", "faremmo", "fareste", "farebbero"));

          passThroughOrFillIn(namedArgs, "sub123s", root + "faccia", false);
          passThroughOrFillIn(namedArgs, "sub1p", root + "facciamo", false);
          passThroughOrFillIn(namedArgs, "sub2p", root + "facciate", false);
          passThroughOrFillIn(namedArgs, "sub3p", root + "facciano", false);

          passThroughOrFillIn(namedArgs, "impsub12s", root + "facessi", false);
          passThroughOrFillIn(namedArgs, "impsub3s", root + "facesse", false);
          passThroughOrFillIn(namedArgs, "impsub1p", root + "facessimo", false);
          passThroughOrFillIn(namedArgs, "impsub2p", root + "faceste", false);
          passThroughOrFillIn(namedArgs, "impsub3p", root + "facessero", false);

          passThroughOrFillIn(namedArgs, "imp2s", root + "fa", true);
          passThroughOrFillIn(namedArgs, "imp3s", root + "faccia", true);
          passThroughOrFillIn(namedArgs, "imp1p", root + "facciamo", true);
          passThroughOrFillIn(namedArgs, "imp2p", root + "fate", true);
          passThroughOrFillIn(namedArgs, "imp3p", root + "facciano", true);

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
        
        final String inf = namedArgs.get("inf");
        
        // TODO: center everything horizontally.
        builder.append("<table style=\"background:#F0F0F0\">");
        
        builder.append("<tr>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">infinito</th>");
        builder.append("<td colspan=\"1\">");
        appendAndIndexWikiCallback.dispatch(MapUtil.safeRemove(namedArgs, "inf", "-"), null);
        builder.append("</td>");
        builder.append("</tr>\n");

        builder.append("<tr>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">verbo ausiliare</th>");
        builder.append("<td colspan=\"1\">");
        appendAndIndexWikiCallback.dispatch(MapUtil.safeRemove(namedArgs, "aux", "-"), null);
        builder.append("</td>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">gerundio</th>");
        builder.append("<td colspan=\"1\">");
        outputKeyVariations(appendAndIndexWikiCallback, builder, "ger", namedArgs, true);
        builder.append("</td>");
        builder.append("</tr>\n");

        builder.append("<tr>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">participio presente</th>");
        builder.append("<td colspan=\"1\">");
        outputKeyVariations(appendAndIndexWikiCallback, builder, "presp", namedArgs, true);
        builder.append("</td>");
        builder.append("<th colspan=\"1\" style=\"background:#e2e4c0\">participio passato</th>");
        builder.append("<td colspan=\"1\">");
        outputKeyVariations(appendAndIndexWikiCallback, builder, "pastp", namedArgs, true);
        builder.append("</td>");
        builder.append("</tr>\n");
        
        final List<String> prefixes = (inf != null && inf.endsWith("si")) ? it_reflexive_pronouns : it_empty; 

        String style = " style=\"background:#c0cfe4\"";
        outputDataRow(appendAndIndexWikiCallback, style, "indicativo", style, "th", "", new LinkedHashMap<String, String>(it_indicativePronouns), it_empty, false);
        outputDataRow(appendAndIndexWikiCallback, style, "presente", "", "td", "pres", namedArgs, prefixes, true);
        outputDataRow(appendAndIndexWikiCallback, style, "imperfetto", "", "td", "imperf", namedArgs, prefixes, true);
        outputDataRow(appendAndIndexWikiCallback, style, "passato remoto", "", "td", "prem", namedArgs, prefixes, true);
        outputDataRow(appendAndIndexWikiCallback, style, "futuro", "", "td", "fut", namedArgs, prefixes, true);

        style = " style=\"background:#c0d8e4\"";
        outputDataRow(appendAndIndexWikiCallback, style, "condizionale", style, "th", "", new LinkedHashMap<String, String>(it_indicativePronouns), it_empty, false);
        outputDataRow(appendAndIndexWikiCallback, style, "presente", "", "td", "cond", namedArgs, prefixes, true);

        style = " style=\"background:#c0e4c0\"";
        outputDataRow(appendAndIndexWikiCallback, style, "congiuntivo", style, "th", "", new LinkedHashMap<String, String>(it_subjunctivePronouns), it_empty, false);
        namedArgs.put("sub3s2", namedArgs.remove("sub3s"));
        namedArgs.put("sub1s", namedArgs.get("sub123s"));
        namedArgs.put("sub2s", namedArgs.get("sub123s"));
        namedArgs.put("sub3s", namedArgs.remove("sub123s"));
        namedArgs.put("sub1s2", namedArgs.get("sub123s2"));
        namedArgs.put("sub2s2", namedArgs.get("sub123s2"));
        namedArgs.put("sub3s2", namedArgs.remove("sub123s2"));
        outputDataRow(appendAndIndexWikiCallback, style, "presente", "", "td", "sub", namedArgs, prefixes, true);
        namedArgs.put("impsub1s", namedArgs.get("impsub12s"));
        namedArgs.put("impsub2s", namedArgs.remove("impsub12s"));
        namedArgs.put("impsub1s2", namedArgs.get("impsub12s2"));
        namedArgs.put("impsub2s2", namedArgs.remove("impsub12s2"));
        outputDataRow(appendAndIndexWikiCallback, style, "imperfetto", "", "td", "impsub", namedArgs, prefixes, true);

        style = " style=\"background:#e4d4c0\"";
        outputDataRow(appendAndIndexWikiCallback, style, "imperativo", style, "th", "", new LinkedHashMap<String, String>(it_imperativePronouns), it_empty, false);
        outputDataRow(appendAndIndexWikiCallback, style, "", "", "td", "imp", namedArgs, it_empty, false);  // these are attached to the stem.

        builder.append("</table>\n");
        
        if (!namedArgs.isEmpty()) {
            System.err.println("NON-EMPTY namedArgs: " + namedArgs);
            if ("muovesse".equals(namedArgs.get("impsib3s2"))) {
                return false;
            }
            if ("percuotesse".equals(namedArgs.get("impsib3s2"))) {
                return false;
            }
            // Too many to deal with:
            //assert false;
            return false;
        }

        return true;
      }

        private void outputDataRow(AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback,
                String col1Style, String headerName, 
                String col2Style, final String type2, 
                String moodName, Map<String, String> namedArgs, final List<String> prefixes, final boolean isForm) {
            final StringBuilder builder = appendAndIndexWikiCallback.builder;
            builder.append("<tr>");
            builder.append("<th colspan=\"1\"").append(col1Style).append(">").append(headerName).append("</th>");
            int i = 0;
            for (final String number : it_number_s_p) {
                for (final String person : it_person_1_2_3) {
                    // Output <td> or <th>
                    builder.append("<").append(type2).append("").append(col2Style).append(">");
                    final String keyBase = String.format("%s%s%s", moodName, person, number);
                    appendAndIndexWikiCallback.dispatch(prefixes.get(i++), null);
                    outputKeyVariations(appendAndIndexWikiCallback, builder, keyBase, namedArgs, isForm);
                    // Output <td> or <th>
                    builder.append("</").append(type2).append(">");
                }
            }
            builder.append("</tr>\n");
        }
    }
  
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
  static final List<String> it_reflexive_pronouns = Arrays.asList("mi ", "ti ", "si ", "ci ", "vi ", "si ");
  static final List<String> it_empty = Arrays.asList("", "", "", "", "", "");
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

  private static <T extends AbstractWiktionaryParser> void outputKeyVariations(AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback,
        final StringBuilder builder, final String keyBase, Map<String, String> namedArgs, boolean isForm) {
    for (int suffix = 0; suffix <= 4; ++suffix) {
        final String key = suffix == 0 ? keyBase : keyBase + suffix;
        final String val = namedArgs.remove(key);
        if (val != null && !val.trim().equals("")) {
            if (suffix > 0) {
                builder.append(", ");
            }
            appendAndIndexWikiCallback.dispatch(val, null);
            if (isForm) {
                appendAndIndexWikiCallback.parser.addLinkToCurrentEntry(val, null, EntryTypeName.WIKTIONARY_INFLECTED_FORM_MULTI);
            }
        }
    }
  }


}
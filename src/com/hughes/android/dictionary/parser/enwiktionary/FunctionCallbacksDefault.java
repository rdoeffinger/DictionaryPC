package com.hughes.android.dictionary.parser.enwiktionary;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.util.ListUtil;

public final class FunctionCallbacksDefault {
  
  static final Logger LOG = Logger.getLogger(EnWiktionaryXmlParser.class.getName());
  
  static final Map<String,FunctionCallback> DEFAULT = new LinkedHashMap<String, FunctionCallback>();
  static {
    FunctionCallback callback = new TranslationCallback();
    DEFAULT.put("t", callback);
    DEFAULT.put("t+", callback);
    DEFAULT.put("t-", callback);
    DEFAULT.put("tø", callback);
    DEFAULT.put("apdx-t", callback);
    
    DEFAULT.put("qualifier", new QualifierCallback());

    callback = new EncodingCallback();
    Set<String> encodings = new LinkedHashSet<String>(Arrays.asList(
        "zh-ts", "zh-tsp",
        "sd-Arab", "ku-Arab", "Arab", "unicode", "Laoo", "ur-Arab", "Thai", 
        "fa-Arab", "Khmr", "Cyrl", "IPAchar", "ug-Arab", "ko-inline", 
        "Jpan", "Kore", "Hebr", "rfscript", "Beng", "Mong", "Knda", "Cyrs",
        "yue-tsj", "Mlym", "Tfng", "Grek", "yue-yue-j"));
    for (final String encoding : encodings) {
      DEFAULT.put(encoding, callback);
    }
    
    callback = new Gender();
    DEFAULT.put("m", callback);
    DEFAULT.put("f", callback);
    DEFAULT.put("n", callback);
    DEFAULT.put("p", callback);
    DEFAULT.put("g", callback);

    DEFAULT.put("l", new l());
    DEFAULT.put("italbrac", new italbrac());
    DEFAULT.put("gloss", new gloss());

    callback = new AppendArg0();
    DEFAULT.put("term", callback);

    callback = new Ignore();
    DEFAULT.put("trreq", callback);
    DEFAULT.put("t-image", callback);

    DEFAULT.put("not used", new not_used());
  }

  // ------------------------------------------------------------------

  static final class TranslationCallback implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs, final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {

      final String transliteration = namedArgs.remove("tr");
      namedArgs.keySet().removeAll(EnWiktionaryXmlParser.USELESS_WIKI_ARGS);
      if (args.size() < 2) {
        LOG.warning("{{t...}} with wrong args: title=" + parser.title);
        return false;
      }
      final String langCode = ListUtil.get(args, 0);
      final String word = ListUtil.get(args, 1);
      final String gender = ListUtil.get(args, 2);
      // TODO: deal with second (and third...) gender, and alt.
      
      appendAndIndexWikiCallback.dispatch(word, EntryTypeName.WIKTIONARY_TITLE_MULTI);
      
      if (gender != null) {
        appendAndIndexWikiCallback.builder.append(String.format(" {%s}", gender));
      }
      if (transliteration != null) {
        appendAndIndexWikiCallback.builder.append(" (tr. ");
        appendAndIndexWikiCallback.dispatch(transliteration, EntryTypeName.WIKTIONARY_TRANSLITERATION);
        appendAndIndexWikiCallback.builder.append(")");
      }
      return true;
    }
    
  }

  // ------------------------------------------------------------------
  
  static final class QualifierCallback implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        LOG.warning("weird qualifier: ");
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
  
  static final class EncodingCallback implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        LOG.warning("weird encoding: " + wikiTokenizer.token());
      }
      final String wikiText = args.get(0);
      appendAndIndexWikiCallback.dispatch(wikiText, appendAndIndexWikiCallback.entryTypeName);
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class Gender implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
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
  
  static final class l implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      // TODO: rewrite this!
      // encodes text in various langs.
      // lang is arg 0.
      // TODO: set that we're inside L
      // EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT
      WikiTokenizer.dispatch(args.get(1), false, appendAndIndexWikiCallback);
      // TODO: transliteration
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class AppendArg0 implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      // TODO: transliteration
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class italbrac implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append("[");
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      appendAndIndexWikiCallback.builder.append("]");
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class gloss implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append("[");
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      appendAndIndexWikiCallback.builder.append("]");
      return true;
    }
  }
  
  // ------------------------------------------------------------------
  
  static final class Ignore implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class not_used implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      appendAndIndexWikiCallback.builder.append("(not used)");
      return true;
    }
  }

  

}

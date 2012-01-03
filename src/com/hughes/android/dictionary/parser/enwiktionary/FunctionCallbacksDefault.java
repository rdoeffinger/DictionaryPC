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
    DEFAULT.put("defn", callback);
    DEFAULT.put("rfdef", callback);

    DEFAULT.put("not used", new not_used());
    
    DEFAULT.put("form of", new FormOf());
  }

  
  static final class NameAndArgs implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs, final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      
      appendAndIndexWikiCallback.builder.append(name);
      for (int i = 0; i < args.size(); ++i) {
        if (args.get(i).length() > 0) {
          appendAndIndexWikiCallback.builder.append("|");
          appendAndIndexWikiCallback.dispatch(args.get(i), null, null);
        }
      }
      for (final Map.Entry<String, String> entry : namedArgs.entrySet()) {
        appendAndIndexWikiCallback.builder.append("|");
        appendAndIndexWikiCallback.dispatch(entry.getKey(), null, null);
        appendAndIndexWikiCallback.builder.append("=");
        appendAndIndexWikiCallback.dispatch(entry.getValue(), null, null);
      }
      return true;
    }
  }
  static NameAndArgs NAME_AND_ARGS = new NameAndArgs();

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
      // 
      final EntryTypeName entryTypeName;
      switch (parser.state) {
      case TRANSLATION_LINE: entryTypeName = EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT; break;
      case ENGLISH_DEF_OF_FOREIGN: entryTypeName = EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK; break;
      default: throw new IllegalStateException("Invalid enum value: " + parser.state);
      }
      final String langCode = args.get(0);
      if ("en".equals(langCode)) {
        appendAndIndexWikiCallback.dispatch(args.get(1), parser.enIndexBuilder, entryTypeName);
      } else {
        appendAndIndexWikiCallback.dispatch(args.get(1), parser.foreignIndexBuilder, entryTypeName);
      }
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

  
  // --------------------------------------------------------------------
  // --------------------------------------------------------------------
  

  static final class FormOf implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      String formName = name;
      if (name.equals("form of")) {
        formName = ListUtil.remove(args, 0, null);
      }
      if (formName == null) {
        LOG.warning("Missing form name: " + parser.title);
        formName = "form of";
      }
      String baseForm = ListUtil.get(args, 1, "");
      if ("".equals(baseForm)) {
        baseForm = ListUtil.get(args, 0, null);
        ListUtil.remove(args, 1, "");
      } else {
        ListUtil.remove(args, 0, null);
      }
      namedArgs.keySet().removeAll(EnWiktionaryXmlParser.USELESS_WIKI_ARGS);
      
      appendAndIndexWikiCallback.builder.append("{");
      NAME_AND_ARGS.onWikiFunction(wikiTokenizer, formName, args, namedArgs, parser, appendAndIndexWikiCallback);
      appendAndIndexWikiCallback.builder.append("}");
      if (baseForm != null && appendAndIndexWikiCallback.indexedEntry != null) {
        parser.foreignIndexBuilder.addEntryWithString(appendAndIndexWikiCallback.indexedEntry, baseForm, EntryTypeName.WIKTIONARY_BASE_FORM_MULTI);
      } else {
        // null baseForm happens in Danish.
        LOG.warning("Null baseform: " + parser.title);
      }
      return true;
    }
  }
  
  static final FormOf FORM_OF = new FormOf();

}

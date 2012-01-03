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
    DEFAULT.put("attention", callback);
    DEFAULT.put("zh-attention", callback);

    DEFAULT.put("not used", new not_used());
    DEFAULT.put("form of", new FormOf());
    DEFAULT.put("wikipedia", new wikipedia());
    
    callback = new InflOrHead();
    DEFAULT.put("infl", callback);
    DEFAULT.put("head", callback);
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
  

  // --------------------------------------------------------------------
  // --------------------------------------------------------------------
  
  static final class wikipedia implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
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

  static final class InflOrHead implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      // See: http://en.wiktionary.org/wiki/Template:infl
      final String langCode = ListUtil.get(args, 0);
      String head = namedArgs.remove("head");
      if (head == null) {
        head = namedArgs.remove("title"); // Bug
      }
      if (head == null) {
        head = parser.title;
      } else {
        head = WikiTokenizer.toPlainText(head);
      }
      parser.titleAppended = true;
      
      namedArgs.keySet().removeAll(EnWiktionaryXmlParser.USELESS_WIKI_ARGS);

      final String tr = namedArgs.remove("tr");
      String g = namedArgs.remove("g");
      if (g == null) {
        g = namedArgs.remove("gender");
      }
      final String g2 = namedArgs.remove("g2");
      final String g3 = namedArgs.remove("g3");

      appendAndIndexWikiCallback.dispatch(head, EntryTypeName.WIKTIONARY_TITLE_MULTI);

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
        appendAndIndexWikiCallback.builder.append(" (tr. ");
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
  static final class it_noun implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
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
        LOG.warning("Invalid it-noun: " + wikiTokenizer.token());
      }
      return true;
    }
  }

  static {
    DEFAULT.put("it-proper noun", new it_proper_noun());
  } 
  static final class it_proper_noun implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback) {
      return false;
    }
  }

}

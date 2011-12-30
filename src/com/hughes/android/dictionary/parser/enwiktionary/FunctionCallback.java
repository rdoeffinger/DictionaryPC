package com.hughes.android.dictionary.parser.enwiktionary;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.util.ListUtil;

public interface FunctionCallback {
  
  static final Logger LOG = Logger.getLogger(EnWiktionaryXmlParser.class.getName());
  
  boolean onWikiFunction(
      final String name,
      final List<String> args, 
      final Map<String,String> namedArgs,
      final EnWiktionaryXmlParser parser,
      final AppendAndIndexWikiCallback appendAndIndexWikiCallback,
      final String title);

  static final class TranslationCallback implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final String name, final List<String> args,
        final Map<String, String> namedArgs, final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback,
        final String title) {

      final String transliteration = namedArgs.remove("tr");
      
      if (args.size() < 2 || args.size() > 3 || namedArgs.isEmpty()) {
        LOG.warning("{{t}} with too few args: " + ", title=" + title);
        return false;
      }
      final String langCode = ListUtil.get(args, 0);
      final String word = ListUtil.get(args, 1);
      final String gender = ListUtil.get(args, 2);
      
// TODO      appendAndIndexWikiCallback we're inside translation....
      //EntryTypeName.WIKTIONARY_TITLE_SINGLE, EntryTypeName.WIKTIONARY_TITLE_MULTI
      new WikiTokenizer(word, false).dispatch(appendAndIndexWikiCallback);
      
      if (gender != null) {
        appendAndIndexWikiCallback.builder.append(String.format(" {%s}", gender));
      }
      if (transliteration != null) {
     // TODO      appendAndIndexWikiCallback we're inside translation....
        // EntryTypeName.WIKTIONARY_TRANSLITERATION
        appendAndIndexWikiCallback.builder.append("(tr. ");
        new WikiTokenizer(transliteration).dispatch(appendAndIndexWikiCallback);
        appendAndIndexWikiCallback.builder.append(")");
      }
      return true;
    }
    
  }

  // ------------------------------------------------------------------
  
  static final class QualifierCallback implements FunctionCallback {
    @Override
    public boolean onWikiFunction(final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnWiktionaryXmlParser parser,
        final AppendAndIndexWikiCallback appendAndIndexWikiCallback,
        final String title) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        LOG.warning("weird qualifier: ");
        return false;
      }
      String qualifier = args.get(0);
      // Unindexed!
      appendAndIndexWikiCallback.builder.append("(");
      new WikiTokenizer(qualifier, false).dispatch(appendAndIndexWikiCallback);
      appendAndIndexWikiCallback.builder.append(")");
      return true;
    }
  }

}

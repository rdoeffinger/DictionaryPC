package com.hughes.android.dictionary.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.hughes.android.dictionary.engine.DictionaryBuilder;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.parser.WikiWord.FormOf;
import com.hughes.android.dictionary.parser.WikiWord.Translation;
import com.hughes.util.ListUtil;
import com.hughes.util.StringUtil;

public class EnWiktionaryXmlParser extends org.xml.sax.helpers.DefaultHandler implements WikiCallback {
  
  static final Pattern partOfSpeechHeader = Pattern.compile(
      "Noun|Verb|Adjective|Adverb|Pronoun|Conjunction|Interjection|" +
      "Preposition|Proper noun|Article|Prepositional phrase|Acronym|" +
      "Abbreviation|Initialism|Contraction|Prefix|Suffix|Symbol|Letter|" +
      "Ligature|Idiom|Phrase|" +
      // These are @deprecated:
      "Noun form|Verb form|Adjective form|Nominal phrase|Noun phrase|" +
      "Verb phrase|Transitive verb|Intransitive verb|Reflexive verb|" +
      // These are extras I found:
      "Determiner|Numeral|Number|Cardinal number|Ordinal number|Proverb|" +
      "Particle|Interjection|Pronominal adverb" +
      "Han character|Hanzi|Hanja|Kanji|Katakana character|Syllable");

  static final Pattern wikiMarkup =  Pattern.compile("\\[\\[|\\]\\]|''+");

  final DictionaryBuilder dictBuilder;
  
  final IndexBuilder[] indexBuilders;
  final Pattern[] langPatterns;
  final int enIndexBuilder;

  StringBuilder titleBuilder;
  StringBuilder textBuilder;
  StringBuilder currentBuilder = null;

  public EnWiktionaryXmlParser(final DictionaryBuilder dictBuilder, final Pattern[] langPatterns, final int enIndexBuilder) {
    assert langPatterns.length == 2;
    this.dictBuilder = dictBuilder;
    this.indexBuilders = dictBuilder.indexBuilders.toArray(new IndexBuilder[0]);
    this.langPatterns = langPatterns;
    this.enIndexBuilder = enIndexBuilder;
  }

  @Override
  public void startElement(String uri, String localName, String qName,
      Attributes attributes) {
    currentBuilder = null;
    if ("page".equals(qName)) {
      titleBuilder = new StringBuilder();
      
      // Start with "\n" to better match certain strings.
      textBuilder = new StringBuilder("\n");
    } else if ("title".equals(qName)) {
      currentBuilder = titleBuilder;
    } else if ("text".equals(qName)) {
      currentBuilder = textBuilder;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (currentBuilder != null) {
      currentBuilder.append(ch, start, length);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName)
      throws SAXException {
    currentBuilder = null;
    if ("page".equals(qName)) {
      endPage();
    }
  }
  

  public void parse(final File file) throws ParserConfigurationException,
      SAXException, IOException {
    final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    parser.parse(file, this);
  }
  
  int pageCount = 0;
  private void endPage() {
    title = titleBuilder.toString();
    ++pageCount;
    if (pageCount % 1000 == 0) {
      System.out.println("pageCount=" + pageCount);
    }
    if (title.startsWith("Wiktionary:") ||
        title.startsWith("Template:") ||
        title.startsWith("Appendix:") ||
        title.startsWith("Category:") ||
        title.startsWith("Index:") ||
        title.startsWith("MediaWiki:") ||
        title.startsWith("TransWiki:") ||
        title.startsWith("Citations:") ||
        title.startsWith("Concordance:") ||
        title.startsWith("Help:")) {
      return;
    }
    currentDepth = 0;
    words.clear();
    currentHeading = null;
    insidePartOfSpeech = false;
//    System.err.println("Working on page: " + title);
    try {
      WikiParser.parse(textBuilder.toString(), this);
    } catch (Throwable e) {
      System.err.println("Failure on page: " + title);
      e.printStackTrace(System.err); 
    }

   for (final WikiWord word : words) {
     word.wikiWordToQuickDic(dictBuilder, enIndexBuilder);
   }  // WikiWord
   
  }  // endPage()


  // ------------------------------------------------------------------------
  // ------------------------------------------------------------------------
  // ------------------------------------------------------------------------
  // ------------------------------------------------------------------------

  /**
   * Two things can happen:
   * 
   * We can be in a ==German== section.  There we will see English definitions.
   * Each POS should get its own QuickDic entry.  Pretty much everything goes
   * in.
   * 
   * Or we can be in an ==English== section with English definitions
   * and maybe see translations for languages we care about.
   * 
   * In either case, we need to differentiate the subsections (Noun, Verb, etc.)
   * into separate QuickDic entries, but that's tricky--how do we know when we
   * found a subsection?  Just ignore anything containing pronunciation and
   * etymology?
   * 
   * How do we decide when to seal the deal on an entry?
   * 
   * Would be nice if the parser told us about leaving sections....
   * 
   * 
   */

  String title;
  String currentHeading;
  int currentDepth;
  final List<WikiWord> words = new ArrayList<WikiWord>();
  WikiWord currentWord;
  WikiWord.PartOfSpeech currentPartOfSpeech;
  WikiWord.TranslationSense currentTranslationSense;
  boolean insidePartOfSpeech;
  
  StringBuilder wikiBuilder = null;
  
  @Override
  public void onWikiLink(String[] args) {
    if (wikiBuilder == null) {
      return;
    }
    wikiBuilder.append(args[args.length - 1]);
  }
  
  // ttbc: translations to be checked.
  static final Set<String> useRemainingArgTemplates = new LinkedHashSet<String>(Arrays.asList(
      "Arab", "Cyrl", "fa-Arab", "italbrac", "Khmr", "ku-Arab", "IPAchar", "Laoo", 
      "sd-Arab", "Thai", "ttbc", "unicode", "ur-Arab", "yue-yue-j", "zh-ts", 
      "zh-tsp", "zh-zh-p", "ug-Arab", "ko-inline", "Jpan", "Kore", "rfscript", "Latinx"));
  static final Set<String> ignoreTemplates = new LinkedHashSet<String>(Arrays.asList("audio", "rhymes", "hyphenation", "homophones", "wikipedia", "rel-top", "rel-bottom", "sense", "wikisource1911Enc", "g"));
  static final Set<String> grammarTemplates = new LinkedHashSet<String>(Arrays.asList("impf", "pf", "pf.", "indeclinable"));
  static final Set<String> passThroughTemplates = new LinkedHashSet<String>(Arrays.asList("zzzzzzzzzzzzzzz"));

  @Override
  public void onTemplate(final List<String> positionalArgs, final Map<String,String> namedArgs) {
    if (positionalArgs.isEmpty()) {
      // This happens very rarely with special templates.
      return;
    }
    final String name = positionalArgs.get(0);
    
    namedArgs.remove("lang");
    namedArgs.remove("nocat");
    namedArgs.remove("nocap");
    namedArgs.remove("sc");

    // Pronunciation
    if (currentWord != null) {
      if (name.equals("a")) {
        // accent tag
        currentWord.currentPronunciation = new StringBuilder();
        currentWord.accentToPronunciation.put(positionalArgs.get(1), currentWord.currentPronunciation);
        return;
      }
      
      if (name.equals("IPA") || name.equals("SAMPA") || name.equals("X-SAMPA") || name.equals("enPR")) {
        namedArgs.remove("lang");
        for (int i = 0; i < 100 && !namedArgs.isEmpty(); ++i) {
          final String pron = namedArgs.remove("" + i);
          if (pron != null) {
            positionalArgs.add(pron);
          } else {
            if (i > 10) {
              break;
            }
          }
        }
        if (!(positionalArgs.size() >= 2 && namedArgs.isEmpty())) {
          System.err.println("Invalid pronunciation: " + positionalArgs.toString() + namedArgs.toString());
        }
        if (currentWord.currentPronunciation == null) {
          currentWord.currentPronunciation = new StringBuilder();
          currentWord.accentToPronunciation.put("", currentWord.currentPronunciation);
        }
        if (currentWord.currentPronunciation.length() > 0) {
          currentWord.currentPronunciation.append("; ");
        }
        for (int i = 1; i < positionalArgs.size(); ++i) {
          if (i > 1) {
            currentWord.currentPronunciation.append(",");
          }
          final String pron = wikiMarkup.matcher(positionalArgs.get(1)).replaceAll("");
          currentWord.currentPronunciation.append(pron).append("");
        }
        currentWord.currentPronunciation.append(" (").append(name).append(")");
        return;
      }
      
      if (name.equals("qualifier")) {
        //assert positionalArgs.size() == 2 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs.toString();
        if (wikiBuilder == null) {
          return;
        }
        wikiBuilder.append(" (").append(positionalArgs.get(1)).append(")");
        return;
      }
      
      if (name.equals("...")) {
        // Skipping any elided text for brevity.
        wikiBuilder.append("...");
        return;
      }
      
      if (passThroughTemplates.contains(name)) {
        assert positionalArgs.size() == 1 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs;
        wikiBuilder.append(name);
        return;
      }
      
      if (ignoreTemplates.contains(name)) {
        return;
      }
      
      if ("Pronunciation".equals(currentHeading)) {
        System.err.println("Unhandled pronunciation template: " + positionalArgs + namedArgs);
        return;
      }
    }  // Pronunciation
    
    // Part of speech
    if (insidePartOfSpeech) {
      
      // form of
      if (name.equals("form of")) {
        namedArgs.remove("sc");
        if (positionalArgs.size() < 3 || positionalArgs.size() > 4) {
          System.err.println("Invalid form of.");
        }
        final String token = positionalArgs.get(positionalArgs.size() == 3 ? 2 : 3);
        final String grammarForm = WikiParser.simpleParse(positionalArgs.get(1));
        currentPartOfSpeech.formOfs.add(new FormOf(grammarForm, token));
        return;
      }
      
      // The fallback plan: append the template!
      if (wikiBuilder != null) {
        wikiBuilder.append("{");
        boolean first = true;
        for (final String arg : positionalArgs) {
          if (!first) {
            wikiBuilder.append(", ");
          }
          first = false;
          wikiBuilder.append(arg);
        }
        // This one isn't so useful.
        for (final Map.Entry<String, String> entry : namedArgs.entrySet()) {
          if (!first) {
            wikiBuilder.append(", ");
          }
          first = false;
          wikiBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        wikiBuilder.append("}");
      }
      
      //System.err.println("Unhandled part of speech template: " + positionalArgs + namedArgs);
      return;
    }  // Part of speech

    
    // Translations
    if (name.equals("trans-top")) {
      assert positionalArgs.size() >= 1 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs + title;
      
      if (currentPartOfSpeech == null) {
        assert currentWord != null && !currentWord.partsOfSpeech.isEmpty() : title; 
        System.err.println("Assuming last part of speech for non-nested translation section: " + title);
        currentPartOfSpeech = ListUtil.getLast(currentWord.partsOfSpeech);
      }
      
      currentTranslationSense = new WikiWord.TranslationSense();
      currentPartOfSpeech.translationSenses.add(currentTranslationSense);
      if (positionalArgs.size() > 1) {
        currentTranslationSense.sense = positionalArgs.get(1);
      }
      return;
    }  // Translations

    if (wikiBuilder == null) {
      return;
    }    
    if (name.equals("m") || name.equals("f") || name.equals("n") || name.equals("c")) {
      assert positionalArgs.size() >= 1 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs.toString();
      wikiBuilder.append("{");
      for (int i = 1; i < positionalArgs.size(); ++i) {
        wikiBuilder.append(i > 1 ? "," : "");
        wikiBuilder.append(positionalArgs.get(i));
      }
      wikiBuilder.append(name).append("}");
      
    } else  if (name.equals("p")) {
      assert positionalArgs.size() == 1 && namedArgs.isEmpty();
      wikiBuilder.append("pl.");

    } else  if (name.equals("s")) {
      assert positionalArgs.size() == 1 && namedArgs.isEmpty() || title.equals("dobra");
      wikiBuilder.append("sg.");
      
    } else  if (grammarTemplates.contains(name)) {
      assert positionalArgs.size() == 1 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs;
      wikiBuilder.append(name).append(".");

    } else  if (name.equals("l")) {
      // This template is designed to generate a link to a specific language-section on the target page.
      wikiBuilder.append(positionalArgs.size() >= 4 ? positionalArgs.get(3) : positionalArgs.get(2));
      
    } else if (name.equals("t") || name.equals("t+") || name.equals("t-") || name.equals("tø")) {
      if (positionalArgs.size() > 2) {
        wikiBuilder.append(positionalArgs.get(2));
      }
      for (int i = 3; i < positionalArgs.size(); ++i) {
        wikiBuilder.append(i == 3 ? " {" : ",");
        wikiBuilder.append(positionalArgs.get(i));
        wikiBuilder.append(i == positionalArgs.size() - 1 ? "}" : "");
      }
      final String transliteration = namedArgs.remove("tr");
      if (transliteration != null) {
        wikiBuilder.append(" (").append(transliteration).append(")");
      }
      
    } else  if (name.equals("trreq")) {
      wikiBuilder.append("{{trreq}}");
      
    } else if (name.equals("qualifier")) {
      //assert positionalArgs.size() == 2 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs.toString();
      wikiBuilder.append(" (").append(positionalArgs.get(1)).append(")");
      
    } else if (useRemainingArgTemplates.contains(name)) {
      for (int i = 1; i < positionalArgs.size(); ++i) {
        if (i != 1) {
          wikiBuilder.append(", ");
        }
        wikiBuilder.append(positionalArgs.get(i));
      }
    } else if (ignoreTemplates.contains(name)) {
      // Do nothing.
      
    } else if (name.equals("initialism")) {
      assert positionalArgs.size() <= 2 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs;
      wikiBuilder.append("Initialism");
    } else if (name.equals("abbreviation")) {
      assert positionalArgs.size() <= 2 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs;
      wikiBuilder.append("Abbreviation");
    } else if (name.equals("acronym")) {
      assert positionalArgs.size() <= 2 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs;
      wikiBuilder.append("Acronym");
    } else {
      if (currentTranslationSense != null) {
        System.err.println("Unhandled template: " + positionalArgs.toString() + namedArgs);
      }
    }
  }

  @Override
  public void onText(String text) {
    if (wikiBuilder != null) {
      wikiBuilder.append(text);
      return;
    }
  }

  @Override
  public void onHeadingStart(int depth) {
    wikiBuilder = new StringBuilder();
    currentDepth = depth;
    if (currentPartOfSpeech != null && depth <= currentPartOfSpeech.depth) {
      currentPartOfSpeech = null;
      insidePartOfSpeech = false;
    }
    if (currentWord != null && depth <= currentWord.depth) {
      currentWord = null;
    }
    
    currentHeading = null;
  }
  
  @Override
  public void onHeadingEnd(int depth) {
    final String name = wikiBuilder.toString().trim();
    wikiBuilder = null;
    currentTranslationSense = null;
    currentHeading = name;
    
    final boolean lang0 = langPatterns[0].matcher(name).matches();
    final boolean lang1 = langPatterns[1].matcher(name).matches();
    if (name.equalsIgnoreCase("English") || lang0 || lang1 || name.equalsIgnoreCase("Translingual")) {
      currentWord = new WikiWord(title, depth);
      if (lang0 && lang1) {
        System.err.println("Word is indexed in both index1 and index2: " + title);
      }
      currentWord.language = name;
      currentWord.index = lang0 ? 0 : (lang1 ? 1 : -1);
      words.add(currentWord);
      return;
    }
    
    if (currentWord == null) {
      return;
    }
    
    if (currentPartOfSpeech != null && depth <= currentPartOfSpeech.depth) {
      currentPartOfSpeech = null;
    }
    
    insidePartOfSpeech = false;
    if (currentPartOfSpeech == null && partOfSpeechHeader.matcher(name).matches()) {
      currentPartOfSpeech = new WikiWord.PartOfSpeech(depth, name);
      currentWord.partsOfSpeech.add(currentPartOfSpeech);
      insidePartOfSpeech = true;
      return;
    }
    
    if (name.equals("Translations")) {
      if (currentWord == null || 
          !currentWord.language.equals("English") || 
          currentPartOfSpeech == null) {
        System.err.println("Unexpected Translations section: " + title);
        return;
      }
      currentTranslationSense = new WikiWord.TranslationSense();
    }
    
  }

  @Override
  public void onListItemStart(String header, int[] section) {
    wikiBuilder = new StringBuilder();
    if (currentWord != null) {
      currentWord.currentPronunciation = null;
    }
  }
  

  @Override
  public void onListItemEnd(String header, int[] section) {
    String item = wikiBuilder.toString().trim();
    final String oldItem = item;
    if (item.length() == 0) {
      return;
    }
    item = WikiParser.simpleParse(item);
    wikiBuilder = null;
        
    // Part of speech
    if (insidePartOfSpeech) {
      assert currentPartOfSpeech != null : title + item;
      if (header.equals("#") || 
          header.equals("##") || 
          header.equals("###") || 
          header.equals("####") || 
          header.equals(":#") || 
          header.equals("::") ||
          header.equals(":::*")) {
        // Definition.
        // :: should append, probably.
        currentPartOfSpeech.newMeaning().meaning = item;
        
      // Source
      } else if (header.equals("#*") ||
                 header.equals("##*") ||
                 header.equals("###*")) {
        currentPartOfSpeech.lastMeaning().newExample().source = item;
        
      // Example
      } else if (header.equals("#:") || 
                 header.equals("#*:") || 
                 header.equals("#:*") || 
                 header.equals("##:") || 
                 header.equals("##*:") || 
                 header.equals("#:*:") || 
                 header.equals("#:*#") ||
                 header.equals("#*:") ||
                 header.equals("*:") || 
                 header.equals("#:::") ||
                 header.equals("#**") ||
                 header.equals("#*:::") ||
                 header.equals("#:#") ||
                 header.equals(":::") ||
                 header.equals("##:*") ||
                 header.equals("###*:")) {
        StringUtil.appendLine(currentPartOfSpeech.lastMeaning().newExample().example, item);
        
      // Example in English
      } else if (header.equals("#::") || 
                 header.equals("#*::") || 
                 header.equals("#:**") ||
                 header.equals("#*#") ||
                 header.equals("##*::")) {
        StringUtil.appendLine(currentPartOfSpeech.lastMeaning().lastExample().exampleInEnglish, item);
        
      // Skip
      } else if (header.equals("*") ||
                 header.equals("**") ||
                 header.equals("***") || 
                 header.equals("*#") ||
                 header.equals(":") ||
                 header.equals("::*") ||
                 header.equals("#**") ||
                 header.equals(":*") ||
                 header.equals("#*:*") ||
                 header.equals("#*:**") || 
                 header.equals("#*:#") || 
                 header.equals("#*:*:") || 
                 header.equals("#*:*") || 
                 header.equals(";")) {
        // might have: * {{seeCites}}
        // * [[w:Arabic numerals|Arabic numerals]]: 2
        //assert item.trim().length() == 0;
        System.err.println("Skipping meaning: " + header + " " + item);
      } else {
        if (title.equals("Yellowknife")) {
          return;
        }
        System.err.println("Busted heading: " + title + "  "+ header + " " + item);
      }
      return;
    }
    // Part of speech
    
    // Translation
    if (currentTranslationSense != null) {
      if (item.indexOf("{{[trreq]{}}}") != -1) {
        return;
      }

      if (currentPartOfSpeech.translationSenses.isEmpty()) {
        currentPartOfSpeech.translationSenses.add(currentTranslationSense);
      }

      final int colonPos = item.indexOf(':');
      if (colonPos == -1) {
        System.err.println("Invalid translation: title=" + title +  ",  item=" + item);
        return;
      }
      final String lang = item.substring(0, colonPos);
      final String trans = item.substring(colonPos + 1).trim();
      for (int i = 0; i < 2; ++i) {
        if (langPatterns[i].matcher(lang).find()) {
          currentTranslationSense.translations.get(i).add(new Translation(lang, trans));
        }
      }
    } // Translation
  }

  @Override
  public void onNewLine() {
  }

  @Override
  public void onNewParagraph() {
  }

  // ----------------------------------------------------------------------
  
  @Override
  public void onComment(String text) {
  }

  @Override
  public void onFormatBold(boolean boldOn) {
  }

  @Override
  public void onFormatItalic(boolean italicOn) {
  }

  @Override
  public void onUnterminated(String start, String rest) {
    System.err.printf("OnUnterminated: %s %s %s\n", title, start, rest);
  }
  @Override
  public void onInvalidHeaderEnd(String rest) {
    throw new RuntimeException(rest);
  }

}

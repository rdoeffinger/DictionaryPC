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
import com.hughes.android.dictionary.parser.WikiWord.TranslationSection;

public class EnWiktionaryXmlParser extends org.xml.sax.helpers.DefaultHandler implements WikiCallback {
  
  static final Pattern partOfSpeechHeader = Pattern.compile(
      "Noun|Verb|Adjective|Adverb|Pronoun|Conjunction|Interjection|" +
      "Preposition|Proper noun|Article|Prepositional phrase|Acronym|" +
      "Abbreviation|Initialism|Contraction|Prefix|Suffix|Symbol|Letter|" +
      "Ligature|Idiom|Phrase|" +
      // These are @deprecated:
      "Noun form|Verb form|Adjective form|Nominal phrase|Noun phrase|" +
      "Verb phrase|Transitive verb|Intransitive verb|Reflexive verb");

  static final Pattern wikiMarkup =  Pattern.compile("\\[\\[|\\]\\]|''+");


  final DictionaryBuilder dict;
  
  final IndexBuilder[] indexBuilders;
  final Pattern[] langPatterns;

  StringBuilder titleBuilder;
  StringBuilder textBuilder;
  StringBuilder currentBuilder = null;

  public EnWiktionaryXmlParser(final DictionaryBuilder builder, final Pattern[] langPatterns, final int enIndexBuilder) {
    assert langPatterns.length == 2;
    this.dict = builder;
    this.indexBuilders = dict.indexBuilders.toArray(new IndexBuilder[0]);
    this.langPatterns = langPatterns;
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
  
  private void endPage() {
    title = titleBuilder.toString();
    currentDepth = 0;
    words.clear();
    currentHeading = null;
    WikiParser.parse(textBuilder.toString(), this);

   for (final WikiWord word : words) {
     System.out.println("\n" + title + ", " + word.language + ", pron=" + word.accentToPronunciation);
     if (word.partsOfSpeech.isEmpty() && title.indexOf(":") == -1) {
       System.err.println("Word with no POS: " + title);
     }
     for (final WikiWord.PartOfSpeech partOfSpeech : word.partsOfSpeech) {
       System.out.println("  pos: " + partOfSpeech.name);
       
       for (final TranslationSection translationSection : partOfSpeech.translationSections) {
         System.out.println("    sense: " + translationSection.sense);
         
       }
     }
   }
  }

  
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
  WikiWord.TranslationSection currentTranslationSection;
  
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
      "zh-tsp", "zh-zh-p"));
  static final Set<String> ignoreTemplates = new LinkedHashSet<String>(Arrays.asList(""));
  static final Set<String> grammarTemplates = new LinkedHashSet<String>(Arrays.asList("impf", "pf"));

  @Override
  public void onTemplate(final List<String> positionalArgs, final Map<String,String> namedArgs) {
    final String name = positionalArgs.get(0);

    // Pronunciation
    if (name.equals("a")) {
      // accent tag
      currentWord.currentPronunciation = new StringBuilder();
      currentWord.accentToPronunciation.put(positionalArgs.get(1), currentWord.currentPronunciation);
      return;
    }
    if (name.equals("IPA") || name.equals("SAMPA") || name.equals("enPR") || name.equals("rhymes")) {
      namedArgs.remove("lang");
      assert positionalArgs.size() >= 2 && namedArgs.isEmpty() : positionalArgs.toString() + namedArgs.toString(); 
      if (currentWord.currentPronunciation == null) {
        currentWord.currentPronunciation = new StringBuilder();
        currentWord.accentToPronunciation.put("", currentWord.currentPronunciation);
      }
      currentWord.currentPronunciation.append(name).append(": ");
      for (int i = 1; i < positionalArgs.size(); ++i) {
        if (i > 1) {
          currentWord.currentPronunciation.append(", ");
        }
        final String pron = wikiMarkup.matcher(positionalArgs.get(1)).replaceAll("");
        currentWord.currentPronunciation.append(pron).append("");
      }
      return;
    }
    if (name.equals("audio")) {
      return;
    }
    if ("Pronunciation".equals(currentHeading)) {
      System.err.println("Unhandled template: " + name);
    }

    // Translations
    if (name.equals("trans-top")) {
      assert positionalArgs.size() == 2 && namedArgs.isEmpty();
      currentTranslationSection = new WikiWord.TranslationSection();
      currentPartOfSpeech.translationSections.add(currentTranslationSection);
      if (positionalArgs.size() > 1) {
        currentTranslationSection.sense = positionalArgs.get(1);
      }
      return;
    }

    if (wikiBuilder == null) {
      return;
    }    
    if (name == "") {
    } else  if (name.equals("m") || name.equals("f") || name.equals("n") || name.equals("c")) {
      wikiBuilder.append("{").append(name).append("}");
    } else  if (name.equals("p")) {
      wikiBuilder.append("pl.");
    } else  if (name.equals("s")) {
      wikiBuilder.append("sg.");
    } else  if (grammarTemplates.contains(name)) {
      wikiBuilder.append(name).append(".");
    } else  if (name.equals("l")) {
      wikiBuilder.append(positionalArgs.size() >= 4 ? positionalArgs.get(3) : positionalArgs.get(2));
    } else if (name.equals("t") || name.equals("t+") || name.equals("t-") || name.equals("tø")) {
      if (positionalArgs.size() >= 2) {
        wikiBuilder.append(positionalArgs.get(1));
      }
      if (positionalArgs.size() >= 3) {
        wikiBuilder.append(" {").append(positionalArgs.get(1)).append("}");
      }
      final String transliteration = namedArgs.remove("tr");
      if (transliteration != null) {
        wikiBuilder.append(" (").append(transliteration).append(")");
      }
    } else  if (name.equals("trreq")) {
      wikiBuilder.append("{{trreq}}");
    } else if (name.equals("qualifier")) {
      wikiBuilder.append(" (").append(positionalArgs.get(1)).append(")");
    } else if (useRemainingArgTemplates.contains(name)) {
      for (int i = 1; i < positionalArgs.size(); ++i) {
        if (i != 1) {
          wikiBuilder.append(", ");
        }
        wikiBuilder.append(positionalArgs.get(i));
      }
    } else if (ignoreTemplates.contains(name)) {
    } else if (name.equals("initialism")) {
      wikiBuilder.append("Initialism");
    } else {
      if (currentTranslationSection != null) {
        System.err.println("Unhandled template: " + name);
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
    }
    if (currentWord != null && depth <= currentWord.depth) {
      currentWord = null;
    }
  }
  
  @Override
  public void onHeadingEnd(int depth) {
    final String name = wikiBuilder.toString().trim();
    wikiBuilder = null;
    currentTranslationSection = null;
    currentHeading = name;
    
    final boolean lang1 = langPatterns[0].matcher(name).matches();
    final boolean lang2 = langPatterns[1].matcher(name).matches();
    if (name.equalsIgnoreCase("English") || lang1 || lang2) {
      currentWord = new WikiWord(depth);
      currentWord.language = name;
      currentWord.isLang1 = lang1;
      currentWord.isLang2 = lang2;
      words.add(currentWord);
      return;
    }
    
    if (currentWord == null) {
      return;
    }
    
    if (partOfSpeechHeader.matcher(name).matches()) {
      currentPartOfSpeech = new WikiWord.PartOfSpeech(depth, name);
      currentWord.partsOfSpeech.add(currentPartOfSpeech);
      return;
    }
    
    if (name.equals("Translations")) {
      if (currentWord == null || 
          !currentWord.language.equals("English") || 
          currentPartOfSpeech == null) {
        System.out.println("Unexpected Translations section: " + title);
        return;
      }
      currentTranslationSection = new WikiWord.TranslationSection();
      currentPartOfSpeech.translationSections.add(currentTranslationSection);
    }
    
    if (name.equals("Translations")) {
      if (currentWord == null || 
          !currentWord.language.equals("English") || 
          currentPartOfSpeech == null) {
        System.out.println("Unexpected Translations section: " + title);
        return;
      }
      currentTranslationSection = new WikiWord.TranslationSection();
      currentPartOfSpeech.translationSections.add(currentTranslationSection);
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
    final String item = wikiBuilder.toString();
    wikiBuilder = null;
    
    if (item.indexOf("{{trreq}}") != -1) {
      return;
    }
    
    if (currentTranslationSection != null) {
      final int colonPos = item.indexOf(':');
      if (colonPos == -1) {
        System.err.println("Invalid translation: " + item);
        return;
      }
      final String lang = item.substring(0, colonPos);
      final String trans = item.substring(colonPos + 1);
      for (int i = 0; i < 2; ++i) {
        if (langPatterns[i].matcher(lang).find()) {
          currentTranslationSection.translations.get(i).add(trans);
        }
      }
    }
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
    throw new RuntimeException(rest);
  }
  @Override
  public void onInvalidHeaderEnd(String rest) {
    throw new RuntimeException(rest);
  }

}

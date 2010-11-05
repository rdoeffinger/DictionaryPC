package com.hughes.android.dictionary.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.hughes.android.dictionary.engine.DictionaryBuilder;
import com.hughes.android.dictionary.engine.IndexBuilder;

public class EnWiktionaryXmlParser extends org.xml.sax.helpers.DefaultHandler implements WikiCallback {

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
    WikiParser.parse(textBuilder.toString(), this);
  }
  
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
  int currentDepth;
  final List<WikiWord> words = new ArrayList<WikiWord>();
  WikiWord currentWord;
  WikiWord.PartOfSpeech currentPartOfSpeech;
  WikiWord.TranslationSection currentTranslationSection;
  
  StringBuilder wikiBuilder = null;
  
  // ------------------------------------------------------------------------

  @Override
  public void onWikiLink(String[] args) {
    if (wikiBuilder != null) {
      wikiBuilder.append(args[args.length - 1]);
    }
  }

  @Override
  public void onTemplate(String[][] args) {
    final String name = args[0][1];
    if (name == "") {
      
    } else {
      //System.out.println("Unhandled template: " + name);
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
  
  final Pattern partOfSpeechHeader = Pattern.compile(
      "Noun|Verb|Adjective|Adverb|Pronoun|Conjunction|Interjection|" +
      "Preposition|Proper noun|Article|Prepositional phrase|Acronym|" +
      "Abbreviation|Initialism|Contraction|Prefix|Suffix|Symbol|Letter|" +
      "Ligature|Idiom|Phrase|" +
      // These are @deprecated:
      "Noun form|Verb form|Adjective form|Nominal phrase|Noun phrase|" +
      "Verb phrase|Transitive verb|Intransitive verb|Reflexive verb");

  @Override
  public void onHeadingEnd(int depth) {
    final String name = wikiBuilder.toString().trim();
    wikiBuilder = null;
    
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
      currentPartOfSpeech = new WikiWord.PartOfSpeech(depth);
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
    } else {
      currentTranslationSection = null;
    }
  }

  @Override
  public void onListItemStart(String header, int[] section) {
    wikiBuilder = new StringBuilder();
  }
  

  @Override
  public void onListItemEnd(String header, int[] section) {
    final String item = wikiBuilder.toString();
    wikiBuilder = null;
    
    if (currentTranslationSection != null) {
      final int colonPos = item.indexOf(':');
      if (colonPos == -1) {
        System.out.println("Invalid translation: " + item);
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
  
  public void onTransTrop(final String[][] args) {
    currentTranslationSection = new WikiWord.TranslationSection();
    currentPartOfSpeech.translationSections.add(currentTranslationSection);
    
    if (args.length > 1) {
      currentTranslationSection.sense = args[1][1];
    }
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

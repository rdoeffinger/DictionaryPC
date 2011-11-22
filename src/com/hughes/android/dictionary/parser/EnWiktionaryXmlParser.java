package com.hughes.android.dictionary.parser;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.DictionaryBuilder;
import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;

public class EnWiktionaryXmlParser {
  
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

  final IndexBuilder enIndexBuilder;
  final IndexBuilder otherIndexBuilder;
  final Pattern langPattern;
  final Pattern langCodePattern;
  final boolean swap;

  public EnWiktionaryXmlParser(final IndexBuilder enIndexBuilder, final IndexBuilder otherIndexBuilder, final Pattern langPattern, final Pattern langCodePattern, final boolean swap) {
    this.enIndexBuilder = enIndexBuilder;
    this.otherIndexBuilder = otherIndexBuilder;
    this.langPattern = langPattern;
    this.langCodePattern = langCodePattern;
    this.swap = swap;
  }

  
  public void parse(final File file, final int pageLimit) throws IOException {
    int pageCount = 0;
    final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    while (true) {
      if (pageLimit >= 0 && pageCount >= pageLimit) {
        return;
      }
      
      final String title;
      try {
        title = dis.readUTF();
      } catch (EOFException e) {
        dis.close();
        return;
      }
      final String heading = dis.readUTF();
      final int bytesLength = dis.readInt();
      final byte[] bytes = new byte[bytesLength];
      dis.readFully(bytes);
      final String text = new String(bytes, "UTF8");
      
      parseSection(title, heading, text);

      ++pageCount;
      if (pageCount % 1000 == 0) {
        System.out.println("pageCount=" + pageCount);
      }
    }
  }
  
  private void parseSection(final String title, final String heading, final String text) {
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
    
    if (heading.replaceAll("=", "").equals("English")) {
      doEnglishWord(title, text);
    } else {
      doForeignWord(title, text);
    }
        
  }  // endPage()
  
  // -------------------------------------------------------------------------
  
  String pos = null;
  int posDepth = -1;

  private void doEnglishWord(String title, String text) {
    final WikiLineReader wikiLineReader = new WikiLineReader(text);
    String line;
    while ((line = wikiLineReader.readLine()) != null) {
      final WikiHeading wikiHeading = WikiHeading.getHeading(line);
      if (wikiHeading != null) {
        
        if (wikiHeading.depth <= posDepth) {
          pos = null;
          posDepth = -1;
        }
        
        if (partOfSpeechHeader.matcher(wikiHeading.name).matches()) {
          posDepth = wikiHeading.depth;
          pos = wikiHeading.name;
        } else if (wikiHeading.name.equals("Translations")) {
          doTranslations(title, wikiLineReader);
        } else if (wikiHeading.name.equals("Pronunciation")) {
          //doPronunciation(wikiLineReader);
        }
      }
    }
  }


  private static Set<String> encodings = new LinkedHashSet<String>(Arrays.asList("zh-ts",
      "sd-Arab", "ku-Arab", "Arab", "unicode", "Laoo", "ur-Arab", "Thai", 
      "fa-Arab", "Khmr", "zh-tsp", "Cyrl", "IPAchar", "ug-Arab", "ko-inline", 
      "Jpan", "Kore", "Hebr", "rfscript", "Beng", "Mong", "Knda", "Cyrs",
      "yue-tsj", "Mlym", "Tfng", "Grek", "yue-yue-j"));
  
  private void doTranslations(final String title, final WikiLineReader wikiLineReader) {
    String line;
    String sense = null;
    boolean done = false;
    while ((line = wikiLineReader.readLine()) != null) {
      if (WikiHeading.getHeading(line) != null) {
        wikiLineReader.stuffLine(line);
        return;
      }
      if (done) {
        continue;
      }
      
      // Check whether we care about this line:
      
      //line = WikiLineReader.removeSquareBrackets(line);
      
      if (line.startsWith("{{")) {
        
        WikiFunction wikiFunction;
        while ((wikiFunction = WikiFunction.getFunction(line)) != null) {
          if (wikiFunction.name.equals("trans-top")) {
            sense = null;
            if (wikiFunction.args.size() >= 1) {
              sense = wikiFunction.args.get(0);
              //System.out.println("Sense: " + sense);
            }
          } else if (wikiFunction.name.equals("trans-bottom")) {
            sense = null;
          } else if (wikiFunction.name.equals("trans-mid")) {
          } else if (wikiFunction.name.equals("trans-see")) {
          } else if (wikiFunction.name.startsWith("checktrans")) {
            done = true;
          } else {
            System.err.println("Unexpected translation wikifunction: " + line + ", title=" + title);
          }
          line = wikiFunction.replaceWith(line, "");
          
        }
        
      } else if (line.startsWith("*")) {
        // This line could produce an output...
        
        // First strip the language and check whether it matches.
        // And hold onto it for sub-lines.
        final int colonIndex = line.indexOf(":");
        if (colonIndex == -1) {
          continue;
        }
        
        final String lang = line.substring(0, colonIndex);
        if (!this.langPattern.matcher(lang).find()) {
          continue;
        }
        
        String rest = line.substring(colonIndex + 1).trim();
        doTranslationLine(line, title, sense, rest);
        
      } else if (line.equals("")) {
      } else if (line.startsWith(":")) {
      } else if (line.startsWith("[[") && line.endsWith("]]")) {
      } else if (line.startsWith("''See''")) {
      } else if (line.startsWith("''")) {
      } else if (line.equals("----")) {
      } else {
        System.err.println("Unexpected translation line: " + line + ", title=" + title);
      }
      
    }
    
  }
  
  private void doTranslationLine(final String line, final String title, final String sense, String rest) {

    // Good chance we'll actually file this one...
    final PairEntry pairEntry = new PairEntry();
    final IndexedEntry indexedEntry = new IndexedEntry(pairEntry);

    final StringBuilder otherText = new StringBuilder();
    
    WikiFunction wikiFunction;
    while ((wikiFunction = WikiFunction.getFunction(rest)) != null) {
      if (wikiFunction.start > 0) {
        String plainText = rest.substring(0, wikiFunction.start); 
        otherText.append("").append(plainText);
        otherIndexBuilder.addEntryWithString(indexedEntry, plainText, EntryTypeName.WIKTIONARY_OTHER_TEXT);
      }
      rest = rest.substring(wikiFunction.end);
      
      if (wikiFunction.name.equals("t") || wikiFunction.name.equals("t+") || wikiFunction.name.equals("t-") || wikiFunction.name.equals("tø")) {
        if (wikiFunction.args.size() < 2) {
          System.err.println("{{t}} with too few args: " + line + ", title=" + title);
          continue;
        }
        final String langCode = wikiFunction.getArg(0);
        if (this.langCodePattern.matcher(langCode).matches()) {
          final String word = wikiFunction.getArg(1);
          final String gender = wikiFunction.getArg(2);
          final String transliteration = wikiFunction.getNamedArg("tr");
          if (otherText.length() > 0) {
            otherText.append("");
          }
          otherText.append(word);
          otherIndexBuilder.addEntryWithString(indexedEntry, word, EntryTypeName.WIKTIONARY_TITLE_SINGLE, EntryTypeName.WIKTIONARY_TITLE_MULTI);
          if (gender != null) {
            otherText.append(String.format(" {%s}", gender));
          }
          if (transliteration != null) {
            otherText.append(String.format(" (tr. %s)", transliteration));
            otherIndexBuilder.addEntryWithString(indexedEntry, transliteration, EntryTypeName.WIKTIONARY_TRANSLITERATION);
          }
        }
      } else if (wikiFunction.name.equals("qualifier")) {
        String qualifier = wikiFunction.getArg(0);
        if (!wikiFunction.namedArgs.isEmpty() || wikiFunction.args.size() > 1) {
          System.err.println("weird qualifier: " + line);
        }
        otherText.append("(").append(qualifier).append(")");
      } else if (encodings.contains(wikiFunction.name)) {
        otherText.append("").append(wikiFunction.getArg(0));
        otherIndexBuilder.addEntryWithString(indexedEntry, wikiFunction.getArg(0), EntryTypeName.WIKTIONARY_OTHER_TEXT);
      } else if (wikiFunction.name.equals("m") || wikiFunction.name.equals("f") || wikiFunction.name.equals("n")) {
        otherText.append("{");
        otherText.append(wikiFunction.name);
        for (int i = 0; i < wikiFunction.args.size(); ++i) {
          otherText.append("|").append(wikiFunction.getArg(i));
        }
        otherText.append("}");
      } else if (wikiFunction.name.equals("g")) {
        otherText.append("{g}");
      } else if (wikiFunction.name.equals("l")) {
        // encodes text in various langs.
        // lang is arg 0.
        otherText.append("").append(wikiFunction.getArg(1));
        otherIndexBuilder.addEntryWithString(indexedEntry, wikiFunction.getArg(1), EntryTypeName.WIKTIONARY_OTHER_TEXT);
        // TODO: transliteration
      } else if (wikiFunction.name.equals("term")) {
        // cross-reference to another dictionary
        otherText.append("").append(wikiFunction.getArg(0));
        otherIndexBuilder.addEntryWithString(indexedEntry, wikiFunction.getArg(0), EntryTypeName.WIKTIONARY_OTHER_TEXT);
        // TODO: transliteration
      } else if (wikiFunction.name.equals("italbrac") || wikiFunction.name.equals("gloss")) {
        // TODO: put this text aside to use it.
        otherText.append("[").append(wikiFunction.getArg(0)).append("]");
        otherIndexBuilder.addEntryWithString(indexedEntry, wikiFunction.getArg(0), EntryTypeName.WIKTIONARY_OTHER_TEXT);
      } else if (wikiFunction.name.equals("ttbc")) {
      } else if (wikiFunction.name.equals("trreq")) {
      } else if (wikiFunction.name.equals("not used")) {
        otherText.append("(not used)");
      } else if (wikiFunction.name.equals("t-image")) {
        // American sign language
      } else if (wikiFunction.args.isEmpty() && wikiFunction.namedArgs.isEmpty()) {
        otherText.append("{UNK. FUNC.: ").append(wikiFunction.name).append("}");
      } else {
        System.err.println("Unexpected t+- wikifunction: " + line + ", title=" + title);
      }
    }
    String plainText = rest; 
    otherText.append("").append(plainText);
    otherIndexBuilder.addEntryWithString(indexedEntry, plainText, EntryTypeName.WIKTIONARY_OTHER_TEXT);
    
    StringBuilder englishText = new StringBuilder();
    
    englishText.append(title);
    if (sense != null) {
      englishText.append(" (").append(sense).append(")");
      enIndexBuilder.addEntryWithString(indexedEntry, sense, EntryTypeName.WIKTIONARY_TRANSLATION_SENSE, EntryTypeName.WIKTIONARY_TRANSLATION_SENSE);
    }
    if (pos != null) {
      englishText.append(" (").append(pos.toLowerCase()).append(")");
    }
    enIndexBuilder.addEntryWithString(indexedEntry, title, EntryTypeName.WIKTIONARY_TITLE_SINGLE, EntryTypeName.WIKTIONARY_TITLE_MULTI);
    
    final Pair pair = new Pair(englishText.toString(), WikiParser.simpleParse(otherText.toString()), swap);
    pairEntry.pairs.add(pair);
    assert (pairsAdded.add(pair.toString()));
    if (pair.toString().equals("libero {m} :: free (adjective)")) {
      System.out.println();
    }

  }
  
  Set<String> pairsAdded = new LinkedHashSet<String>();
  
  // -------------------------------------------------------------------------
  
  private void doForeignWord(String title, String text) {
    final WikiLineReader wikiLineReader = new WikiLineReader(text);
    String line;
    while ((line = wikiLineReader.readLine()) != null) {
      final WikiHeading wikiHeading = WikiHeading.getHeading(line);
      if (wikiHeading != null) {
        if (wikiHeading.name.equals("Translations")) {
          System.err.println("Translations not in English section: " + title);
        } else if (wikiHeading.name.equals("Pronunciation")) {
          //doPronunciation(wikiLineReader);
        } else if (partOfSpeechHeader.matcher(wikiHeading.name).matches()) {
          doPartOfSpeech(title, wikiHeading, wikiLineReader);
        }
      }
    }
  }


  private void doPartOfSpeech(String title, final WikiHeading posHeading, WikiLineReader wikiLineReader) {
    String line;
    System.out.println("***" + title);
    System.out.println(posHeading.name);
    while ((line = wikiLineReader.readLine()) != null) {
      WikiHeading heading = WikiHeading.getHeading(line);
      if (heading != null) {
        if (heading.depth <= posHeading.depth) {
          wikiLineReader.stuffLine(line);
          return;
        }
      }
      System.out.println(line);
      
      
    }
  }

  
}

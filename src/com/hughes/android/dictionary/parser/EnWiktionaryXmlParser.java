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
import com.hughes.android.dictionary.engine.IndexBuilder;

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

  final DictionaryBuilder dictBuilder;
  
  final IndexBuilder[] indexBuilders;
  final Pattern langPattern;
  final Pattern langCodePattern;
  final int enIndexBuilder;

  public EnWiktionaryXmlParser(final DictionaryBuilder dictBuilder, final Pattern langPattern, final Pattern langCodePattern, final int enIndexBuilder) {
    this.dictBuilder = dictBuilder;
    this.indexBuilders = dictBuilder.indexBuilders.toArray(new IndexBuilder[0]);
    this.langPattern = langPattern;
    this.langCodePattern = langCodePattern;
    this.enIndexBuilder = enIndexBuilder;
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
      //doForeignWord(title, text);
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
            if (wikiFunction.args.size() >= 2) {
              sense = wikiFunction.args.get(1);
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
        
        String rest = line.substring(colonIndex + 1);
        final StringBuilder lineText = new StringBuilder();
        
        boolean ttbc = false;
        WikiFunction wikiFunction;
        while ((wikiFunction = WikiFunction.getFunction(line)) != null) {
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
            }
          } else if (wikiFunction.name.equals("qualifier")) {
            String qualifier = wikiFunction.getArg(0);
          } else if (encodings.contains(wikiFunction.name)) {
            rest = wikiFunction.replaceWith(rest, wikiFunction.getArg(0));
            wikiFunction = null;
          } else if (wikiFunction.name.equals("m") || wikiFunction.name.equals("f") || wikiFunction.name.equals("n")) {
            String gender = wikiFunction.name;
            for (int i = 0; i < wikiFunction.args.size(); ++i) {
              gender += "|" + wikiFunction.getArg(i);
            }
            rest = wikiFunction.replaceWith(rest, "{" + wikiFunction.name + "}");
            wikiFunction = null;
          } else if (wikiFunction.name.equals("g")) {
            rest = wikiFunction.replaceWith(rest, "{g}");
            wikiFunction = null;
          } else if (wikiFunction.name.equals("l")) {
            // encodes text in various langs.
            rest = wikiFunction.replaceWith(rest, wikiFunction.getArg(1));
            // TODO: transliteration
            wikiFunction = null;
          } else if (wikiFunction.name.equals("term")) {
            // cross-reference to another dictionary
            rest = wikiFunction.replaceWith(rest, wikiFunction.getArg(0));
            // TODO: transliteration
            wikiFunction = null;
          } else if (wikiFunction.name.equals("italbrac") || wikiFunction.name.equals("gloss")) {
            // TODO: put this text aside to use it.
            rest = wikiFunction.replaceWith(rest, "[" + wikiFunction.getArg(0) + "]");
            wikiFunction = null;
          } else if (wikiFunction.name.equals("ttbc")) {
            ttbc = true;
          } else if (wikiFunction.name.equals("trreq")) {
          } else if (wikiFunction.name.equals("not used")) {
            rest = wikiFunction.replaceWith(rest, "[not used]");
            wikiFunction = null;
          } else if (wikiFunction.name.equals("t-image")) {
            // American sign language
          } else if (wikiFunction.args.isEmpty() && wikiFunction.namedArgs.isEmpty()) {
            rest = wikiFunction.replaceWith(rest, "{" + wikiFunction.name + "}");
            wikiFunction = null;
          } else {
            System.err.println("Unexpected t+- wikifunction: " + line + ", title=" + title);
          }
          if (wikiFunction != null) {
            rest = wikiFunction.replaceWith(rest, "");
          }
        }
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
          
        }
      }
    }
  }

  
}

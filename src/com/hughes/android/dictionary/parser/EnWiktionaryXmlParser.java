// Copyright 2011 Google Inc. All Rights Reserved.
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

package com.hughes.android.dictionary.parser;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;

public class EnWiktionaryXmlParser {
  
  // TODO: look for {{ and [[ and <adf> <!-- in output.
  // TODO: process {{ttbc}} lines
  
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
  
  private void parseSection(final String title, String heading, final String text) {
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
    
    heading = heading.replaceAll("=", "").trim(); 
    if (heading.equals("English")) {
      doEnglishWord(title, text);
    } else if (langPattern.matcher(heading).matches()){
      doForeignWord(title, text);
    }
        
  }  // endPage()
  
  // -------------------------------------------------------------------------
  
  String pos = null;
  int posDepth = -1;

  private void doEnglishWord(String title, String text) {
    final WikiTokenizer wikiTokenizer = new WikiTokenizer(text);
    while (wikiTokenizer.nextToken() != null) {
      
      if (wikiTokenizer.isHeading()) {
        final String headerName = wikiTokenizer.headingWikiText();
        
        if (wikiTokenizer.headingDepth() <= posDepth) {
          pos = null;
          posDepth = -1;
        }
        
        if (partOfSpeechHeader.matcher(headerName).matches()) {
          posDepth = wikiTokenizer.headingDepth();
          pos = wikiTokenizer.headingWikiText();
        } else if (headerName.equals("Translations")) {
          doTranslations(title, wikiTokenizer);
        } else if (headerName.equals("Pronunciation")) {
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
  
  private void doTranslations(final String title, final WikiTokenizer wikiTokenizer) {
    String sense = null;
    boolean done = false;
    while (wikiTokenizer.nextToken() != null) {
      if (wikiTokenizer.isHeading()) {
        wikiTokenizer.returnToLineStart();
        return;
      }
      if (done) {
        continue;
      }
      
      // Check whether we care about this line:
      
      //line = WikiLineReader.removeSquareBrackets(line);
      
      if (wikiTokenizer.isFunction()) {
        final String functionName = wikiTokenizer.functionName();
        final List<String> positionArgs = wikiTokenizer.functionPositionArgs();
        
        if (functionName.equals("trans-top")) {
          sense = null;
          if (wikiTokenizer.functionPositionArgs().size() >= 1) {
            sense = positionArgs.get(0);
            // TODO: could emphasize words in [[brackets]] inside sense.
            sense = WikiTokenizer.toPlainText(sense);
            //System.out.println("Sense: " + sense);
          }
        } else if (functionName.equals("trans-bottom")) {
          sense = null;
        } else if (functionName.equals("trans-mid")) {
        } else if (functionName.equals("trans-see")) {
        } else if (functionName.startsWith("checktrans")) {
          //TODO: Check this: done = true;
        } else {
          System.err.println("Unexpected translation wikifunction: " + wikiTokenizer.token() + ", title=" + title);
        }
      } else if (wikiTokenizer.isListItem() && wikiTokenizer.listItemPrefix().startsWith("*")) {
        final String line = wikiTokenizer.listItemWikiText();
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
        
      } else if (wikiTokenizer.remainderStartsWith("''See''")) {
        wikiTokenizer.nextLine();
        System.out.println("Skipping line: " + wikiTokenizer.token());
      } else if (wikiTokenizer.isWikiLink()) {
        final String wikiLink = wikiTokenizer.wikiLinkText();
        if (wikiLink.contains(":") && wikiLink.contains(title)) {
        } else if (wikiLink.contains("Category:")) {
        } else  {
          System.err.println("Unexpected wikiLink: " + wikiTokenizer.token() + ", title=" + title);
        }
      } else if (wikiTokenizer.isNewline() || wikiTokenizer.isMarkup() || wikiTokenizer.isComment()) {
      } else {
        final String token = wikiTokenizer.token();
        if (token.equals("----")) { 
        } else {
          System.err.println("Unexpected translation token: " + wikiTokenizer.token() + ", title=" + title);
        }
      }
      
    }
  }
  
  private static <T> T get(final List<T> list, final int index) {
    return index < list.size() ? list.get(index) : null;
  }
  
  private void doTranslationLine(final String line, final String title, final String sense, final String rest) {
    // Good chance we'll actually file this one...
    final PairEntry pairEntry = new PairEntry();
    final IndexedEntry indexedEntry = new IndexedEntry(pairEntry);

    final StringBuilder otherText = new StringBuilder();
    final WikiTokenizer wikiTokenizer = new WikiTokenizer(rest);
    while (wikiTokenizer.nextToken() != null) {
      
      if (wikiTokenizer.isPlainText()) {
        final String plainText = wikiTokenizer.token(); 
        otherText.append("").append(plainText);
        otherIndexBuilder.addEntryWithString(indexedEntry, plainText, EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
        
      } else if (wikiTokenizer.isWikiLink()) {
        final String plainText = wikiTokenizer.wikiLinkText(); 
        otherText.append("").append(plainText);
        otherIndexBuilder.addEntryWithString(indexedEntry, plainText, EntryTypeName.WIKTIONARY_TRANSLATION_WIKI_TEXT);
        
      } else if (wikiTokenizer.isFunction()) {
        final String functionName = wikiTokenizer.functionName();
        final List<String> args = wikiTokenizer.functionPositionArgs();
        final Map<String,String> namedArgs = wikiTokenizer.functionNamedArgs();
        
        if (functionName.equals("t") || functionName.equals("t+") || functionName.equals("t-") || functionName.equals("tø")) {
          if (args.size() < 2) {
            System.err.println("{{t}} with too few args: " + line + ", title=" + title);
            continue;
          }
          final String langCode = get(args, 0);
          if (this.langCodePattern.matcher(langCode).matches()) {
            final String word = get(args, 1);
            final String gender = get(args, 2);
            final String transliteration = namedArgs.get("tr");
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
        } else if (functionName.equals("qualifier")) {
          String qualifier = args.get(0);
          if (!namedArgs.isEmpty() || args.size() > 1) {
            System.err.println("weird qualifier: " + line);
          }
          otherText.append("(").append(qualifier).append(")");
        } else if (encodings.contains(functionName)) {
          otherText.append("").append(args.get(0));
          otherIndexBuilder.addEntryWithString(indexedEntry, args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
        } else if (functionName.equals("m") || functionName.equals("f") || functionName.equals("n") || functionName.equals("p")) {
          otherText.append("{");
          otherText.append(functionName);
          for (int i = 0; i < args.size(); ++i) {
            otherText.append("|").append(args.get(i));
          }
          otherText.append("}");
        } else if (functionName.equals("g")) {
          otherText.append("{g}");
        } else if (functionName.equals("l")) {
          // encodes text in various langs.
          // lang is arg 0.
          otherText.append("").append(args.get(1));
          otherIndexBuilder.addEntryWithString(indexedEntry, args.get(1), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
          // TODO: transliteration
        } else if (functionName.equals("term")) {
          // cross-reference to another dictionary
          otherText.append("").append(args.get(0));
          otherIndexBuilder.addEntryWithString(indexedEntry, args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
          // TODO: transliteration
        } else if (functionName.equals("italbrac") || functionName.equals("gloss")) {
          // TODO: put this text aside to use it.
          otherText.append("[").append(args.get(0)).append("]");
          otherIndexBuilder.addEntryWithString(indexedEntry, args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
        } else if (functionName.equals("ttbc")) {
        } else if (functionName.equals("trreq")) {
        } else if (functionName.equals("not used")) {
          otherText.append("(not used)");
        } else if (functionName.equals("t-image")) {
          // American sign language
        } else if (args.isEmpty() && namedArgs.isEmpty()) {
          otherText.append("{UNK. FUNC.: ").append(functionName).append("}");
        } else {
          System.err.println("Unexpected t+- wikifunction: " + line + ", title=" + title);
        }
        
      } else if (wikiTokenizer.isNewline()) {
        assert false;
      } else if (wikiTokenizer.isComment()) {
      } else if (wikiTokenizer.isMarkup()) {
      } else {
        System.err.println("Bad translation token: " + wikiTokenizer.token());
      }
      
    }
    
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
    
    final Pair pair = new Pair(trim(englishText.toString()), trim(otherText.toString()), swap);
    pairEntry.pairs.add(pair);
    assert (pairsAdded.add(pair.toString()));
    if (pair.toString().equals("libero {m} :: free (adjective)")) {
      System.out.println();
    }

  }
  
  static final Pattern whitespace = Pattern.compile("\\s+");

  static String trim(final String s) {
    return whitespace.matcher(s).replaceAll(" ").trim();
  }
  
  Set<String> pairsAdded = new LinkedHashSet<String>();
  
  // -------------------------------------------------------------------------
  
  private void doForeignWord(final String title, final String text) {
    final WikiTokenizer wikiTokenizer = new WikiTokenizer(text);
    while (wikiTokenizer.nextToken() != null) {
      if (wikiTokenizer.isHeading()) {
        final String headingName = wikiTokenizer.headingWikiText();
        if (headingName.equals("Translations")) {
          System.err.println("Translations not in English section: " + title);
        } else if (headingName.equals("Pronunciation")) {
          //doPronunciation(wikiLineReader);
        } else if (partOfSpeechHeader.matcher(headingName).matches()) {
          doPartOfSpeech(title, headingName, wikiTokenizer.headingDepth(), wikiTokenizer);
        }
      } else {
      }
    }
  }


  private void doPartOfSpeech(String title, final String posHeading, final int posDepth, WikiTokenizer wikiTokenizer) {
    System.out.println("***" + title);
    System.out.println(posHeading);
    //final StringBuilder foreignBuilder = new StringBuilder();
    
    String side = null;
    Collection<String> forms = Collections.emptyList();
    
    int currentHeadingDepth = posDepth;
    while (wikiTokenizer.nextToken() != null) {
      if (wikiTokenizer.isHeading()) {
        currentHeadingDepth = wikiTokenizer.headingDepth();
        
        if (currentHeadingDepth <= posDepth) {
          wikiTokenizer.returnToLineStart();
          return;
        }
      }
      
      if (currentHeadingDepth > posDepth) {
        // TODO
        continue;
      }
      
      if (wikiTokenizer.isFunction()) {
        final String name = wikiTokenizer.functionName();
        final List<String> args = wikiTokenizer.functionPositionArgs();
        final Map<String,String> namedArgs = wikiTokenizer.functionNamedArgs();
        // First line is generally a repeat of the title with some extra information.
        // We need to build up the left side (foreign text, tokens) separately from the
        // right side (English).  The left-side may get paired with multiple right sides.
        // The left side should get filed under every form of the word in question (singular, plural).
        
        // For verbs, the conjugation comes later on in a deeper section.
        // Ideally, we'd want to file every English entry with the verb
        // under every verb form coming from the conjugation.
        // Ie. under "fa": see: "make :: fare" and "do :: fare"
        // But then where should we put the conjugation table?
        // I think just under fare.  But then we need a way to link to the entry (actually the row, since entries doesn't show up!)
        // for the conjugation table from "fa".
        // Would like to be able to link to a lang#token.
        if (name.equals("it-noun")) {
          assert forms.isEmpty();
          final String base = get(args, 0);
          final String gender = get(args, 1);
          final String singular = base + get(args, 2);
          final String plural = base + get(args, 3);
          side = String.format("%s {%s}, %s {pl}", singular, gender, plural, plural);
          forms = Arrays.asList(singular, plural);
        } else if (name.equals("it-proper noun")) {
          // TODO
        } else if (name.equals("it-adj")) {
          // TODO
        } else if (name.startsWith("it-conj")) {
          if (name.equals("it-conj-are")) {
            itConjAre(args, namedArgs);
          } else if (name.equals("it-conj-ere")) {
          } else if (name.equals("it-conj-ire")) {
          } else {
            System.err.println("Unknown conjugation: " + wikiTokenizer.token());
          }
          
        } else {
          System.err.println("Unknown function: " + wikiTokenizer.token());
        }
        
      } else if (wikiTokenizer.isListItem()) {
        handleForeignListItem(side != null ? side : title, title, forms, wikiTokenizer);

      } else if (wikiTokenizer.isWikiLink()) {

      } else {
      }
      
    }
  }

  private void itConjAre(List<String> args, Map<String, String> namedArgs) {
    final String base = args.get(0);
    final String aux = args.get(1);
    
    putIfMissing(namedArgs, "inf", base + "are");
    putIfMissing(namedArgs, "aux", aux);
    putIfMissing(namedArgs, "ger", base + "ando");
    putIfMissing(namedArgs, "presp", base + "ante");
    putIfMissing(namedArgs, "pastp", base + "ato");
    // Present
    putIfMissing(namedArgs, "pres1s", base + "o");
    putIfMissing(namedArgs, "pres2s", base + "i");
    putIfMissing(namedArgs, "pres3s", base + "a");
    putIfMissing(namedArgs, "pres1p", base + "iamo");
    putIfMissing(namedArgs, "pres2p", base + "ate");
    putIfMissing(namedArgs, "pres3p", base + "ano");
    // Imperfect
    putIfMissing(namedArgs, "imperf1s", base + "avo");
    putIfMissing(namedArgs, "imperf2s", base + "avi");
    putIfMissing(namedArgs, "imperf3s", base + "ava");
    putIfMissing(namedArgs, "imperf1p", base + "avamo");
    putIfMissing(namedArgs, "imperf2p", base + "avate");
    putIfMissing(namedArgs, "imperf3p", base + "avano");
    // Passato remoto
    putIfMissing(namedArgs, "prem1s", base + "ai");
    putIfMissing(namedArgs, "prem2s", base + "asti");
    putIfMissing(namedArgs, "prem3s", base + "ò");
    putIfMissing(namedArgs, "prem1p", base + "ammo");
    putIfMissing(namedArgs, "prem2p", base + "aste");
    putIfMissing(namedArgs, "prem3p", base + "arono");
    // Future
    putIfMissing(namedArgs, "fut1s", base + "erò");
    putIfMissing(namedArgs, "fut2s", base + "erai");
    putIfMissing(namedArgs, "fut3s", base + "erà");
    putIfMissing(namedArgs, "fut1p", base + "eremo");
    putIfMissing(namedArgs, "fut2p", base + "erete");
    putIfMissing(namedArgs, "fut3p", base + "eranno");
    // Conditional
    putIfMissing(namedArgs, "cond1s", base + "erei");
    putIfMissing(namedArgs, "cond2s", base + "eresti");
    putIfMissing(namedArgs, "cond3s", base + "erebbe");
    putIfMissing(namedArgs, "cond1p", base + "eremmo");
    putIfMissing(namedArgs, "cond2p", base + "ereste");
    putIfMissing(namedArgs, "cond3p", base + "erebbero");
    // Subjunctive / congiuntivo
    putIfMissing(namedArgs, "sub123s", base + "i");
    putIfMissing(namedArgs, "sub1p", base + "iamo");
    putIfMissing(namedArgs, "sub2p", base + "iate");
    putIfMissing(namedArgs, "sub3p", base + "ino");
    // Imperfect subjunctive
    putIfMissing(namedArgs, "impsub12s", base + "assi");
    putIfMissing(namedArgs, "impsub3s", base + "asse");
    putIfMissing(namedArgs, "impsub1p", base + "assimo");
    putIfMissing(namedArgs, "impsub2p", base + "aste");
    putIfMissing(namedArgs, "impsub3p", base + "assero");
    // Imperative
    putIfMissing(namedArgs, "imp2s", base + "a");
    putIfMissing(namedArgs, "imp3s", base + "i");
    putIfMissing(namedArgs, "imp1p", base + "iamo");
    putIfMissing(namedArgs, "imp2p", base + "ate");
    putIfMissing(namedArgs, "imp3p", base + "ino");


    itConj(args, namedArgs);
  }


  private void itConj(List<String> args, Map<String, String> namedArgs) {
    // TODO Auto-generated method stub
    
  }


  private static void putIfMissing(final Map<String, String> namedArgs, final String key,
      final String value) {
    final String oldValue = namedArgs.get(key);
    if (oldValue == null || oldValue.length() == 0) {
      namedArgs.put(key, value);
    }
  }
  
  // TODO: check how ='' and =| are manifested....
  // TODO: get this right in -are
  private static void putOrNullify(final Map<String, String> namedArgs, final String key,
      final String value) {
    final String oldValue = namedArgs.get(key);
    if (oldValue == null/* || oldValue.length() == 0*/) {
      namedArgs.put(key, value);
    } else {
      if (oldValue.equals("''")) {
        namedArgs.put(key, "");
      }
    }
  }

  final List<String> listPrefixes = new ArrayList<String>(); 
  final List<String> listLines = new ArrayList<String>(); 
  
static final Pattern UNINDEXED_WIKI_TEXT = Pattern.compile(
    "(first|second|third)-person (singular|plural)|" +
    "present tense|" +
    "imperative"
    );

  private void handleForeignListItem(final String foreignText, String title, final Collection<String> forms, final WikiTokenizer wikiTokenizer) {
    
    final String prefix = wikiTokenizer.listItemPrefix();
    if (prefix.length() > 1) {
      System.err.println("Prefix too long: " + wikiTokenizer.token());
      return;
    }
    
    listPrefixes.clear();
    listLines.clear();
    listPrefixes.add(prefix);
    listLines.add(wikiTokenizer.listItemWikiText());
    while(wikiTokenizer.nextToken() != null &&
        wikiTokenizer.isNewline() || 
        wikiTokenizer.isComment() ||
        (wikiTokenizer.isListItem() && 
            wikiTokenizer.listItemPrefix().length() > prefix.length() && 
            wikiTokenizer.listItemPrefix().startsWith(prefix))) {
      if (wikiTokenizer.isListItem()) {
        listPrefixes.add(wikiTokenizer.listItemPrefix());
        listLines.add(wikiTokenizer.listItemWikiText());
      }
    }
    if (wikiTokenizer.nextToken() != null) {
      wikiTokenizer.returnToLineStart();
    }
    System.out.println("list lines: " + listLines);
    System.out.println("list prefixes: " + listPrefixes);
    
    final PairEntry pairEntry = new PairEntry();
    final IndexedEntry indexedEntry = new IndexedEntry(pairEntry);
    
    final String foreign = trim(title);

    final StringBuilder englishBuilder = new StringBuilder();

    final String mainLine = listLines.get(0);
    
    final WikiTokenizer englishTokenizer = new WikiTokenizer(mainLine);
    while (englishTokenizer.nextToken() != null) {
      // TODO handle form of....
      if (englishTokenizer.isPlainText()) {
        englishBuilder.append(englishTokenizer.token());
        enIndexBuilder.addEntryWithString(indexedEntry, englishTokenizer.token(), EntryTypeName.WIKTIONARY_ENGLISH_DEF);
      } else if (englishTokenizer.isWikiLink()) {
        final String text = englishTokenizer.wikiLinkText();
        final String link = englishTokenizer.wikiLinkDest();
        if (link != null) {
          if (link.contains("#English")) {
            englishBuilder.append(text);
            enIndexBuilder.addEntryWithString(indexedEntry, text, EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK);
          } else if (link.contains("#") && this.langPattern.matcher(link).find()) {
            englishBuilder.append(text);
            otherIndexBuilder.addEntryWithString(indexedEntry, text, EntryTypeName.WIKTIONARY_ENGLISH_DEF_OTHER_LANG);
          } else {
            System.err.println("Special link: " + englishTokenizer.token());
            // TODO: something here...
          }
        } else {
          // link == null
          englishBuilder.append(text);
          if (!UNINDEXED_WIKI_TEXT.matcher(text).find()) {
            enIndexBuilder.addEntryWithString(indexedEntry, text, EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK);
          }
        }
      } else if (englishTokenizer.isFunction()) {
        final String name = englishTokenizer.functionName();
        if (name.contains(" conjugation of ") || 
            name.contains(" form of ") || 
            name.contains(" feminine of ") || 
            name.contains(" plural of ")) {
          // Ignore these in the index, they're really annoying....
          englishBuilder.append(englishTokenizer.token());
        } else {
          System.err.println("Unexpected function: " + englishTokenizer.token());
        }
      } else {
        if (englishTokenizer.isComment() || englishTokenizer.isMarkup()) {
        } else {
          System.err.println("Unexpected definition text: " + englishTokenizer.token());
        }
      }
    }
    final String english = trim(englishBuilder.toString());
    if (english.length() > 0) {
      final Pair pair = new Pair(english, trim(foreignText), this.swap);
      pairEntry.pairs.add(pair);
      otherIndexBuilder.addEntryWithString(indexedEntry, title, EntryTypeName.WIKTIONARY_TITLE_SINGLE, EntryTypeName.WIKTIONARY_TITLE_MULTI);
      for (final String form : forms) {
        otherIndexBuilder.addEntryWithString(indexedEntry, form, EntryTypeName.WIKTIONARY_FORM_SINGLE, EntryTypeName.WIKTIONARY_FORM_MULTI);
      }
    }
  }

  
}

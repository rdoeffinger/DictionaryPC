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

package com.hughes.android.dictionary.parser.enwiktionary;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.EntrySource;
import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;
import com.hughes.android.dictionary.parser.WikiTokenizer;

public class EnWiktionaryXmlParser {
  
  static final Logger LOG = Logger.getLogger(EnWiktionaryXmlParser.class.getName());
  
  // TODO: process {{ttbc}} lines
  
  static final Pattern partOfSpeechHeader = Pattern.compile(
      "Noun|Verb|Adjective|Adverb|Pronoun|Conjunction|Interjection|" +
      "Preposition|Proper noun|Article|Prepositional phrase|Acronym|" +
      "Abbreviation|Initialism|Contraction|Prefix|Suffix|Symbol|Letter|" +
      "Ligature|Idiom|Phrase|\\{\\{acronym\\}\\}|\\{\\{initialism\\}\\}|" +
      "\\{\\{abbreviation\\}\\}|" +
      // These are @deprecated:
      "Noun form|Verb form|Adjective form|Nominal phrase|Noun phrase|" +
      "Verb phrase|Transitive verb|Intransitive verb|Reflexive verb|" +
      // These are extras I found:
      "Determiner|Numeral|Number|Cardinal number|Ordinal number|Proverb|" +
      "Particle|Interjection|Pronominal adverb" +
      "Han character|Hanzi|Hanja|Kanji|Katakana character|Syllable");
  
  EntrySource entrySource;
  final IndexBuilder enIndexBuilder;
  final IndexBuilder foreignIndexBuilder;
  final Pattern langPattern;
  final Pattern langCodePattern;
  final boolean swap;
  
  // State used while parsing.
  enum State {
    TRANSLATION_LINE,
    ENGLISH_DEF_OF_FOREIGN,
    ENGLISH_EXAMPLE,
    FOREIGN_EXAMPLE,
  }
  State state = null;
  String title;

  public EnWiktionaryXmlParser(final IndexBuilder enIndexBuilder, final IndexBuilder otherIndexBuilder, final Pattern langPattern, final Pattern langCodePattern, final boolean swap) {
    this.enIndexBuilder = enIndexBuilder;
    this.foreignIndexBuilder = otherIndexBuilder;
    this.langPattern = langPattern;
    this.langCodePattern = langCodePattern;
    this.swap = swap;
  }

  
  public void parse(final File file, final EntrySource entrySource, final int pageLimit) throws IOException {
    this.entrySource = entrySource;
    int pageCount = 0;
    final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    try {
    while (true) {
      if (pageLimit >= 0 && pageCount >= pageLimit) {
        return;
      }
      
      try {
        title = dis.readUTF();
      } catch (EOFException e) {
        LOG.log(Level.INFO, "EOF reading split.");
        dis.close();
        return;
      }
      final String heading = dis.readUTF();
      final int bytesLength = dis.readInt();
      final byte[] bytes = new byte[bytesLength];
      dis.readFully(bytes);
      final String text = new String(bytes, "UTF8");
      
      parseSection(heading, text);

      ++pageCount;
      if (pageCount % 1000 == 0) {
        LOG.info("pageCount=" + pageCount);
      }
    }
    } finally {
      System.out.println("lang Counts: " + appendAndIndexWikiCallback.langCodeToTCount);
      appendAndIndexWikiCallback.langCodeToTCount.keySet().removeAll(EnWiktionaryLangs.isoCodeToWikiName.keySet());
      System.out.println("unused Counts: " + appendAndIndexWikiCallback.langCodeToTCount);
    }
  }
  
  private void parseSection(String heading, final String text) {
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
      doEnglishWord(text);
    } else if (langPattern.matcher(heading).find()){
      doForeignWord(heading, text);
    }
        
  }  // endPage()
  
  // -------------------------------------------------------------------------
  
  private void doEnglishWord(String text) {
    
    String pos = null;
    int posDepth = -1;

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
          // TODO: if we're inside the POS section, we should handle the first title line...
          
        } else if (headerName.equals("Translations")) {
          if (pos == null) {
            LOG.info("Translations without POS (but using anyway): " + title);
          }
          doTranslations(wikiTokenizer, pos);
        } else if (headerName.equals("Pronunciation")) {
          //doPronunciation(wikiLineReader);
        }
      } else if (wikiTokenizer.isFunction()) {
        final String name = wikiTokenizer.functionName();
        if (name.equals("head") && pos == null) {
          LOG.warning("{{head}} without POS: " + title);
        }
      }
    }
  }
  
  final AppendAndIndexWikiCallback appendAndIndexWikiCallback = new AppendAndIndexWikiCallback(this);
  {
    appendAndIndexWikiCallback.functionCallbacks.putAll(FunctionCallbacksDefault.DEFAULT);
  }
  
  private void doTranslations(final WikiTokenizer wikiTokenizer, final String pos) {
    if (title.equals("absolutely")) {
      //System.out.println();
    }
    
    String topLevelLang = null;
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
      
      if (wikiTokenizer.isFunction()) {
        final String functionName = wikiTokenizer.functionName();
        final List<String> positionArgs = wikiTokenizer.functionPositionArgs();
        
        if (functionName.equals("trans-top")) {
          sense = null;
          if (wikiTokenizer.functionPositionArgs().size() >= 1) {
            sense = positionArgs.get(0);
            // TODO: could emphasize words in [[brackets]] inside sense.
            sense = WikiTokenizer.toPlainText(sense);
            //LOG.info("Sense: " + sense);
          }
        } else if (functionName.equals("trans-bottom")) {
          sense = null;
        } else if (functionName.equals("trans-mid")) {
        } else if (functionName.equals("trans-see")) {
          // TODO: would also be nice...
        } else if (functionName.startsWith("picdic")) {
        } else if (functionName.startsWith("checktrans")) {
          done = true;
        } else if (functionName.startsWith("ttbc")) {
          wikiTokenizer.nextLine();
          // TODO: would be great to handle ttbc
          // TODO: Check this: done = true;
        } else {
          LOG.warning("Unexpected translation wikifunction: " + wikiTokenizer.token() + ", title=" + title);
        }
      } else if (wikiTokenizer.isListItem()) {
        final String line = wikiTokenizer.listItemWikiText();
        // This line could produce an output...
        
        if (line.contains("ich hoan dich gear")) {
          //System.out.println();
        }
        
        // First strip the language and check whether it matches.
        // And hold onto it for sub-lines.
        final int colonIndex = line.indexOf(":");
        if (colonIndex == -1) {
          continue;
        }
        
        final String lang = trim(WikiTokenizer.toPlainText(line.substring(0, colonIndex)));
        final boolean appendLang;
        if (wikiTokenizer.listItemPrefix().length() == 1) {
          topLevelLang = lang;
          final boolean thisFind = langPattern.matcher(lang).find();
          if (!thisFind) {
            continue;
          }
          appendLang = !langPattern.matcher(lang).matches();
        } else if (topLevelLang == null) {
          continue;
        } else {
          // Two-level -- the only way we won't append is if this second level matches exactly.
          if (!langPattern.matcher(lang).matches() && !langPattern.matcher(topLevelLang).find()) {
            continue;
          }
          appendLang = !langPattern.matcher(lang).matches();
        }
        
        String rest = line.substring(colonIndex + 1).trim();
        if (rest.length() > 0) {
          doTranslationLine(line, appendLang ? lang : null, pos, sense, rest);
        }
        
      } else if (wikiTokenizer.remainderStartsWith("''See''")) {
        wikiTokenizer.nextLine();
        LOG.fine("Skipping See line: " + wikiTokenizer.token());
      } else if (wikiTokenizer.isWikiLink()) {
        final String wikiLink = wikiTokenizer.wikiLinkText();
        if (wikiLink.contains(":") && wikiLink.contains(title)) {
        } else if (wikiLink.contains("Category:")) {
        } else  {
          LOG.warning("Unexpected wikiLink: " + wikiTokenizer.token() + ", title=" + title);
        }
      } else if (wikiTokenizer.isNewline() || wikiTokenizer.isMarkup() || wikiTokenizer.isComment()) {
      } else {
        final String token = wikiTokenizer.token();
        if (token.equals("----")) { 
        } else {
          LOG.warning("Unexpected translation token: " + wikiTokenizer.token() + ", title=" + title);
        }
      }
      
    }
  }
  
  private void doTranslationLine(final String line, final String lang, final String pos, final String sense, final String rest) {
    state = State.TRANSLATION_LINE;
    // Good chance we'll actually file this one...
    final PairEntry pairEntry = new PairEntry(entrySource);
    final IndexedEntry indexedEntry = new IndexedEntry(pairEntry);
    
    final StringBuilder foreignText = new StringBuilder();
    appendAndIndexWikiCallback.reset(foreignText, indexedEntry);
    appendAndIndexWikiCallback.dispatch(rest, foreignIndexBuilder, EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
    
    if (foreignText.length() == 0) {
      LOG.warning("Empty foreignText: " + line);
      return;
    }
    
    if (lang != null) {
      foreignText.insert(0, String.format("(%s) ", lang));
    }
    
    StringBuilder englishText = new StringBuilder();
    
    englishText.append(title);
    if (sense != null) {
      englishText.append(" (").append(sense).append(")");
      enIndexBuilder.addEntryWithString(indexedEntry, sense, EntryTypeName.WIKTIONARY_TRANSLATION_SENSE);
    }
    if (pos != null) {
      englishText.append(" (").append(pos.toLowerCase()).append(")");
    }
    enIndexBuilder.addEntryWithString(indexedEntry, title, EntryTypeName.WIKTIONARY_TITLE_MULTI);
    
    final Pair pair = new Pair(trim(englishText.toString()), trim(foreignText.toString()), swap);
    pairEntry.pairs.add(pair);
    if (!pairsAdded.add(pair.toString())) {
      LOG.warning("Duplicate pair: " + pair.toString());
    }
  }


  Set<String> pairsAdded = new LinkedHashSet<String>();
  
  // -------------------------------------------------------------------------
  
  private void doForeignWord(final String lang, final String text) {
    final WikiTokenizer wikiTokenizer = new WikiTokenizer(text);
    while (wikiTokenizer.nextToken() != null) {
      if (wikiTokenizer.isHeading()) {
        final String headingName = wikiTokenizer.headingWikiText();
        if (headingName.equals("Translations")) {
          LOG.warning("Translations not in English section: " + title);
        } else if (headingName.equals("Pronunciation")) {
          //doPronunciation(wikiLineReader);
        } else if (partOfSpeechHeader.matcher(headingName).matches()) {
          doForeignPartOfSpeech(lang, headingName, wikiTokenizer.headingDepth(), wikiTokenizer);
        }
      } else {
      }
    }
  }
  
  static final class ListSection {
    final String firstPrefix;
    final String firstLine;
    final List<String> nextPrefixes = new ArrayList<String>();
    final List<String> nextLines = new ArrayList<String>();
    
    public ListSection(String firstPrefix, String firstLine) {
      this.firstPrefix = firstPrefix;
      this.firstLine = firstLine;
    }

    @Override
    public String toString() {
      return firstPrefix + firstLine + "{ " + nextPrefixes + "}";
    }
  }


  int foreignCount = 0;
  final Collection<String> wordForms = new ArrayList<String>();
  boolean titleAppended = false;

  private void doForeignPartOfSpeech(final String lang, String posHeading, final int posDepth, WikiTokenizer wikiTokenizer) {
    if (++foreignCount % 1000 == 0) {
      LOG.info("***" + lang + ", " + title + ", pos=" + posHeading + ", foreignCount=" + foreignCount);
    }
    if (title.equals("6")) {
      System.out.println();
    }
    
    final StringBuilder foreignBuilder = new StringBuilder();
    final List<ListSection> listSections = new ArrayList<ListSection>();
    
    appendAndIndexWikiCallback.reset(foreignBuilder, null);
    this.state = State.ENGLISH_DEF_OF_FOREIGN;  // TODO: this is wrong, need new category....
    titleAppended = false;
    wordForms.clear();
    
    try {
    
    ListSection lastListSection = null;
    
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
        // TODO: deal with other neat info sections
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
        
        appendAndIndexWikiCallback.onFunction(wikiTokenizer, name, args, namedArgs);
        
      } else if (wikiTokenizer.isListItem()) {
        final String prefix = wikiTokenizer.listItemPrefix();
        if (lastListSection != null && 
            prefix.startsWith(lastListSection.firstPrefix) && 
            prefix.length() > lastListSection.firstPrefix.length()) {
          lastListSection.nextPrefixes.add(prefix);
          lastListSection.nextLines.add(wikiTokenizer.listItemWikiText());
        } else {
          lastListSection = new ListSection(prefix, wikiTokenizer.listItemWikiText());
          listSections.add(lastListSection);
        }
      } else if (lastListSection != null) {
        // Don't append anything after the lists, because there's crap.
      } else if (wikiTokenizer.isWikiLink()) {
        // Unindexed!
        foreignBuilder.append(wikiTokenizer.wikiLinkText());
        
      } else if (wikiTokenizer.isPlainText()) {
        // Unindexed!
        foreignBuilder.append(wikiTokenizer.token());
        
      } else if (wikiTokenizer.isMarkup() || wikiTokenizer.isNewline() || wikiTokenizer.isComment()) {
        // Do nothing.
      } else {
        LOG.warning("Unexpected token: " + wikiTokenizer.token());
      }
    }
    
    } finally {
      // Here's where we exit.
      // Should we make an entry even if there are no foreign list items?
      String foreign = foreignBuilder.toString().trim();
      if (!titleAppended && !foreign.toLowerCase().startsWith(title.toLowerCase())) {
        foreign = String.format("%s %s", title, foreign);
      }
      if (!langPattern.matcher(lang).matches()) {
        foreign = String.format("(%s) %s", lang, foreign);
      }
      for (final ListSection listSection : listSections) {
        doForeignListSection(foreign, title, wordForms, listSection);
      }
    }
  }
  
  
  // Might only want to remove "lang" if it's equal to "zh", for example.
  static final Set<String> USELESS_WIKI_ARGS = new LinkedHashSet<String>(
      Arrays.asList(
          "lang",
          "sc",
          "sort",
          "cat",
          "xs",
          "nodot"));

  public boolean entryIsFormOfSomething = false;

  private void doForeignListSection(final String foreignText, String title, final Collection<String> forms, final ListSection listSection) {
    state = State.ENGLISH_DEF_OF_FOREIGN;
    final String prefix = listSection.firstPrefix;
    if (prefix.length() > 1) {
      // Could just get looser and say that any prefix longer than first is a sublist.
      LOG.warning("Prefix too long: " + listSection);
      return;
    }
    
    final PairEntry pairEntry = new PairEntry(entrySource);
    final IndexedEntry indexedEntry = new IndexedEntry(pairEntry);

    entryIsFormOfSomething = false;
    final StringBuilder englishBuilder = new StringBuilder();
    final String mainLine = listSection.firstLine;
    appendAndIndexWikiCallback.reset(englishBuilder, indexedEntry);
    appendAndIndexWikiCallback.dispatch(mainLine, enIndexBuilder, EntryTypeName.WIKTIONARY_ENGLISH_DEF);

    final String english = trim(englishBuilder.toString());
    if (english.length() > 0) {
      final Pair pair = new Pair(english, trim(foreignText), this.swap);
      pairEntry.pairs.add(pair);
      foreignIndexBuilder.addEntryWithString(indexedEntry, title, entryIsFormOfSomething ? EntryTypeName.WIKTIONARY_IS_FORM_OF_SOMETHING_ELSE : EntryTypeName.WIKTIONARY_TITLE_MULTI);
      for (final String form : forms) {
        foreignIndexBuilder.addEntryWithString(indexedEntry, form, EntryTypeName.WIKTIONARY_INFLECTED_FORM_MULTI);
      }
    }
    
    // Do examples.
    String lastForeign = null;
    for (int i = 0; i < listSection.nextPrefixes.size(); ++i) {
      final String nextPrefix = listSection.nextPrefixes.get(i);
      final String nextLine = listSection.nextLines.get(i);

      // TODO: This splitting is not sensitive to wiki code.
      int dash = nextLine.indexOf("&mdash;");
      int mdashLen = 7;
      if (dash == -1) {
        dash = nextLine.indexOf("—");
        mdashLen = 1;
      }
      if (dash == -1) {
        dash = nextLine.indexOf(" - ");
        mdashLen = 3;
      }
      
      if ((nextPrefix.equals("#:") || nextPrefix.equals("##:")) && dash != -1) {
        final String foreignEx = nextLine.substring(0, dash);
        final String englishEx = nextLine.substring(dash + mdashLen);
        final Pair pair = new Pair(formatAndIndexExampleString(englishEx, enIndexBuilder, indexedEntry), formatAndIndexExampleString(foreignEx, foreignIndexBuilder, indexedEntry), swap);
        if (pair.lang1 != "--" && pair.lang1 != "--") {
          pairEntry.pairs.add(pair);
        }
        lastForeign = null;
      } else if (nextPrefix.equals("#:") || nextPrefix.equals("##:")){
        final Pair pair = new Pair("--", formatAndIndexExampleString(nextLine, null, indexedEntry), swap);
        lastForeign = nextLine;
        if (pair.lang1 != "--" && pair.lang1 != "--") {
          pairEntry.pairs.add(pair);
        }
      } else if (nextPrefix.equals("#::") || nextPrefix.equals("#**")) {
        if (lastForeign != null && pairEntry.pairs.size() > 0) {
          pairEntry.pairs.remove(pairEntry.pairs.size() - 1);
          final Pair pair = new Pair(formatAndIndexExampleString(nextLine, enIndexBuilder, indexedEntry), formatAndIndexExampleString(lastForeign, foreignIndexBuilder, indexedEntry), swap);
          if (pair.lang1 != "--" || pair.lang2 != "--") {
            pairEntry.pairs.add(pair);
          }
          lastForeign = null;
        } else {
          LOG.warning("TODO: English example with no foreign: " + title + ", " + nextLine);
          final Pair pair = new Pair("--", formatAndIndexExampleString(nextLine, null, indexedEntry), swap);
          if (pair.lang1 != "--" || pair.lang2 != "--") {
            pairEntry.pairs.add(pair);
          }
        }
      } else if (nextPrefix.equals("#*")) {
        // Can't really index these.
        final Pair pair = new Pair("--", formatAndIndexExampleString(nextLine, null, indexedEntry), swap);
        lastForeign = nextLine;
        if (pair.lang1 != "--" || pair.lang2 != "--") {
          pairEntry.pairs.add(pair);
        }
      } else if (nextPrefix.equals("#::*") || nextPrefix.equals("##") || nextPrefix.equals("#*:") || nextPrefix.equals("#:*") || true) {
        final Pair pair = new Pair("--", formatAndIndexExampleString(nextLine, null, indexedEntry), swap);
        if (pair.lang1 != "--" || pair.lang2 != "--") {
          pairEntry.pairs.add(pair);
        }
//      } else {
//        assert false;
      }
    }
  }
  
  private String formatAndIndexExampleString(final String example, final IndexBuilder indexBuilder, final IndexedEntry indexedEntry) {
    // TODO:
//    if (wikiTokenizer.token().equals("'''")) {
//      insideTripleQuotes = !insideTripleQuotes;
//    }
    final StringBuilder builder = new StringBuilder();
    appendAndIndexWikiCallback.reset(builder, indexedEntry);
    appendAndIndexWikiCallback.entryTypeName = EntryTypeName.WIKTIONARY_EXAMPLE;
    appendAndIndexWikiCallback.entryTypeNameSticks = true;
    try {
      // TODO: this is a hack needed because we don't safely split on the dash.
      appendAndIndexWikiCallback.dispatch(example, indexBuilder, EntryTypeName.WIKTIONARY_EXAMPLE);
    } catch (AssertionError e) {
      return "--";
    }
    final String result = trim(builder.toString());
    return result.length() > 0 ? result : "--";
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

  static final Pattern whitespace = Pattern.compile("\\s+");
  static String trim(final String s) {
    return whitespace.matcher(s).replaceAll(" ").trim();
  }

  
}

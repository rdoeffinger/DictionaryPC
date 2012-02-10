// Copyright 2012 Google Inc. All Rights Reserved.
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

package com.hughes.android.dictionary.parser.wiktionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.util.ListUtil;

public abstract class EnParser extends AbstractWiktionaryParser {

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
  
  // Might only want to remove "lang" if it's equal to "zh", for example.
  static final Set<String> USELESS_WIKI_ARGS = new LinkedHashSet<String>(
      Arrays.asList(
          "lang",
          "sc",
          "sort",
          "cat",
          "cat2",
          "xs",
          "nodot"));

  static boolean isIgnorableTitle(final String title) {
    return title.startsWith("Wiktionary:") ||
        title.startsWith("Template:") ||
        title.startsWith("Appendix:") ||
        title.startsWith("Category:") ||
        title.startsWith("Index:") ||
        title.startsWith("MediaWiki:") ||
        title.startsWith("TransWiki:") ||
        title.startsWith("Citations:") ||
        title.startsWith("Concordance:") ||
        title.startsWith("Help:");
  }
  
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

  public boolean entryIsFormOfSomething = false;
  final Collection<String> wordForms = new ArrayList<String>();
  boolean titleAppended = false;


  final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback = new AppendAndIndexWikiCallback<EnParser>(this);
  {
    appendAndIndexWikiCallback.functionCallbacks.putAll(FunctionCallbacks.DEFAULT);
  }
  
  EnParser(final IndexBuilder enIndexBuilder, final IndexBuilder otherIndexBuilder, final Pattern langPattern, final Pattern langCodePattern, final boolean swap) {
    this.enIndexBuilder = enIndexBuilder;
    this.foreignIndexBuilder = otherIndexBuilder;
    this.langPattern = langPattern;
    this.langCodePattern = langCodePattern;
    this.swap = swap;
  }
  
  // --------------------------------------------------------------------

  static final class EnToTranslationParser extends EnParser {

    EnToTranslationParser(final IndexBuilder enIndexBuilder,
        final IndexBuilder otherIndexBuilder, final Pattern langPattern,
        final Pattern langCodePattern, final boolean swap) {
      super(enIndexBuilder, otherIndexBuilder, langPattern, langCodePattern, swap);
    }

    @Override
    void parseSection(String heading, String text) {
      if (isIgnorableTitle(title)) {
        return;
      }
      heading = heading.replaceAll("=", "").trim(); 
      if (!heading.contains("English")) {
        return;
      }

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
           incrementCount("WARNING:trans-see");
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
          
//          if (line.contains("ich hoan dich gear")) {
//            //System.out.println();
//          }
          
          // First strip the language and check whether it matches.
          // And hold onto it for sub-lines.
          final int colonIndex = line.indexOf(":");
          if (colonIndex == -1) {
            continue;
          }
          
          final String lang = trim(WikiTokenizer.toPlainText(line.substring(0, colonIndex)));
          incrementCount("tCount:" + lang);
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
          incrementCount("WARNING: ''See''" );
          LOG.fine("Skipping See line: " + wikiTokenizer.token());
        } else if (wikiTokenizer.isWikiLink()) {
          final String wikiLink = wikiTokenizer.wikiLinkText();
          if (wikiLink.contains(":") && wikiLink.contains(title)) {
          } else if (wikiLink.contains("Category:")) {
          } else  {
            incrementCount("WARNING: Unexpected wikiLink" );
            LOG.warning("Unexpected wikiLink: " + wikiTokenizer.token() + ", title=" + title);
          }
        } else if (wikiTokenizer.isNewline() || wikiTokenizer.isMarkup() || wikiTokenizer.isComment()) {
        } else {
          final String token = wikiTokenizer.token();
          if (token.equals("----")) { 
          } else {
            LOG.warning("Unexpected translation token: " + wikiTokenizer.token() + ", title=" + title);
            incrementCount("WARNING: Unexpected translation token" );
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
        incrementCount("WARNING: Empty foreignText" );
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
        incrementCount("WARNING: Duplicate pair" );
      }
    }
  }  // EnToTranslationParser
  
  // -----------------------------------------------------------------------
  
  
  static final class ForeignParser extends EnParser {

    ForeignParser(final IndexBuilder enIndexBuilder,
        final IndexBuilder otherIndexBuilder, final Pattern langPattern,
        final Pattern langCodePattern, final boolean swap) {
      super(enIndexBuilder, otherIndexBuilder, langPattern, langCodePattern, swap);
    }

    @Override
    void parseSection(String heading, String text) {
      if (isIgnorableTitle(title)) {
        return;
      }
      final String lang = heading.replaceAll("=", "").trim();
      if (!langPattern.matcher(lang).find()){
        return;
      }
      
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
          // It's not a heading.
          // TODO: optimization: skip to next heading.
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
//        } else {
//          assert false;
        }
      }
    }
    
    private String formatAndIndexExampleString(final String example, final IndexBuilder indexBuilder, final IndexedEntry indexedEntry) {
      // TODO:
//      if (wikiTokenizer.token().equals("'''")) {
//        insideTripleQuotes = !insideTripleQuotes;
//      }
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

  }  // ForeignParser
  
  // -----------------------------------------------------------------------
  
  static class FunctionCallbacks {
  
  static final Map<String,FunctionCallback<EnParser>> DEFAULT = new LinkedHashMap<String, FunctionCallback<EnParser>>();
  
  static {
    FunctionCallback<EnParser> callback = new TranslationCallback();
    DEFAULT.put("t", callback);
    DEFAULT.put("t+", callback);
    DEFAULT.put("t-", callback);
    DEFAULT.put("tø", callback);
    DEFAULT.put("apdx-t", callback);
    
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
    
    callback = new l_term();
    DEFAULT.put("l", callback);
    DEFAULT.put("term", callback);

    callback = new Gender();
    DEFAULT.put("m", callback);
    DEFAULT.put("f", callback);
    DEFAULT.put("n", callback);
    DEFAULT.put("p", callback);
    DEFAULT.put("g", callback);
    
    callback = new AppendArg0();

    callback = new Ignore();
    DEFAULT.put("trreq", callback);
    DEFAULT.put("t-image", callback);
    DEFAULT.put("defn", callback);
    DEFAULT.put("rfdef", callback);
    DEFAULT.put("rfdate", callback);
    DEFAULT.put("rfex", callback);
    DEFAULT.put("rfquote", callback);
    DEFAULT.put("attention", callback);
    DEFAULT.put("zh-attention", callback);


    callback = new FormOf();
    DEFAULT.put("form of", callback);
    DEFAULT.put("conjugation of", callback);
    DEFAULT.put("participle of", callback);
    DEFAULT.put("present participle of", callback);
    DEFAULT.put("past participle of", callback);
    DEFAULT.put("feminine past participle of", callback);
    DEFAULT.put("gerund of", callback);
    DEFAULT.put("feminine of", callback);
    DEFAULT.put("plural of", callback);
    DEFAULT.put("feminine plural of", callback);
    DEFAULT.put("inflected form of", callback);
    DEFAULT.put("alternative form of", callback);
    DEFAULT.put("dated form of", callback);
    DEFAULT.put("apocopic form of", callback);
    
    callback = new InflOrHead();
    DEFAULT.put("infl", callback);
    DEFAULT.put("head", callback);
    
    callback = new AppendName();
    DEFAULT.put("...", callback);
    
    DEFAULT.put("qualifier", new QualifierCallback());
    DEFAULT.put("italbrac", new italbrac());
    DEFAULT.put("gloss", new gloss());
    DEFAULT.put("not used", new not_used());
    DEFAULT.put("wikipedia", new wikipedia());
  }
  
  static final NameAndArgs<EnParser> NAME_AND_ARGS = new NameAndArgs<EnParser>();

  // ------------------------------------------------------------------

  static final class TranslationCallback implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs, final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {

      final String transliteration = namedArgs.remove("tr");
      final String alt = namedArgs.remove("alt");
      namedArgs.keySet().removeAll(USELESS_WIKI_ARGS);
      if (args.size() < 2) {
        LOG.warning("{{t...}} with wrong args: title=" + parser.title);
        return false;
      }
      final String langCode = ListUtil.get(args, 0);
      if (!appendAndIndexWikiCallback.langCodeToTCount.containsKey(langCode)) {
        appendAndIndexWikiCallback.langCodeToTCount.put(langCode, new AtomicInteger());
      }
      appendAndIndexWikiCallback.langCodeToTCount.get(langCode).incrementAndGet();
      final String word = ListUtil.get(args, 1);
      appendAndIndexWikiCallback.dispatch(alt != null ? alt : word, EntryTypeName.WIKTIONARY_TITLE_MULTI);

      // Genders...
      if (args.size() > 2) {
        appendAndIndexWikiCallback.builder.append(" {");
        for (int i = 2; i < args.size(); ++i) {
          if (i > 2) {
            appendAndIndexWikiCallback.builder.append("|");
          }
          appendAndIndexWikiCallback.builder.append(args.get(i));
        }
        appendAndIndexWikiCallback.builder.append("}");
      }

      if (transliteration != null) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(transliteration, EntryTypeName.WIKTIONARY_TRANSLITERATION);
        appendAndIndexWikiCallback.builder.append(")");
      }
      
      if (alt != null) {
        // If alt wasn't null, we appended alt instead of the actual word
        // we're filing under..
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(word, EntryTypeName.WIKTIONARY_TITLE_MULTI);
        appendAndIndexWikiCallback.builder.append(")");
      }

      // Catch-all for anything else...
      if (!namedArgs.isEmpty()) {
        appendAndIndexWikiCallback.builder.append(" {");
        appendNamedArgs(namedArgs, appendAndIndexWikiCallback);
        appendAndIndexWikiCallback.builder.append("}");
      }
      
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class QualifierCallback implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
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
  
  static final class EncodingCallback implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      if (!namedArgs.isEmpty()) {
        LOG.warning("weird encoding: " + wikiTokenizer.token());
      }
      if (args.size() == 0) {
        // Things like "{{Jpan}}" exist.
        return true;
      }
      
      for (int i = 0; i < args.size(); ++i) {
        if (i > 0) {
          appendAndIndexWikiCallback.builder.append(", ");
        }
        final String arg = args.get(i);
//        if (arg.equals(parser.title)) {
//          parser.titleAppended = true;
//        }
        appendAndIndexWikiCallback.dispatch(arg, appendAndIndexWikiCallback.entryTypeName);
      }
      
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class Gender implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
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
  
  static final class l_term implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      
      // for {{l}}, lang is arg 0, but not for {{term}}
      if (name.equals("term")) {
        args.add(0, "");
      }
      
      final EntryTypeName entryTypeName;
      switch (parser.state) {
      case TRANSLATION_LINE: entryTypeName = EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT; break;
      case ENGLISH_DEF_OF_FOREIGN: entryTypeName = EntryTypeName.WIKTIONARY_ENGLISH_DEF_WIKI_LINK; break;
      default: throw new IllegalStateException("Invalid enum value: " + parser.state);
      }
      
      final String langCode = args.get(0);
      final IndexBuilder indexBuilder;
      if ("".equals(langCode)) {
        indexBuilder = parser.foreignIndexBuilder;
      } else if ("en".equals(langCode)) {
        indexBuilder = parser.enIndexBuilder;
      } else {
        indexBuilder = parser.foreignIndexBuilder;
      }
      
      String displayText = ListUtil.get(args, 2, "");
      if (displayText.equals("")) {
        displayText = ListUtil.get(args, 1, null);
      }
      
      if (displayText != null) {
        appendAndIndexWikiCallback.dispatch(displayText, indexBuilder, entryTypeName);
      } else {
        LOG.warning("no display text: " + wikiTokenizer.token());
      }
      
      final String tr = namedArgs.remove("tr");
      if (tr != null) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(tr, indexBuilder, EntryTypeName.WIKTIONARY_TRANSLITERATION);
        appendAndIndexWikiCallback.builder.append(")");
      }
      
      final String gloss = ListUtil.get(args, 3, "");
      if (!gloss.equals("")) {
        appendAndIndexWikiCallback.builder.append(" (");
        appendAndIndexWikiCallback.dispatch(gloss, parser.enIndexBuilder, EntryTypeName.WIKTIONARY_ENGLISH_DEF);
        appendAndIndexWikiCallback.builder.append(")");
      }
      
      namedArgs.keySet().removeAll(USELESS_WIKI_ARGS);
      if (!namedArgs.isEmpty()) {
        appendAndIndexWikiCallback.builder.append(" {").append(name);
        appendNamedArgs(namedArgs, appendAndIndexWikiCallback);
        appendAndIndexWikiCallback.builder.append("}");
      }

      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class AppendArg0 implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      // TODO: transliteration
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class italbrac implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append("(");
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      appendAndIndexWikiCallback.builder.append(")");
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class gloss implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      if (args.size() != 1 || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append("(");
      appendAndIndexWikiCallback.dispatch(args.get(0), EntryTypeName.WIKTIONARY_TRANSLATION_OTHER_TEXT);
      appendAndIndexWikiCallback.builder.append(")");
      return true;
    }
  }
  
  // ------------------------------------------------------------------
  
  static final class Ignore implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      return true;
    }
  }

  // ------------------------------------------------------------------
  
  static final class not_used implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      appendAndIndexWikiCallback.builder.append("(not used)");
      return true;
    }
  }


  // ------------------------------------------------------------------
  
  static final class AppendName implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      if (!args.isEmpty() || !namedArgs.isEmpty()) {
        return false;
      }
      appendAndIndexWikiCallback.builder.append(name);
      return true;
    }
  }

  // --------------------------------------------------------------------
  // --------------------------------------------------------------------
  

  static final class FormOf implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      parser.entryIsFormOfSomething = true;
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
      namedArgs.keySet().removeAll(USELESS_WIKI_ARGS);
      
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
  
  static final class wikipedia implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
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

  static final class InflOrHead implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      // See: http://en.wiktionary.org/wiki/Template:infl
      final String langCode = ListUtil.get(args, 0);
      String head = namedArgs.remove("head");
      if (head == null) {
        head = namedArgs.remove("title"); // Bug
      }
      if (head == null) {
        head = parser.title;
      }
      parser.titleAppended = true;
      
      namedArgs.keySet().removeAll(USELESS_WIKI_ARGS);

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
        appendAndIndexWikiCallback.builder.append(" (");
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
  static final class it_noun implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
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
  static final class it_proper_noun implements FunctionCallback<EnParser> {
    @Override
    public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
        final Map<String, String> namedArgs,
        final EnParser parser,
        final AppendAndIndexWikiCallback<EnParser> appendAndIndexWikiCallback) {
      return false;
    }
  }

  }


}

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

import java.util.List;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;
import com.hughes.android.dictionary.parser.WikiTokenizer;

public final class EnToTranslationParser extends EnParser {

    public EnToTranslationParser(final IndexBuilder enIndexBuilder,
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
      indexedEntry.isValid = true;
      
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
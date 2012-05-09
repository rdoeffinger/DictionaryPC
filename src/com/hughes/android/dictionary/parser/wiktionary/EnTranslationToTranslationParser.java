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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.android.dictionary.parser.wiktionary.EnFunctionCallbacks.TranslationCallback;
import com.hughes.util.ListUtil;

public final class EnTranslationToTranslationParser extends AbstractWiktionaryParser {
  
    final IndexBuilder[] indexBuilders;
    final Pattern[] langCodePatterns;

    PairEntry pairEntry = null;
    IndexedEntry indexedEntry = null;
    StringBuilder[] builders = null; 
    
  final Set<String> Ts = new LinkedHashSet<String>(Arrays.asList("t", "t+",
      "t-", "tø", "apdx-t", "ttbc"));
    
    public EnTranslationToTranslationParser(final IndexBuilder[] indexBuilders,
        final Pattern[] langCodePatterns) {
      this.indexBuilders = indexBuilders;
      this.langCodePatterns = langCodePatterns;
    }
    
    @Override
    void removeUselessArgs(Map<String, String> namedArgs) {
      namedArgs.keySet().removeAll(EnParser.USELESS_WIKI_ARGS);
    }
    
    @Override
    void parseSection(String heading, String text) {
      if (EnParser.isIgnorableTitle(title)) {
        return;
      }
      final WikiTokenizer wikiTokenizer = new WikiTokenizer(text);
      while (wikiTokenizer.nextToken() != null) {
        if (wikiTokenizer.isFunction()) {
          final String name = wikiTokenizer.functionName();
          if (Ts.contains(name)) {
            onT(wikiTokenizer);
          } else if (name.equals("trans-top")) {
            startEntry(title, wikiTokenizer.token());
          } else if (name.equals("trans-bottom")) {
            finishEntry(title);
          }
        }
      }
    }
    
    final TranslationCallback<EnTranslationToTranslationParser> translationCallback = new TranslationCallback<EnTranslationToTranslationParser>();
    
  final AppendAndIndexWikiCallback<EnTranslationToTranslationParser> appendAndIndexWikiCallback = new AppendAndIndexWikiCallback<EnTranslationToTranslationParser>(
      this);
  {
    for (final String t : Ts) {
      appendAndIndexWikiCallback.functionCallbacks.put(t, translationCallback);
    }
  }
    
  private void onT(WikiTokenizer wikiTokenizer) {
    final List<String> args = wikiTokenizer.functionPositionArgs();
    final String langCode = ListUtil.get(args, 0);
    for (int p = 0; p < 2; ++p) {
      if (langCodePatterns[p].matcher(langCode).matches()) {
        appendAndIndexWikiCallback.builder = builders[p];
        appendAndIndexWikiCallback.indexBuilder = indexBuilders[p];
        appendAndIndexWikiCallback.onFunction(wikiTokenizer,
            wikiTokenizer.functionName(), wikiTokenizer.functionPositionArgs(),
            wikiTokenizer.functionNamedArgs());
      }
    }
  }

    void startEntry(final String title, final String func) {
      if (pairEntry != null) {
        LOG.warning("startEntry() twice" + func);
        finishEntry(title);
      }
      
      pairEntry = new PairEntry(entrySource);
      indexedEntry = new IndexedEntry(pairEntry);
      builders = new StringBuilder[] { new StringBuilder(), new StringBuilder() }; 
    }
    
    void finishEntry(final String title) {
      if (pairEntry == null) {
        LOG.warning("finalizeEntry() twice" + title);
        return;
      }
      final String lang1 = builders[0].toString();
      final String lang2 = builders[1].toString();
      if (lang1.length() > 0 && lang2.length() > 0) {
        pairEntry.pairs.add(new Pair(lang1, lang2));
        indexedEntry.isValid = true;
      }
      
      pairEntry = null;
      indexedEntry = null;
      builders = null;
    }

  }
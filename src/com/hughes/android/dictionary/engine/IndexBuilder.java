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

package com.hughes.android.dictionary.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.hughes.android.dictionary.engine.Index.IndexEntry;
import com.hughes.android.dictionary.parser.DictFileParser;

public class IndexBuilder {
  
  final DictionaryBuilder dictionaryBuilder;
  public final Index index;
  final Set<String> stoplist;

  final SortedMap<String, TokenData> tokenToData;

  IndexBuilder(final DictionaryBuilder dictionaryBuilder, final String shortName, final String longName, final Language language, final String normalizerRules, final Set<String> stoplist, final boolean swapPairEntries) {
    this.dictionaryBuilder = dictionaryBuilder;
    index = new Index(dictionaryBuilder.dictionary, shortName, longName, language, normalizerRules, swapPairEntries);
    tokenToData = new TreeMap<String, TokenData>(new NormalizeComparator(index.normalizer(), language.getCollator()));
    this.stoplist = stoplist;
  }
  
  public void build() {
    final Set<IndexedEntry> tokenEntryDatas = new HashSet<IndexedEntry>();
    final List<RowBase> rows = index.rows;
    index.mainTokenCount = 0;
    for (final TokenData tokenData : tokenToData.values()) {
      tokenEntryDatas.clear();
      final int indexIndex = index.sortedIndexEntries.size();
      final int startRow = rows.size();
      
      final TokenRow tokenRow = new TokenRow(indexIndex, rows.size(), index, tokenData.hasMainEntry);
      rows.add(tokenRow);
      if (tokenRow.hasMainEntry) {
        index.mainTokenCount++;
      }
//      System.out.println("Added TokenRow: " + rows.get(rows.size() - 1));
      int numRows = 0;
//      System.out.println("TOKEN: " + tokenData.token);
      for (final Map.Entry<EntryTypeName, List<IndexedEntry>> typeToEntry : tokenData.typeToEntries.entrySet()) {
        for (final IndexedEntry entryData : typeToEntry.getValue()) {
          if (entryData.index() == -1) {
            entryData.addToDictionary(dictionaryBuilder.dictionary);
            assert entryData.index() >= 0;
          }
          if (tokenEntryDatas.add(entryData)) {
            rows.add(new PairEntry.Row(entryData.index(), rows.size(), index));
            ++entryData.entry.entrySource.numEntries;
            ++numRows;
            
//            System.out.print("  " + typeToEntry.getKey() + ": ");
  //          rows.get(rows.size() - 1).print(System.out);
//            System.out.println();
          }
        }
      }
      index.sortedIndexEntries.add(new Index.IndexEntry(tokenData.token, index
          .normalizer().transliterate(tokenData.token), startRow, numRows));
    }
    
    final List<IndexEntry> entriesSortedByNumRows = new ArrayList<IndexEntry>(index.sortedIndexEntries);
    Collections.sort(entriesSortedByNumRows, new Comparator<IndexEntry>() {
      @Override
      public int compare(IndexEntry object1, IndexEntry object2) {
        return object2.numRows - object1.numRows;
      }});
    System.out.println("Most common tokens:");
    for (int i = 0; i < 50 && i < entriesSortedByNumRows.size(); ++i) {
      System.out.println("  " + entriesSortedByNumRows.get(i));
    }
  }
  
  static class TokenData {
    final String token;
        
    final Map<EntryTypeName, List<IndexedEntry>> typeToEntries = new EnumMap<EntryTypeName, List<IndexedEntry>>(EntryTypeName.class);
    boolean hasMainEntry = false;
    
    TokenData(final String token) {
      assert token.equals(token.trim());
      assert token.length() > 0;
      this.token = token;
    }
  }

  private TokenData getOrCreateTokenData(final String token) {
    TokenData tokenData = tokenToData.get(token);
    if (tokenData == null) {
      tokenData = new TokenData(token);
      tokenToData.put(token, tokenData);
    }
    return tokenData;
  }

  private List<IndexedEntry> getOrCreateEntries(final String token, final EntryTypeName entryTypeName) {
    final TokenData tokenData = getOrCreateTokenData(token);
    List<IndexedEntry> entries = tokenData.typeToEntries.get(entryTypeName);
    if (entryTypeName.overridesStopList) {
      tokenData.hasMainEntry = true;
    }
    if (entries == null) {
      entries = new ArrayList<IndexedEntry>();
      tokenData.typeToEntries.put(entryTypeName, entries);
    }
    return entries;
  }

  public void addEntryWithTokens(final IndexedEntry indexedEntry, final Set<String> tokens,
      final EntryTypeName entryTypeName) {
    if (indexedEntry == null) {
      System.out.println("asdfasdf");
    }
    assert indexedEntry != null;
    for (final String token : tokens) {
      if (entryTypeName.overridesStopList || !stoplist.contains(token)) {
        getOrCreateEntries(token, entryTypeName).add(indexedEntry);
      }
    }    
  }

  public void addEntryWithString(final IndexedEntry indexedEntry, final String untokenizedString,
      final EntryTypeName entryTypeName) {
    final Set<String> tokens = DictFileParser.tokenize(untokenizedString, DictFileParser.NON_CHAR);
    addEntryWithTokens(indexedEntry, tokens, tokens.size() == 1 ? entryTypeName.singleWordInstance : entryTypeName);
  }

  public void addEntryWithStringNoSingle(final IndexedEntry indexedEntry, final String untokenizedString,
      final EntryTypeName entryTypeName) {
    final Set<String> tokens = DictFileParser.tokenize(untokenizedString, DictFileParser.NON_CHAR);
    addEntryWithTokens(indexedEntry, tokens, entryTypeName);
  }
}

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

import java.util.*;

import com.hughes.android.dictionary.engine.Index.IndexEntry;
import com.hughes.android.dictionary.parser.DictFileParser;

public class IndexBuilder {

    final DictionaryBuilder dictionaryBuilder;
    public final Index index;
    final Set<String> stoplist;

    final Map<String, TokenData> fastTokenToData;
    final SortedMap<FastCompareString, TokenData> tokenToData;

    IndexBuilder(final DictionaryBuilder dictionaryBuilder, final String shortName, final String longName, final Language language, final String normalizerRules, final Set<String> stoplist, final boolean swapPairEntries) {
        this.dictionaryBuilder = dictionaryBuilder;
        index = new Index(dictionaryBuilder.dictionary, shortName, longName, language, normalizerRules, swapPairEntries, stoplist);
        tokenToData = new TreeMap<>(new FastNormalizeComparator(index.getSortComparator()));
        fastTokenToData = new HashMap<>();
        this.stoplist = stoplist;
    }

    public void build() {
        final Set<IndexedEntry> tokenIndexedEntries = new HashSet<>();
        final List<RowBase> rows = index.rows;
        index.mainTokenCount = 0;
        for (final TokenData tokenData : tokenToData.values()) {
            tokenIndexedEntries.clear();
            final int indexIndex = index.sortedIndexEntries.size();
            final int startRow = rows.size();

            TokenRow tokenRow = null;
            if (!tokenData.htmlEntries.isEmpty()) {
                tokenRow = new TokenRow(indexIndex, rows.size(), index, tokenData.hasMainEntry);
                rows.add(tokenRow);
            }

//    System.out.println("Added TokenRow: " + rows.get(rows.size() - 1));

            int numRows = 0;  // off by one--doesn't count the token row!
//      System.out.println("TOKEN: " + tokenData.token);
            for (final Map.Entry<EntryTypeName, List<IndexedEntry>> typeToIndexedEntries : tokenData.typeToEntries.entrySet()) {
                for (final IndexedEntry indexedEntry : typeToIndexedEntries.getValue()) {
                    if (!indexedEntry.isValid) {
                        continue;
                    }

                    if (tokenRow == null) {
                        tokenRow = new TokenRow(indexIndex, rows.size(), index, tokenData.hasMainEntry);
                        rows.add(tokenRow);
                    }

                    if (indexedEntry.entry.index() == -1) {
                        indexedEntry.entry.addToDictionary(dictionaryBuilder.dictionary);
                        assert indexedEntry.entry.index() >= 0;
                    }
                    if (tokenIndexedEntries.add(indexedEntry) && !tokenData.htmlEntries.contains(indexedEntry.entry)) {
                        rows.add(indexedEntry.entry.CreateRow(rows.size(), index));
                        ++indexedEntry.entry.entrySource.numEntries;
                        ++numRows;

//            System.out.print("  " + typeToEntry.getKey() + ": ");
                        //          rows.get(rows.size() - 1).print(System.out);
//            System.out.println();
                    }
                }
            }

            if (tokenRow != null) {
                if (tokenRow.hasMainEntry) {
                    index.mainTokenCount++;
                }

                final Index.IndexEntry indexEntry = new Index.IndexEntry(index, tokenData.token, index
                        .normalizer().transliterate(tokenData.token), startRow, numRows, tokenData.htmlEntries);
                index.sortedIndexEntries.add(indexEntry);
            }
        }

        final List<IndexEntry> entriesSortedByNumRows = new ArrayList<>(index.sortedIndexEntries);
        entriesSortedByNumRows.sort((object1, object2) -> object2.numRows - object1.numRows);
        System.out.println("Most common tokens:");
        for (int i = 0; i < 50 && i < entriesSortedByNumRows.size(); ++i) {
            System.out.println("  " + entriesSortedByNumRows.get(i));
        }
    }

    public static class TokenData {
        final String token;

        final Map<EntryTypeName, List<IndexedEntry>> typeToEntries = new EnumMap<>(EntryTypeName.class);
        public boolean hasMainEntry = false;

        public final List<HtmlEntry> htmlEntries = new ArrayList<>();

        TokenData(final String token) {
            assert token.equals(token.trim());
            assert !token.isEmpty();
            this.token = token;
        }
    }

    public TokenData getOrCreateTokenData(final String token) {
        TokenData tokenData = fastTokenToData.get(token);
        if (tokenData != null) return tokenData;
        tokenData = new TokenData(token);
        final FastCompareString c = new FastCompareString(token);
        if (tokenToData.put(c, tokenData) != null) {
            // The parallel HashMap assumes that the TreeMap Comparator
            // is compatible with the equals it uses to compare.
            throw new RuntimeException("TokenData TreeMap and HashMap out of sync, Comparator may be broken?");
        }
        fastTokenToData.put(token, tokenData);
        return tokenData;
    }

    private List<IndexedEntry> getOrCreateEntries(final String token, final EntryTypeName entryTypeName) {
        final TokenData tokenData = getOrCreateTokenData(token);
        List<IndexedEntry> entries = tokenData.typeToEntries.get(entryTypeName);
        if (entryTypeName.mainWord) {
            tokenData.hasMainEntry = true;
        }
        if (entries == null) {
            entries = new ArrayList<>();
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

package com.hughes.android.dictionary.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.hughes.android.dictionary.Language;

public class IndexBuilder {
  
  final DictionaryBuilder dictionaryBuilder;
  final Index index;

  final SortedMap<String, TokenData> tokenToData;

  @SuppressWarnings("unchecked")
  IndexBuilder(final DictionaryBuilder dictionaryBuilder, final String shortName, final String longName, final Language language) {
    this.dictionaryBuilder = dictionaryBuilder;
    index = new Index(dictionaryBuilder.dictionary, shortName, longName, language);
    tokenToData = new TreeMap<String, TokenData>(language.getSortCollator());
  }
  
  public void build() {
    final Set<EntryData> tokenEntryDatas = new HashSet<EntryData>();
    final List<RowBase> rows = index.rows;
    for (final TokenData tokenData : tokenToData.values()) {
      tokenEntryDatas.clear();
      final int indexRow = index.sortedIndexEntries.size();
      index.sortedIndexEntries.add(new Index.IndexEntry(tokenData.token, rows.size()));
      rows.add(new TokenRow(indexRow, rows.size(), index));
      int count = 0;
      for (final List<EntryData> entryDatas : tokenData.typeToEntries.values()) {
        for (final EntryData entryData : entryDatas) {
          if (tokenEntryDatas.add(entryData)) {
            rows.add(new PairEntry.Row(entryData.index(), rows.size(), index));
            ++count;
          }
        }
      }
      System.out.println(count + " ENTRIES FOR TOKEN " + tokenData.token);
    }
  }
  
  static class TokenData {
    final String token;
        
    final Map<EntryTypeName, List<EntryData>> typeToEntries = new EnumMap<EntryTypeName, List<EntryData>>(EntryTypeName.class);
    
    TokenData(final String token) {
      assert token.equals(token.trim());
      assert token.length() > 0;
      this.token = token;
    }
  }

  public TokenData getOrCreateTokenData(final String token) {
    TokenData tokenData = tokenToData.get(token);
    if (tokenData == null) {
      tokenData = new TokenData(token);
      tokenToData.put(token, tokenData);
    }
    return tokenData;
  }

  public List<EntryData> getOrCreateEntries(final String token, final EntryTypeName entryTypeName) {
    final TokenData tokenData = getOrCreateTokenData(token);
    List<EntryData> entries = tokenData.typeToEntries.get(entryTypeName);
    if (entries == null) {
      entries = new ArrayList<EntryData>();
      tokenData.typeToEntries.put(entryTypeName, entries);
    }
    return entries;
  }
  

}

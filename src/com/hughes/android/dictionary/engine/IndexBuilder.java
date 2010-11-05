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


public class IndexBuilder {
  
  final DictionaryBuilder dictionaryBuilder;
  final Index index;

  final SortedMap<String, TokenData> tokenToData;

  @SuppressWarnings("unchecked")
  IndexBuilder(final DictionaryBuilder dictionaryBuilder, final String shortName, final String longName, final Language language, final boolean swapPairEntries) {
    this.dictionaryBuilder = dictionaryBuilder;
    index = new Index(dictionaryBuilder.dictionary, shortName, longName, language, swapPairEntries);
    tokenToData = new TreeMap<String, TokenData>(language.getSortCollator());
  }
  
  public void build() {
    final Set<EntryData> tokenEntryDatas = new HashSet<EntryData>();
    final List<RowBase> rows = index.rows;
    for (final TokenData tokenData : tokenToData.values()) {
      tokenEntryDatas.clear();
      final int indexIndex = index.sortedIndexEntries.size();
      final int startRow = rows.size();
      rows.add(new TokenRow(indexIndex, rows.size(), index));
//      System.out.println("Added TokenRow: " + rows.get(rows.size() - 1));
      int numRows = 0;
//      System.out.println("TOKEN: " + tokenData.token);
      for (final Map.Entry<EntryTypeName, List<EntryData>> typeToEntry : tokenData.typeToEntries.entrySet()) {
        for (final EntryData entryData : typeToEntry.getValue()) {
          if (tokenEntryDatas.add(entryData)) {
            rows.add(new PairEntry.Row(entryData.index(), rows.size(), index));
            ++numRows;
            
//            System.out.print("  " + typeToEntry.getKey() + ": ");
  //          rows.get(rows.size() - 1).print(System.out);
//            System.out.println();
          }
        }
      }
      index.sortedIndexEntries.add(new Index.IndexEntry(tokenData.token, startRow, numRows));
    }
    
    final List<IndexEntry> sortedEntries = new ArrayList<IndexEntry>(index.sortedIndexEntries);
    Collections.sort(sortedEntries, new Comparator<IndexEntry>() {
      @Override
      public int compare(IndexEntry object1, IndexEntry object2) {
        return object2.numRows - object1.numRows;
      }});
    System.out.println("Most common tokens:");
    for (int i = 0; i < 50 && i < sortedEntries.size(); ++i) {
      System.out.println("  " + sortedEntries.get(i));
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

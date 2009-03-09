package com.hughes.android.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.Dictionary.IndexEntry;
import com.hughes.android.dictionary.Dictionary.Row;

public class DictionaryBuilder {

  static final Pattern WHITESPACE = Pattern.compile("\\s+");

  public static void createIndex(final Dictionary dict, final byte lang) {

    final SortedMap<String, TokenData> sortedIndex = new TreeMap<String, TokenData>(
        EntryFactory.entryFactory.getEntryComparator());
    final EntryData entryDatas[] = new EntryData[dict.entries.size()];

    for (int e = 0; e < dict.entries.size(); ++e) {
      final Entry entry = dict.entries.get(e);
      final String text = entry.getIndexableText(lang);
      final Set<String> tokens = new LinkedHashSet<String>(Arrays
          .asList(WHITESPACE.split(text.trim())));
      entryDatas[e] = new EntryData(tokens.size());
      for (final String token : tokens) {
        TokenData tokenData = sortedIndex.get(token);
        if (tokenData == null) {
          tokenData = new TokenData(token);
          sortedIndex.put(token, tokenData);
        }
        tokenData.entries.add(e);
      }
    }

    // Sort it.

    final Comparator<Integer> entryComparator = new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return entryDatas[o1].numTokens < entryDatas[o2].numTokens ? -1
            : entryDatas[o1].numTokens == entryDatas[o2].numTokens ? 0 : 1;
      }
    };

    for (final TokenData tokenData : sortedIndex.values()) {
      Collections.sort(tokenData.entries, entryComparator);
    }

    // Put it all together.

    final List<Row> rows = dict.languages[lang].rows;
    final List<IndexEntry> indexEntries = dict.languages[lang].sortedIndex;

    int tokenDataIndex = 0;
    for (final TokenData tokenData : sortedIndex.values()) {
      final int startRow = rows.size();
      final IndexEntry indexEntry = new IndexEntry(tokenData.token, startRow);
      indexEntries.add(indexEntry);

      final Row tokenRow = new Row(-(tokenDataIndex + 1));
      rows.add(tokenRow);

      for (final Integer e : tokenData.entries) {
        final Row entryRow = new Row(e);
        rows.add(entryRow);
      }
      ++tokenDataIndex;
    }

  }

  static final class EntryData {
    final int numTokens;

    public EntryData(int numTokens) {
      this.numTokens = numTokens;
    }
  }

  static final class TokenData {
    final String token;
    final List<Integer> entries = new ArrayList<Integer>();

    int startRow;

    public TokenData(String token) {
      this.token = token;
    }
  }

}

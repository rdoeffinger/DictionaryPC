package com.hughes.android.dictionary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.Dictionary.IndexEntry;
import com.hughes.android.dictionary.Dictionary.Row;

public class DictionaryBuilder {

  static final Pattern WHITESPACE = Pattern.compile("\\s+");

  public static void main(String[] args) throws IOException,
      ClassNotFoundException {
    if (args.length != 2) {
      System.err.println("inputfile outputfile");
      return;
    }

    final Dictionary dict = new Dictionary("de", "en");
    final RandomAccessFile dictionaryFile = new RandomAccessFile(args[0], "r");
    String line;
    int lineCount = 0;
    long fileLocation = 0;
    while ((line = dictionaryFile.readLine()) != null) {
      assert ((int) fileLocation) == fileLocation;
      line = line.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      final Entry entry = Entry.parseFromLine(line);
      if (entry == null) {
        System.err.println("Invalid entry: " + line);
        continue;
      }

      dict.entries.add(entry);

      if (lineCount % 10000 == 0) {
        System.out.println("IndexBuilder: " + "lineCount=" + lineCount);
      }
      lineCount++;
      fileLocation = dictionaryFile.getFilePointer();
    }
    dictionaryFile.close();

    createIndex(dict, Entry.LANG1);
    createIndex(dict, Entry.LANG2);

    System.out.println("Writing dictionary.");
    final RandomAccessFile dictOut = new RandomAccessFile(args[1], "rw");
    dictOut.setLength(0);
    dict.write(dictOut);
    dictOut.close();
  }

  public static void createIndex(final Dictionary dict, final byte lang) {
    System.out.println("Creating index: " + lang);

    final Map<String, TokenData> tokenDatas = new HashMap<String, TokenData>();
    final EntryData entryDatas[] = new EntryData[dict.entries.size()];

    for (int e = 0; e < dict.entries.size(); ++e) {
      final Entry entry = dict.entries.get(e);
      final String text = entry.getIndexableText(lang);
      final Set<String> tokens = new LinkedHashSet<String>(Arrays
          .asList(WHITESPACE.split(text.trim())));
      entryDatas[e] = new EntryData(tokens.size());
      for (final String token : tokens) {
        TokenData tokenData = tokenDatas.get(token);
        if (tokenData == null) {
          tokenData = new TokenData(token);
          tokenDatas.put(token, tokenData);
        }
        tokenData.entries.add(e);
      }

      if (e % 10000 == 0) {
        System.out.println("createIndex: " + "e=" + e);
      }
    }

    // Sort it.

    final List<TokenData> sortedIndex = new ArrayList<TokenData>(tokenDatas
        .values());
    Collections.sort(sortedIndex);

    final Comparator<Integer> entryComparator = new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return entryDatas[o1].numTokens < entryDatas[o2].numTokens ? -1
            : entryDatas[o1].numTokens == entryDatas[o2].numTokens ? 0 : 1;
      }
    };

    for (final TokenData tokenData : tokenDatas.values()) {
      Collections.sort(tokenData.entries, entryComparator);
    }

    // Put it all together.

    final List<Row> rows = dict.languages[lang].rows;
    final List<IndexEntry> indexEntries = dict.languages[lang].sortedIndex;

    for (int t = 0; t < sortedIndex.size(); ++t) {
      final TokenData tokenData = sortedIndex.get(t);
      final int startRow = rows.size();
      final IndexEntry indexEntry = new IndexEntry(tokenData.token, startRow);
      indexEntries.add(indexEntry);

      final Row tokenRow = new Row(-(t + 1));
      rows.add(tokenRow);

      for (final Integer e : tokenData.entries) {
        final Row entryRow = new Row(e);
        rows.add(entryRow);
      }
    }

  }

  static final class EntryData {
    final int numTokens;

    public EntryData(int numTokens) {
      this.numTokens = numTokens;
    }
  }

  static final class TokenData implements Comparable<TokenData> {
    final String token;
    final List<Integer> entries = new ArrayList<Integer>();

    int startRow;

    public TokenData(String token) {
      this.token = token;
    }

    @Override
    public int compareTo(TokenData that) {
      return EntryFactory.entryFactory.getEntryComparator().compare(this.token,
          that.token);
    }
  }

}

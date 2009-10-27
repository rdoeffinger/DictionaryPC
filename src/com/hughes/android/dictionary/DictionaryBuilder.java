package com.hughes.android.dictionary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hughes.android.dictionary.Dictionary.IndexEntry;
import com.hughes.android.dictionary.Dictionary.Row;

public class DictionaryBuilder {
  
  static final List<InputFile> inputFiles = Arrays.asList(
      new InputFile("c:\\thad\\de-en-chemnitz.txt", Charset.forName("UTF8"), true),
      // Thad's extra sauce: 
      new InputFile("c:\\thad\\de-en-dictcc.txt", Charset.forName("Cp1252"), false)
      );
  static final String dictOutFilename = "c:\\thad\\de-en.dict";
  
  static class InputFile {
    final String file;
    final Charset charset;
    final boolean hasMultipleSubentries;
    public InputFile(String file, Charset charset, boolean hasMultipleSubentries) {
      this.file = file;
      this.charset = charset;
      this.hasMultipleSubentries = hasMultipleSubentries;
    }
  }

  public static void main(String[] args) throws IOException,
      ClassNotFoundException {

    final Dictionary dict = new Dictionary("de-en.txt - a German-English dictionary\n" +
    		"Version: devel, 2009-04-17\n" +
    		"Source: http://dict.tu-chemnitz.de/\n" +
    		"Thanks to Frank Richter.", Language.DE, Language.EN);
    System.out.println(Charset.forName("Cp1252"));
    for (final InputFile inputFile : inputFiles) {
      processInputFile(dict, inputFile);
    }
    
    createIndex(dict, Entry.LANG1);
    createIndex(dict, Entry.LANG2);

    System.out.println("Writing dictionary.");
    final RandomAccessFile dictOut = new RandomAccessFile(dictOutFilename, "rw");
    dictOut.setLength(0);
    dict.write(dictOut);
    dictOut.close();
  }

  private static void processInputFile(final Dictionary dict, final InputFile inputFile) throws FileNotFoundException, IOException {
    final BufferedReader dictionaryIn = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile.file), inputFile.charset));
    String line;
    int lineCount = 0;
    while ((line = dictionaryIn.readLine()) != null) {
//      System.out.println(line);
      line = line.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      final Entry entry = Entry.parseFromLine(line, inputFile.hasMultipleSubentries);
      if (entry == null) {
        System.err.println("Invalid entry: " + line);
        continue;
      }

      dict.entries.add(entry);

      if (lineCount % 10000 == 0) {
        System.out.println("IndexBuilder: " + "lineCount=" + lineCount);
      }
      lineCount++;
    }
    dictionaryIn.close();
  }

  public static void createIndex(final Dictionary dict, final byte lang) {
    System.out.println("Creating index: " + lang);

    final Map<String, TokenData> tokenDatas = new HashMap<String, TokenData>();
    final EntryData entryDatas[] = new EntryData[dict.entries.size()];

    for (int e = 0; e < dict.entries.size(); ++e) {
      final Entry entry = dict.entries.get(e);
      final Set<String> tokens = entry.getIndexableTokens(lang);
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

    System.out.println("Sorting TokenData...");
    final List<TokenData> sortedIndex = new ArrayList<TokenData>(tokenDatas
        .values());
    Collections.sort(sortedIndex, new Comparator<TokenData>() {
      @Override
      public int compare(TokenData tokenData0, TokenData tokenData1) {
        return dict.languageDatas[lang].language.sortComparator.compare(tokenData0.token, tokenData1.token);
      }});

    System.out.println("Sorting entries within each TokenData...");
    final Comparator<Integer> entryComparator = new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        // TODO: better this
        // Relevant (first token match) chemnitz entries first
        // first token position in entry
        // entry length in chars
        return entryDatas[o1].numTokens < entryDatas[o2].numTokens ? -1
            : entryDatas[o1].numTokens == entryDatas[o2].numTokens ? 0 : 1;
      }
    };
    for (final TokenData tokenData : tokenDatas.values()) {
      Collections.sort(tokenData.entries, entryComparator);
    }

    // Put it all together.
    System.out.println("Assembling final data structures...");
    final List<Row> rows = dict.languageDatas[lang].rows;
    final List<IndexEntry> indexEntries = dict.languageDatas[lang].sortedIndex;
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

  static final class TokenData {
    final String token;
    final List<Integer> entries = new ArrayList<Integer>();

    int startRow;

    public TokenData(final String token) {
      this.token = token;
    }
  }

}

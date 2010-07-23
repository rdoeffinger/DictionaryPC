package com.hughes.android.dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.hughes.android.dictionary.Dictionary.IndexEntry;
import com.hughes.android.dictionary.Dictionary.LanguageData;
import com.hughes.android.dictionary.Dictionary.Row;
import com.hughes.util.Args;
import com.hughes.util.FileUtil;

public class DictionaryBuilder {
  
  public static void main(String[] args) throws IOException,
      ClassNotFoundException, ParserConfigurationException, SAXException {
    
    final Map<String,String> keyValueArgs = Args.keyValueArgs(args);
    
    final Language lang1 = Language.lookup(keyValueArgs.remove("lang1"));
    final Language lang2 = Language.lookup(keyValueArgs.remove("lang2"));
    if (lang1 == null || lang2 == null) {
      fatalError("--lang1= and --lang2= must both be specified.");
    }
    
    final String dictOutFilename = keyValueArgs.remove("dictOut");
    if (dictOutFilename == null) {
      fatalError("--dictOut= must be specified.");
    }
    
    String summaryText = keyValueArgs.remove("summaryText");
    if (summaryText == null) {
      fatalError("--summaryText= must be specified.");
    }
    if (summaryText.startsWith("@")) {
      summaryText = FileUtil.readToString(new File(summaryText.substring(1)));
    }
    
    final String maxEntriesString = keyValueArgs.remove("maxEntries");
    final int maxEntries = maxEntriesString == null ? Integer.MAX_VALUE : Integer.parseInt(maxEntriesString);
    
    System.out.println("lang1=" + lang1);
    System.out.println("lang2=" + lang2);
    System.out.println("summaryText=" + summaryText);
    System.out.println("dictOut=" + dictOutFilename);    

    final Dictionary dict = new Dictionary(summaryText, lang1, lang2);

    for (int i = 0; i < 100; ++i) {
      final String prefix = "input" + i;
      if (keyValueArgs.containsKey(prefix)) {
        final File file = new File(keyValueArgs.remove(prefix));
        System.out.println("Processing: " + file);
        String charsetName = keyValueArgs.remove(prefix + "Charset");
        if (charsetName == null) {
          charsetName = "UTF8";
        }
        final Charset charset = Charset.forName(charsetName);
        String inputName = keyValueArgs.remove(prefix + "Name");
        if (inputName == null) {
          fatalError("Must specify human readable name for: " + prefix + "Name");
        }

        String inputFormat = keyValueArgs.remove(prefix + "Format");
        if ("dictcc".equals(inputFormat)) {
          processLinedInputFile(dict, file, charset, false, maxEntries);
        } else if ("chemnitz".equals(inputFormat)) {
          processLinedInputFile(dict, file, charset, true, maxEntries);
        } else if ("wiktionary".equals(inputFormat)) {
          new WiktionaryXmlParser(dict).parse(file);
        } else {
          fatalError("Invalid or missing input format: " + inputFormat);
        }
        
        dict.sources.add(inputName);
        System.out.println("Done: " + file + "\n\n");
      }
    }
    
    if (!keyValueArgs.isEmpty()) {
      System.err.println("WARNING: couldn't parse arguments: " + keyValueArgs);
    }
    
    createIndex(dict, SimpleEntry.LANG1);
    createIndex(dict, SimpleEntry.LANG2);

    System.out.println("Writing dictionary.");
    final RandomAccessFile dictOut = new RandomAccessFile(dictOutFilename, "rw");
    dictOut.setLength(0);
    dict.write(dictOut);
    dictOut.close();
    
    final Random random = new Random(0);
    for (byte lang = 0; lang < 2; ++lang) {
      final LanguageData languageData = dict.languageDatas[lang];
      System.out.println("\nRandom words for: " + languageData.language.getSymbol());
      for (int i = 0; i < 20; ++i) {
        final int w = random.nextInt(languageData.sortedIndex.size());
        final IndexEntry entry = languageData.sortedIndex.get(w);
        final List<Row> rows = languageData.rows;
        int r = entry.startRow;
        System.out.println(languageData.rowToString(rows.get(r), false));
        ++r;
        while (r < rows.size() && !rows.get(r).isToken()) {
          System.out.println("  " + languageData.rowToString(rows.get(r), false));
          ++r;
        }
      }
    }
  }

  private static void fatalError(String string) {
    System.err.println(string);
    System.exit(1);
  }

  private static void processLinedInputFile(final Dictionary dict, final File file,
      final Charset charset, final boolean hasMultipleSubentries,
      final int maxEntries) throws FileNotFoundException, IOException {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
    String line;
    int lineCount = 0;
    while ((line = reader.readLine()) != null && lineCount < maxEntries) {
      if (maxEntries < 200) { 
        System.out.println(line);
      }
      line = line.trim();
      if (line.equals("") || line.startsWith("#")) {
        continue;
      }

      final SimpleEntry entry = SimpleEntry.parseFromLine(line, hasMultipleSubentries);
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
    reader.close();
  }

  public static void createIndex(final Dictionary dict, final byte lang) {
    System.out.println("Creating index: " + lang);

    final Map<String, TokenData> tokenToData = new TreeMap<String, TokenData>(dict.languageDatas[lang].language.sortComparator);

    for (int e = 0; e < dict.entries.size(); ++e) {
      final SimpleEntry entry = dict.entries.get(e);
      final Set<String> tokens = entry.getIndexableTokens(lang);
      for (final String token : tokens) {
        TokenData tokenData = tokenToData.get(token);
        if (tokenData == null) {
          tokenData = new TokenData(token);
          tokenToData.put(token, tokenData);
        }
        tokenData.entries.add(new TokenEntryData(lang, token, entry, e));
      }

      if (e % 10000 == 0) {
        System.out.println("createIndex: " + "e=" + e);
      }
    }

    // Sort it.

    System.out.println("Sorting TokenData...");
    final List<TokenData> sortedTokenData = new ArrayList<TokenData>(tokenToData
        .values());

    System.out.println("Sorting entries within each TokenData...");
    for (final TokenData tokenData : sortedTokenData) {
      Collections.sort(tokenData.entries);
    }

    // Put it all together.
    System.out.println("Assembling final data structures...");
    final List<Row> rows = dict.languageDatas[lang].rows;
    final List<IndexEntry> indexEntries = dict.languageDatas[lang].sortedIndex;
    for (int t = 0; t < sortedTokenData.size(); ++t) {
      final TokenData tokenData = sortedTokenData.get(t);
      final int startRow = rows.size();
      final IndexEntry indexEntry = new IndexEntry(tokenData.token, startRow);
      indexEntries.add(indexEntry);

      final Row tokenRow = new Row(-(t + 1));
      rows.add(tokenRow);

      for (final TokenEntryData entryData : tokenData.entries) {
        final Row entryRow = new Row(entryData.entryIndex);
        rows.add(entryRow);
      }
    }

  }

  static final class TokenEntryData implements Comparable<TokenEntryData> {
    final String token;
    final SimpleEntry entry;
    final int entryIndex;
    
    private static final int bigNoOverflow = 100000;

    int minSubEntryIndexOf = bigNoOverflow;
    int minSubEntryLength = bigNoOverflow;
    int minSubEntry = bigNoOverflow;

    public TokenEntryData(final byte lang, final String token, final SimpleEntry entry, final int entryIndex) {
      this.token = token;
      this.entry = entry;
      this.entryIndex = entryIndex;
      
      final String[] subentries = entry.getAllText(lang);
      for (int s = 0; s < subentries.length; ++s) {
        final String subentry = subentries[s];
        int indexOf = subentry.indexOf(token);
        if (indexOf != -1) {
          minSubEntryIndexOf = Math.min(minSubEntryIndexOf, indexOf); 
          minSubEntryLength = Math.min(minSubEntryLength, subentry.length());
          minSubEntry = Math.min(minSubEntry, s);
        }
      }
    }

    @Override
    public int compareTo(final TokenEntryData that) {
      assert this.token.equals(that.token);
      
      if (this.minSubEntryIndexOf != that.minSubEntryIndexOf) {
        return this.minSubEntryIndexOf - that.minSubEntryIndexOf;
      }
      if (this.minSubEntryLength != that.minSubEntryLength) {
        return this.minSubEntryLength - that.minSubEntryLength;
      }
      return this.minSubEntry - that.minSubEntry;
    }
  }

  static final class TokenData {
    final String token;
    final List<TokenEntryData> entries = new ArrayList<TokenEntryData>();

    int startRow;

    public TokenData(final String token) {
      this.token = token;
    }
  }

}

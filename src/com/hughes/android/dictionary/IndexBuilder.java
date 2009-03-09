package com.hughes.android.dictionary;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.hughes.util.FileUtil;

public class IndexBuilder {

  static final Pattern WHITESPACE = Pattern.compile("\\s+");
  static final Pattern NONALPHA = Pattern.compile("[^A-Za-z]+");

  public static void main(String[] args) throws IOException,
      ClassNotFoundException {
    if (args.length != 1) {
      System.err.println("No input file.");
      return;
    }
    final String dictionaryFileName = args[0];
    createIndex(dictionaryFileName, Entry.LANG1);
    createIndex(dictionaryFileName, Entry.LANG2);
  }

  private static void createIndex(final String dictionaryFileName,
      final byte lang) throws IOException, FileNotFoundException,
      ClassNotFoundException {
    Node rootBuilder;
    rootBuilder = processDictionaryLines(dictionaryFileName, lang);
    FileUtil.write(rootBuilder, String.format("%s_builder_%d.serialized", dictionaryFileName, lang));
    rootBuilder = (Node) FileUtil.read(String.format("%s_builder_%d.serialized", dictionaryFileName, lang));

    rootBuilder.forEachNode(new Function<Node>() {
      @Override
      public void invoke(final Node node) {
        for (final List<EntryDescriptor> entryDescriptors : node.entryDescriptorsMap.values()) {
          Collections.sort(entryDescriptors);
        }
      }});
    
    // Dump twice to get accurate file locations.
    for (int i = 0; i < 2; ++i) {
      final RandomAccessFile raf = new RandomAccessFile(String.format(Dictionary.INDEX_FORMAT, dictionaryFileName, lang), "rw"); 
      rootBuilder.dump(raf);
      raf.close();
    }
  }

  // ----------------------------------------------------------------
  
  static final class EntryDescriptor implements Comparable<EntryDescriptor>, Serializable {
    final int offset;
    final int numTokens;
    public EntryDescriptor(int offset, int numTokens) {
      this.offset = offset;
      this.numTokens = numTokens;
    }
    @Override
    public boolean equals(Object obj) {
      final EntryDescriptor that = (EntryDescriptor) obj;
      return this.offset == that.offset;
    }
    @Override
    public int hashCode() {
      return offset;
    }
    @Override
    public int compareTo(EntryDescriptor o) {
      return this.numTokens < o.numTokens ? -1 : this.numTokens == o.numTokens ? 0 : 1;
    }
  }


  // ----------------------------------------------------------------

  static Node processDictionaryLines(final String dictionaryFileName, final byte lang) throws IOException {
    final Node root = new Node("");
    final RandomAccessFile dictionaryFile = new RandomAccessFile(dictionaryFileName, "r");
    String line;
    final Entry entry = new Entry();
    int lineCount = 0;
    long fileLocation = 0;
    while ((line = dictionaryFile.readLine()) != null) {
      assert ((int) fileLocation) == fileLocation;

      line = line.trim();
      if (line.isEmpty() || line.startsWith("#") || !entry.parseFromLine(line)) {
        continue;
      }
      final String text = entry.getIndexableText(Entry.LANG1);
      final String[] tokens = WHITESPACE.split(text);
      final Map<String,String> tokenToNormalizedMap = new LinkedHashMap<String,String>();
      for (String token : tokens) {
        if (token.length() <= 1 || !Character.isLetter(token.charAt(0))) {
          continue;
        }
        tokenToNormalizedMap.put(token, EntryFactory.entryFactory.normalizeToken(token));
      }
      for (final Map.Entry<String, String> tokenToNormalized : tokenToNormalizedMap.entrySet()) {
        final String normalizedToken = tokenToNormalized.getValue();
        final Node node = root.getNode(normalizedToken, 0, true);
        node.addToken(tokenToNormalized.getKey(), new EntryDescriptor((int) fileLocation, tokens.length));
        assert node == root.getNode(normalizedToken, 0, false);
        assert normalizedToken
            .equals(root.getNode(normalizedToken, 0, false).normalizedToken);
      }

      if (lineCount % 10000 == 0) {
        System.out.println("IndexBuilder: " + "lineCount=" + lineCount);
      }
      
      lineCount++;
      fileLocation = dictionaryFile.getFilePointer();
    }
    dictionaryFile.close();
    
    root.recursiveSetDescendantCounts();
    
    return root;
  }

}

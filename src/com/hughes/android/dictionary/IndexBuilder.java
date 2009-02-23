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

  static final class Node implements Serializable {
    final String normalizedToken;
    
    final TreeMap<String, Node> children = new TreeMap<String, Node>();
    final TreeMap<String,List<EntryDescriptor>> entryDescriptorsMap = new TreeMap<String, List<EntryDescriptor>>();
    
//    final List<EntryDescriptor> offsets = new ArrayList<EntryDescriptor>();
    
    int indexFileLocation = -1;

    private int descendantTokenCount;
    private int descendantEntryCount = 0;

    public Node(final String normalizedToken) {
      if (normalizedToken.length() == 0) {
        System.out.println("Created root.");
      }
      this.normalizedToken = normalizedToken.intern();
    }

    public Node getNode(final String nToken, final int pos,
        final boolean create) {
      assert this.normalizedToken.equals(nToken.substring(0, pos));

      if (pos == nToken.length()) {
        assert normalizedToken.equals(nToken);
        return this;
      }

      final String rest = nToken.substring(pos);
      assert rest.length() > 0;

      final Map.Entry<String, Node> lcsEntry;
      final String lcs;
      {
        final Map.Entry<String, Node> floorEntry = children.floorEntry(rest);
        final Map.Entry<String, Node> ceilingEntry = children
            .ceilingEntry(rest);
        final String floorLcs = floorEntry == null ? "" : StringUtil
            .longestCommonSubstring(rest, floorEntry.getKey());
        final String ceilingLcs = ceilingEntry == null ? "" : StringUtil
            .longestCommonSubstring(rest, ceilingEntry.getKey());
        if (floorLcs.length() > ceilingLcs.length()) {
          lcsEntry = floorEntry;
          lcs = floorLcs;
        } else {
          lcsEntry = ceilingEntry;
          lcs = ceilingLcs;
        }
      }

      // No LCS, have to add everything.
      if (lcs.length() == 0) {
        if (!create) {
          return null;
        }
        final Node result = new Node(nToken);
        final Object old = children.put(rest.intern(), result);
        assert old == null;
        // System.out.println("  Adding final chunk: " + rest);
        return result;
      }

      assert lcsEntry != null;

      // The map already contained the LCS.
      if (lcs.length() == lcsEntry.getKey().length()) {
        assert lcs.equals(lcsEntry.getKey());
        final Node result = lcsEntry.getValue().getNode(nToken,
            pos + lcs.length(), create);
        assert result.normalizedToken.equals(nToken);
        return result;
      }

      if (!create) {
        return null;
      }

      // Have to split, inserting the LCS.
      // System.out.println("  Splitting " + lcsEntry + "/" + word + " @ " +
      // lcs);
      final Node newChild = new Node(nToken.substring(0, pos + lcs.length()));
      final Object old = children.put(lcs.intern(), newChild);
      assert old == null;
      children.remove(lcsEntry.getKey());
      newChild.children.put(lcsEntry.getKey().substring(lcs.length())
          .intern(), lcsEntry.getValue());

      if (lcs.equals(rest)) {
        return newChild;
      }
      final Node result = new Node(nToken);
      final Object old2 = newChild.children.put(rest.substring(lcs.length())
          .intern(), result);
      assert old2 == null;
      // System.out.println("  newchildren=" + newChild.children);

      return result;
    }

    void forEachNode(final Function<Node> f) {
      f.invoke(this);
      for (final Node child : children.values()) {
        child.forEachNode(f);
      }
    }

    int descendantCount() {
      int count = 1;
      for (final Node child : children.values()) {
        count += child.descendantCount();
      }
      return count;
    }

    void recursiveSetDescendantCounts() {
      descendantTokenCount = entryDescriptorsMap.size();
      descendantEntryCount = 0;

      for (final Node child : children.values()) {
        child.recursiveSetDescendantCounts();
        descendantTokenCount += child.descendantTokenCount;
        descendantEntryCount += child.descendantEntryCount;
      }

      for (final List<EntryDescriptor> entryDescriptors : entryDescriptorsMap.values()) {
        descendantEntryCount += entryDescriptors.size();
      }
    }

    @Override
    public String toString() {
      return normalizedToken;
    }
    
    void dump(final RandomAccessFile file) throws IOException {
      if (indexFileLocation == -1) {
        indexFileLocation = (int) file.getFilePointer();
      } else {
        assert indexFileLocation == file.getFilePointer();
      }
      
      // Children to location.
      file.writeInt(children.size());
      for (final Map.Entry<String, Node> child : children.entrySet()) {
        file.writeUTF(child.getKey());
        file.writeInt(child.getValue().indexFileLocation);
      }
      
      // Entries.
      file.writeInt(entryDescriptorsMap.size());
      for (final Map.Entry<String, List<EntryDescriptor>> entry : entryDescriptorsMap.entrySet()) {
        file.writeUTF(entry.getKey());
        file.writeInt(entry.getValue().size());
        for (int i = 0; i < entry.getValue().size(); ++i) {
          file.writeInt(entry.getValue().get(i).offset);
        }
      }

      // Dump counts.
      file.writeInt(descendantTokenCount);
      file.writeInt(descendantEntryCount);
      
      // Dump children.
      for (final Map.Entry<String, Node> child : children.entrySet()) {
        child.getValue().dump(file);
      }
    }

    public void addToken(final String token, final EntryDescriptor entryDescriptor) {
      List<EntryDescriptor> entryDescriptors = this.entryDescriptorsMap.get(token);
      if (entryDescriptors == null) {
        entryDescriptors = new ArrayList<EntryDescriptor>();
        this.entryDescriptorsMap.put(token, entryDescriptors);
      }
      entryDescriptors.add(entryDescriptor);
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

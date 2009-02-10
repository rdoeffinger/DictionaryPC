package com.hughes.android.dictionary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    final String file = args[0];
    final byte lang = Entry.LANG1;
    Node rootBuilder;
    rootBuilder = createIndex(file, lang);
    FileUtil.write(rootBuilder, String.format("%s_builder_%d.serialized", file, lang));
    rootBuilder = (Node) FileUtil.read(String.format("%s_builder_%d.serialized", file, lang));

//    final AtomicInteger c = new AtomicInteger();
    rootBuilder.forEachNode(new Function<Node>() {
      @Override
      public void invoke(Node t) {
        Collections.sort(t.offsets);
//        if (t.offsets.size() > 128) {
//          System.out.println(t);
//          c.incrementAndGet();
//        }
      }});
//    System.out.println(c);
    
//    rootBuilder.recursiveSetDescendantOffsetCount();
//    rootBuilder.packDescendants(128);

    // Dump twice to get accurate file locations.
    for (int i = 0; i < 2; ++i) {
      final RandomAccessFile raf = new RandomAccessFile(String.format(Dictionary.INDEX_FORMAT, file, lang), "rw"); 
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
    private static final long serialVersionUID = -5423134653901704956L;
    
    final TreeMap<String, Node> children = new TreeMap<String, Node>();
    final List<EntryDescriptor> offsets = new ArrayList<EntryDescriptor>();
    final String sequence;
    
    int descendantOffsetCount = 0;
    
    int indexFileLocation = -1;

    public Node(String sequence) {
      if (sequence.length() == 0) {
        System.out.println("Created root.");
      }
      this.sequence = sequence.intern();
    }

    public Node getIndexNode(final String word, final int pos,
        final boolean create) {
      assert this.sequence.equals(word.substring(0, pos));

      if (pos == word.length()) {
        assert sequence.equals(word);
        return this;
      }

      final String rest = word.substring(pos);
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
        final Node result = new Node(word);
        final Object old = children.put(rest.intern(), result);
        assert old == null;
        // System.out.println("  Adding final chunk: " + rest);
        return result;
      }

      assert lcsEntry != null;

      // The map already contained the LCS.
      if (lcs.length() == lcsEntry.getKey().length()) {
        assert lcs.equals(lcsEntry.getKey());
        final Node result = lcsEntry.getValue().getIndexNode(word,
            pos + lcs.length(), create);
        assert result.sequence.equals(word);
        return result;
      }

      if (!create) {
        return null;
      }

      // Have to split, inserting the LCS.
      // System.out.println("  Splitting " + lcsEntry + "/" + word + " @ " +
      // lcs);
      final Node newChild = new Node(word.substring(0, pos + lcs.length()));
      final Object old = children.put(lcs.intern(), newChild);
      assert old == null;
      children.remove(lcsEntry.getKey());
      newChild.children.put(lcsEntry.getKey().substring(lcs.length())
          .intern(), lcsEntry.getValue());

      if (lcs.equals(rest)) {
        return newChild;
      }
      final Node result = new Node(word);
      final Object old2 = newChild.children.put(rest.substring(lcs.length())
          .intern(), result);
      assert old2 == null;
      // System.out.println("  newchildren=" + newChild.children);

      return result;
    }

    MemoryIndex.Node toIndexNode() {
      final MemoryIndex.Node result = new MemoryIndex.Node(children.size(), offsets
          .size());
      int i = 0;
      for (final Map.Entry<String, Node> entry : children.entrySet()) {
        result.chars[i] = entry.getKey();
        result.children[i] = entry.getValue().toIndexNode();
        i++;
      }
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

    void recursiveSetDescendantOffsetCount() {
      descendantOffsetCount = offsets.size();
      for (final Node child : children.values()) {
        child.recursiveSetDescendantOffsetCount();
        descendantOffsetCount += child.descendantOffsetCount;
      }
    }

    public void packDescendants(final int maxDescendants) {
      if (descendantOffsetCount <= maxDescendants) {
        final Set<EntryDescriptor> descendantOffsets = new LinkedHashSet<EntryDescriptor>();
        recursiveAddDescendants(descendantOffsets);
        assert descendantOffsets.size() <= maxDescendants;
        offsets.clear();
        offsets.addAll(descendantOffsets);
        children.clear();
      } else {
        for (final Node child : children.values()) {
          child.packDescendants(maxDescendants);
        }
      }
    }

    private void recursiveAddDescendants(final Set<EntryDescriptor> descendantOffsets) {
      descendantOffsets.addAll(this.offsets);
      for (final Node child : children.values()) {
        child.recursiveAddDescendants(descendantOffsets);
      }
    }

    @Override
    public String toString() {
      return sequence + ":" + offsets.size();
    }
    
    void dump(final RandomAccessFile file) throws IOException {
      if (indexFileLocation == -1) {
        indexFileLocation = (int) file.getFilePointer();
      } else {
        assert indexFileLocation == file.getFilePointer();
      }
      
      // Children.
      file.writeInt(children.size());
      for (final Map.Entry<String, Node> child : children.entrySet()) {
        file.writeUTF(child.getKey());
        file.writeInt(child.getValue().indexFileLocation);
      }
      
      // Offsets.
      file.writeInt(offsets.size());
      for (int i = 0; i < offsets.size(); i++) {
        file.writeInt(offsets.get(i).offset);
      }
      
      // Dump children.
      for (final Map.Entry<String, Node> child : children.entrySet()) {
        child.getValue().dump(file);
      }
    }
  }

  // ----------------------------------------------------------------

  static Node createIndex(final String file, final byte lang) throws IOException {
    final Node root = new Node("");
    final RandomAccessFile raf = new RandomAccessFile(file, "r");
    String line;
    final Entry entry = new Entry();
    int lineCount = 0;
    long fileLocation = 0;
    while ((line = raf.readLine()) != null) {
      assert ((int) fileLocation) == fileLocation;

      line = line.trim();
      if (line.isEmpty() || line.startsWith("#") || !entry.parseFromLine(line)) {
        continue;
      }
      final String text = entry.getIndexableText(Entry.LANG1);
      final String[] tokens = WHITESPACE.split(text);
      final Set<String> tokenSet = new LinkedHashSet<String>();
      for (String token : tokens) {
        if (token.length() <= 1 || !Character.isLetter(token.charAt(0))) {
          continue;
        }
        tokenSet.add(entry.normalizeToken(token, lang));
      }
      for (final String normalized : tokenSet) {
        // System.out.println("Inserting: " + normalized);
        if ("die".equals(normalized) || "eine".equals(normalized)) {
          // System.out.println("hello");
        }
        final Node node = root.getIndexNode(normalized, 0, true);
        node.offsets.add(new EntryDescriptor((int) fileLocation, tokens.length));
        assert node == root.getIndexNode(normalized, 0, false);
        assert normalized
            .equals(root.getIndexNode(normalized, 0, false).sequence);
      }

      if (lineCount % 10000 == 0) {
        System.out.println("IndexBuilder: " + "lineCount=" + lineCount);
      }
      
      lineCount++;
      fileLocation = raf.getFilePointer();
    }
    raf.close();
    return root;
  }

}

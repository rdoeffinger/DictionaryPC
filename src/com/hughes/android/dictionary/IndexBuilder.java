package com.hughes.android.dictionary;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.Index.Node;
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
//    rootBuilder = createIndex(file, lang);
//    FileUtil.write(rootBuilder, String.format("%s_builder_%d.serialized", file, lang));
    rootBuilder = (Node) FileUtil.read(String.format("%s_builder_%d.serialized", file, lang));
    
    final AtomicInteger c = new AtomicInteger();
    rootBuilder.forEachNode(new Function<Node>() {
      @Override
      public void invoke(Node t) {
        if (t.offsetsList.size() > 200) {
          System.out.println(t);
          c.incrementAndGet();
        }
      }});
    System.out.println(c);
    
    rootBuilder.recursiveSetDescendantOffsetCount();
    rootBuilder.packDescendants(128);

    final DataOutputStream os = new DataOutputStream(new FileOutputStream(
        String.format("%s_index_%d", file, lang)));
    final Index.Node root = rootBuilder.toIndexNode();
    root.write(os);
    os.close();
    
    FileUtil.write(root, String.format("%s_index_%d.serialized", file, lang));
    
    Object o = FileUtil.read(String.format("%s_index_%d.serialized", file, lang));


  }

  // ----------------------------------------------------------------

  static final class Node implements Serializable {
    private static final long serialVersionUID = -5423134653901704956L;
    
    final TreeMap<String, Node> childrenMap = new TreeMap<String, Node>();
    final List<Integer> offsetsList = new ArrayList<Integer>();
    final String sequence;
    int descendantOffsetCount = 0;

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
        final Map.Entry<String, Node> floorEntry = childrenMap.floorEntry(rest);
        final Map.Entry<String, Node> ceilingEntry = childrenMap
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
        final Object old = childrenMap.put(rest.intern(), result);
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
      final Object old = childrenMap.put(lcs.intern(), newChild);
      assert old == null;
      childrenMap.remove(lcsEntry.getKey());
      newChild.childrenMap.put(lcsEntry.getKey().substring(lcs.length())
          .intern(), lcsEntry.getValue());

      if (lcs.equals(rest)) {
        return newChild;
      }
      final Node result = new Node(word);
      final Object old2 = newChild.childrenMap.put(rest.substring(lcs.length())
          .intern(), result);
      assert old2 == null;
      // System.out.println("  newChildrenMap=" + newChild.childrenMap);

      return result;
    }

    Index.Node toIndexNode() {
      final Index.Node result = new Index.Node(childrenMap.size(), offsetsList
          .size());
      int i = 0;
      for (final Map.Entry<String, Node> entry : childrenMap.entrySet()) {
        result.chars[i] = entry.getKey();
        result.children[i] = entry.getValue().toIndexNode();
        i++;
      }
      return result;
    }

    void forEachNode(final Function<Node> f) {
      f.invoke(this);
      for (final Node child : childrenMap.values()) {
        child.forEachNode(f);
      }
    }

    int descendantCount() {
      int count = 1;
      for (final Node child : childrenMap.values()) {
        count += child.descendantCount();
      }
      return count;
    }

    void recursiveSetDescendantOffsetCount() {
      descendantOffsetCount = offsetsList.size();
      for (final Node child : childrenMap.values()) {
        child.recursiveSetDescendantOffsetCount();
        descendantOffsetCount += child.descendantOffsetCount;
      }
    }

    public void packDescendants(final int maxDescendants) {
      if (descendantOffsetCount <= maxDescendants) {
        final Set<Integer> descendantOffsets = new LinkedHashSet<Integer>();
        recursiveAddDescendants(descendantOffsets);
        assert descendantOffsets.size() <= maxDescendants;
        offsetsList.clear();
        offsetsList.addAll(descendantOffsets);
        childrenMap.clear();
      } else {
        for (final Node child : childrenMap.values()) {
          child.packDescendants(maxDescendants);
        }
      }
    }

    private void recursiveAddDescendants(final Set<Integer> descendantOffsets) {
      descendantOffsets.addAll(this.offsetsList);
      for (final Node child : childrenMap.values()) {
        child.recursiveAddDescendants(descendantOffsets);
      }
    }


    @Override
    public String toString() {
      return sequence + ":" + offsetsList.size();
    }

  }

  // ----------------------------------------------------------------

  static Node createIndex(final String file, final byte lang) throws IOException {
    final Node root = new Node("");
    final RandomAccessFile raf = new RandomAccessFile(file, "r");
    String line;
    final Entry entry = new Entry();
    int lineCount = 0;
    while ((line = raf.readLine()) != null) {
      final long fileLocation = raf.getFilePointer();
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
        node.offsetsList.add((int) fileLocation);
        assert node == root.getIndexNode(normalized, 0, false);
        assert normalized
            .equals(root.getIndexNode(normalized, 0, false).sequence);
      }

      if (lineCount % 10000 == 0) {
        System.out.println("IndexBuilder: " + "lineCount=" + lineCount);
      }
      lineCount++;
    }
    raf.close();
    return root;
  }

}

package com.hughes.android.dictionary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

import com.hughes.android.dictionary.Index.Node;
import com.hughes.util.FileUtil;

public class IndexTest extends TestCase {

  static final String file = "c:\\dict-de-en.txt";
  static final String file_index = file + "_index_0";
  
  public void testLookup() throws IOException {
    System.out.println("testLookup");
    final Index index = new Index(file_index);
    final Node node = index.lookup("handhubwagen");
    assertNotNull(node);
    
    final RandomAccessFile raf = new RandomAccessFile(file, "r");
    for (int i = 0; i < node.offsets.length; ++i) {
      final String entry = FileUtil.readLine(raf, node.offsets[i]);
      System.out.println(entry);
      assertTrue(entry.toLowerCase().contains("handhubwagen"));
    }
  }

  public void testGetDescendantOffsets() throws IOException {
    System.out.println("testGetDescendantOffsets");
    final Index index = new Index(file_index);
    
    final Node node = index.lookup("handhebe");
    assertNotNull(node);
    assertEquals("handhebel", node.text);
    final Set<Integer> offsets = new LinkedHashSet<Integer>();
    node.getDescendantEntryOffsets(offsets, 10);
    final RandomAccessFile raf = new RandomAccessFile(file, "r");
    for (final Integer offset : offsets) {
      final String entry = FileUtil.readLine(raf, offset);
      System.out.println(entry);
      assertTrue(entry.toLowerCase().contains(node.text));
    }
  }

}

package com.hughes.android.dictionary;
//package com.hughes.android.dictionary;
//
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.util.LinkedHashSet;
//import java.util.Set;
//
//import junit.framework.TestCase;
//
//import com.hughes.android.dictionary.Index.Node;
//import com.hughes.util.FileUtil;
//
//public class IndexTest extends TestCase {
//
//  static final String file = "c:\\dict-de-en.txt";
//  static final String file_index = file + "_index_0";
//  
//  public void testRoot() throws IOException {
//    System.out.println("  testRoot");
//    final Index index = new Index(file_index);
//    final Node node = index.lookup("");
//    assertNotNull(node);
//    
//    assertEquals(312220, node.descendantTokenCount);
//    assertEquals(1087063, node.descendantEntryCount);
//    
//    for (final String token : node.tokenToOffsets.keySet()) {
//      System.out.println(token);
//      assertTrue(token.toLowerCase().contains("handhubwagen"));
//    }
//  }
//  
//  public void testLookup() throws IOException {
//    System.out.println("  testLookup");
//    final Index index = new Index(file_index);
//    final Node node = index.lookup("handhubwagen");
//    assertNotNull(node);
//    
//    assertEquals(1, node.descendantTokenCount);
//    assertEquals(2, node.descendantEntryCount);
//    
//    for (final String token : node.tokenToOffsets.keySet()) {
//      System.out.println(token);
//      assertTrue(token.toLowerCase().contains("handhubwagen"));
//    }
//  }
//
//  public void testGetDescendantOffsets() throws IOException {
//    System.out.println("  testGetDescendantOffsets");
//    final Index index = new Index(file_index);
//    
//    final Node node = index.lookup("handhebe");
//    assertNotNull(node);
//    assertEquals("handhebel", node.nodeHandle.normalizedToken);
//    final Set<Integer> offsets = new LinkedHashSet<Integer>();
//    node.getDescendantEntryOffsets(offsets, 10);
//    final RandomAccessFile raf = new RandomAccessFile(file, "r");
//    for (final Integer offset : offsets) {
//      final String entry = FileUtil.readLine(raf, offset);
//      System.out.println(entry);
//      assertTrue(entry.toLowerCase().contains(node.nodeHandle.normalizedToken));
//    }
//  }
//
//  public void testGetDescendants() throws IOException {
//    System.out.println("  testGetDescendant");
//    final Index index = new Index(file_index);
//    final RandomAccessFile raf = new RandomAccessFile(file, "r");
//    for (int i = 1000000; i < 1000050; ++i) {
//      final Object o = index.root.getDescendant(i);
//      if (o instanceof Integer) {
//        System.out.println("  " + FileUtil.readLine(raf, (Integer)o));
//      } else {
//        System.out.println(o);
//      }
//    }
//    raf.close();
//  }
//
//}

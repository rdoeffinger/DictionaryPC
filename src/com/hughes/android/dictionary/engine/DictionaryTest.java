package com.hughes.android.dictionary.engine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

public class DictionaryTest extends TestCase {
  
  RandomAccessFile raf;
  Dictionary dict;
  Index deIndex; 
  
  @Override
  public void setUp() {
    try {
      raf = new RandomAccessFile("testdata/de_en.dict", "r");
      dict = new Dictionary(raf);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    deIndex = dict.indices.get(0);
}
  
  @Override
  public void tearDown() {
    try {
      raf.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  

  public void testGermanMetadata() throws IOException {
    assertEquals("de", deIndex.shortName);
    assertEquals("de->en", deIndex.longName);
  }
  
  public void testGermanIndex() throws IOException {
    for (final Index.IndexEntry indexEntry : deIndex.sortedIndexEntries) {
      System.out.println("testing: " + indexEntry.token);
      final TokenRow row = deIndex.find(indexEntry.token, new AtomicBoolean(
          false));
      assertEquals(indexEntry.token.toLowerCase(), row.getToken().toLowerCase());
    }

    assertEquals("aaac", deIndex.find("AAAC", new AtomicBoolean(false)).getToken());
    assertEquals("aaac", deIndex.find("aaac", new AtomicBoolean(false)).getToken());
    assertEquals("aaac", deIndex.find("AAAc", new AtomicBoolean(false)).getToken());
    assertEquals("aaac", deIndex.find("aaac", new AtomicBoolean(false)).getToken());
  }
  
  public void testGermanTokenRows() {
    // Pre-cache a few of these, just to make sure that's working.
    for (int i = 0; i < deIndex.rows.size(); i += 7) {
      deIndex.rows.get(i).getTokenRow(true);
    }
    
    // Do the exhaustive searching.
    TokenRow lastTokenRow = null;
    for (final RowBase row : deIndex.rows) {
      if (row instanceof TokenRow) {
        lastTokenRow = (TokenRow) row;
      }
      assertEquals(lastTokenRow, row.getTokenRow(true));
    }

    // Now they're all cached, we shouldn't have to search.
    for (final RowBase row : deIndex.rows) {
      if (row instanceof TokenRow) {
        lastTokenRow = (TokenRow) row;
      }
      // This will break if the Row cache isn't big enough.
      assertEquals(lastTokenRow, row.getTokenRow(false));
    }

  }


}

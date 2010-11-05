package com.hughes.android.dictionary.engine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.hughes.android.dictionary.engine.Index.SearchResult;


public class DictionaryTest extends TestCase {
    
  public void testGermanMetadata() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.dict", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    assertEquals("de", deIndex.shortName);
    assertEquals("de->en", deIndex.longName);
    
    raf.close();
  }
  
  public void testGermanIndex() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.dict", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    for (final Index.IndexEntry indexEntry : deIndex.sortedIndexEntries) {
      System.out.println("testing: " + indexEntry.token);
      final Index.SearchResult searchResult = deIndex.findLongestSubstring(indexEntry.token, new AtomicBoolean(
          false));
      assertEquals(indexEntry.token.toLowerCase(), searchResult.insertionPoint.token.toLowerCase());
      assertEquals(indexEntry.token.toLowerCase(), searchResult.longestPrefix.token.toLowerCase());
    }

    // TODO: maybe if user types capitalization, use it.
    assertSearchResult("aaac", "aaac", deIndex.findLongestSubstring("aaac", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findLongestSubstring("AAAC", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findLongestSubstring("AAAc", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findLongestSubstring("aAac", new AtomicBoolean(false)));

    // Before the beginning.
    assertSearchResult("40", "40" /* special case */, deIndex.findLongestSubstring("", new AtomicBoolean(false)));
    assertSearchResult("40", "40" /* special case */, deIndex.findLongestSubstring("__", new AtomicBoolean(false)));
    
    // After the end.
    assertSearchResult("Zweckorientiertheit", "zählen", deIndex.findLongestSubstring("ZZZZZ", new AtomicBoolean(false)));

    assertSearchResult("ab", "aaac", deIndex.findLongestSubstring("aaaca", new AtomicBoolean(false)));
    assertSearchResult("machen", "machen", deIndex.findLongestSubstring("m", new AtomicBoolean(false)));

    assertFalse(deIndex.findLongestSubstring("macdddd", new AtomicBoolean(false)).success);


    assertSearchResult("überprüfe", "überprüfe", deIndex.findLongestSubstring("ueberprüfe", new AtomicBoolean(false)));
    assertSearchResult("überprüfe", "überprüfe", deIndex.findLongestSubstring("ueberpruefe", new AtomicBoolean(false)));

    assertSearchResult("überprüfe", "überprüfe", deIndex.findLongestSubstring("ueberpBLEH", new AtomicBoolean(false)));
    assertSearchResult("überprüfe", "überprüfe", deIndex.findLongestSubstring("überprBLEH", new AtomicBoolean(false)));

    assertSearchResult("überprüfen", "überprüfe", deIndex.findLongestSubstring("überprüfeBLEH", new AtomicBoolean(false)));

    // Check that search in lowercase works.
    assertSearchResult("Alibi", "Alibi", deIndex.findLongestSubstring("alib", new AtomicBoolean(false)));
    assertTrue(deIndex.findLongestSubstring("alib", new AtomicBoolean(false)).success);
    System.out.println(deIndex.findLongestSubstring("alib", new AtomicBoolean(false)).toString());
    
    raf.close();
  }
  
  private void assertSearchResult(final String insertionPoint, final String longestPrefix,
      final SearchResult actual) {
    assertEquals(insertionPoint, actual.insertionPoint.token);
    assertEquals(longestPrefix, actual.longestPrefix.token);
  }

  public void testGermanTokenRows() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.dict", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
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
    
    raf.close();
  }
  
  public void testGermanSort() {
    assertEquals("aüÄÄ", Language.de.textNorm("aueAeAE", false));
    final List<String> words = Arrays.asList(
        "er-ben",
        "erben",
        "Erben",
        "Erbse",
        "Erbsen",
        "essen",
        "Essen",
        "Grosformat",
        "Grosformats",
        "Grossformat",
        "Großformat",
        "Grossformats",
        "Großformats",
        "Großpoo",
        "Großpoos",
        "Hörweite",
        "hos",
        "Höschen",
        "Hostel",
        "hulle",
        "Hulle",
        "hülle",
        "huelle",
        "Hülle",
        "Huelle",
        "Hum"
        );
    assertEquals(0, Language.de.sortComparator.compare("hülle", "huelle"));
    assertEquals(0, Language.de.sortComparator.compare("huelle", "hülle"));
    
    assertEquals(-1, Language.de.sortComparator.compare("hülle", "Hülle"));
    assertEquals(0, Language.de.findComparator.compare("hülle", "Hülle"));
    assertEquals(-1, Language.de.findComparator.compare("hulle", "Hülle"));

    
    for (final String s : words) {
      System.out.println(s + "\t" + Language.de.textNorm(s, false));
    }
    final List<String> sorted = new ArrayList<String>(words);
//    Collections.shuffle(shuffled, new Random(0));
    Collections.sort(sorted, Language.de.sortComparator);
    System.out.println(sorted.toString());
    for (int i = 0; i < words.size(); ++i) {
      System.out.println(words.get(i) + "\t" + sorted.get(i));
      assertEquals(words.get(i), sorted.get(i));
    }
  }

  @SuppressWarnings("unchecked")
  public void testEnglishSort() {

    final List<String> words = Arrays.asList(
        "pre-print", 
        "preppie", 
        "preppy",
        "preprocess");
    
    final List<String> sorted = new ArrayList<String>(words);
    Collections.sort(sorted, Language.en.getSortCollator());
    for (int i = 0; i < words.size(); ++i) {
      if (i > 0) {
        assertTrue(Language.en.getSortCollator().compare(words.get(i-1), words.get(i)) < 0);
      }
      System.out.println(words.get(i) + "\t" + sorted.get(i));
      assertEquals(words.get(i), sorted.get(i));
    }
    
    assertTrue(Language.en.getSortCollator().compare("pre-print", "preppy") < 0);

  }
  
  public void testLanguage() {
    assertEquals(Language.de, Language.lookup("de"));
    assertEquals(Language.en, Language.lookup("en"));
    assertEquals("es", Language.lookup("es").getSymbol());
  }

  public void testTextNorm() {
    assertEquals("hoschen", "Höschen".toLowerCase(Language.de.locale));
  }

  public void testChemnitz() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en_chemnitz.dict", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    //assertSearchResult("Höschen", "Hos", deIndex.findLongestSubstring("Hos", new AtomicBoolean(false)));
    //assertSearchResult("Höschen", "hos", deIndex.findLongestSubstring("hos", new AtomicBoolean(false)));
 

    raf.close();
  }

}

package com.hughes.android.dictionary.engine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.hughes.android.dictionary.engine.Index.IndexEntry;
import com.ibm.icu.text.Transliterator;


public class DictionaryTest extends TestCase {
    
  public void testGermanMetadata() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    assertEquals("de", deIndex.shortName);
    assertEquals("de->en", deIndex.longName);
    
    raf.close();
  }
  
  public void testGermanIndex() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    for (final Index.IndexEntry indexEntry : deIndex.sortedIndexEntries) {
      System.out.println("testing: " + indexEntry.token);
      final IndexEntry searchResult = deIndex.findInsertionPoint(indexEntry.token, new AtomicBoolean(
          false));
      assertEquals(indexEntry.token.toLowerCase(), searchResult.token.toLowerCase());
    }

    // TODO: maybe if user types capitalization, use it.
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("aaac", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("AAAC", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("AAAc", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("aAac", new AtomicBoolean(false)));

    // Before the beginning.
    assertSearchResult("40", "40" /* special case */, deIndex.findInsertionPoint("", new AtomicBoolean(false)));
    assertSearchResult("40", "40" /* special case */, deIndex.findInsertionPoint("__", new AtomicBoolean(false)));
    
    // After the end.
    assertSearchResult("Zweckorientiertheit", "zählen", deIndex.findInsertionPoint("ZZZZZ", new AtomicBoolean(false)));

    assertSearchResult("ab", "aaac", deIndex.findInsertionPoint("aaaca", new AtomicBoolean(false)));
    assertSearchResult("machen", "machen", deIndex.findInsertionPoint("m", new AtomicBoolean(false)));
    assertSearchResult("machen", "machen", deIndex.findInsertionPoint("macdddd", new AtomicBoolean(false)));


    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("ueberprüfe", new AtomicBoolean(false)));
    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("ueberpruefe", new AtomicBoolean(false)));

    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("ueberpBLEH", new AtomicBoolean(false)));
    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("überprBLEH", new AtomicBoolean(false)));

    assertSearchResult("überprüfen", "überprüfe", deIndex.findInsertionPoint("überprüfeBLEH", new AtomicBoolean(false)));

    // Check that search in lowercase works.
    assertSearchResult("Alibi", "Alibi", deIndex.findInsertionPoint("alib", new AtomicBoolean(false)));
    System.out.println(deIndex.findInsertionPoint("alib", new AtomicBoolean(false)).toString());
    
    raf.close();
  }
  
  private void assertSearchResult(final String insertionPoint, final String longestPrefix,
      final IndexEntry actual) {
    assertEquals(insertionPoint, actual.token);
  }

  public void testGermanTokenRows() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.quickdic", "r");
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
    final Transliterator normalizer = Transliterator.createFromRules("", Language.de.getDefaultNormalizerRules(), Transliterator.FORWARD);
    assertEquals("aüääss", normalizer.transform("aueAeAEß"));
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
        "Hörvermögen",
        "Hörweite",
        "hos",
        "Höschen",
        "Hostel",
        "hulle",
        "Hulle",
        "huelle",
        "Huelle",
        "hülle",
        "Hülle",
        "Huellen",
        "Hüllen",
        "Hum"
        );
    final NormalizeComparator comparator = new NormalizeComparator(normalizer, Language.de.getCollator());
    assertEquals(1, comparator.compare("hülle", "huelle"));
    assertEquals(-1, comparator.compare("huelle", "hülle"));
    
    assertEquals(-1, comparator.compare("hülle", "Hülle"));
    
    assertEquals("hülle", normalizer.transform("Hülle"));
    assertEquals("hulle", normalizer.transform("Hulle"));

    
    final List<String> sorted = new ArrayList<String>(words);
//    Collections.shuffle(shuffled, new Random(0));
    Collections.sort(sorted, comparator);
    System.out.println(sorted.toString());
    for (int i = 0; i < words.size(); ++i) {
      System.out.println(words.get(i) + "\t" + sorted.get(i));
      assertEquals(words.get(i), sorted.get(i));
    }
  }

  public void testEnglishSort() {
    final Transliterator normalizer = Transliterator.createFromRules("", Language.en.getDefaultNormalizerRules(), Transliterator.FORWARD);

    final List<String> words = Arrays.asList(
        "pre-print", 
        "preppie", 
        "preppy",
        "preprocess");
    
    final List<String> sorted = new ArrayList<String>(words);
    final NormalizeComparator comparator = new NormalizeComparator(normalizer, Language.en.getCollator());
    Collections.sort(sorted, comparator);
    for (int i = 0; i < words.size(); ++i) {
      if (i > 0) {
        assertTrue(comparator.compare(words.get(i-1), words.get(i)) < 0);
      }
      System.out.println(words.get(i) + "\t" + sorted.get(i));
      assertEquals(words.get(i), sorted.get(i));
    }
    
    assertTrue(comparator.compare("pre-print", "preppy") < 0);

  }
  
  public void testLanguage() {
    assertEquals(Language.de, Language.lookup("de"));
    assertEquals(Language.en, Language.lookup("en"));
    assertEquals("es", Language.lookup("es").getSymbol());
  }

  public void testTextNorm() {
    //final Transliterator transliterator = Transliterator.getInstance("Any-Latin; Upper; Lower; 'oe' > 'o'; NFD; [:Nonspacing Mark:] Remove; NFC", Transliterator.FORWARD);
    final Transliterator transliterator = Transliterator.createFromRules("", ":: Any-Latin; :: Upper; :: Lower; 'oe' > 'o'; :: NFD; :: [:Nonspacing Mark:] Remove; :: NFC ;", Transliterator.FORWARD);
    assertEquals("hoschen", transliterator.transliterate("Höschen"));
    assertEquals("hoschen", transliterator.transliterate("Hoeschen"));
    assertEquals("grosspoo", transliterator.transliterate("Großpoo"));

    assertEquals("kyanpasu", transliterator.transliterate("キャンパス"));
    assertEquals("alphabetikos katalogos", transliterator.transliterate("Αλφαβητικός Κατάλογος"));
    assertEquals("biologiceskom", transliterator.transliterate("биологическом"));
  }

  public void testChemnitz() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("dictOutputs/de-en_chemnitz.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    assertSearchResult("Höschen", "Hos", deIndex.findInsertionPoint("Hos", new AtomicBoolean(false)));
    assertSearchResult("Höschen", "hos", deIndex.findInsertionPoint("hos", new AtomicBoolean(false)));

    raf.close();
  }

}

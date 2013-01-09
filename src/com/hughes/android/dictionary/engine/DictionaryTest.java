// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hughes.android.dictionary.engine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.hughes.android.dictionary.engine.Index.IndexEntry;
import com.hughes.util.CollectionUtil;


public class DictionaryTest extends TestCase {
  
  static final String TEST_OUTPUTS = com.hughes.android.dictionary.engine.DictionaryBuilderTest.TEST_OUTPUTS;
  public static final String OUTPUTS = "data/outputs/";

  @Override
  protected void setUp() {
    while (!TransliteratorManager.init(null)) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
  public void testURLFormatting() {
  }

  public void testEnItWiktionary() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "EN-IT.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index enIndex = dict.indices.get(0);
    
    final RowBase row = enIndex.rows.get(4);
    assertEquals("-ical", row.getRawText(false));
    
    final Index itIndex = dict.indices.get(1);
    {
    final List<RowBase> rows = itIndex.multiWordSearch("come mai", Arrays.asList("come", "mai"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertTrue(rows.get(0).toString().startsWith("come mai@"));
    assertTrue(rows.get(0) instanceof TokenRow);
    assertTrue(!((TokenRow)rows.get(0)).getIndexEntry().htmlEntries.isEmpty());
    }

    {
    final List<RowBase> rows = itIndex.multiWordSearch("buon g", Arrays.asList("buon", "g"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertTrue(rows.get(0).toString().startsWith("buon giorno@"));
    assertTrue(rows.get(0) instanceof TokenRow);
    assertTrue(!((TokenRow)rows.get(0)).getIndexEntry().htmlEntries.isEmpty());
    }

    {
        final IndexEntry searchResult = itIndex.findInsertionPoint("azzurro", new AtomicBoolean(
                false));
        HtmlEntry htmlEntry = searchResult.htmlEntries.get(0);
        System.out.println("azzurro:\n" + htmlEntry.getHtml());
    }

    raf.close();
  }

//  public void testFr() throws IOException {
//      final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "FR.quickdic", "r");
//      final Dictionary dict = new Dictionary(raf);
//      final Index frIndex = dict.indices.get(0);
//      
//      // Now they're all cached, we shouldn't have to search.
//      for (final IndexEntry indexEntry : frIndex.sortedIndexEntries) {
//          System.out.println(indexEntry.token);
//      }
//
//      raf.close();
//  }

  
  public void testDeEnWiktionary() throws IOException {
      final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "DE-EN.quickdic", "r");
      final Dictionary dict = new Dictionary(raf);
            
      final Index deIndex = dict.indices.get(0);

      {
          final IndexEntry searchResult = deIndex.findInsertionPoint("rot", new AtomicBoolean(
                  false));
          HtmlEntry htmlEntry = searchResult.htmlEntries.get(0);
          System.out.println("rot:\n" + htmlEntry.getHtml());
      }

      raf.close();
    }

  public void testGermanMetadata() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(TEST_OUTPUTS + "de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    assertEquals("DE", deIndex.shortName);
    assertEquals("DE->EN", deIndex.longName);
    
    assertEquals(2, dict.sources.size());
    assertEquals("chemnitz", dict.sources.get(0).name);
    assertEquals("dictcc", dict.sources.get(1).name);
    
    assertEquals("dictcc", dict.pairEntries.get(0).entrySource.name);
    assertEquals("chemnitz", dict.pairEntries.get(1).entrySource.name);
    
    raf.close();
  }
  
  public void testGermanIndex() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(TEST_OUTPUTS + "de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    for (final Index.IndexEntry indexEntry : deIndex.sortedIndexEntries) {
      System.out.println("testing: " + indexEntry.token);
      final IndexEntry searchResult = deIndex.findInsertionPoint(indexEntry.token, new AtomicBoolean(
          false));
      assertEquals("Looked up: " + indexEntry.token, indexEntry.token.toLowerCase(), searchResult.token.toLowerCase());
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
    final RandomAccessFile raf = new RandomAccessFile(TEST_OUTPUTS + "de-en.quickdic", "r");
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
  
  public void testChemnitz() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(TEST_OUTPUTS + "de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    assertSearchResult("Höschen", "Hos", deIndex.findInsertionPoint("Hos", new AtomicBoolean(false)));
    assertSearchResult("Höschen", "hos", deIndex.findInsertionPoint("hos", new AtomicBoolean(false)));
    
    raf.close();
  }

  public void testMultiSearch() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(TEST_OUTPUTS + "de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);

    {
    final List<RowBase> rows = deIndex.multiWordSearch("aaa aaab", Arrays.asList("aaa", "aaab"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    }
    
    raf.close();
  }
  
  public void testMultiSearchIt() throws IOException {
      final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "IT.quickdic", "r");
      final Dictionary dict = new Dictionary(raf);
      final Index index = dict.indices.get(0);

      {
      final List<RowBase> rows = index.multiWordSearch("fare centro", 
              Arrays.asList("fare", "centro"), new AtomicBoolean(false));
      System.out.println(CollectionUtil.join(rows, "\n  "));
      assertTrue(rows.toString(), rows.size() > 0);
      assertTrue(rows.get(0).toString().startsWith("fare centro@"));
      }
  }

  public void testMultiSearchDeBig() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "DE-EN.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index enIndex = dict.indices.get(1);

    {
    final List<RowBase> rows = enIndex.multiWordSearch("train station", Arrays.asList("train", "station"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertTrue(rows.get(0).toString().startsWith("train station@"));
    }

    {
    final List<RowBase> rows = enIndex.multiWordSearch("a train station", Arrays.asList("a", "train", "station"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertEquals("Bahnhofsuhr {{de-noun|g=f|plural=Bahnhofsuhren}}\tstation clock (at a train station)", rows.get(0).toString());
    }

    {
    final List<RowBase> rows = enIndex.multiWordSearch("a station", Arrays.asList("a", "station"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertEquals("Abfahrthalle {en-noun}\tDeparture room of a station.", rows.get(0).toString());
    }

    {
    // Should print: Giving up, too many words with prefix: p
    final List<RowBase> rows = enIndex.multiWordSearch("p eat", Arrays.asList("p", "eat"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertTrue(rows.toString().contains("verschlingen; verputzen\tto dispatch (eat)"));
    }

    {
    // Should print: Giving up, too many words with prefix: p
    final List<RowBase> rows = enIndex.multiWordSearch("p p", Arrays.asList("p", "p"), new AtomicBoolean(false));
    assertTrue(rows.size() >= 1000);
    }

    {
    // Should print: Giving up, too many words with prefix: a
    final List<RowBase> rows = enIndex.multiWordSearch("a a", Arrays.asList("a", "a"), new AtomicBoolean(false));
    assertTrue(rows.size() >= 1000);
    }

    {
    // Should print: Giving up, too many words with prefix: a
    final List<RowBase> rows = enIndex.multiWordSearch("b ba", Arrays.asList("b", "ba"), new AtomicBoolean(false));
    assertTrue(rows.size() >= 1000);
    }

    {
    // Should print: Giving up, too many words with prefix: a
    final List<RowBase> rows = enIndex.multiWordSearch("b ba", Arrays.asList("b", "ba"), new AtomicBoolean(false));
    assertTrue(rows.size() >= 1000);
    }

    raf.close();
  }

  public void testMultiSearchBigAF() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "AF-EN.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index enIndex = dict.indices.get(1);

    {
    final List<RowBase> rows = enIndex.multiWordSearch("pig eats", Arrays.asList("pig", "eats"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertEquals("vark\tpig (someone who overeats or eats rapidly) (noun)", rows.get(0).toString());
    }

    {
    final List<RowBase> rows = enIndex.multiWordSearch("pig eat", Arrays.asList("pig", "eat"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertEquals("vark\tpig (someone who overeats or eats rapidly) (noun)", rows.get(0).toString());
    }

    {
    final List<RowBase> rows = enIndex.multiWordSearch("pi ea", Arrays.asList("pi", "ea"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertTrue(rows.toString().contains("vark\tpig (someone who overeats or eats rapidly) (noun)"));
    }

    {
    final List<RowBase> rows = enIndex.multiWordSearch("p eat", Arrays.asList("p", "eat"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertTrue(rows.toString().contains("vark\tpig (someone who overeats or eats rapidly) (noun)"));
    }

    
    raf.close();
  }


  public void testExactSearch() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "EN-cmn.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index cmnIndex = dict.indices.get(1);

    final Random random = new Random(10);
    
    for (int i = 0; i < 1000; ++i) {
      final int ii = random.nextInt(cmnIndex.sortedIndexEntries.size());
      final IndexEntry indexEntry = cmnIndex.sortedIndexEntries.get(ii);
      final IndexEntry found = cmnIndex.findExact(indexEntry.token);
      assertNotNull(found);
      assertEquals(indexEntry.token, found.token);
      assertEquals(indexEntry, found);  // Test of caching....
    }
    
    raf.close();
  }

  public void testThai() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "EN-TH.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index thIndex = dict.indices.get(1);

    final IndexEntry entry = thIndex.findInsertionPoint("ดี", new AtomicBoolean(false));
    assertEquals("di", entry.token);
    
    raf.close();
  }

  public void testNorwegian() throws IOException {
      final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "EN-NL.quickdic", "r");
      final Dictionary dict = new Dictionary(raf);
      final Index nlIndex = dict.indices.get(1);

      IndexEntry entry = nlIndex.findInsertionPoint("Xhosa", new AtomicBoolean(false));
      assertEquals("Xhosa", entry.token);

      entry = nlIndex.findInsertionPoint("Zyne", new AtomicBoolean(false));
      assertEquals("Zyne", entry.token);

      raf.close();
  }

}

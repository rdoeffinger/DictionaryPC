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

  public void testEnItWiktionary() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "EN-IT_enwiktionary.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index enIndex = dict.indices.get(0);
    
    final RowBase row = enIndex.rows.get(4);
    assertEquals("The numeral 00\tzeranta (noun) {m|f|inv}", row.getRawText(false));

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
    final List<RowBase> rows = deIndex.multiWordSearch(Arrays.asList("aaa", "aaab"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    }
    
    raf.close();
  }

  public void testMultiSearchBig() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile(OUTPUTS + "DE-EN_chemnitz_enwiktionary.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index enIndex = dict.indices.get(1);

    {
    final List<RowBase> rows = enIndex.multiWordSearch(Arrays.asList("train", "station"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertEquals("Bahnhof {{de-noun|g=m|genitive=Bahnhofs|genitive2=Bahnhofes|plural=Bahnhöfe}}\ttrain station", rows.get(0).toString());
    }

    {
    final List<RowBase> rows = enIndex.multiWordSearch(Arrays.asList("a", "train", "station"), new AtomicBoolean(false));
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() > 0);
    assertEquals("Bahnhofsuhr {{de-noun|g=f|plural=Bahnhofsuhren}}\tstation clock (at a train station)", rows.get(0).toString());
    }

    {
    final List<RowBase> rows = enIndex.multiWordSearch(Arrays.asList("a", "station"), new AtomicBoolean(false));
    // TODO: bug, "a" isn't in stoplist for now...
    System.out.println(CollectionUtil.join(rows, "\n  "));
    assertTrue(rows.toString(), rows.size() == 0);
    //assertEquals("Bahnhofsuhr {{de-noun|g=f|plural=Bahnhofsuhren}}\tstation clock (at a train station)", rows.get(0).toString());
    }

    raf.close();
  }

}

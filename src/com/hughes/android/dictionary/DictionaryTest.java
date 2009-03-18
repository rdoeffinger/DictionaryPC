package com.hughes.android.dictionary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.hughes.android.dictionary.Dictionary.IndexEntry;
import com.hughes.android.dictionary.Dictionary.Language;
import com.hughes.android.dictionary.Dictionary.Row;

public class DictionaryTest extends TestCase {

  public void testDictionary() throws IOException {
    final File file = File.createTempFile("asdf", "asdf");
    file.deleteOnExit();

//    final Dictionary goldenDict;
    final List<Entry> entries = Arrays.asList(
        new Entry("der Hund", "the dog"),
        new Entry("Die grosse Katze", "The big cat"), 
        new Entry("die Katze", "the cat"),
        new Entry("gross", "big"),
        new Entry("Dieb", "thief"),
        new Entry("rennen", "run"));

    {
      final Dictionary dict = new Dictionary("de", "en");
      for (final Entry entry : entries) {
        dict.entries.add(entry);
      }
      DictionaryBuilder.createIndex(dict, Entry.LANG1);
      DictionaryBuilder.createIndex(dict, Entry.LANG2);
      final RandomAccessFile raf = new RandomAccessFile(file, "rw");
      dict.write(raf);
      raf.close();
      
//      goldenDict = dict;
    }

    final RandomAccessFile raf = new RandomAccessFile(file, "r");
    final Dictionary dict = new Dictionary(raf);
    
    assertEquals(entries, dict.entries);
    
    assertEquals("der", dict.languages[0].sortedIndex.get(0).word);
    assertEquals("Die", dict.languages[0].sortedIndex.get(1).word);
    
    for (final IndexEntry indexEntry : dict.languages[0].sortedIndex) {
      System.out.println(indexEntry);
    }

    int rowCount = 0;
    for (final Row row : dict.languages[0].rows) {
      if (row.index >= 0) {
        System.out.println("  " + rowCount + ":" + dict.entries.get(row.index));
      } else {
        System.out.println(rowCount + ":" + dict.languages[0].sortedIndex.get(-row.index - 1));
      }
      ++rowCount;
    }

    for (int l = 0; l <= 1; l++) {
      final Language lang = dict.languages[l];
      for (int i = 0; i < lang.sortedIndex.size(); i++) {
        final IndexEntry indexEntry = lang.sortedIndex.get(i);
        if (indexEntry.word.toLowerCase().equals("dieb"))
          System.out.println();
        final IndexEntry lookedUpEntry = lang.sortedIndex.get(lang.lookup(indexEntry.word, new AtomicBoolean(false)));
        if (!indexEntry.word.toLowerCase().equals(lookedUpEntry.word.toLowerCase()))
          System.out.println();
        assertEquals(indexEntry.word.toLowerCase(), lookedUpEntry.word.toLowerCase());
      }
    }
    
    assertEquals("Die", dict.languages[0].sortedIndex.get(dict.languages[0].lookup("die", new AtomicBoolean())).word);

  }
  
  public void testTextNorm() throws IOException {
//    final File file = File.createTempFile("asdf", "asdf");
//    file.deleteOnExit();

//      final Dictionary goldenDict;
    final List<Entry> entries = Arrays.asList(
        new Entry("der Hund", "the dog"),
        new Entry("Die grosse Katze", "The big cat"), 
        new Entry("die Katze", "the cat"),
        new Entry("gross", "big"),
        new Entry("Dieb", "thief"),
        new Entry("rennen", "run"));

  }

}

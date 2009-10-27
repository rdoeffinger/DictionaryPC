package com.hughes.android.dictionary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.hughes.android.dictionary.Dictionary.IndexEntry;
import com.hughes.android.dictionary.Dictionary.LanguageData;
import com.hughes.android.dictionary.Dictionary.Row;

public class DictionaryTest extends TestCase {

  public void testDictionary() throws IOException {
    final File file = File.createTempFile("asdf", "asdf");
    file.deleteOnExit();

//    final Dictionary goldenDict;
    final List<Entry> entries = Arrays.asList(
        Entry.parseFromLine("der Hund :: the dog", false),
        Entry.parseFromLine("Die grosse Katze :: The big cat", false), 
        Entry.parseFromLine("die Katze :: the cat", false),
        Entry.parseFromLine("gross :: big", false),
        Entry.parseFromLine("Dieb :: thief", false),
        Entry.parseFromLine("rennen :: run", false));

    {
      final Dictionary dict = new Dictionary("test", Language.DE, Language.EN);
      dict.entries.addAll(entries);
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
    
    assertEquals("der", dict.languageDatas[0].sortedIndex.get(0).word);
    assertEquals("die", dict.languageDatas[0].sortedIndex.get(1).word);
    
    assertEquals(0, dict.languageDatas[0].getPrevTokenRow(0));
    assertEquals(0, dict.languageDatas[0].getPrevTokenRow(2));
    assertEquals(0, dict.languageDatas[0].getPrevTokenRow(1));
    assertEquals(4, dict.languageDatas[0].getPrevTokenRow(6));

    assertEquals(2, dict.languageDatas[0].getNextTokenRow(0));
    assertEquals(2, dict.languageDatas[0].getNextTokenRow(1));
    assertEquals(4, dict.languageDatas[0].getNextTokenRow(2));
    assertEquals(8, dict.languageDatas[0].getNextTokenRow(6));
    assertEquals(dict.languageDatas[0].rows.size() - 1, dict.languageDatas[0].getNextTokenRow(dict.languageDatas[0].rows.size() - 2));
    assertEquals(dict.languageDatas[0].rows.size() - 1, dict.languageDatas[0].getNextTokenRow(dict.languageDatas[0].rows.size() - 1));

    for (final IndexEntry indexEntry : dict.languageDatas[0].sortedIndex) {
      System.out.println(indexEntry);
    }

    int rowCount = 0;
    for (final Row row : dict.languageDatas[0].rows) {
      if (row.index >= 0) {
        System.out.println("  " + rowCount + ":" + dict.entries.get(row.index));
      } else {
        System.out.println(rowCount + ":" + dict.languageDatas[0].sortedIndex.get(-row.index - 1));
      }
      ++rowCount;
    }

    for (int l = 0; l <= 1; l++) {
      final LanguageData languageData = dict.languageDatas[l];
      for (int i = 0; i < languageData.sortedIndex.size(); i++) {
        final IndexEntry indexEntry = languageData.sortedIndex.get(i);
        if (indexEntry.word.toLowerCase().equals("dieb"))
          System.out.println();
        final IndexEntry lookedUpEntry = languageData.sortedIndex.get(languageData.lookup(indexEntry.word, new AtomicBoolean(false)));
        if (!indexEntry.word.toLowerCase().equals(lookedUpEntry.word.toLowerCase()))
          System.out.println();
        assertEquals(indexEntry.word.toLowerCase(), lookedUpEntry.word.toLowerCase());
      }
    }
    
    assertEquals("die", dict.languageDatas[0].sortedIndex.get(dict.languageDatas[0].lookup("Die", new AtomicBoolean())).word);
    assertEquals("die", dict.languageDatas[0].sortedIndex.get(dict.languageDatas[0].lookup("die", new AtomicBoolean())).word);

  }
  
  public void testTextNorm() throws IOException {
    System.out.println("\n\ntestTextNorm");
    final List<Entry> entries = Arrays.asList(
        Entry.parseFromLine("Hund {m} :: dog", true),
        Entry.parseFromLine("'CHRISTOS' :: doh", true),
        Entry.parseFromLine("\"Pick-up\"-Presse {f} :: baler", true),
        Entry.parseFromLine("(Ach was), echt? [auch ironisch] :: No shit! [also ironic]", true),
        Entry.parseFromLine("(akuter) Myokardinfarkt {m} <AMI / MI> :: (acute) myocardial infarction <AMI / MI>", true),
        Entry.parseFromLine("(reine) Vermutung {f} :: guesswork", true),
        Entry.parseFromLine("(mit) 6:1 vorne liegen :: to be 6-1 up [football]", true),
        Entry.parseFromLine("(auf) den Knopf drücken [auch fig.: auslösen] :: to push the button [also fig.: initiate]", false),
        Entry.parseFromLine("Adjektiv {n} /Adj./; Eigenschaftswort {n} [gramm.] | Adjektive {pl}; Eigenschaftswoerter {pl} :: adjective /adj./ | adjectives", true),
        Entry.parseFromLine("Älteste {m,f}; Ältester :: oldest; eldest", true),
        Entry.parseFromLine("\"...\", schloss er an. :: '...,' he added.", true),
        Entry.parseFromLine("besonderer | besondere | besonderes :: extra", false),
        Entry.parseFromLine("| zu Pferde; zu Pferd | reiten :: horseback | on horseback | go on horseback", true),
        Entry.parseFromLine("Hauptaugenmerk {m} | sein Hauptaugenmerk richten auf ::  | to focus (one's) attention on", true),
        Entry.parseFromLine("&#963;-Algebra {f} :: &#963;-field", true)
        );

    assertFalse(entries.contains(null));
    
    // Hyphenated words get put both multiple listings.

    final Dictionary dict = new Dictionary("test", Language.DE, Language.EN);
    dict.entries.addAll(entries);
    DictionaryBuilder.createIndex(dict, Entry.LANG1);
    DictionaryBuilder.createIndex(dict, Entry.LANG2);
    
    for (int lang = 0; lang <= 1; lang++) {
      final LanguageData languageData = dict.languageDatas[lang];
      System.out.println("\n" + languageData.language);
      final Set<String> words = new LinkedHashSet<String>();
      for (int i = 0; i < languageData.sortedIndex.size(); i++) {
        final IndexEntry indexEntry = languageData.sortedIndex.get(i);
        System.out.println(indexEntry);
        words.add(indexEntry.word);
      }
      if (lang == 0) {
        assertTrue(words.contains("CHRISTOS"));
        assertTrue(words.contains("akuter"));
        assertTrue(words.contains("σ-Algebra"));

        assertFalse(words.contains("-Algebra"));
      } else {
        assertTrue(words.contains("σ-field"));
        assertTrue(words.contains("6-1"));
      }
    }

  }
  
  public void testGermanSort() {
    assertEquals("aüÄ", Language.DE.textNorm("aueAe"));
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
        "hulle",
        "Hulle",
        "hülle",
        "huelle",
        "Hülle",
        "Huelle",
        "Hum"
        );
    assertEquals(0, Language.DE.sortComparator.compare("hülle", "huelle"));
    assertEquals(0, Language.DE.sortComparator.compare("huelle", "hülle"));
    
    assertEquals(-1, Language.DE.sortComparator.compare("hülle", "Hülle"));
    assertEquals(0, Language.DE.findComparator.compare("hülle", "Hülle"));
    assertEquals(-1, Language.DE.findComparator.compare("hulle", "Hülle"));

    
    for (final String s : words) {
      System.out.println(s + "\t" + Language.DE.textNorm(s));
    }
    final List<String> sorted = new ArrayList<String>(words);
//    Collections.shuffle(shuffled, new Random(0));
    Collections.sort(sorted, Language.DE.sortComparator);
    System.out.println(sorted.toString());
    for (int i = 0; i < words.size(); ++i) {
      System.out.println(words.get(i) + "\t" + sorted.get(i));
      assertEquals(words.get(i), sorted.get(i));
    }
  }

  public void testEnglishSort() {

    final List<String> words = Arrays.asList(
        "pre-print", 
        "preppie", 
        "preppy",
        "preprocess");
    
    final List<String> sorted = new ArrayList<String>(words);
    Collections.sort(sorted, Language.EN.sortComparator);
    for (int i = 0; i < words.size(); ++i) {
      if (i > 0) {
        assertTrue(Language.EN.sortComparator.compare(words.get(i-1), words.get(i)) < 0);
      }
      System.out.println(words.get(i) + "\t" + sorted.get(i));
      assertEquals(words.get(i), sorted.get(i));
    }
    
    assertTrue(Language.EN.sortCollator.compare("pre-print", "preppy") < 0);

  }

}

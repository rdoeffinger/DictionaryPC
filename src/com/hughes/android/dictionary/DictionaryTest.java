package com.hughes.android.dictionary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
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
      final Dictionary dict = new Dictionary(Language.DE, Language.EN);
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
    assertEquals("Die", dict.languageDatas[0].sortedIndex.get(1).word);
    
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
    
    assertEquals("Die", dict.languageDatas[0].sortedIndex.get(dict.languageDatas[0].lookup("die", new AtomicBoolean())).word);

  }
  
  public void testTextNorm() throws IOException {
    final List<Entry> entries = Arrays.asList(
        Entry.parseFromLine("Hund {m} :: dog", true),
        Entry.parseFromLine("\"Pick-up\"-Presse {f} :: baler", true),
        Entry.parseFromLine("(Ach was), echt? [auch ironisch] :: No shit! [also ironic]", true),
        Entry.parseFromLine("(akuter) Myokardinfarkt {m} <AMI / MI> :: (acute) myocardial infarction <AMI / MI>", true),
        Entry.parseFromLine("(reine) Vermutung {f} :: guesswork", true),
        Entry.parseFromLine("(mit) 6:1 vorne liegen :: to be 6-1 up [football]", true),
        Entry.parseFromLine("(auf) den Knopf drücken [auch fig.: auslösen] :: to push the button [also fig.: initiate]", false),
        Entry.parseFromLine("Adjektiv {n} /Adj./; Eigenschaftswort {n} [gramm.] | Adjektive {pl}; EigenschaftswÃ¶rter {pl} :: adjective /adj./ | adjectives", true),
        Entry.parseFromLine("Ã„lteste {m,f}; Ã„ltester :: oldest; eldest", true),
        Entry.parseFromLine("\"...\", schloss er an. :: '...,' he added.", true),
        Entry.parseFromLine("besonderer | besondere | besonderes :: extra", false),
        Entry.parseFromLine("| zu Pferde; zu Pferd | reiten :: horseback | on horseback | go on horseback", true),
        Entry.parseFromLine("Hauptaugenmerk {m} | sein Hauptaugenmerk richten auf ::  | to focus (one's) attention on", true)
        );

    assertFalse(entries.contains(null));

    // Hyphenated words get put both multiple listings.

    final Dictionary dict = new Dictionary(Language.DE, Language.EN);
    dict.entries.addAll(entries);
    DictionaryBuilder.createIndex(dict, Entry.LANG1);
    DictionaryBuilder.createIndex(dict, Entry.LANG2);
    
    for (int l = 0; l <= 1; l++) {
      final LanguageData languageData = dict.languageDatas[l];
      System.out.println("\n" + languageData.language);
      for (int i = 0; i < languageData.sortedIndex.size(); i++) {
        final IndexEntry indexEntry = languageData.sortedIndex.get(i);
        System.out.println(indexEntry);
      }
    }

  }


}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import com.ibm.icu.text.Transliterator;

public class LanguageTest extends TestCase {
  
  public void testGermanSort() {
    System.out.println(Language.isoCodeToWikiName.values());
    
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



}

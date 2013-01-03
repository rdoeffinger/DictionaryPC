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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.hughes.android.dictionary.parser.DictFileParser;
import com.hughes.android.dictionary.parser.wiktionary.WiktionaryLangs;
import com.ibm.icu.text.Transliterator;

public class LanguageTest extends TestCase {
  
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
    assertEquals("es", Language.lookup("es").getIsoCode());
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
  public void testHalfTextNorm() {
    final Transliterator transliterator = Transliterator.createFromRules("", ":: Any-Latin; ' ' > ; :: Lower; ", Transliterator.FORWARD);
    assertEquals("kyanpasu", transliterator.transliterate("キャンパス"));
    assertEquals("alphabētikóskatálogos", transliterator.transliterate("Αλφαβητικός Κατάλογος"));
    assertEquals("biologičeskom", transliterator.transliterate("биологическом"));

    assertEquals("xièxiè", transliterator.transliterate("謝謝"));
    assertEquals("xièxiè", transliterator.transliterate("谢谢"));

    assertEquals("diànnǎo", transliterator.transliterate("電腦"));
    assertEquals("diànnǎo", transliterator.transliterate("电脑"));
    assertEquals("jìsuànjī", transliterator.transliterate("計算機"));
    assertEquals("jìsuànjī", transliterator.transliterate("计算机"));
  }

  
  public void testChinese() {
    final Language cmn = Language.lookup("cmn");
    final Transliterator transliterator = Transliterator.createFromRules("", cmn.getDefaultNormalizerRules(), Transliterator.FORWARD);
    
    assertEquals("xiexie", transliterator.transliterate("謝謝"));
    assertEquals("xiexie", transliterator.transliterate("谢谢"));

    assertEquals("diannao", transliterator.transliterate("電腦"));
    assertEquals("diannao", transliterator.transliterate("电脑"));
    assertEquals("jisuanji", transliterator.transliterate("計算機"));
    assertEquals("jisuanji", transliterator.transliterate("计算机"));
    
    assertEquals("chengjiu", transliterator.transliterate("成就"));
    
  }
  
  public void testArabic() {
    final Language ar = Language.lookup("ar");
    final Transliterator transliterator = Transliterator.createFromRules("", ar.getDefaultNormalizerRules(), Transliterator.FORWARD);
    // These don't seem quite right....
    assertEquals("haswb", transliterator.transliterate("حاسوب"));
    assertEquals("kmbywtr", transliterator.transliterate("كمبيوتر"));

    assertEquals("{\u200e كمبيوتر \u200e}", Language.fixBidiText("{كمبيوتر}"));
    assertEquals("{a=\u200e كمبيوتر \u200e}", Language.fixBidiText("{a=كمبيوتر}"));
    assertEquals("(\u200e كمبيوتر \u200e)", Language.fixBidiText("(كمبيوتر)"));
    assertEquals("أنثى أنْثَى (’únθā) {f}, إناث (’ināθ) {p}, اناثى (’anāθā) {p}", Language.fixBidiText("أنثى أنْثَى (’únθā) {f}, إناث (’ināθ) {p}, اناثى (’anāθā) {p}"));
       
  }

  public void testThai() {
    final Language th = Language.lookup("TH");
    final Transliterator transliterator = Transliterator.createFromRules("", th.getDefaultNormalizerRules(), Transliterator.FORWARD);
    // Not sure these are right, just to know...
    assertEquals("d", transliterator.transliterate("ด"));
    assertEquals("di", transliterator.transliterate("ด ี"));
    assertEquals("dii", transliterator.transliterate("ดีี"));
    
    assertEquals(Collections.singleton("ดีี"), DictFileParser.tokenize("ดีี", DictFileParser.NON_CHAR));
  }

  
  public void testEnWiktionaryNames() {
    final Set<String> enLangs = new LinkedHashSet<String>(WiktionaryLangs.isoCodeToEnWikiName.keySet());
    final List<String> names = new ArrayList<String>();
    for (final String code : WiktionaryLangs.isoCodeToEnWikiName.keySet()) {
      names.add(WiktionaryLangs.isoCodeToEnWikiName.get(code));
      enLangs.add(code.toLowerCase());
    }
    Collections.sort(names);
    System.out.println(names);
    //assertEquals(enLangs, Language.isoCodeToResources.keySet());
    assertEquals(enLangs, Language.isoCodeToResources.keySet());
  }

}

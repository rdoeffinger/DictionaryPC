package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WikiWord {
  final int depth;
  
  String language;
  
  final Map<String, StringBuilder> accentToPronunciation = new LinkedHashMap<String, StringBuilder>();
  StringBuilder currentPronunciation = null;

  boolean isLang1;
  boolean isLang2;
  
  final List<PartOfSpeech> partsOfSpeech = new ArrayList<WikiWord.PartOfSpeech>();
  
  final Map<String, List<String>> otherSections = new LinkedHashMap<String, List<String>>();
  
  public WikiWord(int depth) {
    this.depth = depth;
  }

  static class PartOfSpeech {
    final int depth;
    final String name;

    final List<Meaning> meaning = new ArrayList<WikiWord.Meaning>();
    
    final List<TranslationSection> translationSections = new ArrayList<WikiWord.TranslationSection>();
        
    final Map<String, String> otherSections = new LinkedHashMap<String, String>();

    public PartOfSpeech(final int depth, String name) {
      this.depth = depth;
      this.name = name;
    }
  }
  
  static class TranslationSection {
    String sense;
    List<List<String>> translations = new ArrayList<List<String>>();
    {
      translations.add(new ArrayList<String>());
      translations.add(new ArrayList<String>());
    }
  }
  
  static class Meaning {
    String meaning;
    Example example;
  }
  
  static class Example {
    String example;
    String exampleInEnglish;
  }

}

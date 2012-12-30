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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.hughes.android.dictionary.parser.wiktionary.WiktionaryLangs;

public class WiktionarySplitter extends org.xml.sax.helpers.DefaultHandler {

  // The matches the whole line, otherwise regexes don't work well on French:
  // {{=uk=}}
  static final Pattern headingStart = Pattern.compile("^(=+)[^=].*$", Pattern.MULTILINE);
  
  final Map<String,List<Selector>> pathToSelectors = new LinkedHashMap<String, List<Selector>>();
  List<Selector> currentSelectors = null;
  
  StringBuilder titleBuilder;
  StringBuilder textBuilder;
  StringBuilder currentBuilder = null;

  public static void main(final String[] args) throws Exception {
    final WiktionarySplitter wiktionarySplitter = new WiktionarySplitter();
    wiktionarySplitter.go();
  }
  
  private WiktionarySplitter() {
    List<Selector> selectors;
    for (final String code : WiktionaryLangs.wikiCodeToIsoCodeToWikiName.keySet()) {
      //if (!code.equals("fr")) {continue;}
      selectors = new ArrayList<WiktionarySplitter.Selector>();
      pathToSelectors.put(String.format("data/inputs/%swiktionary-pages-articles.xml", code), selectors);
      for (final Map.Entry<String, String> entry : WiktionaryLangs.wikiCodeToIsoCodeToWikiName.get(code).entrySet()) {
        final String dir = String.format("data/inputs/wikiSplit/%s", code);
        new File(dir).mkdirs();
        selectors.add(new Selector(String.format("%s/%s.data", dir, entry.getKey()), entry.getValue()));
      }
    }
  }

  private void go() throws Exception {
    final SAXParser parser = SAXParserFactoryImpl.newInstance().newSAXParser();

    // Configure things.
    for (final Map.Entry<String, List<Selector>> pathToSelectorsEntry : pathToSelectors.entrySet()) {
      
      currentSelectors = pathToSelectorsEntry.getValue();
      
      for (final Selector selector : currentSelectors) {
        selector.out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(selector.outFilename)));
      }
  
      // Do it.
      try {
        parser.parse(new File(pathToSelectorsEntry.getKey()), this);
      } catch (Exception e) {
        System.err.println("Exception during parse, lastPageTitle=" + lastPageTitle + ", titleBuilder=" + titleBuilder.toString());
        throw e;
      }
      
      // Shutdown.
      for (final Selector selector : currentSelectors) {
        selector.out.close();
      }
      
    }
  }

  String lastPageTitle = null;
  int pageCount = 0;
  private void endPage() {
    final String title = titleBuilder.toString();
    lastPageTitle = title;
    if (++pageCount % 1000 == 0) {
      System.out.println("endPage: " + title + ", count=" + pageCount);
    }
    if (title.startsWith("Wiktionary:") || 
            title.startsWith("Appendix:") || 
            title.startsWith("Help:") ||
            title.startsWith("Index:") ||
            title.startsWith("MediaWiki:") || 
            title.startsWith("Citations:") || 
            title.startsWith("Concordance:") || 
            title.startsWith("Glossary:") || 
            title.startsWith("Rhymes:") || 
            title.startsWith("Category:") || 
            title.startsWith("Wikisaurus:") || 
            title.startsWith("Unsupported titles/") || 
            title.startsWith("Transwiki:") || 
            title.startsWith("File:") || 
            title.startsWith("Thread:") || 
            title.startsWith("Template:") ||
            title.startsWith("Summary:") ||
            // DE
            title.startsWith("Datei:") ||
            title.startsWith("Verzeichnis:") ||
            title.startsWith("Vorlage:") ||
            title.startsWith("Thesaurus:") ||
            title.startsWith("Kategorie:") ||
            title.startsWith("Hilfe:") ||
            // FR:
            title.startsWith("Annexe:") ||
            title.startsWith("Catégori:") ||
            title.startsWith("Modèle:") ||
            title.startsWith("Thésaurus:") ||
            title.startsWith("Projet:") ||
            title.startsWith("Aide:") ||
            title.startsWith("Fichier:") ||
            title.startsWith("Wiktionnaire:") ||
            title.startsWith("Catégorie:") ||
            title.startsWith("Portail:") ||
            title.startsWith("utiliusateur:") ||
            title.startsWith("Kategorio:") ||
            // IT
            title.startsWith("Wikizionario:") ||
            title.startsWith("Appendice:") ||
            title.startsWith("Categoria:") ||
            title.startsWith("Aiuto:") ||
            title.startsWith("Portail:") ||

            // sentinel
            false
            ) {
        return;
    }
    if (title.contains(":")) {
        if (!title.startsWith("Sign gloss:")) {
            System.err.println("title with colon: " + title);
        }
    }
    
    String text = textBuilder.toString();
    
    while (text.length() > 0) {
      // Find start.
      final Matcher startMatcher = headingStart.matcher(text);
      if (!startMatcher.find()) {
        return;
      }
      text = text.substring(startMatcher.end());
      
      final String heading = startMatcher.group();
      for (final Selector selector : currentSelectors) {
        if (selector.pattern.matcher(heading).find()) {
          
          // Find end.
          final int depth = startMatcher.group(1).length();
          final Pattern endPattern = Pattern.compile(String.format("^={1,%d}[^=].*$", depth), Pattern.MULTILINE);
          
          final Matcher endMatcher = endPattern.matcher(text);
          final int end;
          if (endMatcher.find()) {
            end = endMatcher.start();
          } else {
            end = text.length();
          }
          
          final String sectionText = text.substring(0, end);
          final Section section = new Section(title, heading, sectionText);
          
          try {
            selector.out.writeUTF(section.title);
            selector.out.writeUTF(section.heading);
            final byte[] bytes = section.text.getBytes("UTF8");
            selector.out.writeInt(bytes.length);
            selector.out.write(bytes);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          
          text = text.substring(end);
        }
      }
    }
    
  }

  // -----------------------------------------------------------------------

  static class Section implements java.io.Serializable {
    private static final long serialVersionUID = -7676549898325856822L;

    final String title;
    final String heading;
    final String text;
    
    public Section(final String title, final String heading, final String text) {
      this.title = title;
      this.heading = heading;
      this.text = text;
      
      //System.out.printf("TITLE:%s\nHEADING:%s\nTEXT:%s\n\n\n\n\n\n", title, heading, text);
    }
  }
  
  static class Selector {
    final String outFilename;
    final Pattern pattern;

    DataOutputStream out;

    public Selector(final String filename, final String pattern) {
      this.outFilename = filename;
      this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }
  }

  // -----------------------------------------------------------------------
  
    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) {
      currentBuilder = null;
      if ("page".equals(qName)) {
        titleBuilder = new StringBuilder();
        
        // Start with "\n" to better match certain strings.
        textBuilder = new StringBuilder("\n");
      } else if ("title".equals(qName)) {
        currentBuilder = titleBuilder;
      } else if ("text".equals(qName)) {
        currentBuilder = textBuilder;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (currentBuilder != null) {
        currentBuilder.append(ch, start, length);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      currentBuilder = null;
      if ("page".equals(qName)) {
        endPage();
      }
    }
    
    public void parse(final File file) throws ParserConfigurationException,
        SAXException, IOException {
      final SAXParser parser = SAXParserFactoryImpl.newInstance().newSAXParser();
      parser.parse(file, this);
    }
    
}

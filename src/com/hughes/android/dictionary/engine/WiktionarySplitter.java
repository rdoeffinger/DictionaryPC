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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class WiktionarySplitter extends org.xml.sax.helpers.DefaultHandler {
  
  private static final String FILE_TO_SPLIT = "data/inputs/enwiktionary-20111224-pages-articles.xml";
  
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
    DataOutputStream out;
    Pattern pattern;
    
    public Selector(final String filename, final String pattern) throws IOException {
      this.out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
      this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }
  }

  final List<Selector> selectors = new ArrayList<Selector>();
  StringBuilder titleBuilder;
  StringBuilder textBuilder;
  StringBuilder currentBuilder = null;

  public static void main(final String[] args) throws SAXException, IOException, ParserConfigurationException {
    final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    final WiktionarySplitter wiktionarySplitter = new WiktionarySplitter();
    
    // Configure things.
    
    final List<Selector> selectors = wiktionarySplitter.selectors;
    for (int i = 1; i < args.length; i += 2) {
      final Selector selector = new Selector(args[i], args[i+1]);
      selectors.add(selector);
    }

    if (selectors.isEmpty()) {
      for (final Map.Entry<String, String> entry : Language.isoCodeToWikiName.entrySet()) {
        selectors.add(new Selector(String.format("data/inputs/enWikiSplit/%s.data", entry.getKey()), entry.getValue()));
      }
    }
    
    // Do it.
    parser.parse(new File(FILE_TO_SPLIT), wiktionarySplitter);
    
    // Shutdown.
    for (final Selector selector : selectors) {
      selector.out.close();
    }
  }

  static final Pattern headingStart = Pattern.compile("^(=+)[^=]+=+", Pattern.MULTILINE);
  
  int pageCount = 0;
  private void endPage() {
    final String title = titleBuilder.toString();
    if (++pageCount % 1000 == 0) {
      System.out.println("endPage: " + title + ", count=" + pageCount);
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
      for (final Selector selector : selectors) {
        if (selector.pattern.matcher(heading).find()) {
          
          // Find end.
          final int depth = startMatcher.group(1).length();
          final Pattern endPattern = Pattern.compile(String.format("^={1,%d}[^=]+=+", depth), Pattern.MULTILINE);
          
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
      final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      parser.parse(file, this);
    }
    
}

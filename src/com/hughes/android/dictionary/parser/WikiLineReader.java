package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WikiLineReader {
  
  private final List<String> lineStack = new ArrayList<String>();
  private String wikiText;
  
  private static final Pattern markup = Pattern.compile("$|''|\\{\\{|\\[\\[|(==+)\\s*$|<!--|<pre>", Pattern.MULTILINE);

  public String readLine() {
    if (wikiText.length() == 0) {
      return null;
    }
    
    final StringBuilder builder = new StringBuilder();
    lineStack.clear();
    int i = 0;
    while (i < wikiText.length()) {
      if (wikiText.startsWith("\n")) {
      } else if (wikiText.startsWith("<pre>", i)) {
        i = 
      } else if (wikiText.startsWith("<!--")) {
      } else if (wikiText.startsWith("{{")) {
      } else if (wikiText.startsWith("[[")) {
        
      }
    }
    
  }
  
  static int safeIndexOf(final String s, final int start, final String target, final String nest, final String backup) {
    
  }
  
  

}

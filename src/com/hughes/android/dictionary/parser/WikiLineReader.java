package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLineReader {
  
  private final List<String> lineStack = new ArrayList<String>();
  
  private final String wikiText;
  private int lineStart = 0;
  
  private static final Pattern wikiLineEvent = Pattern.compile("$|\\{\\{|\\[\\[|\\}\\}|\\]\\]|<!--|<pre>", Pattern.MULTILINE);

  private static final Pattern whitespace = Pattern.compile("\\s+");
  
  public WikiLineReader(final String wikiText) {
    this.wikiText = wikiText;
  }

  public String readLine() {
    while (lineStart < wikiText.length() && 
        Character.isWhitespace(wikiText.charAt(lineStart)) && 
        wikiText.charAt(lineStart) != '\n') {
      ++lineStart;
    }
    if (lineStart >= wikiText.length()) {
      return null;
    }

    int lineEnd = lineStart;
    lineStack.clear();
    int firstNewline = -1;
    final Matcher matcher = wikiLineEvent.matcher(wikiText);
    while (lineEnd < wikiText.length()) {
      if (!matcher.find(lineEnd)) {
        lineEnd = wikiText.length();
        break;
      }
      lineEnd = matcher.end();
      if (lineEnd == wikiText.length()) {
        break;
      }
      if (matcher.group().equals("")) {
        assert (wikiText.charAt(matcher.start()) == '\n');
        ++lineEnd;
        if (lineStack.size() == 0) {
          break;
        } else {
          if (firstNewline == -1) {
            firstNewline = matcher.end();
          }
        }
      }
      
      if (matcher.group().equals("[[") || matcher.group().equals("{{")) {
        lineStack.add(matcher.group());
      } else if (matcher.group().equals("}}") || matcher.group().equals("]]")) {
        if (lineStack.size() > 0) {
          final String removed = lineStack.remove(lineStack.size() - 1);
          if (removed.equals("{{") && !matcher.group().equals("}}")) {
            System.err.println("Error");
          }
          if (removed.equals("[[") && !matcher.group().equals("]]")) {
            System.err.println("Error");
          }
        } else {
          System.err.println("Error");
        }
      } else if (matcher.group().equals("<!--")) {
        lineEnd = safeIndexOf(wikiText, lineEnd, "-->", "\n");
      } else if (matcher.group().equals("<pre>")) {
        lineEnd = safeIndexOf(wikiText, lineEnd, "</pre>", "\n");
      }
    }
    if (lineStack.size() > 0 && firstNewline != -1) {
      lineEnd = firstNewline + 1;
    }
    final String result = wikiText.substring(lineStart, lineEnd);
    lineStart = lineEnd;
    return result;
  }
    
    
  static int safeIndexOf(final String s, final int start, final String target, final String backup) {
    int close = s.indexOf(target, start);
    if (close != -1) {
      return close + target.length();
    }
    close = s.indexOf(backup, start);
    if (close != -1) {
      return close + backup.length();
    }
    return s.length();
  }
  
  public static String cleanUpLine(String line) {
    int pos;
    while ((pos = line.indexOf("<!--")) != -1) {
      int end = line.indexOf("-->");
      if (end != -1) {
        line = line.substring(0, pos) + line.substring(end + 3);
      }
    }
    final Matcher matcher = whitespace.matcher(line);
    line = matcher.replaceAll(" ");
    line = line.trim();
    return line;
  }

}

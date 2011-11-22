package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WikiTokenizer {

  //private static final Pattern wikiTokenEvent = Pattern.compile("($)", Pattern.MULTILINE);
  private static final Pattern wikiTokenEvent = Pattern.compile("(\\{\\{|\\}\\}|\\[\\[|\\]\\]|<!--|''|$)", Pattern.MULTILINE);
  private static final String listChars = "*#:;";
  
    
    final String wikiText;
    final Matcher matcher;

    boolean justReturnedNewline = true;
    int end = 0;
    int start = -1;

    public String header;
    public int headerDepth;
    
    final List<String> tokenStack = new ArrayList<String>();
    
  public WikiTokenizer(final String wikiText) {
    this.wikiText = wikiText;
    this.matcher = wikiTokenEvent.matcher(wikiText);
  }
    
  private void clear() {
    header = null;
    headerDepth = 0;
    tokenStack.clear();
  }


  public WikiTokenizer nextToken() {
    this.clear();
    
    start = end;
    
    final int len = wikiText.length();
    if (start >= len) {
      return null;
    }
    
    // Eat a newline if we're looking at one:
    final boolean atNewline = wikiText.charAt(end) == '\n';
    if (atNewline) {
      justReturnedNewline = true;
      ++end;
      return this;
    }
    
    if (justReturnedNewline) {
      final char firstChar = wikiText.charAt(end);
      if (firstChar == '=') {
        final int headerStart = end;
        while (++end < len && wikiText.charAt(end) == '=') {}
        final int headerTitleStart = end;
        while (++end < len && wikiText.charAt(end) != '=' && wikiText.charAt(end) != '\n') {}
        final int headerTitleEnd = end;
        while (++end < len && wikiText.charAt(end) == '=') {}
        final int headerEnd = end;
        
        return this;
      }
      if (listChars.indexOf(firstChar) != -1) {
        while (++end < len && listChars.indexOf(wikiText.charAt(end)) != -1) {}
        end = escapedFind(start, "\n");
        return this;
      }
    }
    justReturnedNewline = false;

    if (wikiText.startsWith("'''", start)) {
      end = start + 3;
      return this;
    }
    
    if (wikiText.startsWith("''", start)) {
      end = start + 2;
      return this;
    }

    if (wikiText.startsWith("[[", start)) {
      end = escapedFind(start + 2, "]]");
      return this;
    }

    if (wikiText.startsWith("{{", start)) {
      end = escapedFind(start + 2, "}}");
      return this;
    }

    if (wikiText.startsWith("<pre>", start)) {
      end = safeIndexOf(wikiText, start, "</pre>", "\n");
      return this;
    }

    if (wikiText.startsWith("<math>", start)) {
      end = safeIndexOf(wikiText, start, "</math>", "\n");
      return this;
    }

    if (wikiText.startsWith("<!--", start)) {
      end = safeIndexOf(wikiText, start, "-->", "\n");
      return this;
    }

    if (wikiText.startsWith("}}", start) || wikiText.startsWith("]]", start)) {
      System.err.println("Close without open!");
      end += 2;
      return this;
    }

    
    if (this.matcher.find(start)) {
      end = this.matcher.start(1);
      if (end == start) {
        System.err.println(this.matcher.group());
        assert false;
      }
      return this;
    }
    
    end = wikiText.length();
    return this;
    
  }
  
  public String token() {
    return wikiText.substring(start, end);
  }
  
  private int escapedFind(final int start, final String toFind) {
    assert tokenStack.isEmpty();
    
    int end = start;
    while (end < wikiText.length()) {
      if (matcher.find(end)) {
        final String matchText = matcher.group();
        final int matchStart = matcher.start();
        
        if (matchText.length() == 0) {
          assert matchStart == wikiText.length() || wikiText.charAt(matchStart) == '\n';
          if (tokenStack.isEmpty() && toFind.equals("\n")) {
            return matchStart;
          }
          ++end;
        } else if (tokenStack.isEmpty() && matchText.equals(toFind)) {
          // The normal return....
          return matcher.end();
        } else if (matchText.equals("[[") || matchText.equals("{{")) {
          tokenStack.add(matchText);
        } else if (matchText.equals("]]") || matchText.equals("}}")) {
          if (tokenStack.size() > 0) {
            final String removed = tokenStack.remove(tokenStack.size() - 1);
            if (removed.equals("{{") && !matcher.group().equals("}}")) {
              System.err.println("Unmatched {{ error: " + wikiText.substring(start));
              return safeIndexOf(wikiText, start, "\n", "\n");
            } else if (removed.equals("[[") && !matcher.group().equals("]]")) {
              System.err.println("Unmatched [[ error: " + wikiText.substring(start));
              return safeIndexOf(wikiText, start, "\n", "\n");
            }
          } else {
            System.err.println("Pop too many error: " + wikiText.substring(start).replaceAll("\n", "\\n"));
            // If we were looking for a newline
            return safeIndexOf(wikiText, start, "\n", "\n");
          }
        } else if (matchText.equals("<!--")) {
          end = wikiText.indexOf("-->");
          if (end == -1) {
            System.err.println("Unmatched <!-- error: " + wikiText.substring(start));
          }
        } else {
          assert false : "Match text='" + matchText + "'";
          throw new IllegalStateException();
        }
      } else {
        // Hmmm, we didn't find the closing symbol we were looking for...
        System.err.println("Couldn't find: " + toFind + ", "+ wikiText.substring(start));
        return safeIndexOf(wikiText, start, "\n", "\n");
      }
      
      // Inside the while loop.
      end = Math.max(end, matcher.end());
    }
    return end;
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

}

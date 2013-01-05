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

package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WikiTokenizer {
  
  public static interface Callback {
    void onPlainText(final String text);
    void onMarkup(WikiTokenizer wikiTokenizer);
    void onWikiLink(WikiTokenizer wikiTokenizer);
    void onNewline(WikiTokenizer wikiTokenizer);
    void onFunction(final WikiTokenizer tokenizer, String functionName, List<String> functionPositionArgs,
        Map<String, String> functionNamedArgs);
    void onHeading(WikiTokenizer wikiTokenizer);
    void onListItem(WikiTokenizer wikiTokenizer);
    void onComment(WikiTokenizer wikiTokenizer);
    void onHtml(WikiTokenizer wikiTokenizer);
  }
  
  public static class DoNothingCallback implements Callback {

    @Override
    public void onPlainText(String text) {
    }

    @Override
    public void onMarkup(WikiTokenizer wikiTokenizer) {
    }

    @Override
    public void onWikiLink(WikiTokenizer wikiTokenizer) {
    }

    @Override
    public void onNewline(WikiTokenizer wikiTokenizer) {
    }

    @Override
    public void onFunction(WikiTokenizer tokenizer, String functionName,
        List<String> functionPositionArgs, Map<String, String> functionNamedArgs) {
    }

    @Override
    public void onHeading(WikiTokenizer wikiTokenizer) {
    }

    @Override
    public void onListItem(WikiTokenizer wikiTokenizer) {
    }

    @Override
    public void onComment(WikiTokenizer wikiTokenizer) {
    }

    @Override
    public void onHtml(WikiTokenizer wikiTokenizer) {
    }
  }
  
  //private static final Pattern wikiTokenEvent = Pattern.compile("($)", Pattern.MULTILINE);
  private static final Pattern wikiTokenEvent = Pattern.compile("(" +
  		"\\{\\{|\\}\\}|" +
  		"\\[\\[|\\]\\]|" +
  		"\\||" +  // Need the | because we might have to find unescaped pipes
        "=|" +  // Need the = because we might have to find unescaped =
  		"<!--|" +
  		"''|" +
        "<pre>|" +
        "<math>|" +
        "<ref>|" +
  		"$)", Pattern.MULTILINE);
  private static final String listChars = "*#:;";
  
    
  final String wikiText;
  final Matcher matcher;

  boolean justReturnedNewline = true;
  int lastLineStart = 0;
  int end = 0;
  int start = -1;

  final List<String> errors = new ArrayList<String>();
  final List<String> tokenStack = new ArrayList<String>();
  

  private String headingWikiText;
  private int headingDepth;
  private int listPrefixEnd;
  private boolean isPlainText;
  private boolean isMarkup;
  private boolean isComment;
  private boolean isFunction;
  private boolean isWikiLink;
  private boolean isHtml;
  private int firstUnescapedPipePos;
  
  private int lastUnescapedPipePos;
  private int lastUnescapedEqualsPos;
  private final List<String> positionArgs = new ArrayList<String>();
  private final Map<String,String> namedArgs = new LinkedHashMap<String,String>();
  

  public WikiTokenizer(final String wikiText) {
    this(wikiText, true);
  }

  public WikiTokenizer(String wikiText, final boolean isNewline) {
    wikiText = wikiText.replaceAll("\u2028", "\n");
    wikiText = wikiText.replaceAll("\u0085", "\n");
    this.wikiText = wikiText;
    this.matcher = wikiTokenEvent.matcher(wikiText);
    justReturnedNewline = isNewline;
  }

  private void clear() {
    errors.clear();
    tokenStack.clear();

    headingWikiText = null;
    headingDepth = -1;
    listPrefixEnd = -1;
    isPlainText = false;
    isMarkup = false;
    isComment = false;
    isFunction = false;
    isWikiLink = false;
    isHtml = false;
    
    firstUnescapedPipePos = -1;
    lastUnescapedPipePos = -1;
    lastUnescapedEqualsPos = -1;
    positionArgs.clear();
    namedArgs.clear();
  }

  private static final Pattern POSSIBLE_WIKI_TEXT = Pattern.compile(
      "\\{\\{|" +
      "\\[\\[|" +
      "<!--|" +
      "''|" +
      "<pre>|" +
      "<math>|" +
      "<ref>|" +
      "[\n]"
      );

  public static void dispatch(final String wikiText, final boolean isNewline, final Callback callback) {
    // Optimization...
    if (!POSSIBLE_WIKI_TEXT.matcher(wikiText).find()) {
      callback.onPlainText(wikiText);
    } else {
      final WikiTokenizer tokenizer = new WikiTokenizer(wikiText, isNewline);
      while (tokenizer.nextToken() != null) {
        if (tokenizer.isPlainText()) {
          callback.onPlainText(tokenizer.token());
        } else if (tokenizer.isMarkup()) {
          callback.onMarkup(tokenizer);
        } else if (tokenizer.isWikiLink()) {
          callback.onWikiLink(tokenizer);
        } else if (tokenizer.isNewline()) {
          callback.onNewline(tokenizer);
        } else if (tokenizer.isFunction()) {
          callback.onFunction(tokenizer, tokenizer.functionName(), tokenizer.functionPositionArgs(), tokenizer.functionNamedArgs());
        } else if (tokenizer.isHeading()) {
          callback.onHeading(tokenizer);
        } else if (tokenizer.isListItem()) {
          callback.onListItem(tokenizer);
        } else if (tokenizer.isComment()) {
          callback.onComment(tokenizer);
        } else if (tokenizer.isHtml()) {
          callback.onHtml(tokenizer);
        } else if (!tokenizer.errors.isEmpty()) {
          // Log was already printed....
        } else {
          throw new IllegalStateException("Unknown wiki state: " + tokenizer.token());
        }
      }
    }
  }
  
  public List<String> errors() {
    return errors;
  }
  
  public boolean isNewline() {
    return justReturnedNewline;
  }
  
  public void returnToLineStart() {
    end = start = lastLineStart;
    justReturnedNewline = true;
  }
  
  public boolean isHeading() {
    return headingWikiText != null;
  }
  
  public String headingWikiText() {
    assert isHeading();
    return headingWikiText;
  }
  
  public int headingDepth() {
    assert isHeading();
    return headingDepth;
  }
  
  public boolean isMarkup() {
    return isMarkup;
  }

  public boolean isComment() {
    return isComment;
  }

  public boolean isListItem() {
    return listPrefixEnd != -1;
  }
  
  public String listItemPrefix() {
    assert isListItem();
    return wikiText.substring(start, listPrefixEnd);
  }
  
  public static String getListTag(char c) {
    if (c == '#') {
      return "ol";
    }
    return "ul";
  }

  public String listItemWikiText() {
    assert isListItem();
    return wikiText.substring(listPrefixEnd, end);
  }
  
  public boolean isFunction() {
    return isFunction;
  }

  public String functionName() {
    assert isFunction();
    // "{{.."
    if (firstUnescapedPipePos != -1) {
      return trimNewlines(wikiText.substring(start + 2, firstUnescapedPipePos).trim());
    }
    final int safeEnd = Math.max(start + 2, end - 2);
    return trimNewlines(wikiText.substring(start + 2, safeEnd).trim());
  }
  
  public List<String> functionPositionArgs() {
    return positionArgs;
  }

  public Map<String, String> functionNamedArgs() {
    return namedArgs;
  }

  public boolean isPlainText() {
    return isPlainText;
  }

  public boolean isWikiLink() {
    return isWikiLink;
  }

  public String wikiLinkText() {
    assert isWikiLink();
    // "[[.."
    if (lastUnescapedPipePos != -1) {
      return trimNewlines(wikiText.substring(lastUnescapedPipePos + 1, end - 2));
    }
    assert start + 2 < wikiText.length() && end >= 2: wikiText;
    return trimNewlines(wikiText.substring(start + 2, end - 2));
  }

  public String wikiLinkDest() {
    assert isWikiLink();
    // "[[.."
    if (firstUnescapedPipePos != -1) {
      return trimNewlines(wikiText.substring(start + 2, firstUnescapedPipePos));
    }
    return null;
  }
  
  public boolean isHtml() {
    return isHtml;
  }

  public boolean remainderStartsWith(final String prefix) {
    return wikiText.startsWith(prefix, start);
  }
  
  public void nextLine() {
    final int oldStart = start;
    while(nextToken() != null && !isNewline()) {}
    if (isNewline()) {
      --end;
    }
    start = oldStart;
  }

  
  public WikiTokenizer nextToken() {
    this.clear();
    
    start = end;
    
    if (justReturnedNewline) {
      lastLineStart = start;
    }
    
    try {
    
    final int len = wikiText.length();
    if (start >= len) {
      return null;
    }
    
    // Eat a newline if we're looking at one:
    final boolean atNewline = wikiText.charAt(end) == '\n' || wikiText.charAt(end) == '\u2028';
    if (atNewline) {
      justReturnedNewline = true;
      ++end;
      return this;
    }
    
    if (justReturnedNewline) {   
      justReturnedNewline = false;

      final char firstChar = wikiText.charAt(end);
      if (firstChar == '=') {
        final int headerStart = end;
        // Skip ===...
        while (++end < len && wikiText.charAt(end) == '=') {}
        final int headerTitleStart = end;
        headingDepth = headerTitleStart - headerStart;
        // Skip non-=...
        if (end < len) {
          final int nextNewline = safeIndexOf(wikiText, end, "\n", "\n");
          final int closingEquals = escapedFindEnd(end, "=");
          if (wikiText.charAt(closingEquals - 1) == '=') {
            end = closingEquals - 1;
          } else {
            end = nextNewline;
          }
        }
        final int headerTitleEnd = end;
        headingWikiText = wikiText.substring(headerTitleStart, headerTitleEnd);
        // Skip ===...
        while (end < len && ++end < len && wikiText.charAt(end) == '=') {}
        final int headerEnd = end;
        if (headerEnd - headerTitleEnd != headingDepth) {
          errors.add("Mismatched header depth: " + token());
        }
        return this;
      }
      if (listChars.indexOf(firstChar) != -1) {
        while (++end < len && listChars.indexOf(wikiText.charAt(end)) != -1) {}
        listPrefixEnd = end;
        end = escapedFindEnd(start, "\n");
        return this;
      }
    }

    if (wikiText.startsWith("'''", start)) {
      isMarkup = true;
      end = start + 3;
      return this;
    }
    
    if (wikiText.startsWith("''", start)) {
      isMarkup = true;
      end = start + 2;
      return this;
    }

    if (wikiText.startsWith("[[", start)) {
      end = escapedFindEnd(start + 2, "]]");
      isWikiLink = errors.isEmpty();
      return this;
    }

    if (wikiText.startsWith("{{", start)) {      
      end = escapedFindEnd(start + 2, "}}");
      isFunction = errors.isEmpty();
      return this;
    }

    if (wikiText.startsWith("<pre>", start)) {
      end = safeIndexOf(wikiText, start, "</pre>", "\n");
      isHtml = true;
      return this;
    }

    if (wikiText.startsWith("<ref>", start)) {
        end = safeIndexOf(wikiText, start, "</ref>", "\n");
        isHtml = true;
        return this;
      }

    if (wikiText.startsWith("<math>", start)) {
      end = safeIndexOf(wikiText, start, "</math>", "\n");
      isHtml = true;
      return this;
    }

    if (wikiText.startsWith("<!--", start)) {
      isComment = true;
      end = safeIndexOf(wikiText, start, "-->", "\n");
      return this;
    }

    if (wikiText.startsWith("}}", start) || wikiText.startsWith("]]", start)) {
      errors.add("Close without open!");
      end += 2;
      return this;
    }

    if (wikiText.charAt(start) == '|' || wikiText.charAt(start) == '=') {
      isPlainText = true;
      ++end;
      return this;
    }

    
    if (this.matcher.find(start)) {
      end = this.matcher.start(1);
      isPlainText = true;
      if (end == start) {
        errors.add("Empty group: " + this.matcher.group());
        assert false;
      }
      return this;
    }
    
    end = wikiText.length();
    return this;
    
    } finally {
      if (!errors.isEmpty()) {
        System.err.println("Errors: " + errors + ", token=" + token());
      }
    }
    
  }
  
  public String token() {
    final String token = wikiText.substring(start, end);
    assert token.equals("\n") || !token.endsWith("\n") : "token='" + token + "'";
    return token;
  }
  
  private int escapedFindEnd(final int start, final String toFind) {
    assert tokenStack.isEmpty();
    
    final boolean insideFunction = toFind.equals("}}");
    
    int end = start;
    int firstNewline = -1;
    while (end < wikiText.length()) {
      if (matcher.find(end)) {
        final String matchText = matcher.group();
        final int matchStart = matcher.start();
        
        assert matcher.end() > end || matchText.length() == 0: "Group=" + matcher.group();
        if (matchText.length() == 0) {
          assert matchStart == wikiText.length() || wikiText.charAt(matchStart) == '\n' : wikiText + ", " + matchStart;
          if (firstNewline == -1) {
            firstNewline = matcher.end();
          }
          if (tokenStack.isEmpty() && toFind.equals("\n")) {
            return matchStart;
          }
          ++end;
        } else if (tokenStack.isEmpty() && matchText.equals(toFind)) {
          // The normal return....
          if (insideFunction) {
            addFunctionArg(insideFunction, matchStart);
          }
          return matcher.end();
        } else if (matchText.equals("[[") || matchText.equals("{{")) {
          tokenStack.add(matchText);
        } else if (matchText.equals("]]") || matchText.equals("}}")) {
          if (tokenStack.size() > 0) {
            final String removed = tokenStack.remove(tokenStack.size() - 1);
            if (removed.equals("{{") && !matcher.group().equals("}}")) {
              errors.add("Unmatched {{ error: " + wikiText.substring(start));
              return safeIndexOf(wikiText, start, "\n", "\n");
            } else if (removed.equals("[[") && !matcher.group().equals("]]")) {
              errors.add("Unmatched [[ error: " + wikiText.substring(start));
              return safeIndexOf(wikiText, start, "\n", "\n");
            }
          } else {
            errors.add("Pop too many error: " + wikiText.substring(start).replaceAll("\n", "\\\\n"));
            // If we were looking for a newline
            return safeIndexOf(wikiText, start, "\n", "\n");
          }
        } else if (matchText.equals("|")) { 
          if (tokenStack.isEmpty()) {
            addFunctionArg(insideFunction, matchStart);
          }
        } else if (matchText.equals("=")) {
          if (tokenStack.isEmpty()) {
            lastUnescapedEqualsPos = matchStart;
          }
          // Do nothing.  These can match spuriously, and if it's not the thing
          // we're looking for, keep on going.
        } else if (matchText.equals("<!--")) {
          end = wikiText.indexOf("-->");
          if (end == -1) {
            errors.add("Unmatched <!-- error: " + wikiText.substring(start));
            return safeIndexOf(wikiText, start, "\n", "\n");
          }
        } else if (matchText.equals("''") || (matchText.startsWith("<") && matchText.endsWith(">"))) {
          // Don't care.
        } else {
          assert false : "Match text='" + matchText + "'";
          throw new IllegalStateException();
        }
      } else {
        // Hmmm, we didn't find the closing symbol we were looking for...
        errors.add("Couldn't find: " + toFind + ", "+ wikiText.substring(start));
        return safeIndexOf(wikiText, start, "\n", "\n");
      }
      
      // Inside the while loop.  Just go forward.
      end = Math.max(end, matcher.end());
    }
    if (toFind.equals("\n") && tokenStack.isEmpty()) {
      // We were looking for the end, we got it.
      return end;
    }
    if (firstNewline != -1) {
      errors.add("Couldn't find: " + toFind + ", "+ wikiText.substring(start));
      return firstNewline;
    }
    return end;
  }

  private void addFunctionArg(final boolean insideFunction, final int matchStart) {
    if (firstUnescapedPipePos == -1) {
      firstUnescapedPipePos = lastUnescapedPipePos = matchStart;
    } else if (insideFunction) {
      if (lastUnescapedEqualsPos > lastUnescapedPipePos) {
        final String key = wikiText.substring(lastUnescapedPipePos + 1, lastUnescapedEqualsPos);
        final String value = wikiText.substring(lastUnescapedEqualsPos + 1, matchStart);
        namedArgs.put(trimNewlines(key), trimNewlines(value));
      } else {
        final String value = wikiText.substring(lastUnescapedPipePos + 1, matchStart);
        positionArgs.add(trimNewlines(value));
      }
    }
    lastUnescapedPipePos = matchStart;
  }
  
  static final String trimNewlines(String s) {
    while (s.startsWith("\n")) {
      s = s.substring(1);
    }
    while (s.endsWith("\n")) {
      s = s.substring(0, s.length() - 1);
    }
    return s.replaceAll("\n", " ");
  }

  static int safeIndexOf(final String s, final int start, final String target, final String backup) {
    int close = s.indexOf(target, start);
    if (close != -1) {
      // Don't step over a \n.
      return close + (target.equals("\n") ? 0 : target.length());
    }
    close = s.indexOf(backup, start);
    if (close != -1) {
      return close + (backup.equals("\n") ? 0 : backup.length());
    }
    return s.length();
  }

  public static String toPlainText(final String wikiText) {
    final WikiTokenizer wikiTokenizer = new WikiTokenizer(wikiText);
    final StringBuilder builder = new StringBuilder();
    while (wikiTokenizer.nextToken() != null) {
      if (wikiTokenizer.isPlainText()) {
        builder.append(wikiTokenizer.token());
      } else if (wikiTokenizer.isWikiLink()) {
        builder.append(wikiTokenizer.wikiLinkText());
      } else if (wikiTokenizer.isNewline()) {
        builder.append("\n");
      } else if (wikiTokenizer.isFunction()) {
        builder.append(wikiTokenizer.token());
      }
    }
    return builder.toString();
  }

  public static StringBuilder appendFunction(final StringBuilder builder, final String name, List<String> args,
      final Map<String, String> namedArgs) {
    builder.append(name);
    for (final String arg : args) {
      builder.append("|").append(arg);
    }
    for (final Map.Entry<String, String> entry : namedArgs.entrySet()) {
      builder.append("|").append(entry.getKey()).append("=").append(entry.getValue());
    }
    return builder;
  }

}

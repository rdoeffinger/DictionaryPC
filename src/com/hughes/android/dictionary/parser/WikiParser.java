package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiParser {
  
  private static final Pattern markup = Pattern.compile("$|''|\\{\\{|\\[\\[|(==+)\\s*$|<!--|<pre>", Pattern.MULTILINE);
  private static final Pattern listStart = Pattern.compile("^[*#;:]+");
  private static final Pattern pipeSplit = Pattern.compile("\\s*\\|\\s*");
  private static final Pattern whitespace = Pattern.compile("\\s+");
  private static final Pattern headerStart = Pattern.compile("^==+");
  
  
  static void parse(final String wikiText, final WikiCallback callback) {
    
    boolean boldOn = false;
    boolean italicOn = false;
    int insideHeaderDepth = -1;
    String lastListItem = null;

    final List<String> positionalArgs = new ArrayList<String>();
    final Map<String, String> namedArgs = new LinkedHashMap<String, String>();

    String rest = wikiText;
    while (rest.length() > 0) {
      final Matcher matcher = markup.matcher(rest);
      if (matcher.find()) {
        final int nextMarkupPos = matcher.start();
        if (nextMarkupPos != 0) {
          String text = rest.substring(0, nextMarkupPos);
          whitespace.matcher(text).replaceAll(" ");
          callback.onText(text);
        }
        rest = rest.substring(nextMarkupPos);
        
        if (rest.startsWith("\n")) {
          rest = rest.substring(1);
          
          if (insideHeaderDepth != -1) {
            throw new RuntimeException("barf");
          }
          if (lastListItem != null) {
            callback.onListItemEnd(lastListItem, null);
          }
          
          final Matcher headerMatcher = headerStart.matcher(rest);
          if (headerMatcher.find()) {
            insideHeaderDepth = headerMatcher.group().length();            
            callback.onHeadingStart(insideHeaderDepth);
            rest = rest.substring(headerMatcher.group().length());
            continue;
          }
          
          if (listStart.matcher(rest).find()) {
            lastListItem = matcher.group();
            callback.onListItemStart(lastListItem, null);
            rest = rest.substring(lastListItem.length());
            continue;
          } else if (lastListItem != null) {
            callback.onNewParagraph();
            lastListItem = null;
          }
          
          if (rest.startsWith("\n")) {
            callback.onNewParagraph();
            continue;
          }
          callback.onNewLine();
        } else if (rest.startsWith("'''")) {
          boldOn = !boldOn;
          callback.onFormatBold(boldOn);
          rest = rest.substring(3);
        } else if (rest.startsWith("''")) {
          italicOn = !italicOn;
          callback.onFormatItalic(italicOn);
          rest = rest.substring(2);
        } else if (rest.startsWith("{{")) {
          int end = rest.indexOf("}}");
          if (end == -1) {
            callback.onUnterminated("{{", rest);
            return;
          }
          final String template = rest.substring(2, end).trim();
          final String[] templateArray = pipeSplit.split(template);
          positionalArgs.clear();
          namedArgs.clear();
          for (int i = 0; i < templateArray.length; ++i) {
            int equalPos = templateArray[i].indexOf('=');
            if (equalPos == -1) {
              positionalArgs.add(templateArray[i]);
            } else {
              namedArgs.put(templateArray[i].substring(0, equalPos), templateArray[i].substring(equalPos + 1));
            }
          }
          callback.onTemplate(positionalArgs, namedArgs);
          rest = rest.substring(end + 2);
        } else if (rest.startsWith("[[")) {
          int end = rest.indexOf("]]");
          if (end == -1) {
            callback.onUnterminated("[[", rest);
            return;
          }
          final String wikiLink = rest.substring(2, end);
          final String[] args = pipeSplit.split(wikiLink);
          callback.onWikiLink(args);
          rest = rest.substring(end + 2);
        } else if (rest.startsWith("=")) {
          final String match = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
          if (insideHeaderDepth == -1) {
          } else {
            if (match.length() != insideHeaderDepth) {
              callback.onInvalidHeaderEnd(rest);
              return;
            }
            callback.onHeadingEnd(insideHeaderDepth);
            insideHeaderDepth = -1;
          }
          rest = rest.substring(match.length());
        } else if (rest.startsWith("<!--")) {
          int end = rest.indexOf("-->");
          if (end == -1) {
            callback.onUnterminated("<!--", rest);
            return;
          }
          callback.onComment(rest.substring(4, end));
          rest = rest.substring(end + 3);
        } else if (rest.startsWith("<pre>")) {
          int end = rest.indexOf("</pre>");
          if (end == -1) {
            callback.onUnterminated("<pre>", rest);
            return;
          }
          callback.onText(rest.substring(5, end));
          rest = rest.substring(end + 6);
        } else {
          throw new RuntimeException("barf!");
        }
      }  // matcher.find()
    }
  }

}

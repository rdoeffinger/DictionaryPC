package com.hughes.android.dictionary.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiParser {
  
  private static final Pattern markup = Pattern.compile("$|''|\\{\\{|\\[\\[|^[*#;:]+|^(==+)\\s*|(==+)\\s*$|<!--|<pre>", Pattern.MULTILINE);
  private static final Pattern listStart = Pattern.compile("^[*#;:]");
  private static final Pattern pipeSplit = Pattern.compile("\\s*\\|\\s*");
  private static final Pattern whitespace = Pattern.compile("\\s+");
  
  static void parse(final String wikiText, final WikiCallback callback) {
    
    boolean boldOn = false;
    boolean italicOn = false;
    int insideHeaderDepth = -1;
    String lastListItem = null;
    
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
          if (insideHeaderDepth != -1) {
            throw new RuntimeException("barf");
          }
          if (lastListItem != null) {
            callback.onListItemEnd(lastListItem, null);
          }
          if (!listStart.matcher(rest.substring(1)).matches()) {
            lastListItem = null;
          }
          if (rest.startsWith("\n\n")) {
            // TODO(thadh): eat all the newlines.
            callback.onNewParagraph();
            rest = rest.substring(2); 
          } else {
            callback.onNewLine();
            rest = rest.substring(1);
          }
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
          final String[][] templateArgs = new String[templateArray.length][];
          for (int i = 0; i < templateArray.length; ++i) {
            int equalPos = templateArray[i].indexOf('=');
            if (equalPos == -1) {
              templateArgs[i] = new String[] { null, templateArray[i] };
            } else {
              templateArgs[i] = new String[] { templateArray[i].substring(0, equalPos), templateArray[i].substring(equalPos + 1) };
            }
          }
          callback.onTemplate(templateArgs);
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
            insideHeaderDepth = match.length();            
            callback.onHeadingStart(insideHeaderDepth);
          } else {
            if (match.length() != insideHeaderDepth) {
              callback.onInvalidHeaderEnd(rest);
              return;
            }
            callback.onHeadingEnd(insideHeaderDepth);
            insideHeaderDepth = -1;
          }
          rest = rest.substring(match.length());
        } else if (rest.startsWith("*") || rest.startsWith("#") || rest.startsWith(";") || rest.startsWith(":")) {
          lastListItem = matcher.group();
          callback.onListItemStart(lastListItem, null);
          rest = rest.substring(lastListItem.length());
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

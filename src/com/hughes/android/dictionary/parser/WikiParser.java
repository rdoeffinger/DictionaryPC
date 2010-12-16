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
          rest = rest.substring(nextMarkupPos);
        }
        
        if (rest.equals("")) {
          continue;
        } else if (rest.startsWith("\n")) {
          rest = rest.substring(1);
          
          if (insideHeaderDepth != -1) {
            throw new RuntimeException("barf");
          }
          if (lastListItem != null) {
            callback.onListItemEnd(lastListItem, null);
          }
          
          final Matcher headerMatcher = headerStart.matcher(rest);
          if (headerMatcher.find()) {
            lastListItem = null;
            insideHeaderDepth = headerMatcher.group().length();            
            callback.onHeadingStart(insideHeaderDepth);
            rest = rest.substring(headerMatcher.group().length());
            continue;
          }

          final Matcher listStartMatcher = listStart.matcher(rest);
          if (listStartMatcher.find()) {
            lastListItem = listStartMatcher.group();
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
          //todo: this doesn't work.  can't split pipes inside [[asdf|asdf]]
          final List<String> templateArray = new ArrayList<String>();
          contextSensitivePipeSplit(template, templateArray);
          positionalArgs.clear();
          namedArgs.clear();
          for (int i = 0; i < templateArray.size(); ++i) {
            
            int equalPos = -1;
            do {
              equalPos = templateArray.get(i).indexOf('=', equalPos + 1);
            } while (equalPos > 1 && templateArray.get(i).charAt(equalPos - 1) == ' ');

            if (equalPos == -1) {
              positionalArgs.add(templateArray.get(i));
            } else {
              namedArgs.put(templateArray.get(i).substring(0, equalPos), templateArray.get(i).substring(equalPos + 1));
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
          throw new RuntimeException("barf: " + rest);
        }
      }  // matcher.find()
    }
  }
  
  private static final Pattern openBracketOrPipe = Pattern.compile("($)|(\\[\\[)|(\\s*\\|\\s*)");
  private static void contextSensitivePipeSplit(String template, final List<String> result) {
    StringBuilder builder = new StringBuilder();
    while (template.length() > 0) {
      final Matcher matcher = openBracketOrPipe.matcher(template);
      if (matcher.find()) {
        // append to the match.
        builder.append(template.substring(0, matcher.start()));
        if (matcher.group(2) != null) {  // [[
          // append to the close ]].
          final int closeIndex = template.indexOf("]]", matcher.end());
          builder.append(template.substring(matcher.start(), closeIndex + 2));
          template = template.substring(closeIndex + 2);
        } else if (matcher.group(3) != null) { // |
          result.add(builder.toString());
          builder = new StringBuilder();
          template = template.substring(matcher.end());
        } else {
          template = template.substring(matcher.start());
          assert template.length() == 0 : template;
        }
      } else {
        assert false;
      }
    }
    result.add(builder.toString());
  }

  // ------------------------------------------------------------------------

  public static String simpleParse(final String wikiText) {
    final StringBuilderCallback callback = new StringBuilderCallback();
    parse(wikiText, callback);
    return callback.builder.toString();
  }
  
  static final class StringBuilderCallback implements WikiCallback {

    final StringBuilder builder = new StringBuilder();
    
    @Override
    public void onComment(String text) {
    }

    @Override
    public void onFormatBold(boolean boldOn) {
    }

    @Override
    public void onFormatItalic(boolean italicOn) {
    }

    @Override
    public void onWikiLink(String[] args) {
      builder.append(args[args.length - 1]);
    }

    @Override
    public void onTemplate(List<String> positionalArgs,
        Map<String, String> namedArgs) {
      builder.append("{{").append(positionalArgs).append(namedArgs).append("}}");
    }

    @Override
    public void onText(String text) {
      builder.append(text);
    }

    @Override
    public void onHeadingStart(int depth) {
    }

    @Override
    public void onHeadingEnd(int depth) {
    }

    @Override
    public void onNewLine() {
    }

    @Override
    public void onNewParagraph() {
    }

    @Override
    public void onListItemStart(String header, int[] section) {
    }

    @Override
    public void onListItemEnd(String header, int[] section) {
    }

    @Override
    public void onUnterminated(String start, String rest) {
      throw new RuntimeException(start + rest);
    }

    @Override
    public void onInvalidHeaderEnd(String rest) {
      throw new RuntimeException(rest);
    }
    
  }


}

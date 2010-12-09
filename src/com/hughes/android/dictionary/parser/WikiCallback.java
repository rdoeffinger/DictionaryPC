package com.hughes.android.dictionary.parser;

import java.util.List;
import java.util.Map;


public interface WikiCallback {

  void onComment(final String text);

  void onFormatBold(final boolean boldOn);
  void onFormatItalic(final boolean italicOn);

  void onWikiLink(final String[] args);

  void onTemplate(final List<String> positionalArgs, final Map<String,String> namedArgs);

  // Will never contain a newline unless it's in a <pre>
  void onText(final String text);

  // Only at start of line.
  void onHeadingStart(final int depth);
  void onHeadingEnd(final int depth);
  
  
  void onNewLine();
  void onNewParagraph();

  void onListItemStart(final String header, final int[] section);
  void onListItemEnd(final String header, final int[] section);

  // Errors
  void onUnterminated(final String start, String rest);
  void onInvalidHeaderEnd(String rest);
  
}

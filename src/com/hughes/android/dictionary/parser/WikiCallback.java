package com.hughes.android.dictionary.parser;


public interface WikiCallback {

  void onComment(final String text);

  void onFormatBold(final boolean boldOn);
  void onFormatItalic(final boolean italicOn);

  void onWikiLink(final String[] args);

  void onTemplate(final String[][] args);

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

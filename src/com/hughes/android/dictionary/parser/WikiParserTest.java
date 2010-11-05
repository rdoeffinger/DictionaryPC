package com.hughes.android.dictionary.parser;

import junit.framework.TestCase;

public class WikiParserTest extends TestCase {
  
  public void testSimple() {
    final String text =
      "Hi" + "\n" +
      "Hello ''thad'' you're <!-- not --> '''pretty''' cool '''''over''''' there." + "\n" +
      "hi <!--" + "\n" +
      "multi-line" + "\n" +
      "# comment -->" + "\n" +
      "" + "\n" +
      "# li" + "\n" +
      "# li2" + "\n" +
      "## li2.2" + "\n" +
      "Hi again." + "\n" +
      "here's [[some blah|some]] wikitext." + "\n" +
      "here's a {{template|blah=2|blah2=3|" + "\n" +
      "blah3=3}} and some more text." + "\n" +
      "== Header 2 ==" + "\n" +
      "=== {{header-template}} ===" + "\n";
    
    final String expected = "Hi Hello <i>thad</i> you're \n" +
        "comment: not \n" +
        " <b>pretty</b> cool <b><i>over</b></i> there. hi \n" +
        "comment:\n" +
        "multi-line\n" +
        "# comment \n" +
        "\n" +
        "\n" +
        "# li\n" +
        " # li2\n" +
        " ## li2.2\n" +
        " Hi again. here's [[some]] wikitext. here's a \n" +
        "template:template\n" +
        " and some more text. \n" +
        "HEADER   Header 2 \n" +
        " \n" +
        "HEADER    \n" +
        "template:header-template\n" +
        " \n" +
        " ";
    final PrintWikiCallback callback = new PrintWikiCallback();
    WikiParser.parse(text, callback);
    assertEquals(expected, callback.builder.toString());
    
  }
  
  
  static final class PrintWikiCallback implements WikiCallback {
    final StringBuilder builder = new StringBuilder();

    @Override
    public void onComment(String text) {
      builder.append("\ncomment:").append(text).append("\n");
    }

    @Override
    public void onFormatBold(boolean boldOn) {
      builder.append(boldOn ? "<b>" : "</b>");
    }

    @Override
    public void onFormatItalic(boolean italicOn) {
      builder.append(italicOn ? "<i>" : "</i>");
    }

    @Override
    public void onWikiLink(String[] args) {
      builder.append("[[").append(args[args.length - 1]).append("]]");
    }

    @Override
    public void onTemplate(String[][] args) {
      builder.append("\ntemplate:").append(args[0][0]).append("\n");
    }

    @Override
    public void onText(String text) {
      builder.append(text);
    }

    @Override
    public void onHeadingStart(int depth) {
      builder.append("\nHEADER");
      for (int i = 0; i < depth; ++i) {
        builder.append(" ");
      }
    }

    @Override
    public void onHeadingEnd(int depth) {
      builder.append("\n");
    }
    
    @Override
    public void onNewLine() {
      builder.append(" ");
    }

    @Override
    public void onNewParagraph() {
      builder.append("\n\n");
    }

    @Override
    public void onListItemStart(String header, int[] section) {
      builder.append(header);
    }

    @Override
    public void onListItemEnd(String header, int[] section) {
      builder.append("\n");
    }

    @Override
    public void onUnterminated(String start, String rest) {
      throw new RuntimeException("bad");
    }

    @Override
    public void onInvalidHeaderEnd(String rest) {
      throw new RuntimeException("bad");
    }
    
  }
  


}

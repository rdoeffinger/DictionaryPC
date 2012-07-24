package com.hughes.android.dictionary.parser.wiktionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.HtmlEntry;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.parser.WikiTokenizer;

public class WholeSectionToHtmlParser extends AbstractWiktionaryParser {
  
  public static final String NAME = "WholeSectionToHtmlParser";
  public static final Pattern skipSections = Pattern.compile(".*Translations.*");
  
  final IndexBuilder titleIndexBuilder;
  
  public  WholeSectionToHtmlParser(final IndexBuilder titleIndexBuilder) {
    this.titleIndexBuilder = titleIndexBuilder;
  }

  @Override
  void parseSection(String heading, String text) {
    HtmlEntry htmlEntry = new HtmlEntry(entrySource, title);
    IndexedEntry indexedEntry = new IndexedEntry(htmlEntry);

    final AppendAndIndexWikiCallback<WholeSectionToHtmlParser> callback = new AppendCallback(this);
    callback.builder = new StringBuilder();
    callback.indexedEntry = indexedEntry;
    callback.dispatch(text, null);

    htmlEntry.html = callback.builder.toString();
    indexedEntry.isValid = true;
    titleIndexBuilder.addEntryWithString(indexedEntry, title, EntryTypeName.WIKTIONARY_TITLE_MULTI_DETAIL);
  }

  @Override
  void removeUselessArgs(Map<String, String> namedArgs) {
  }
  
  class AppendCallback extends AppendAndIndexWikiCallback<WholeSectionToHtmlParser> {
    public AppendCallback(WholeSectionToHtmlParser parser) {
      super(parser);
    }

    @Override
    public void onPlainText(String plainText) {
      super.onPlainText(plainText);
    }

    @Override
    public void onWikiLink(WikiTokenizer wikiTokenizer) {
      super.onWikiLink(wikiTokenizer);
    }

    @Override
    public void onFunction(WikiTokenizer wikiTokenizer, String name,
        List<String> args, Map<String, String> namedArgs) {
      super.onFunction(wikiTokenizer, name, args, namedArgs);
    }

    @Override
    public void onHtml(WikiTokenizer wikiTokenizer) {
      super.onHtml(wikiTokenizer);
    }
    
    @Override
    public void onNewline(WikiTokenizer wikiTokenizer) {
    }

    @Override
    public void onHeading(WikiTokenizer wikiTokenizer) {
      final String headingText = wikiTokenizer.headingWikiText();
      final int depth = wikiTokenizer.headingDepth();
      if (skipSections.matcher(headingText).matches()) {
        while ((wikiTokenizer = wikiTokenizer.nextToken()) != null) {
          if (wikiTokenizer.isHeading() && wikiTokenizer.headingDepth() <= depth) {
            wikiTokenizer.returnToLineStart();
            return;
          }
        }
        return;
      }
      onPlainText(String.format("\n<h%d>", depth));
      dispatch(headingText, null);
      onPlainText(String.format("</h%d>\n", depth));
    }

    final List<Character> listPrefixStack = new ArrayList<Character>();
    @Override
    public void onListItem(WikiTokenizer wikiTokenizer) {
      if (builder.length() != 0 && builder.charAt(builder.length() - 1) != '\n') {
        builder.append("\n");
      }
      final String prefix = wikiTokenizer.listItemPrefix();
      while (listPrefixStack.size() < prefix.length()) {
        onPlainText(String.format("<%s>", WikiTokenizer.getListTag(prefix.charAt(listPrefixStack.size()))));
        listPrefixStack.add(prefix.charAt(listPrefixStack.size()));
      }
      onPlainText("<li>");
      dispatch(wikiTokenizer.listItemWikiText(), null);
      onPlainText("</li>\n");
      
      WikiTokenizer nextToken = wikiTokenizer.nextToken();
      boolean returnToLineStart = false;
      if (nextToken != null && nextToken.isNewline()) {
        nextToken = nextToken.nextToken();
        returnToLineStart = true;
      }
      final String nextListHeader;
      if (nextToken == null || !nextToken.isListItem()) {
        nextListHeader = "";
      } else {
        nextListHeader = nextToken.listItemPrefix();
      }
      if (returnToLineStart) {
        wikiTokenizer.returnToLineStart();
      }
      while (listPrefixStack.size() > nextListHeader.length()) {
        final char prefixChar = listPrefixStack.remove(listPrefixStack.size() - 1);
        onPlainText(String.format("</%s>\n", WikiTokenizer.getListTag(prefixChar)));
      }
    }

    boolean boldOn = false;
    boolean italicOn = false;
    @Override
    public void onMarkup(WikiTokenizer wikiTokenizer) {
      if ("'''".equals(wikiTokenizer.token())) {
        if (!boldOn) {
          onPlainText("<b>");
        } else {
          onPlainText("</b>");
        }
        boldOn = !boldOn;
      } else if ("''".equals(wikiTokenizer.token())) {
        if (!italicOn) {
          onPlainText("<em>");
        } else {
          onPlainText("</em>");
        }
        italicOn = !italicOn;
      } else {
        assert false;
      }
    }
    
  }

}


package com.hughes.android.dictionary.parser.wiktionary;

import com.hughes.android.dictionary.engine.HtmlEntry;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexBuilder.TokenData;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.util.StringUtil;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WholeSectionToHtmlParser extends AbstractWiktionaryParser {

    public static final String NAME = "WholeSectionToHtmlParser";

    interface LangConfig {
        boolean skipSection(final String name);
        boolean skipWikiLink(final WikiTokenizer wikiTokenizer);
        String adjustWikiLink(String wikiLinkDest);
        void addFunctionCallbacks(
                Map<String, FunctionCallback<WholeSectionToHtmlParser>> functionCallbacks);
    }
    static final Map<String,LangConfig> isoToLangConfig = new LinkedHashMap<String,LangConfig>();
    static {
        final Pattern enSkipSections = Pattern.compile(".*Translations|Anagrams|References.*");
        isoToLangConfig.put("EN", new LangConfig() {
            @Override
            public boolean skipSection(String headingText) {
                return enSkipSections.matcher(headingText).matches();
            }

            @Override
            public boolean skipWikiLink(WikiTokenizer wikiTokenizer) {
                final String wikiText = wikiTokenizer.wikiLinkText();
                if (wikiText.startsWith("Category:")) {
                    return true;
                }
                return false;
            }
            @Override
            public String adjustWikiLink(String wikiLinkDest) {
                if (wikiLinkDest.startsWith("w:") || wikiLinkDest.startsWith("Image:")) {
                    return null;
                }
                return wikiLinkDest;
            }

            @Override
            public void addFunctionCallbacks(
                    Map<String, FunctionCallback<WholeSectionToHtmlParser>> functionCallbacks) {
                EnFunctionCallbacks.addGenericCallbacks(functionCallbacks);
            }});
    }

    final IndexBuilder titleIndexBuilder;
    final String skipLangIso;
    final LangConfig langConfig;

    public WholeSectionToHtmlParser(final IndexBuilder titleIndexBuilder, final String wiktionaryIso, final String skipLangIso) {
        this.titleIndexBuilder = titleIndexBuilder;
        assert isoToLangConfig.containsKey(wiktionaryIso): wiktionaryIso;
        this.langConfig = isoToLangConfig.get(wiktionaryIso);
        this.skipLangIso = skipLangIso;
    }

    @Override
    void parseSection(String heading, String text) {
        HtmlEntry htmlEntry = new HtmlEntry(entrySource, StringEscapeUtils.escapeHtml3(title));
        IndexedEntry indexedEntry = new IndexedEntry(htmlEntry);

        final AppendAndIndexWikiCallback<WholeSectionToHtmlParser> callback = new AppendCallback(
                this);
        langConfig.addFunctionCallbacks(callback.functionCallbacks);

        callback.builder = new StringBuilder();
        callback.indexedEntry = indexedEntry;
        callback.dispatch(text, null);

        htmlEntry.html = callback.builder.toString();
        indexedEntry.isValid = true;

        final TokenData tokenData = titleIndexBuilder.getOrCreateTokenData(title);

        htmlEntry.addToDictionary(titleIndexBuilder.index.dict);
        tokenData.htmlEntries.add(htmlEntry);
        // titleIndexBuilder.addEntryWithString(indexedEntry, title,
        // EntryTypeName.WIKTIONARY_TITLE_MULTI_DETAIL);
    }

    @Override
    void removeUselessArgs(Map<String, String> namedArgs) {
    }
    
    static final Pattern ALL_ASCII = Pattern.compile("[\\p{ASCII}]*");

    class AppendCallback extends AppendAndIndexWikiCallback<WholeSectionToHtmlParser> {
        public AppendCallback(WholeSectionToHtmlParser parser) {
            super(parser);
        }

        @Override
        public void onPlainText(String plainText) {
            final String htmlEscaped = StringEscapeUtils.escapeHtml3(plainText);
            if (ALL_ASCII.matcher(htmlEscaped).matches()) {
                super.onPlainText(htmlEscaped);
            } else { 
                super.onPlainText(StringUtil.escapeToPureHtmlUnicode(plainText));
            }
        }

        @Override
        public void onWikiLink(WikiTokenizer wikiTokenizer) {
            if (wikiTokenizer.wikiLinkText().endsWith(":" + title)) {
                // Skips wikilinks like: [[en::dick]]
                return;
            }
            if (langConfig.skipWikiLink(wikiTokenizer)) {
                return;
            }
            String linkDest;
            if (wikiTokenizer.wikiLinkDest() != null) {
                linkDest = langConfig.adjustWikiLink(wikiTokenizer.wikiLinkDest());
            } else {
                linkDest = wikiTokenizer.wikiLinkText();
            }
            if (linkDest != null) {
                builder.append(String.format("<a href=\"%s\">", linkDest));
                super.onWikiLink(wikiTokenizer);
                builder.append(String.format("</a>"));
            } else {
                super.onWikiLink(wikiTokenizer);
            }
        }

        @Override
        public void onFunction(WikiTokenizer wikiTokenizer, String name,
                List<String> args, Map<String, String> namedArgs) {
            if (skipLangIso.equalsIgnoreCase(namedArgs.get("lang"))) {
                namedArgs.remove("lang");
            }
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
            if (langConfig.skipSection(headingText)) {
                while ((wikiTokenizer = wikiTokenizer.nextToken()) != null) {
                    if (wikiTokenizer.isHeading() && wikiTokenizer.headingDepth() <= depth) {
                        wikiTokenizer.returnToLineStart();
                        return;
                    }
                }
                return;
            }
            builder.append(String.format("\n<h%d>", depth));
            dispatch(headingText, null);
            builder.append(String.format("</h%d>\n", depth));
        }

        final List<Character> listPrefixStack = new ArrayList<Character>();

        @Override
        public void onListItem(WikiTokenizer wikiTokenizer) {
            if (builder.length() != 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append("\n");
            }
            final String prefix = wikiTokenizer.listItemPrefix();
            while (listPrefixStack.size() < prefix.length()) {
                builder.append(String.format("<%s>",
                        WikiTokenizer.getListTag(prefix.charAt(listPrefixStack.size()))));
                listPrefixStack.add(prefix.charAt(listPrefixStack.size()));
            }
            builder.append("<li>");
            dispatch(wikiTokenizer.listItemWikiText(), null);
            builder.append("</li>\n");

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
                builder.append(String.format("</%s>\n", WikiTokenizer.getListTag(prefixChar)));
            }
        }

        boolean boldOn = false;
        boolean italicOn = false;

        @Override
        public void onMarkup(WikiTokenizer wikiTokenizer) {
            if ("'''".equals(wikiTokenizer.token())) {
                if (!boldOn) {
                    builder.append("<b>");
                } else {
                    builder.append("</b>");
                }
                boldOn = !boldOn;
            } else if ("''".equals(wikiTokenizer.token())) {
                if (!italicOn) {
                    builder.append("<em>");
                } else {
                    builder.append("</em>");
                }
                italicOn = !italicOn;
            } else {
                assert false;
            }
        }

    }

}


package com.hughes.android.dictionary.parser.wiktionary;

import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.HtmlEntry;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexBuilder.TokenData;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.util.StringUtil;
import com.sun.xml.internal.rngom.util.Uri;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WholeSectionToHtmlParser extends AbstractWiktionaryParser {

    public static final String NAME = "WholeSectionToHtmlParser";

    interface LangConfig {
        boolean skipSection(final String name);
        EntryTypeName sectionNameToEntryType(String sectionName);
        boolean skipWikiLink(final WikiTokenizer wikiTokenizer);
        String adjustWikiLink(String wikiLinkDest, final String wikiLinkText);
        void addFunctionCallbacks(
                Map<String, FunctionCallback<WholeSectionToHtmlParser>> functionCallbacks);
    }
    static final Map<String,LangConfig> isoToLangConfig = new LinkedHashMap<String,LangConfig>();
    static {
        final Pattern enSkipSections = Pattern.compile(".*(Translations|Anagrams|References).*");
        isoToLangConfig.put("EN", new LangConfig() {
            @Override
            public boolean skipSection(String headingText) {
                return enSkipSections.matcher(headingText).matches();
            }
            
            @Override
            public EntryTypeName sectionNameToEntryType(String sectionName) {
                if (sectionName.equalsIgnoreCase("Synonyms")) {
                    return EntryTypeName.SYNONYM_MULTI;
                }
                if (sectionName.equalsIgnoreCase("Antonyms")) {
                    return EntryTypeName.ANTONYM_MULTI;
                }
                if (EnParser.partOfSpeechHeader.matcher(sectionName).matches()) {
                    // We need to put it in the other index, too (probably)
                    return null;
                }
                if (sectionName.equalsIgnoreCase("Derived Terms")) {
                    return null;
                }
                return null;
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
            public String adjustWikiLink(String wikiLinkDest, String wikiLinkText) {
                if (wikiLinkDest.startsWith("w:") || wikiLinkDest.startsWith("Image:")) {
                    return null;
                }
                final int hashPos = wikiLinkDest.indexOf("#");
                if (hashPos != -1) {
                    wikiLinkDest = wikiLinkDest.substring(0, hashPos);
                    if (wikiLinkDest.isEmpty()) {
                        wikiLinkDest = wikiLinkText;
                    }
                }
                return wikiLinkDest;
            }

            @Override
            public void addFunctionCallbacks(
                    Map<String, FunctionCallback<WholeSectionToHtmlParser>> functionCallbacks) {
                EnFunctionCallbacks.addGenericCallbacks(functionCallbacks);
            }
        });
        
        final Pattern deSkipSections = Pattern.compile(".*(Übersetzungen|Referenzen|Quellen).*");
        isoToLangConfig.put("DE", new LangConfig() {
            @Override
            public boolean skipSection(String headingText) {
                return deSkipSections.matcher(headingText).matches();
            }
            
            @Override
            public EntryTypeName sectionNameToEntryType(String sectionName) {
                if (sectionName.equalsIgnoreCase("Synonyme")) {
                    return EntryTypeName.SYNONYM_MULTI;
                }
                if (sectionName.equalsIgnoreCase("Gegenwörter")) {
                    return EntryTypeName.ANTONYM_MULTI;
                }
                return null;
            }
            
            @Override
            public boolean skipWikiLink(WikiTokenizer wikiTokenizer) {
                final String wikiText = wikiTokenizer.wikiLinkText();
                if (wikiText.startsWith("???Category:")) {
                    return true;
                }
                return false;
            }
            @Override
            public String adjustWikiLink(String wikiLinkDest, String wikiLinkText) {
                if (wikiLinkDest.startsWith("w:") || wikiLinkDest.startsWith("Image:")) {
                    return null;
                }
                final int hashPos = wikiLinkDest.indexOf("#");
                if (hashPos != -1) {
                    wikiLinkDest = wikiLinkDest.substring(0, hashPos);
                    if (wikiLinkDest.isEmpty()) {
                        wikiLinkDest = wikiLinkText;
                    }
                }
                return wikiLinkDest;
            }

            @Override
            public void addFunctionCallbacks(
                    Map<String, FunctionCallback<WholeSectionToHtmlParser>> functionCallbacks) {
                DeFunctionCallbacks.addGenericCallbacks(functionCallbacks);
            }
        });
        
        final Pattern itSkipSections = Pattern.compile(".*(Traduzione|Note / Riferimenti).*");
        isoToLangConfig.put("IT", new LangConfig() {
            @Override
            public boolean skipSection(String headingText) {
                return itSkipSections.matcher(headingText).matches();
            }
            
            @Override
            public EntryTypeName sectionNameToEntryType(String sectionName) {
                if (sectionName.equalsIgnoreCase("Sinonimi")) {
                    return EntryTypeName.SYNONYM_MULTI;
                }
                if (sectionName.equalsIgnoreCase("Antonimi/Contrari")) {
                    return EntryTypeName.ANTONYM_MULTI;
                }
                return null;
            }
            
            @Override
            public boolean skipWikiLink(WikiTokenizer wikiTokenizer) {
                final String wikiText = wikiTokenizer.wikiLinkText();
                if (wikiText.startsWith("???Category:")) {
                    return true;
                }
                return false;
            }
            @Override
            public String adjustWikiLink(String wikiLinkDest, String wikiLinkText) {
                if (wikiLinkDest.startsWith("w:") || wikiLinkDest.startsWith("Image:")) {
                    return null;
                }
                final int hashPos = wikiLinkDest.indexOf("#");
                if (hashPos != -1) {
                    wikiLinkDest = wikiLinkDest.substring(0, hashPos);
                    if (wikiLinkDest.isEmpty()) {
                        wikiLinkDest = wikiLinkText;
                    }
                }
                return wikiLinkDest;
            }

            @Override
            public void addFunctionCallbacks(
                    Map<String, FunctionCallback<WholeSectionToHtmlParser>> functionCallbacks) {
                ItFunctionCallbacks.addGenericCallbacks(functionCallbacks);
            }
        });


        final Pattern frSkipSections = Pattern.compile(".*(Traductions).*");
        isoToLangConfig.put("FR", new LangConfig() {
            @Override
            public boolean skipSection(String headingText) {
                return frSkipSections.matcher(headingText).matches();
            }
            
            @Override
            public EntryTypeName sectionNameToEntryType(String sectionName) {
                if (sectionName.equalsIgnoreCase("Synonymes")) {
                    return EntryTypeName.SYNONYM_MULTI;
                }
                return null;
            }
            
            @Override
            public boolean skipWikiLink(WikiTokenizer wikiTokenizer) {
                return false;
            }
            @Override
            public String adjustWikiLink(String wikiLinkDest, String wikiLinkText) {
                if (wikiLinkDest.startsWith("w:") || wikiLinkDest.startsWith("Image:")) {
                    return null;
                }
                final int hashPos = wikiLinkDest.indexOf("#");
                if (hashPos != -1) {
                    wikiLinkDest = wikiLinkDest.substring(0, hashPos);
                    if (wikiLinkDest.isEmpty()) {
                        wikiLinkDest = wikiLinkText;
                    }
                }
                return wikiLinkDest;
            }

            @Override
            public void addFunctionCallbacks(
                    Map<String, FunctionCallback<WholeSectionToHtmlParser>> functionCallbacks) {
                FrFunctionCallbacks.addGenericCallbacks(functionCallbacks);
            }
        });
    }

    final IndexBuilder titleIndexBuilder;
    final IndexBuilder defIndexBuilder;
    final String skipLangIso;
    final LangConfig langConfig;
    final String webUrlTemplate;
    

    public WholeSectionToHtmlParser(final IndexBuilder titleIndexBuilder, final IndexBuilder defIndexBuilder, final String wiktionaryIso, final String skipLangIso,
            final String webUrlTemplate) {
        this.titleIndexBuilder = titleIndexBuilder;
        this.defIndexBuilder = defIndexBuilder;
        assert isoToLangConfig.containsKey(wiktionaryIso): wiktionaryIso;
        this.langConfig = isoToLangConfig.get(wiktionaryIso);
        this.skipLangIso = skipLangIso;
        this.webUrlTemplate = webUrlTemplate;
    }
    
    IndexedEntry indexedEntry = null;

    @Override
    public void parseSection(String heading, String text) {
        assert entrySource != null;
        final HtmlEntry htmlEntry = new HtmlEntry(entrySource, title);
        indexedEntry = new IndexedEntry(htmlEntry);

        final AppendAndIndexWikiCallback<WholeSectionToHtmlParser> callback = new AppendCallback(
                this);
        langConfig.addFunctionCallbacks(callback.functionCallbacks);

        callback.builder = new StringBuilder();
        callback.indexedEntry = indexedEntry;
        callback.dispatch(text, null);

        if (webUrlTemplate != null) {
            final String webUrl = String.format(webUrlTemplate, title);
            callback.builder.append(String.format("<p> <a href=\"%s\">%s</a>", Uri.escapeDisallowedChars(webUrl), escapeHtmlLiteral(webUrl)));
        }
        htmlEntry.html = callback.builder.toString();
        indexedEntry.isValid = true;

        final TokenData tokenData = titleIndexBuilder.getOrCreateTokenData(title);
        tokenData.hasMainEntry = true;

        htmlEntry.addToDictionary(titleIndexBuilder.index.dict);
        tokenData.htmlEntries.add(htmlEntry);
        // titleIndexBuilder.addEntryWithString(indexedEntry, title,
        // EntryTypeName.WIKTIONARY_TITLE_MULTI_DETAIL);
        
        indexedEntry = null;
    }

    @Override
    void removeUselessArgs(Map<String, String> namedArgs) {
    }
    
    @Override
    public void addLinkToCurrentEntry(String token, final String lang, EntryTypeName entryTypeName) {
        if (lang == null || lang.equals(skipLangIso)) {
            titleIndexBuilder.addEntryWithString(indexedEntry, token, entryTypeName);
        }
    }
    
    public static String escapeHtmlLiteral(final String plainText) {
        final String htmlEscaped = StringEscapeUtils.escapeHtml3(plainText);
        if (StringUtil.isAscii(htmlEscaped)) {
            return htmlEscaped;
        } else { 
            return StringUtil.escapeUnicodeToPureHtml(plainText);
        }

    }



    class AppendCallback extends AppendAndIndexWikiCallback<WholeSectionToHtmlParser> {
        public AppendCallback(WholeSectionToHtmlParser parser) {
            super(parser);
        }

        @Override
        public void onPlainText(String plainText) {
            super.onPlainText(escapeHtmlLiteral(plainText));
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
                linkDest = langConfig.adjustWikiLink(wikiTokenizer.wikiLinkDest(), wikiTokenizer.wikiLinkText());
            } else {
                linkDest = wikiTokenizer.wikiLinkText();
            }
            if (sectionEntryTypeName != null) {
                // TODO: inside a definition, this could be the wrong language.
                titleIndexBuilder.addEntryWithString(indexedEntry, wikiTokenizer.wikiLinkText(), sectionEntryTypeName);
            }
            if (!StringUtil.isNullOrEmpty(linkDest)) {
                builder.append(String.format("<a href=\"%s\">", HtmlEntry.formatQuickdicUrl("", linkDest)));
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
        
        EntryTypeName sectionEntryTypeName;
        IndexBuilder currentIndexBuilder;

        @Override
        public void onHeading(WikiTokenizer wikiTokenizer) {
            final String headingText = wikiTokenizer.headingWikiText();
            sectionEntryTypeName = langConfig.sectionNameToEntryType(headingText);
            final int depth = wikiTokenizer.headingDepth();
            if (langConfig.skipSection(headingText)) {
                //System.out.println("Skipping section:" + headingText);
                while ((wikiTokenizer = wikiTokenizer.nextToken()) != null) {
                    if (wikiTokenizer.isHeading() && wikiTokenizer.headingDepth() <= depth) {
                        // System.out.println("Resume on: " + wikiTokenizer.token());
                        wikiTokenizer.returnToLineStart();
                        return;
                    } else {
                        // System.out.println("Skipped: " + wikiTokenizer.token());
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

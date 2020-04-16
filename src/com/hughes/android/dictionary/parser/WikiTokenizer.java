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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WikiTokenizer {

    public interface Callback {
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
    private static final Pattern wikiTokenEvent = Pattern.compile(
            "\\{\\{|\\}\\}|" +
            "\\[\\[|\\]\\]|" +
            "\\||" +  // Need the | because we might have to find unescaped pipes
            "=|" +  // Need the = because we might have to find unescaped =
            "<!--|" +
            "''|" +
            "<pre>|" +
            "<math>|" +
            "<ref>|" +
            "\n", Pattern.MULTILINE);
    private static final String listChars = "*#:;";


    final String wikiText;
    final Matcher matcher;

    boolean justReturnedNewline = true;
    int lastLineStart = 0;
    int end = 0;
    int start = -1;

    final List<String> errors = new ArrayList<>();
    final List<TokenDelim> tokenStack = new ArrayList<>();


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
    private final List<String> positionArgs = new ArrayList<>();
    private final Map<String,String> namedArgs = new LinkedHashMap<>();


    public WikiTokenizer(final String wikiText) {
        this(wikiText, true);
    }

    public WikiTokenizer(String wikiText, final boolean isNewline) {
        wikiText = wikiText.replace('\u2028', '\n');
        wikiText = wikiText.replace('\u2029', '\n');
        wikiText = wikiText.replace('\u0085', '\n');
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

    private static final Matcher POSSIBLE_WIKI_TEXT = Pattern.compile(
                "\\{\\{|" +
                "\\[\\[|" +
                "<!--|" +
                "''|" +
                "<pre>|" +
                "<math>|" +
                "<ref>|" +
                "\n"
            ).matcher("");

    public static void dispatch(final String wikiText, final boolean isNewline, final Callback callback) {
        // Statistical background, from EN-DE dictionary generation:
        // out of 12083000 calls, 9697686 can be skipped via the test
        // for ', \n and ((c - 0x3b) & 0xff9f) < 2 (which covers among others
        // <, { and [).
        // This increased to 10006466 checking for <, { and [ specifically,
        // and is minimally faster overall.
        // A even more precise one using regex and checking for {{, [[, <!--, '',
        // <pre>, <math>, <ref> and \n increased that to 10032846.
        // Regex thus seems far too costly for a measly increase from 80%/82% to 83% rejection rate
        // However completely removing it changes output (likely a bug), so leave it in for now
        // but at least run it only on the 18% not caught by the faster logic.
        // Original runtime: 1m29.708s
        // Optimized: 1m19.170s
        // Regex removed: 1m20.314s (not statistically significant)
        boolean matched = false;
        for (int i = 0; i < wikiText.length(); i++) {
            int c = wikiText.charAt(i);
            if (c == '\'' || c == '\n' || c == '<' || c == '[' || c == '{') {
                matched = true;
                break;
            }
        }
        if (!matched || !POSSIBLE_WIKI_TEXT.reset(wikiText).find()) {
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
            final boolean atNewline = wikiText.charAt(end) == '\n';
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
                        final int closingEquals = escapedFindEnd(end, TokenDelim.EQUALS);
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
                    end = escapedFindEnd(start, TokenDelim.NEWLINE);
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
                end = escapedFindEnd(start + 2, TokenDelim.DBRACKET_CLOSE);
                isWikiLink = errors.isEmpty();
                return this;
            }

            if (wikiText.startsWith("{{", start)) {
                end = escapedFindEnd(start + 2, TokenDelim.BRACE_CLOSE);
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


            while (end < wikiText.length()) {
                int c = wikiText.charAt(end);
                if (c == '\n' || c == '\'' || ((c - 0x1b) & 0xff9f) < 3) {
                    matcher.region(end, wikiText.length());
                    if (matcher.lookingAt()) break;
                }
                end++;
            }
            if (end != wikiText.length()) {
                isPlainText = true;
                if (end == start) {
                    // stumbled over a new type of newline?
                    // Or matcher is out of sync with checks above
                    errors.add("Empty group: " + this.matcher.group() + " char: " + (int)wikiText.charAt(end));
                    assert false;
                    // Note: all newlines should be normalize to \n before calling this function
                    throw new RuntimeException("matcher not in sync with code, or new type of newline, errors :" + errors);
                }
                return this;
            }

            isPlainText = true;
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

    enum TokenDelim { NEWLINE, BRACE_OPEN, BRACE_CLOSE, DBRACKET_OPEN, DBRACKET_CLOSE, BRACKET_OPEN, BRACKET_CLOSE, PIPE, EQUALS, COMMENT }

    private int tokenDelimLen(TokenDelim d) {
        switch (d) {
            case NEWLINE:
            case BRACKET_OPEN:
            case BRACKET_CLOSE:
            case PIPE:
            case EQUALS:
                return 1;
            case BRACE_OPEN:
            case BRACE_CLOSE:
            case DBRACKET_OPEN:
            case DBRACKET_CLOSE:
                return 2;
            case COMMENT:
                return 4;
            default:
                throw new RuntimeException();
        }
    }

    static final String[] patterns = { "\n", "{{", "}}", "[[", "]]", "[", "]", "|", "=", "<!--" };
    private int escapedFindEnd(final int start, final TokenDelim toFind) {
        assert tokenStack.isEmpty();

        final boolean insideFunction = toFind == TokenDelim.BRACE_CLOSE;

        int end = start;
        int firstNewline = -1;
        int singleBrackets = 0;
        while (end < wikiText.length()) {
            // Manual replacement for matcher.find(end),
            // because Java regexp is a ridiculously slow implementation.
            // Initialize to always match the end.
            TokenDelim match = TokenDelim.NEWLINE;
            int matchStart = end;
            for (; matchStart < wikiText.length(); matchStart++) {
                int i = matchStart;
                int c = wikiText.charAt(i);
                if (c == '\n') break;
                if (c == '{' && wikiText.startsWith("{{", i)) { match = TokenDelim.BRACE_OPEN; break; }
                if (c == '}' && wikiText.startsWith("}}", i)) { match = TokenDelim.BRACE_CLOSE; break; }
                if (c == '[') { match = wikiText.startsWith("[[", i) ? TokenDelim.DBRACKET_OPEN : TokenDelim.BRACKET_OPEN ; break; }
                if (c == ']') { match = wikiText.startsWith("]]", i) ? TokenDelim.DBRACKET_CLOSE : TokenDelim.BRACKET_CLOSE ; break; }
                if (c == '|') { match = TokenDelim.PIPE; break; }
                if (c == '=') { match = TokenDelim.EQUALS; break; }
                if (c == '<' && wikiText.startsWith("<!--", i)) { match = TokenDelim.COMMENT; break; }
            }

            int matchEnd = matchStart + (match == TokenDelim.NEWLINE ? 0 : tokenDelimLen(match));
            if (match != TokenDelim.NEWLINE && tokenStack.isEmpty() && match == toFind) {
                // The normal return....
                if (insideFunction) {
                    addFunctionArg(insideFunction, matchStart);
                }
                return matchEnd;
            }
            switch (match) {
                case NEWLINE:
                assert matchStart == wikiText.length() || wikiText.charAt(matchStart) == '\n' : wikiText + ", " + matchStart;
                if (firstNewline == -1) {
                    firstNewline = matchEnd;
                }
                if (tokenStack.isEmpty() && toFind == TokenDelim.NEWLINE) {
                    return matchStart;
                }
                ++end;
                break;
                case BRACKET_OPEN:
                singleBrackets++;
                break;
                case BRACKET_CLOSE:
                if (singleBrackets > 0) singleBrackets--;
                break;
                case DBRACKET_OPEN:
                case BRACE_OPEN:
                tokenStack.add(match);
                break;
                case DBRACKET_CLOSE:
                case BRACE_CLOSE:
                if (!tokenStack.isEmpty()) {
                    final TokenDelim removed = tokenStack.remove(tokenStack.size() - 1);
                    if (removed == TokenDelim.BRACE_OPEN && match != TokenDelim.BRACE_CLOSE) {
                        if (singleBrackets >= 2) { // assume this is really two closing single ]
                            singleBrackets -= 2;
                            tokenStack.add(removed);
                        } else {
                            errors.add("Unmatched {{ error: " + wikiText.substring(start, matchEnd));
                            return safeIndexOf(wikiText, start, "\n", "\n");
                        }
                    } else if (removed == TokenDelim.DBRACKET_OPEN && match != TokenDelim.DBRACKET_CLOSE) {
                        errors.add("Unmatched [[ error: " + wikiText.substring(start, matchEnd));
                        return safeIndexOf(wikiText, start, "\n", "\n");
                    }
                } else {
                    errors.add("Pop too many " + wikiText.substring(matchStart, matchEnd) + " error: " + wikiText.substring(start, matchEnd).replace("\n", "\\\\n"));
                    // If we were looking for a newline
                    return safeIndexOf(wikiText, start, "\n", "\n");
                }
                break;
                case PIPE:
                if (tokenStack.isEmpty()) {
                    addFunctionArg(insideFunction, matchStart);
                }
                break;
                case EQUALS:
                if (tokenStack.isEmpty()) {
                    lastUnescapedEqualsPos = matchStart;
                }
                // Do nothing.  These can match spuriously, and if it's not the thing
                // we're looking for, keep on going.
                break;
                case COMMENT:
                end = wikiText.indexOf("-->", matchStart);
                if (end == -1) {
                    errors.add("Unmatched <!-- error: " + wikiText.substring(start));
                    return safeIndexOf(wikiText, start, "\n", "\n");
                }
                break;
                default:
                    throw new RuntimeException();
            }

            // Inside the while loop.  Just go forward.
            end = Math.max(end, matchEnd);
        }
        if (toFind == TokenDelim.NEWLINE && tokenStack.isEmpty()) {
            // We were looking for the end, we got it.
            return end;
        }
        errors.add("Couldn't find: " + toFind + ", "+ wikiText.substring(start));
        if (firstNewline != -1) {
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

    static String trimNewlines(String s) {
        while (s.startsWith("\n")) {
            s = s.substring(1);
        }
        while (s.endsWith("\n")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.replace('\n', ' ');
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

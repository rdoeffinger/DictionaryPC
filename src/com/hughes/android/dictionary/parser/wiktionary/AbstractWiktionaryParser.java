// Copyright 2012 Google Inc. All Rights Reserved.
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

package com.hughes.android.dictionary.parser.wiktionary;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import com.hughes.android.dictionary.engine.EntrySource;
import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.ReadAheadBuffer;
import com.hughes.android.dictionary.parser.Parser;
import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.util.EnumUtil;

public abstract class AbstractWiktionaryParser implements Parser {

    static final Logger LOG = Logger.getLogger("WiktionaryParser");

    private static final Pattern SUPERSCRIPT = Pattern.compile("<sup>[0-9]*</sup>");

    final SortedMap<String, AtomicInteger> counters = new TreeMap<>();
    final Set<String> pairsAdded = new LinkedHashSet<>();

    public EntrySource entrySource;
    public String title;


    abstract void parseSection(final String heading, final String text);

    abstract void removeUselessArgs(final Map<String, String> namedArgs);

    private static String replaceSuperscript(String in) {
        Matcher matcher;
        while ((matcher = SUPERSCRIPT.matcher(in)).find()) {
            String replace = "";
            String orig = matcher.group();
            for (int i = 5; i < orig.length() - 6; i++)
            {
                char c = 0;
                switch (orig.charAt(i)) {
                case '0': c = '\u2070'; break;
                case '1': c = '\u00b9'; break;
                case '2': c = '\u00b2'; break;
                case '3': c = '\u00b3'; break;
                case '4': c = '\u2074'; break;
                case '5': c = '\u2075'; break;
                case '6': c = '\u2076'; break;
                case '7': c = '\u2077'; break;
                case '8': c = '\u2078'; break;
                case '9': c = '\u2079'; break;
                }
                if (c == 0) throw new RuntimeException();
                replace += c;
            }
            in = matcher.replaceFirst(replace);
        }
        return in;
    }

    @Override
    public void parse(final File file, final EntrySource entrySource, final int pageLimit) throws IOException {
        this.entrySource = entrySource;
        int pageCount = 0;
        File input = new File(file.getPath() + ".bz2");
        if (!input.exists()) input = new File(file.getPath() + ".gz");
        if (!input.exists()) input = new File(file.getPath() + ".xz");
        DataInputStream dis;
        if (!input.exists()) {
            // Fallback to uncompressed file
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        } else {
            InputStream compressedIn = new BufferedInputStream(new FileInputStream(input));
            try {
                InputStream in = new CompressorStreamFactory().createCompressorInputStream(compressedIn);
                in = new ReadAheadBuffer(in, 20 * 1024 * 1024);
                dis = new DataInputStream(in);
            } catch (CompressorException e) {
                throw new IOException(e);
            }
        }
        try {
            while (true) {
                if (pageLimit >= 0 && pageCount >= pageLimit) {
                    return;
                }

                try {
                    title = dis.readUTF();
                } catch (EOFException e) {
                    LOG.log(Level.INFO, "EOF reading split.");
                    dis.close();
                    return;
                }
                final String heading = dis.readUTF();
                final int bytesLength = dis.readInt();
                final byte[] bytes = new byte[bytesLength];
                dis.readFully(bytes);
                final String text = new String(bytes, StandardCharsets.UTF_8);

                parseSection(heading, replaceSuperscript(text));

                ++pageCount;
                if (pageCount % 1000 == 0) {
                    LOG.info("pageCount=" + pageCount);
                }
            }
        } finally {
            dis.close();
            LOG.info("***COUNTERS***");
            for (final Map.Entry<String, AtomicInteger> entry : counters.entrySet()) {
                LOG.info(entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    static final Pattern whitespace = Pattern.compile("\\s+");
    static String trim(final String s) {
        return whitespace.matcher(s).replaceAll(" ").trim();
    }

    public void incrementCount(final String string) {
        AtomicInteger counter = counters.get(string);
        if (counter == null) {
            counter = new AtomicInteger();
            counters.put(string, counter);
        }
        counter.incrementAndGet();
    }

    public void addLinkToCurrentEntry(final String token, final String lang, final EntryTypeName entryTypeName) {
        assert false : token + ", title=" + title;
    }


    // -------------------------------------------------------------------------

    static class AppendAndIndexWikiCallback<T extends AbstractWiktionaryParser> implements WikiTokenizer.Callback {

        final T parser;
        StringBuilder builder;
        IndexedEntry indexedEntry;
        IndexBuilder indexBuilder;
        final Map<String,FunctionCallback<T>> functionCallbacks = new LinkedHashMap<>();

        boolean entryTypeNameSticks = false;
        EntryTypeName entryTypeName = null;

        final Map<String,AtomicInteger> langCodeToTCount = new LinkedHashMap<>();

        final NameAndArgs<T> nameAndArgs = new NameAndArgs<>();

        public AppendAndIndexWikiCallback(final T parser) {
            this.parser = parser;
        }

        public void reset(final StringBuilder builder, final IndexedEntry indexedEntry) {
            this.builder = builder;
            this.indexedEntry = indexedEntry;
            this.indexBuilder = null;
            entryTypeName = null;
            entryTypeNameSticks = false;
        }

        public void dispatch(final String wikiText, final IndexBuilder indexBuilder, final EntryTypeName entryTypeName) {
            final IndexBuilder oldIndexBuilder = this.indexBuilder;
            final EntryTypeName oldEntryTypeName = this.entryTypeName;
            this.indexBuilder = indexBuilder;
            if (!entryTypeNameSticks) {
                this.entryTypeName = EnumUtil.min(entryTypeName, this.entryTypeName);
            }
            if (entryTypeName == null) this.entryTypeName = null;
            WikiTokenizer.dispatch(wikiText, false, this);
            this.indexBuilder = oldIndexBuilder;
            this.entryTypeName = oldEntryTypeName;
        }

        public String dispatch(final String wikiText, final EntryTypeName entryTypeName) {
            final int start = builder.length();
            dispatch(wikiText, this.indexBuilder, entryTypeName);
            return builder.substring(start);
        }

        @Override
        public void onPlainText(final String plainText) {
            // The only non-recursive callback.  Just appends to the builder, and indexes.
            builder.append(plainText);
            if (indexBuilder != null && entryTypeName != null && indexedEntry != null) {
                indexBuilder.addEntryWithString(indexedEntry, plainText, entryTypeName);
            }
        }

        @Override
        public void onWikiLink(WikiTokenizer wikiTokenizer) {
            final String text = wikiTokenizer.wikiLinkText();
            @SuppressWarnings("unused")
            final String link = wikiTokenizer.wikiLinkDest();
            dispatch(text, entryTypeName);
        }

        @Override
        public void onFunction(
            final WikiTokenizer wikiTokenizer,
            final String name,
            final List<String> args,
            final Map<String, String> namedArgs) {

            FunctionCallback<T> functionCallback = functionCallbacks.get(name);
            if (functionCallback == null || !functionCallback.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, this)) {
                // Default function handling:
                parser.removeUselessArgs(namedArgs);
                final boolean single = args.isEmpty() && namedArgs.isEmpty();
                builder.append(single ? "{" : "{{");

                final IndexBuilder oldIndexBuilder = indexBuilder;
                indexBuilder = null;
                nameAndArgs.onWikiFunction(wikiTokenizer, name, args, namedArgs, parser, this);
                indexBuilder = oldIndexBuilder;

                builder.append(single ? "}" : "}}");
            }
        }

        @Override
        public void onHtml(WikiTokenizer wikiTokenizer) {
            if (wikiTokenizer.token().startsWith("<ref>")) {
                // Do nothing.
                return;
            }
            // Unindexed for now.
            builder.append(wikiTokenizer.token());
        }

        @Override
        public void onMarkup(WikiTokenizer wikiTokenizer) {
            // Do nothing.
        }

        @Override
        public final void onComment(WikiTokenizer wikiTokenizer) {
            // Do nothing.
        }

        @Override
        public void onNewline(WikiTokenizer wikiTokenizer) {
            assert false;
        }

        @Override
        public void onHeading(WikiTokenizer wikiTokenizer) {
            assert false;
        }

        @Override
        public void onListItem(WikiTokenizer wikiTokenizer) {
            assert false;
        }

    }

    // --------------------------------------------------------------------

    static final class NameAndArgs<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
        @Override
        public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
                                      final Map<String, String> namedArgs, final T parser,
                                      final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {

            if (name != null) {
                appendAndIndexWikiCallback.dispatch(name, null);
            }
            for (String arg : args) {
                if (!arg.isEmpty()) {
                    appendAndIndexWikiCallback.builder.append("|");
                    appendAndIndexWikiCallback.dispatch(arg, null, null);
                }
            }
            appendNamedArgs(namedArgs, appendAndIndexWikiCallback);
            return true;
        }
    }
    static NameAndArgs<AbstractWiktionaryParser> NAME_AND_ARGS = new NameAndArgs<>();

    static void appendNamedArgs(final Map<String, String> namedArgs,
                                final AppendAndIndexWikiCallback<?> appendAndIndexWikiCallback) {
        for (final Map.Entry<String, String> entry : namedArgs.entrySet()) {
            appendAndIndexWikiCallback.builder.append("|");
            appendAndIndexWikiCallback.dispatch(entry.getKey(), null, null);
            appendAndIndexWikiCallback.builder.append("=");
            EntryTypeName entryTypeName = null;
            IndexBuilder indexBuilder = null;
            // This doesn't work: we'd need to add to word-forms.
//      System.out.println(entry.getKey());
//      if (entry.getKey().equals("tr")) {
//        entryTypeName = EntryTypeName.WIKTIONARY_TRANSLITERATION;
//        indexBuilder = appendAndIndexWikiCallback.parser.foreignIndexBuilder;
//      }
            appendAndIndexWikiCallback.dispatch(entry.getValue(), indexBuilder, entryTypeName);
        }
    }

}

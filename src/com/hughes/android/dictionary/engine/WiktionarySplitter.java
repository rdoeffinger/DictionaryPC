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

package com.hughes.android.dictionary.engine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.hughes.android.dictionary.parser.wiktionary.WiktionaryLangs;

public class WiktionarySplitter extends org.xml.sax.helpers.DefaultHandler implements Runnable {

    // The matches the whole line, otherwise regexes don't work well on French:
    // {{=uk=}}
    // Spanish has no initial headings, tried to also detect {{ES as such
    // with "^(\\{\\{ES|(=+)[^=]).*$" but that broke English.
    static final Pattern headingStartPattern = Pattern.compile("^(=+)[^=].*$", Pattern.MULTILINE);
    static final Pattern startSpanish = Pattern.compile("\\{\\{ES(\\|[^{}=]*)?}}");

    final Map.Entry<String, List<Selector>> pathToSelectorsEntry;
    List<Selector> currentSelectors = null;

    StringBuilder titleBuilder;
    StringBuilder textBuilder;
    StringBuilder currentBuilder = null;

    public static void main(final String[] args) throws Exception {
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        boolean parallel = args.length > 0 && args[0].equals("parallel");
        if (!parallel) System.out.println("Consider using 'parallel' argument to speed up processing by running in parallel - needs more memory");
        final ExecutorService e = Executors.newCachedThreadPool();
        final Map<String,List<Selector>> pathToSelectors = createSelectorsMap();
        for (final Map.Entry<String, List<Selector>> pathToSelectorsEntry : pathToSelectors.entrySet()) {
            final WiktionarySplitter wiktionarySplitter = new WiktionarySplitter(pathToSelectorsEntry);
            if (parallel) {
                e.submit(wiktionarySplitter);
            } else wiktionarySplitter.go();
        }
        e.shutdown();
    }

    private WiktionarySplitter(final Map.Entry<String, List<Selector>> pathToSelectorsEntry) {
        this.pathToSelectorsEntry = pathToSelectorsEntry;
    }

    private static Map<String,List<Selector>> createSelectorsMap() {
        final Map<String,List<Selector>> pathToSelectors = new LinkedHashMap<>();
        List<Selector> selectors;
        for (final String code : WiktionaryLangs.wikiCodeToIsoCodeToWikiName.keySet()) {
            //if (!code.equals("fr")) {continue;}
            selectors = new ArrayList<>();
            pathToSelectors.put(String.format("data/inputs/%swiktionary-pages-articles.xml", code), selectors);
            for (final Map.Entry<String, String> entry : WiktionaryLangs.wikiCodeToIsoCodeToWikiName.get(code).entrySet()) {
                final String dir = String.format("data/inputs/wikiSplit/%s", code);
                new File(dir).mkdirs();
                selectors.add(new Selector(String.format("%s/%s.data", dir, entry.getKey()), entry.getValue()));
            }
        }
        return pathToSelectors;
    }

    @Override
    public void run() {
        try {
            go();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void go() throws Exception {
        final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

        // Configure things.

            currentSelectors = pathToSelectorsEntry.getValue();

            for (final Selector selector : currentSelectors) {
                OutputStream tmp = new FileOutputStream(selector.outFilename + ".gz");
                tmp = new BufferedOutputStream(tmp);
                tmp = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, tmp);
                tmp = new WriteBuffer(tmp, 1024 * 1024);
                selector.out = new DataOutputStream(tmp);
            }

            // Do it.
            try {
                File input = new File(pathToSelectorsEntry.getKey() + ".bz2");
                if (!input.exists()) input = new File(pathToSelectorsEntry.getKey() + ".gz");
                if (!input.exists()) input = new File(pathToSelectorsEntry.getKey() + ".xz");
                if (!input.exists()) {
                    // Fallback to uncompressed file
                    parser.parse(new File(pathToSelectorsEntry.getKey()), this);
                } else {
                    InputStream compressedIn = new BufferedInputStream(new FileInputStream(input));
                    InputStream in = new CompressorStreamFactory().createCompressorInputStream(compressedIn);
                    in = new ReadAheadBuffer(in, 20 * 1024 * 1024);
                    parser.parse(new BufferedInputStream(in), this);
                }
            } catch (Exception e) {
                System.err.println("Exception during parse, lastPageTitle=" + lastPageTitle + ", titleBuilder=" + titleBuilder + " of file " + pathToSelectorsEntry.getKey());
                throw e;
            }

            // Shutdown.
            for (final Selector selector : currentSelectors) {
                selector.out.close();
            }
    }

    String lastPageTitle = null;
    int pageCount = 0;
    final Matcher[] endPatterns = new Matcher[100];

    private Matcher getEndPattern(int depth) {
        if (endPatterns[depth] == null)
            endPatterns[depth] = Pattern.compile(String.format("^={1,%d}[^=].*$", depth), Pattern.MULTILINE).matcher("");
        return endPatterns[depth];
    }

    private void endPage() {
        final String title = titleBuilder.toString();
        lastPageTitle = title;
        if (++pageCount % 100000 == 0) {
            System.out.println("endPage: " + title + ", count=" + pageCount);
        }
        if (title.startsWith("Unsupported titles/")) return;
        if (title.contains(":")) {
            if (title.startsWith("Wiktionary:") ||
                title.startsWith("Appendix:") ||
                title.startsWith("Help:") ||
                title.startsWith("Index:") ||
                title.startsWith("MediaWiki:") ||
                title.startsWith("Citations:") ||
                title.startsWith("Concordance:") ||
                title.startsWith("Glossary:") ||
                title.startsWith("Rhymes:") ||
                title.startsWith("Category:") ||
                title.startsWith("Wikisaurus:") ||
                title.startsWith("Transwiki:") ||
                title.startsWith("File:") ||
                title.startsWith("Thread:") ||
                title.startsWith("Template:") ||
                title.startsWith("Summary:") ||
                title.startsWith("Module:") ||
                title.startsWith("Reconstruction:") ||
                // DE
                title.startsWith("Datei:") ||
                title.startsWith("Verzeichnis:") ||
                title.startsWith("Vorlage:") ||
                title.startsWith("Thesaurus:") ||
                title.startsWith("Kategorie:") ||
                title.startsWith("Hilfe:") ||
                title.startsWith("Reim:") ||
                title.startsWith("Rekonstruktion:") ||
                title.startsWith("Modul:") ||
                // FR:
                title.startsWith("Annexe:") ||
                title.startsWith("Catégori:") ||
                title.startsWith("Conjugaison:") ||
                title.startsWith("Convention:") ||
                title.startsWith("Modèle:") ||
                title.startsWith("Thésaurus:") ||
                title.startsWith("Projet:") ||
                title.startsWith("Aide:") ||
                title.startsWith("Fichier:") ||
                title.startsWith("Wiktionnaire:") ||
                title.startsWith("Translations:Aide:") ||
                title.startsWith("Translations:Wiktionnaire:") ||
                title.startsWith("Translations:Projet:") ||
                title.startsWith("Catégorie:") ||
                title.startsWith("Portail:") ||
                title.startsWith("Racine:") ||
                title.startsWith("utiliusateur:") ||
                title.startsWith("Kategorio:") ||
                title.startsWith("Tutoriel:") ||
                // IT
                title.startsWith("Wikizionario:") ||
                title.startsWith("Appendice:") ||
                title.startsWith("Categoria:") ||
                title.startsWith("Aiuto:") ||
                title.startsWith("Portail:") ||
                title.startsWith("Modulo:") ||
                // ES
                title.startsWith("Apéndice:") ||
                title.startsWith("Archivo:") ||
                title.startsWith("Ayuda:") ||
                title.startsWith("Categoría:") ||
                title.startsWith("Plantilla:") ||
                title.startsWith("Wikcionario:") ||

                // PT
                title.startsWith("Ajuda:") ||
                title.startsWith("Apêndice:") ||
                title.startsWith("Citações:") ||
                title.startsWith("Portal:") ||
                title.startsWith("Predefinição:") ||
                title.startsWith("Vocabulário:") ||
                title.startsWith("Wikcionário:") ||
                title.startsWith("Módulo:") ||

                // sentinel
                false
               ) return;
            // leave the Flexion: pages in for now and do not warn about them
            if (!title.startsWith("Sign gloss:") && !title.startsWith("Flexion:")) {
                System.err.println("title with colon: " + title);
            }
        }

        String text = textBuilder.toString();
        // Workaround for Spanish wiktionary {{ES}} and {{ES|word}} patterns
        text = startSpanish.matcher(text).replaceAll("== {{lengua|es}} ==");
        String translingual = "";
        int start = 0;
        Matcher headingStart = headingStartPattern.matcher(text);

        while (start < text.length()) {
            // Find start.
            if (!headingStart.find(start)) {
                return;
            }
            start = headingStart.end();

            final String heading = headingStart.group();

            // For Translingual entries just store the text for later
            // use in the per-language sections
            if (heading.contains("Translingual")) {
                // Find end.
                final int depth = headingStart.group(1).length();
                final Matcher endMatcher = getEndPattern(depth).reset(text);

                if (endMatcher.find(start)) {
                    int end = endMatcher.start();
                    translingual = text.substring(start, end);
                    start = end;
                    continue;
                }
            }

            for (final Selector selector : currentSelectors) {
                if (selector.pattern.reset(heading).find()) {
                    // Find end.
                    final int depth = headingStart.group(1).length();
                    final Matcher endMatcher = getEndPattern(depth).reset(text);

                    final int end;
                    if (endMatcher.find(start)) {
                        end = endMatcher.start();
                    } else {
                        end = text.length();
                    }

                    String sectionText = text.substring(start, end);
                    // Hack to remove empty dummy section from French
                    if (sectionText.startsWith("\n=== {{S|étymologie}} ===\n: {{ébauche-étym")) {
                        int dummy_end = sectionText.indexOf("}}", 41) + 2;
                        while (dummy_end + 1 < sectionText.length() &&
                                sectionText.charAt(dummy_end) == '\n' &&
                                sectionText.charAt(dummy_end + 1) == '\n') ++dummy_end;
                        sectionText = sectionText.substring(dummy_end);
                    }
                    if (!heading.contains("Japanese")) sectionText += translingual;
                    try {
                        selector.out.writeUTF(title);
                        selector.out.writeUTF(heading);
                        final byte[] bytes = sectionText.getBytes(StandardCharsets.UTF_8);
                        selector.out.writeInt(bytes.length);
                        selector.out.write(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    start = end;
                    break;
                }
            }
        }

    }

    static class Selector {
        final String outFilename;
        final Matcher pattern;

        DataOutputStream out;

        public Selector(final String filename, final String pattern) {
            this.outFilename = filename;
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher("");
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) {
        currentBuilder = null;
        if ("page".equals(qName)) {
            titleBuilder = new StringBuilder();

            // Start with "\n" to better match certain strings.
            textBuilder = new StringBuilder("\n");
        } else if ("title".equals(qName)) {
            currentBuilder = titleBuilder;
        } else if ("text".equals(qName)) {
            currentBuilder = textBuilder;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (currentBuilder != null) {
            currentBuilder.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        currentBuilder = null;
        if ("page".equals(qName)) {
            endPage();
        }
    }

    public void parse(final File file) throws ParserConfigurationException,
        SAXException, IOException {
        final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(file, this);
    }

}

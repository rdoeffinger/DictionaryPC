package com.hughes.android.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.hughes.util.MapUtil;
import com.hughes.util.StringUtil;

public class WiktionaryXmlParser extends org.xml.sax.helpers.DefaultHandler {

  final Dictionary dict;

  StringBuilder titleBuilder;
  StringBuilder textBuilder;
  StringBuilder currentBuilder = null;

  public WiktionaryXmlParser(final Dictionary dict) {
    this.dict = dict;
  }

  @Override
  public void startElement(String uri, String localName, String qName,
      Attributes attributes) {
    currentBuilder = null;
    if ("page".equals(qName)) {
      titleBuilder = new StringBuilder();
      textBuilder = new StringBuilder();
    } else if ("title".equals(qName)) {
      currentBuilder = titleBuilder;
    } else if ("text".equals(qName)) {
      currentBuilder = textBuilder;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (currentBuilder != null) {
      currentBuilder.append(ch, start, length);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName)
      throws SAXException {
    currentBuilder = null;
    if ("page".equals(qName)) {
      endPage();
    }
  }

  private static final Pattern NEWLINE = Pattern.compile("\n", Pattern.LITERAL);

  // MULTILINE for ^
  private static final Pattern SECTION_HEADER = Pattern
      .compile("=== *\\{\\{Wortart\\|");

  private static final Pattern WORTART_DELIM = Pattern.compile("===",
      Pattern.LITERAL);
  private static final Pattern GENDER = Pattern.compile("\\{\\{([mfn])\\}\\}");

  private static final Pattern WIKI_QUOTE = Pattern.compile("''",
      Pattern.LITERAL);
  private static final Pattern WIKI_DOUBLE_BRACE = Pattern
      .compile("\\{\\{([^}]+)\\}\\}");
  private static final Pattern WIKI_DOUBLE_BRACKET = Pattern
      .compile("\\[\\[([^\\]]+)\\]\\]");
  private static final Pattern WIKI_NEW_SECTION = Pattern.compile("^\\{\\{([^}]+)\\}\\}|^=");

  enum Field {
    Wortart("Wortart", null), Aussprache("Aussprache", null), Bedeutungen(
        "Bedeutungen", Pattern.compile("\\{\\{Bedeutungen\\}\\}")), Synonome(
        "Synonyme", Pattern.compile("\\{\\{Synonyme\\}\\}")), Gegenworte(
        "Gegenworte", Pattern.compile("\\{\\{Gegenworte\\}\\}")), Oberbegriffe(
        "Oberbegriffe", Pattern.compile("\\{\\{Oberbegriffe\\}\\}")), Unterbegriffe(
        "Unterbegriffe", Pattern.compile("\\{\\{Unterbegriffe\\}\\}")), Beispiele(
        "Beispiele", Pattern.compile("\\{\\{Beispiele\\}\\}")), Redewendungen(
        "Redewendungen", Pattern.compile("\\{\\{Redewendungen\\}\\}")), CharakteristischeWortkombinationen(
        "Charakteristische Wortkombinationen", Pattern
            .compile("\\{\\{Charakteristische Wortkombinationen\\}\\}")), AbgeleiteteBegriffe(
        "Abgeleitete Begriffe", Pattern
            .compile("\\{\\{Abgeleitete Begriffe\\}\\}")), Herkunft("Herkunft",
        Pattern.compile("\\{\\{Herkunft\\}\\}"));

    final String name;
    final Pattern listPattern;

    Field(final String name, final Pattern listPattern) {
      this.name = name;
      this.listPattern = listPattern;
    }
  }

  private static final Pattern WORTART = Pattern
      .compile("\\{\\{Wortart\\|([^}]+)\\|([^}]+)\\}\\}");
  private static final Pattern AUSSPRACHE = Pattern.compile(":Hilfe:IPA|IPA:",
      Pattern.LITERAL);

  private final Map<String, AtomicInteger> errorCounts = new TreeMap<String, AtomicInteger>();

  private void endPage() {

    StringBuilder text = textBuilder;
    text = new StringBuilder(WIKI_QUOTE.matcher(text).replaceAll("\""));
    text = new StringBuilder(WIKI_DOUBLE_BRACKET.matcher(text).replaceAll("$1"));

    // Remove comments.
    StringUtil.removeAll(text, Pattern.compile("<!--", Pattern.LITERAL),
        Pattern.compile("-->", Pattern.LITERAL));

    String sectionString;
    while ((sectionString = StringUtil.remove(text, SECTION_HEADER,
        SECTION_HEADER, false)) != null) {
      final StringBuilder section = new StringBuilder(sectionString);

      String wortart = StringUtil.remove(section, WORTART_DELIM, WORTART_DELIM,
          true);
      if (wortart.contains("\n") || !wortart.contains("eutsch")) {
        MapUtil.safeGet(errorCounts, "Invalid wortart: " + wortart,
            AtomicInteger.class).incrementAndGet();
        continue;
      }

      final LinkedHashMap<Field, List<String>> fieldToValue = new LinkedHashMap<Field, List<String>>();

      wortart = wortart.replaceAll("===", "");
      wortart = WORTART.matcher(wortart).replaceAll("$1");
      wortart = GENDER.matcher(wortart).replaceAll("{$1}");
      wortart = WIKI_DOUBLE_BRACE.matcher(wortart).replaceAll("$1");
      wortart = wortart.replaceAll("Wortart\\|", "");
      wortart = wortart.trim();
      fieldToValue.put(Field.Wortart, Collections.singletonList(wortart));

      String aussprache = StringUtil
          .remove(section, AUSSPRACHE, NEWLINE, false);
      if (aussprache != null) {
        aussprache = AUSSPRACHE.matcher(aussprache).replaceFirst("");
        aussprache = WIKI_DOUBLE_BRACE.matcher(aussprache).replaceAll("$1");
        aussprache = aussprache.replaceAll("Lautschrift\\|", "");
        aussprache = aussprache.trim();
        fieldToValue.put(Field.Aussprache, Collections
            .singletonList(aussprache));
      }

      for (final Field field : Field.values()) {
        if (field.listPattern != null) {
          fieldToValue.put(field, extractList(section, field.listPattern));
        }
      }

      System.out.println(titleBuilder);
      for (final Field field : Field.values()) {
        if (fieldToValue.get(field).isEmpty()) {
          fieldToValue.remove(field);
        } else {
          System.out.println(field.name);
          for (final String line : fieldToValue.get(field)) {
            System.out.println("  " + line);
          }
        }
      }
      System.out.println("WHAT'S LEFT:");
      System.out.println(section);
      System.out.println("------------------------------------------------");

    }

    // System.out.println(titleBuilder);
    /*
     * final List<String> pronunciations = new ArrayList<String>(); final
     * CharSequence pronunciationSeq = getSection(text, PRONUNCIATION,
     * SECTION_START); if (pronunciationSeq != null) { final Matcher
     * pronunciationMatcher = PRONUNCIATION_EXAMPLE.matcher(pronunciationSeq);
     * while (pronunciationMatcher.find()) {
     * pronunciations.add(pronunciationMatcher.group(1)); }
     * System.out.println("PRONUNCIATIONS:" + pronunciations); }
     * 
     * String[] meanings = null; final CharSequence meaningsSeq =
     * getSection(text, MEANINGS, SECTION_START); if (meaningsSeq != null) {
     * meanings = LIST.split(meaningsSeq); meanings[0] = "";
     * System.out.println("MEANINGS:" + Arrays.toString(meanings)); }
     * 
     * System.out.println(text);
     */

  }

  private List<String> extractList(final StringBuilder section,
      final Pattern start) {
    final List<String> result = new ArrayList<String>();
    final String linesString = StringUtil.remove(section, start,
        WIKI_DOUBLE_BRACE, false);
    if (linesString != null) {
      String[] lines = linesString.split("\n");
      for (int i = 1; i < lines.length; ++i) {
        String bedeutung = lines[i];
        bedeutung = bedeutung.replaceFirst("^:", "");
        bedeutung = bedeutung.trim();
        if (bedeutung.length() > 0) {
          result.add(bedeutung);
        }
      }
    }
    return result;
  }

  private static CharSequence getSection(CharSequence input, Pattern start,
      Pattern end) {
    Matcher startMatcher = start.matcher(input);
    if (!startMatcher.find()) {
      return null;
    }
    Matcher endMatcher = end.matcher(input);
    if (!endMatcher.find(startMatcher.end())) {
      return input.subSequence(startMatcher.start(), input.length());
    }
    return input.subSequence(startMatcher.start(), endMatcher.start());
  }

  void parse(final File file) throws ParserConfigurationException,
      SAXException, IOException {
    final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    parser.parse(file, this);
    System.out.println(errorCounts);
  }

}

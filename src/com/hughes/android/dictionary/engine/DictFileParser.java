package com.hughes.android.dictionary.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.Language;
import com.hughes.android.dictionary.engine.PairEntry.Pair;

public class DictFileParser {
  
  static final Logger logger = Logger.getLogger(DictFileParser.class.getName());

  // Dictcc
  static final Pattern TAB = Pattern.compile("\\t");

  // Chemnitz
  static final Pattern DOUBLE_COLON = Pattern.compile(" :: ");
  static final Pattern PIPE = Pattern.compile(" \\| ");
  
  static final Pattern SPACES = Pattern.compile("\\s+");
  static final Pattern DE_NOUN = Pattern.compile("([^ ]+) *\\{(m|f|n|pl)\\}");
  static final Pattern EN_VERB = Pattern.compile("^to ([^ ]+)");
  
  static final Pattern BRACKETED = Pattern.compile("\\[([^]]+)\\]");
  static final Pattern PARENTHESIZED = Pattern.compile("\\(([^)]+)\\]");
  
  static final Pattern NON_CHAR_DASH = Pattern.compile("[^-'\\p{L}0-9]+");
  static final Pattern NON_CHAR = Pattern.compile("[^\\p{L}0-9]+");

  static final Pattern TRIM_PUNC = Pattern.compile("^[^\\p{L}0-9]+|[^\\p{L}0-9]+$");

  final Charset charset;
  final boolean flipCols;
  
  final Pattern fieldSplit;
  final Pattern subfieldSplit;
  
  final DictionaryBuilder dictBuilder;
  final IndexBuilder[] langIndexBuilders;
  final IndexBuilder bothIndexBuilder;
  
  final Set<String> alreadyDone = new HashSet<String>();
    
  public DictFileParser(final Charset charset, boolean flipCols,
      final Pattern fieldSplit, final Pattern subfieldSplit,
      final DictionaryBuilder dictBuilder, final IndexBuilder[] langIndexBuilders,
      final IndexBuilder bothIndexBuilder) {
    this.charset = charset;
    this.flipCols = flipCols;
    this.fieldSplit = fieldSplit;
    this.subfieldSplit = subfieldSplit;
    this.dictBuilder = dictBuilder;
    this.langIndexBuilders = langIndexBuilders;
    this.bothIndexBuilder = bothIndexBuilder;
  }

  public void parseFile(final File file) throws IOException {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
    String line;
    while ((line = reader.readLine()) != null) {
      parseLine(line);
    }
  }
  
  private void parseLine(final String line) {
    if (line.startsWith("#") || line.length() == 0) {
      logger.info("Skipping comment line: " + line);
      return;
    }
    final String[] fields = fieldSplit.split(line);
    if (fields.length != 2) {
      logger.warning("Malformed line: " + line);
      return;
    }
    
    fields[0] = SPACES.matcher(fields[0]).replaceAll(" ").trim();
    fields[1] = SPACES.matcher(fields[1]).replaceAll(" ").trim();
    if (flipCols) {
      final String temp = fields[0];
      fields[0] = fields[1];
      fields[1] = temp;
    }
    
    final String[][] subfields = new String[2][];
      if (subfieldSplit != null) {
      subfields[0] = subfieldSplit.split(fields[0]);
      subfields[1] = subfieldSplit.split(fields[1]);
      if (subfields[0].length != subfields[1].length) {
        logger.warning("Number of subfields doesn't match: " + line);
        return;
      }
    } else {
      subfields[0] = new String[] { fields[0] };
      subfields[1] = new String[] { fields[1] };
    }
    
    final Pair[] pairs = new Pair[subfields[0].length];
    for (int i = 0; i < pairs.length; ++i) {
      pairs[i] = new Pair(subfields[0][i], subfields[1][i]);
    }
    final PairEntry pairEntry = new PairEntry(pairs);
    final EntryData entryData = new EntryData(dictBuilder.dictionary.pairEntries.size(), pairEntry);
    dictBuilder.dictionary.pairEntries.add(pairEntry);
    dictBuilder.entryDatas.add(entryData);  // TODO: delete me.
    
    for (int l = 0; l < 2; ++l) {
      alreadyDone.clear();
      
      for (int j = 0; j < subfields[l].length; ++j) {
        String subfield = subfields[l][j];
        final IndexBuilder indexBuilder = langIndexBuilders[l];
        if (indexBuilder.index.sortLanguage == Language.de) {
          subfield = parseField_DE(indexBuilder, subfield, entryData, j);
        } else if (indexBuilder.index.sortLanguage == Language.en) {
          subfield = parseField_EN(indexBuilder, subfield, entryData, j);
        }
        parseFieldGeneric(indexBuilder, subfield, entryData, j, subfields.length);
      }
    }
  }

  private void parseFieldGeneric(final IndexBuilder indexBuilder, String field,
      final EntryData entryData, final int subfieldIdx, final int numSubFields) {
    // remove bracketed and parenthesized stuff.
    final StringBuilder bracketed = new StringBuilder(); 
    final StringBuilder parenthesized = new StringBuilder();
    
    Matcher matcher;
    while ((matcher = BRACKETED.matcher(field)).matches()) {
      bracketed.append(matcher.group(1)).append(" ");
      field = matcher.replaceFirst(" ");
    }

    while ((matcher = PARENTHESIZED.matcher(field)).matches()) {
      parenthesized.append(matcher.group(1)).append(" ");
      field = matcher.replaceFirst(" ");
    }
    
    field = SPACES.matcher(field).replaceAll(" ").trim();

    // split words on non -A-z0-9, do them.
    final String[] tokens = NON_CHAR_DASH.split(field);

    final EntryTypeName entryTypeName;
    if (numSubFields == 1) {
      assert subfieldIdx == 0;
      if (tokens.length == 1) {
        entryTypeName = EntryTypeName.ONE_WORD;
      } else if (tokens.length == 2) {
        entryTypeName = EntryTypeName.TWO_WORDS;
      } else if (tokens.length == 3) {
        entryTypeName = EntryTypeName.THREE_WORDS;
      } else if (tokens.length == 4) {
        entryTypeName = EntryTypeName.FOUR_WORDS;
      } else {
        entryTypeName = EntryTypeName.FIVE_OR_MORE_WORDS;
      }
    } else {
      assert numSubFields > 1;
      if (subfieldIdx == 0) {
        if (tokens.length == 1) {
          entryTypeName = EntryTypeName.MULTIROW_HEAD_ONE_WORD;
        } else {
          entryTypeName = EntryTypeName.MULTIROW_HEAD_MANY_WORDS;
        }
      } else {
        assert subfieldIdx > 0;
        if (tokens.length == 1) {
          entryTypeName = EntryTypeName.MULTIROW_TAIL_ONE_WORD;
        } else {
          entryTypeName = EntryTypeName.MULTIROW_TAIL_MANY_WORDS;
        }
      }
    }

    for (String token : tokens) {
      token = TRIM_PUNC.matcher(token).replaceAll("");
      if (!alreadyDone.contains(token) && token.length() > 0) {
        final List<EntryData> entries = indexBuilder.getOrCreateEntries(token, entryTypeName);
        entries.add(entryData);
        alreadyDone.add(token);
        
        // also split words on dashes, do them, too.
        if (token.contains("-")) {
          final String[] dashed = token.split("-");
          for (final String dashedToken : dashed) {
            if (!alreadyDone.contains(dashedToken) && dashedToken.length() > 0) {
              final List<EntryData> dashEntries = indexBuilder.getOrCreateEntries(dashedToken, EntryTypeName.PART_OF_HYPHENATED);
              dashEntries.add(entryData);
            }
          }
        }

      }  // if (!alreadyDone.contains(token)) {
    }  // for (final String token : tokens) { 
    
    // process bracketed stuff (split on spaces and dashes always)
    final String[] bracketedTokens = NON_CHAR.split(bracketed.toString());
    for (final String token : bracketedTokens) {
      assert !token.contains("-");
      if (!alreadyDone.contains(token) && token.length() > 0) {
        final List<EntryData> entries = indexBuilder.getOrCreateEntries(token, EntryTypeName.BRACKETED);
        entries.add(entryData);
      }
    }
    
    // process paren stuff
    final String[] parenTokens = NON_CHAR.split(bracketed.toString());
    for (final String token : parenTokens) {
      assert !token.contains("-");
      if (!alreadyDone.contains(token) && token.length() > 0) {
        final List<EntryData> entries = indexBuilder.getOrCreateEntries(token, EntryTypeName.PARENTHESIZED);
        entries.add(entryData);
      }
    }
    
  }

  private String parseField_DE(final IndexBuilder indexBuilder, String field,
      final EntryData entryData, final int subfieldIdx) {
    final Matcher matcher = DE_NOUN.matcher(field);
    while (matcher.find()) {
      final String noun = matcher.group(1);
      //final String gender = matcher.group(2);
      if (alreadyDone.add(noun)) {
        // System.out.println("Found DE noun " + noun + ", " + gender);
        final List<EntryData> entries = indexBuilder.getOrCreateEntries(noun, EntryTypeName.NOUN);
        entries.add(entryData);
      }
    }
    return field;
  }
  
  private String parseField_EN(final IndexBuilder indexBuilder, String field,
      final EntryData entryData, final int subfieldIdx) {
    if (field.startsWith("to ")) {
      field = field.substring(3);
    }
    return field;
  }


}

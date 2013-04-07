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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.DictionaryBuilder;
import com.hughes.android.dictionary.engine.EntrySource;
import com.hughes.android.dictionary.engine.IndexedEntry;
import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.Language;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;

public class DictFileParser implements Parser {
  
  static final Logger logger = Logger.getLogger(DictFileParser.class.getName());

  // Dictcc
  public static final Pattern TAB = Pattern.compile("\\t");

  // Chemnitz
  public static final Pattern DOUBLE_COLON = Pattern.compile(" :: ");
  public static final Pattern PIPE = Pattern.compile("\\|");
  
  static final Pattern SPACES = Pattern.compile("\\s+");
  
  static final Pattern BRACKETED = Pattern.compile("\\[([^]]+)\\]");
  static final Pattern PARENTHESIZED = Pattern.compile("\\(([^)]+)\\)");
  static final Pattern CURLY_BRACED = Pattern.compile("\\{([^}]+)\\}");
  
  // http://www.regular-expressions.info/unicode.html
  static final Pattern NON_CHAR_DASH = Pattern.compile("[^-'\\p{L}\\p{M}\\p{N}]+");
  public static final Pattern NON_CHAR = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");

  static final Pattern TRIM_PUNC = Pattern.compile("^[^\\p{L}\\p{M}\\p{N}]+|[^\\p{L}\\p{M}\\p{N}]+$");

  final Charset charset;
  final boolean flipCols;
  
  final Pattern fieldSplit;
  final Pattern subfieldSplit;
  
  final DictionaryBuilder dictBuilder;
  final IndexBuilder[] langIndexBuilders;
  final IndexBuilder bothIndexBuilder;
  
  EntrySource entrySource;
  
  // final Set<String> alreadyDone = new HashSet<String>();
    
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

  @Override
  public void parse(final File file, final EntrySource entrySouce, final int pageLimit) throws IOException {
    this.entrySource = entrySouce;
    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
    String line;
    int count = 0;
    while ((line = reader.readLine()) != null) {
      if (pageLimit >= 0 && count >= pageLimit) {
        return;
      }
      if (count % 10000 == 0) {
        logger.info("count=" + count + ", line=" + line);
      }
      parseLine(line);
      ++count;
    }
  }
  
  private void parseLine(final String line) {
    if (line.startsWith("#") || line.length() == 0) {
      logger.info("Skipping comment line: " + line);
      return;
    }
    final String[] fields = fieldSplit.split(line);
    // dictcc now has a part of speech field as field #3.
    if (fields.length < 2 || fields.length > 3) {
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
        
    final PairEntry pairEntry = new PairEntry(entrySource);
    for (int i = 0; i < subfields[0].length; ++i) {
      subfields[0][i] = subfields[0][i].trim();
      subfields[1][i] = subfields[1][i].trim();
      if (subfields[0][i].length() == 0 && subfields[1][i].length() == 0) {
        logger.warning("Empty pair: " + line);
        continue;
      }
      if (subfields[0][i].length() == 0) {
        subfields[0][i] = "__";
      }
      if (subfields[1][i].length() == 0) {
        subfields[1][i] = "__";
      }
      pairEntry.pairs.add(new Pair(subfields[0][i], subfields[1][i]));
    }
    final IndexedEntry entryData = new IndexedEntry(pairEntry);
    entryData.isValid = true;
    
    for (int l = 0; l < 2; ++l) {
      // alreadyDone.clear();
      
      for (int j = 0; j < subfields[l].length; ++j) {
        String subfield = subfields[l][j];
        final IndexBuilder indexBuilder = langIndexBuilders[l];
        if (indexBuilder.index.sortLanguage == Language.de) {
          subfield = parseField_DE(indexBuilder, subfield, entryData, j);
        } else if (indexBuilder.index.sortLanguage == Language.en) {
          subfield = parseField_EN(indexBuilder, subfield, entryData, j);
        }
        parseFieldGeneric(indexBuilder, subfield, entryData, j, subfields[l].length);
      }
    }
  }

  private void parseFieldGeneric(final IndexBuilder indexBuilder, String field,
      final IndexedEntry entryData, final int subfieldIdx, final int numSubFields) {
    // remove bracketed and parenthesized stuff.
    final StringBuilder bracketed = new StringBuilder(); 
    final StringBuilder parenthesized = new StringBuilder();
    
    Matcher matcher;
    while ((matcher = BRACKETED.matcher(field)).find()) {
      bracketed.append(matcher.group(1)).append(" ");
      field = matcher.replaceFirst(" ");
    }

    while ((matcher = PARENTHESIZED.matcher(field)).find()) {
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
      if (/*!alreadyDone.contains(token) && */token.length() > 0) {
        indexBuilder.addEntryWithTokens(entryData, Collections.singleton(token), entryTypeName);
        // alreadyDone.add(token);
        
        // also split words on dashes, do them, too.
        if (token.contains("-")) {
          final String[] dashed = token.split("-");
          for (final String dashedToken : dashed) {
            if (/*!alreadyDone.contains(dashedToken) && */dashedToken.length() > 0) {
              indexBuilder.addEntryWithTokens(entryData, Collections.singleton(dashedToken), EntryTypeName.PART_OF_HYPHENATED);
            }
          }
        }

      }  // if (!alreadyDone.contains(token)) {
    }  // for (final String token : tokens) { 
    
    // process bracketed stuff (split on spaces and dashes always)
    final String[] bracketedTokens = NON_CHAR.split(bracketed.toString());
    for (final String token : bracketedTokens) {
      assert !token.contains("-");
      if (/*!alreadyDone.contains(token) && */token.length() > 0) {
        indexBuilder.addEntryWithTokens(entryData, Collections.singleton(token), EntryTypeName.BRACKETED);
      }
    }
    
    // process paren stuff
    final String[] parenTokens = NON_CHAR.split(parenthesized.toString());
    for (final String token : parenTokens) {
      assert !token.contains("-");
      if (/*!alreadyDone.contains(token) && */token.length() > 0) {
        indexBuilder.addEntryWithTokens(entryData, Collections.singleton(token), EntryTypeName.PARENTHESIZED);
      }
    }
    
  }

  private String parseField_DE(final IndexBuilder indexBuilder, String field,
      final IndexedEntry entryData, final int subfieldIdx) {
    
//    final Matcher matcher = DE_NOUN.matcher(field);
//    while (matcher.find()) {
//      final String noun = matcher.group(1);
      //final String gender = matcher.group(2);
//      if (alreadyDone.add(noun)) {
        // System.out.println("Found DE noun " + noun + ", " + gender);
//        final List<EntryData> entries = indexBuilder.getOrCreateEntries(noun, EntryTypeName.NOUN);
//        entries.add(entryData);
//      }
//    }

    // In English, curly braces are used for different tenses.
    field = CURLY_BRACED.matcher(field).replaceAll(" ");

    return field;
  }
  
  private String parseField_EN(final IndexBuilder indexBuilder, String field,
      final IndexedEntry entryData, final int subfieldIdx) {
    if (field.startsWith("to ")) {
      field = field.substring(3);
    }
    return field;
  }
  
  public static final Set<String> tokenize(final String text, final Pattern pattern) {
    final String[] split = pattern.split(text);
    final Set<String> result = new LinkedHashSet<String>(Arrays.asList(split));
    result.remove("");
    return result;
  }


}

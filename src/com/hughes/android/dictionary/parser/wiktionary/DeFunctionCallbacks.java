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

import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.AppendAndIndexWikiCallback;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.NameAndArgs;

import java.util.List;
import java.util.Map;

class DeFunctionCallbacks {
  
  static <T extends AbstractWiktionaryParser> void addGenericCallbacks(Map<String, FunctionCallback<T>> callbacks) {
      FunctionCallback<T> callback = new MakeHeadingFromName<T>("====");
      callbacks.put("Aussprache", callback);
      callbacks.put("Worttrennung", callback);
      callbacks.put("Bedeutungen", callback);
      callbacks.put("Herkunft", callback);
      callbacks.put("Synonyme", callback);
      callbacks.put("Gegenwörter", callback);
      callbacks.put("Verkleinerungsformen", callback);
      callbacks.put("Oberbegriffe", callback);
      callbacks.put("Unterbegriffe", callback);
      callbacks.put("Beispiele", callback);
      callbacks.put("Redewendungen", callback);
      callbacks.put("Charakteristische Wortkombinationen", callback);
      callbacks.put("Abgeleitete Begriffe", callback);
      callbacks.put("Übersetzungen", callback);
      callbacks.put("Referenzen", callback);
      callbacks.put("Grammatische Merkmale", callback);
      callbacks.put("Abkürzungen", callback);
      
      // TODO:
      // {{Anmerkung}}
      // {{Anmerkungen}}
      // {{Anmerkung|zum Gebrauch}}
  }

  
  static final NameAndArgs<EnParser> NAME_AND_ARGS = new NameAndArgs<EnParser>();

  
  static final class MakeHeadingFromName<T extends AbstractWiktionaryParser> implements FunctionCallback<T> {
    final String header;
    public MakeHeadingFromName(String header) {
        this.header = header;
    }

    @Override
      public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name, final List<String> args,
          final Map<String, String> namedArgs,
          final T parser,
          final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
        if (!namedArgs.isEmpty() || args.size() != 0) {
            return false;
        }
        //appendAndIndexWikiCallback.builder.append(String.format("<%s>", header));
        appendAndIndexWikiCallback.dispatch("\n" + header + name + header, null);
        //appendAndIndexWikiCallback.builder.append(String.format("</%s>\n", header));
        return true;
      }
    }


}
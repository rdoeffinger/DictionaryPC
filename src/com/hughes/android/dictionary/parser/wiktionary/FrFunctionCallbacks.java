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

import java.util.List;
import java.util.Map;

import com.hughes.android.dictionary.parser.WikiTokenizer;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.AppendAndIndexWikiCallback;
import com.hughes.android.dictionary.parser.wiktionary.AbstractWiktionaryParser.NameAndArgs;
import com.hughes.android.dictionary.parser.wiktionary.ItFunctionCallbacks.Redispatch;

class FrFunctionCallbacks {

    static <T extends AbstractWiktionaryParser> void addGenericCallbacks(Map<String, FunctionCallback<T>> callbacks) {
        callbacks.put("-étym-", new Redispatch<>("\n==== Étymologie ====\n"));
        callbacks.put("-pron-", new Redispatch<>("\n==== Prononciation ====\n"));
        callbacks.put("-voir-", new Redispatch<>("\n==== Voir aussi ====\n"));
        callbacks.put("-drv-", new Redispatch<>("\n==== Dérivés ====\n"));
        callbacks.put("-syn-", new Redispatch<>("\n==== Synonymes ====\n"));

        callbacks.put("-apr-", new Redispatch<>("\n==== Apparentés étymologiques ====\n"));
        callbacks.put("-hyper-", new Redispatch<>("\n==== Hyperonymes ====\n"));
        callbacks.put("-hypo-", new Redispatch<>("\n==== Hyponymes ====\n"));
        callbacks.put("-réf-", new Redispatch<>("\n==== Références ====\n"));
        callbacks.put("-homo-", new Redispatch<>("\n==== Homophones ====\n"));
        callbacks.put("-anagr-", new Redispatch<>("\n==== Anagrammes ====\n"));
        callbacks.put("-voc-", new Redispatch<>("\n==== Vocabulaire apparenté par le sens ====\n"));
        callbacks.put("-exp-", new Redispatch<>("\n==== Expressions ====\n"));
        callbacks.put("-note-", new Redispatch<>("\n==== Note ====\n"));

        callbacks.put("-trad-", new ItFunctionCallbacks.SkipSection<>());
    }


    static final NameAndArgs<EnParser> NAME_AND_ARGS = new NameAndArgs<>();


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
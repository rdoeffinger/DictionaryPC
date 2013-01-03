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

class ItFunctionCallbacks {

    static <T extends AbstractWiktionaryParser> void addGenericCallbacks(
            Map<String, FunctionCallback<T>> callbacks) {
        callbacks.put("-hyph-", new Redispatch<T>("\n==== Sillabazione ====\n"));
        callbacks.put("-pron-", new Redispatch<T>("\n==== Pronuncia ====\n"));
        callbacks.put("-etim-", new Redispatch<T>("\n==== Etimologia / Derivazione ====\n"));
        callbacks.put("-syn-", new Redispatch<T>("\n==== Sinonimi ====\n"));
        callbacks.put("-ant-", new Redispatch<T>("\n==== Antonimi/Contrari ====\n"));
        callbacks.put("-drv-", new Redispatch<T>("\n==== Parole derivate ====\n"));
        callbacks.put("-prov-", new Redispatch<T>("\n==== Proverbi e modi di dire ====\n"));
        callbacks.put("-ref-", new Redispatch<T>("\n==== Note / Riferimenti ====\n"));
        callbacks.put("-rel-", new Redispatch<T>("\n==== Termini correlati ====\n"));
        callbacks.put("-var-", new Redispatch<T>("\n==== Varianti ====\n"));

        callbacks.put("-trans1-", new SkipSection<T>());
        callbacks.put("-trans2-", new SkipSection<T>());
        callbacks.put("-ref-", new SkipSection<T>());
    }

    static final NameAndArgs<EnParser> NAME_AND_ARGS = new NameAndArgs<EnParser>();

    static final class Redispatch<T extends AbstractWiktionaryParser> implements
            FunctionCallback<T> {
        final String newText;

        public Redispatch(String newText) {
            this.newText = newText;
        }

        @Override
        public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name,
                final List<String> args,
                final Map<String, String> namedArgs,
                final T parser,
                final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
            if (!namedArgs.isEmpty() || args.size() != 0) {
                return false;
            }
            appendAndIndexWikiCallback.dispatch(newText, null);
            return true;
        }
    }

    static final class SkipSection<T extends AbstractWiktionaryParser> implements
            FunctionCallback<T> {
        public SkipSection() {
        }

        @Override
        public boolean onWikiFunction(final WikiTokenizer wikiTokenizer, final String name,
                final List<String> args,
                final Map<String, String> namedArgs,
                final T parser,
                final AppendAndIndexWikiCallback<T> appendAndIndexWikiCallback) {
            while (wikiTokenizer.nextToken() != null) {
                if (wikiTokenizer.isFunction() 
                        && wikiTokenizer.functionName().startsWith("-")
                        && wikiTokenizer.functionName().endsWith("-")
                        // Hack to prevent infinite-looping, would be better to check that this func was at the start of the line.
                        && !wikiTokenizer.functionName().contains("trans")) {
                    wikiTokenizer.returnToLineStart();
                    return true;
                }
            }
            return true;
        }
    }

}

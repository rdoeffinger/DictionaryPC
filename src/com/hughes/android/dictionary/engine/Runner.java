// Copyright 2020 Reimar Döffinger. All Rights Reserved.
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

import java.util.Arrays;

public class Runner {
    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Specify WiktionarySplitter, DictionaryBuilder or ConvertToV6 as first argument");
            return;
        }
        String[] newargs = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "WiktionarySplitter" -> WiktionarySplitter.main(newargs);
            case "DictionaryBuilder" -> DictionaryBuilder.main(newargs);
            case "ConvertToV6" -> ConvertToV6.main(newargs);
            case "CheckDictionariesMain" -> CheckDictionariesMain.main(newargs);
            default ->
                    System.out.println("Unknown command '" + args[0] + "'. Use one of WiktionarySplitter, DictionaryBuilder, ConvertToV6 or CheckDictionariesMain instead.");
        }
    }
}

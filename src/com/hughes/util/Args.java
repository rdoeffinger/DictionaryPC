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

package com.hughes.util;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class Args {

    public static Map<String, String> keyValueArgs(final String[] args) {
        final Map<String,String> dest = new LinkedHashMap<>();
        for (String arg : args) {
            int equalsIndex;
            if (arg.startsWith("--") && (equalsIndex = arg.indexOf("=")) >= 0) {
                final String key = arg.substring(2, equalsIndex);
                final String value = arg.substring(equalsIndex + 1, arg.length());
                dest.put(key, value);
            }
        }
        return dest;
    }
}

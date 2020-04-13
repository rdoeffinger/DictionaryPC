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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public final class FileUtil {
    public static String readLine(final RandomAccessFile file, final long startPos) throws IOException {
        file.seek(startPos);
        return file.readLine();
    }

    public static List<String> readLines(final File file) throws IOException {
        final List<String> result = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }

    public static String readToString(final File file) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    public static void writeStringToUTF8File(final String string, final File file) {
        throw new IllegalStateException();
    }

    public static void printString(final File file, final String s) throws IOException {
        final PrintStream out = new PrintStream(new FileOutputStream(file));
        out.print(s);
        out.close();
    }

}

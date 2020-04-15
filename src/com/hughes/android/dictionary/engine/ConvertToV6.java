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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ConvertToV6 {
    public static void main(final String[] args) throws IOException {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Usage: ConvertToV6 <input.v007> <output.v006> [skipHtml]");
            System.out.println("If the option third argument is given as 'skipHtml'");
            System.out.println("the v6 dictionary will be without all HTML entries to reduce its size");
            return;
        }
        boolean skipHtml = false;
        boolean skipHtmlOpt = false;
        if (args.length == 3) {
            if (!args[2].equals("skipHtml") && !args[2].equals("skipHtmlOpt")) {
                System.out.println("Unknown extra argument '" + args[2] + "'");
                return;
            }
            skipHtml = true;
            skipHtmlOpt = args[2].equals("skipHtmlOpt");
        }
        final String inname = args[0];
        final String outname = args[1];
        FileInputStream in;
        try {
            in = new FileInputStream(inname);
        } catch (FileNotFoundException e) {
            System.out.println("Could not open input file '" + inname + "'");
            System.out.println(e);
            return;
        }
        final Dictionary dictionary = new Dictionary(in.getChannel());
        if (dictionary.dictFileVersion <= 6) {
            System.out.println("Input dictionary is already v6 or older!");
            return;
        }
        if (skipHtmlOpt && dictionary.htmlEntries.size() == 0) {
            System.exit(3);
        }
        RandomAccessFile out;
        try {
            out = new RandomAccessFile(outname, "rw");
        } catch (FileNotFoundException e) {
            System.out.println("Could not open output file '" + outname + "'");
            System.out.println(e);
            return;
        }
        if (out.length() > 0) {
            System.out.println("Output file '" + outname + "' already exists, aborting!");
            return;
        }
        new DictionaryV6Writer(dictionary).writev6(out, skipHtml);
        out.close();
        in.close();
    }
}

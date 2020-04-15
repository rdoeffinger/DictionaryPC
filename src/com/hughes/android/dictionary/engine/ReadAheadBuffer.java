// Copyright 2017 Reimar Döffinger
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class ReadAheadBuffer extends PipedInputStream {
    static int BLOCK_SIZE = 1024 * 1024;
    public ReadAheadBuffer(InputStream in, int size) {
        super(size);
        assert size >= 2 * BLOCK_SIZE;
        try {
            pipe = new PipedOutputStream(this);
        } catch (IOException e) {}
        new Thread(() -> {
            try {
                int read;
                final byte[] buffer = new byte[BLOCK_SIZE];
                while ((read = in.read(buffer)) > 0)
                {
                    pipe.write(buffer, 0, read);
                    pipe.flush();
                }
            } catch (IOException e) {}
            try {
                pipe.close();
            } catch (IOException e) {}
        }).start();
    }

    PipedOutputStream pipe;
}

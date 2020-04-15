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
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class WriteBuffer extends PipedOutputStream {
    static int BLOCK_SIZE = 1024 * 1024;
    public WriteBuffer(OutputStream out, int size) {
        assert size >= 2 * BLOCK_SIZE;
        this.out = out;
        try {
            pipe = new PipedInputStream(this, size);
            buffer = new byte[BLOCK_SIZE];
            writeThread = new Thread(() -> {
                int read;
                try {
                    while ((read = pipe.read(buffer)) > 0)
                    {
                        out.write(buffer, 0, read);
                        out.flush();
                    }
                } catch (IOException e) {
                    System.out.println("Error writing to file " + e);
                }
                try {
                    out.close();
                } catch (IOException e) {}
            });
            writeThread.start();
        } catch (IOException e) {}
    }

    public void close() throws IOException
    {
        super.close();
        try {
            writeThread.join();
        } catch (InterruptedException e) {
            System.out.println("Failed waiting for WriteBuffer thread " + e);
        }
    }

    Thread writeThread;
    OutputStream out;
    PipedInputStream pipe;
    byte[] buffer;
}

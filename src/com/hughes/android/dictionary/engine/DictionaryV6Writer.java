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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class DictionaryV6Writer {
    private final Dictionary d;

    public DictionaryV6Writer(Dictionary dictionary) {
        d = dictionary;
    }

    private void writev6Sources(RandomAccessFile out) throws IOException {
        ByteArrayOutputStream toc = new ByteArrayOutputStream();
        DataOutputStream tocout = new DataOutputStream(toc);

        out.writeInt(d.sources.size());
        long tocPos = out.getFilePointer();
        out.seek(tocPos + d.sources.size() * 8 + 8);
        for (EntrySource s : d.sources) {
            long dataPos = out.getFilePointer();
            tocout.writeLong(dataPos);

            out.writeUTF(s.getName());
            out.writeInt(s.getNumEntries());
        }
        long dataPos = out.getFilePointer();
        tocout.writeLong(dataPos);
        tocout.close();

        out.seek(tocPos);
        out.write(toc.toByteArray());
        out.seek(dataPos);
    }

    private void writev6PairEntries(RandomAccessFile out) throws IOException {
        ByteArrayOutputStream toc = new ByteArrayOutputStream();
        DataOutputStream tocout = new DataOutputStream(toc);

        long tocPos = out.getFilePointer();
        long dataPos = tocPos + 4 + d.pairEntries.size() * 8 + 8;

        out.seek(dataPos);
        DataOutputStream outb = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out.getFD())));

        tocout.writeInt(d.pairEntries.size());
        for (PairEntry pe : d.pairEntries) {
            tocout.writeLong(dataPos + outb.size());

            outb.writeShort(pe.entrySource.index());
            outb.writeInt(pe.pairs.size());
            for (PairEntry.Pair p : pe.pairs) {
                outb.writeUTF(p.lang1);
                outb.writeUTF(p.lang2);
            }
        }
        dataPos += outb.size();
        outb.flush();
        tocout.writeLong(dataPos);
        tocout.close();

        out.seek(tocPos);
        out.write(toc.toByteArray());
        out.seek(dataPos);
    }

    private void writev6TextEntries(RandomAccessFile out) throws IOException {
        ByteArrayOutputStream toc = new ByteArrayOutputStream();
        DataOutputStream tocout = new DataOutputStream(toc);

        out.writeInt(d.textEntries.size());
        long tocPos = out.getFilePointer();
        out.seek(tocPos + d.textEntries.size() * 8 + 8);
        for (TextEntry t : d.textEntries) {
            long dataPos = out.getFilePointer();
            tocout.writeLong(dataPos);

            out.writeShort(t.entrySource.index());
            out.writeUTF(t.text);
        }
        long dataPos = out.getFilePointer();
        tocout.writeLong(dataPos);
        tocout.close();

        out.seek(tocPos);
        out.write(toc.toByteArray());
        out.seek(dataPos);
    }

    private void writev6EmptyList(RandomAccessFile out) throws IOException {
        out.writeInt(0);
        out.writeLong(out.getFilePointer() + 8);
    }

    private void writev6HtmlEntries(RandomAccessFile out) throws IOException {
        ByteArrayOutputStream toc = new ByteArrayOutputStream();
        DataOutputStream tocout = new DataOutputStream(toc);

        long tocPos = out.getFilePointer();
        long dataPos = tocPos + 4 + d.htmlEntries.size() * 8 + 8;

        out.seek(dataPos);
        DataOutputStream outb = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out.getFD())));

        tocout.writeInt(d.htmlEntries.size());
        for (HtmlEntry h : d.htmlEntries) {
            tocout.writeLong(dataPos + outb.size());

            outb.writeShort(h.entrySource.index());
            outb.writeUTF(h.title);
            byte[] data = h.getHtml().getBytes(StandardCharsets.UTF_8);
            outb.writeInt(data.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzout = new GZIPOutputStream(baos);
            gzout.write(data);
            gzout.close();
            outb.writeInt(baos.size());
            outb.write(baos.toByteArray());
        }
        dataPos += outb.size();
        outb.flush();
        tocout.writeLong(dataPos);
        tocout.close();

        out.seek(tocPos);
        out.write(toc.toByteArray());
        out.seek(dataPos);
    }

    private void writev6HtmlIndices(DataOutputStream out, long pos, List<HtmlEntry> entries) throws IOException {
        long dataPos = pos + 4 + entries.size() * 8 + 8;

        out.writeInt(entries.size());

        // TOC is trivial, so optimize writing it
        for (int i = 0; i < entries.size(); i++) {
            out.writeLong(dataPos);
            dataPos += 4;
        }
        out.writeLong(dataPos);

        for (HtmlEntry e : entries) {
            out.writeInt(e.index());
        }
    }

    private void writev6IndexEntries(RandomAccessFile out, List<Index.IndexEntry> entries, int[] prunedRowIdx) throws IOException {
        ByteArrayOutputStream toc = new ByteArrayOutputStream();
        DataOutputStream tocout = new DataOutputStream(toc);

        long tocPos = out.getFilePointer();
        long dataPos = tocPos + 4 + entries.size() * 8 + 8;

        out.seek(dataPos);
        DataOutputStream outb = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out.getFD())));

        tocout.writeInt(entries.size());
        for (Index.IndexEntry e : entries) {
            tocout.writeLong(dataPos + outb.size());

            outb.writeUTF(e.token);

            int startRow = e.startRow;
            int numRows = e.numRows;
            if (prunedRowIdx != null) {
                // note: the start row will always be a TokenRow
                // and thus never be pruned
                int newNumRows = 1;
                for (int i = 1; i < numRows; i++) {
                    if (prunedRowIdx[startRow + i] >= 0) newNumRows++;
                }
                startRow = prunedRowIdx[startRow];
                numRows = newNumRows;
            }

            outb.writeInt(startRow);
            outb.writeInt(numRows);
            final boolean hasNormalizedForm = !e.token.equals(e.normalizedToken());
            outb.writeBoolean(hasNormalizedForm);
            if (hasNormalizedForm) outb.writeUTF(e.normalizedToken());
            writev6HtmlIndices(outb, dataPos + outb.size(),
                               prunedRowIdx == null ? e.htmlEntries : Collections.emptyList());
        }
        dataPos += outb.size();
        outb.flush();
        tocout.writeLong(dataPos);
        tocout.close();

        out.seek(tocPos);
        out.write(toc.toByteArray());
        out.seek(dataPos);
    }

    private void writev6Index(RandomAccessFile out, boolean skipHtml) throws IOException {
        ByteArrayOutputStream toc = new ByteArrayOutputStream();
        DataOutputStream tocout = new DataOutputStream(toc);

        out.writeInt(d.indices.size());
        long tocPos = out.getFilePointer();
        out.seek(tocPos + d.indices.size() * 8 + 8);
        for (Index idx : d.indices) {
            // create pruned index for skipHtml feature
            int[] prunedRowIdx = null;
            int prunedSize = 0;
            if (skipHtml) {
                prunedRowIdx = new int[idx.rows.size()];
                for (int i = 0; i < idx.rows.size(); i++) {
                    final RowBase r = idx.rows.get(i);
                    // prune Html entries
                    boolean pruned = r instanceof HtmlEntry.Row;
                    prunedRowIdx[i] = pruned ? -1 : prunedSize;
                    if (!pruned) prunedSize++;
                }
            }

            long dataPos = out.getFilePointer();
            tocout.writeLong(dataPos);

            out.writeUTF(idx.shortName);
            out.writeUTF(idx.longName);
            out.writeUTF(idx.sortLanguage.getIsoCode());
            out.writeUTF(idx.normalizerRules);
            out.writeBoolean(idx.swapPairEntries);
            out.writeInt(idx.mainTokenCount);
            writev6IndexEntries(out, idx.sortedIndexEntries, prunedRowIdx);

            // write stoplist, serializing the whole Set *shudder*
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(idx.stoplist);
            oos.close();
            final byte[] bytes = baos.toByteArray();


            DataOutputStream outb = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out.getFD())));
            outb.writeInt(bytes.length);
            outb.write(bytes);

            outb.writeInt(skipHtml ? prunedSize : idx.rows.size());
            outb.writeInt(5);
            for (RowBase r : idx.rows) {
                int type = 0;
                if (r instanceof PairEntry.Row) {
                    type = 0;
                } else if (r instanceof TokenRow) {
                    final TokenRow tokenRow = (TokenRow)r;
                    type = tokenRow.hasMainEntry ? 1 : 3;
                } else if (r instanceof TextEntry.Row) {
                    type = 2;
                } else if (r instanceof HtmlEntry.Row) {
                    type = 4;
                    if (skipHtml) continue;
                } else {
                    throw new RuntimeException("Row type not supported for v6");
                }
                outb.writeByte(type);
                outb.writeInt(r.referenceIndex);
            }
            outb.flush();
        }
        long dataPos = out.getFilePointer();
        tocout.writeLong(dataPos);
        tocout.close();

        out.seek(tocPos);
        out.write(toc.toByteArray());
        out.seek(dataPos);
    }

    public void writev6(RandomAccessFile raf, boolean skipHtml) throws IOException {
        raf.writeInt(6);
        raf.writeLong(d.creationMillis);
        raf.writeUTF(d.dictInfo);
        System.out.println("sources start: " + raf.getFilePointer());
        writev6Sources(raf);
        System.out.println("pair start: " + raf.getFilePointer());
        writev6PairEntries(raf);
        System.out.println("text start: " + raf.getFilePointer());
        writev6TextEntries(raf);
        System.out.println("html index start: " + raf.getFilePointer());
        if (skipHtml) writev6EmptyList(raf);
        else writev6HtmlEntries(raf);
        System.out.println("indices start: " + raf.getFilePointer());
        writev6Index(raf, skipHtml);
        System.out.println("end: " + raf.getFilePointer());
        raf.writeUTF("END OF DICTIONARY");
    }
}

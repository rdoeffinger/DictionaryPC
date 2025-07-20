package com.hughes.android.dictionary.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hughes.android.dictionary.engine.DictionaryInfo.IndexInfo;
import com.hughes.util.CollectionUtil;

public class CheckDictionariesMain {

    static final String BASE_URL = "https://github.com/rdoeffinger/Dictionary/releases/download/v0.3-dictionaries/";
    static final String VERSION_CODE_OLD = "v006";
    static final String VERSION_CODE = "v007";

    public static void main(String[] args) throws IOException {
        final File dictDir = new File(DictionaryBuilderMain.OUTPUTS);

        final PrintWriter dictionaryInfoOut = new PrintWriter(new File("../Dictionary/res/raw/dictionary_info.txt"));
//    dictionaryInfoOut.println("# LANG_1\t%LANG_2\tFILENAME\tVERSION_CODE\tFILESIZE\tNUM_MAIN_WORDS_1\tNUM_MAIN_WORDS_2\tNUM_ALL_WORDS_1\tNUM_ALL_WORDS_2");

        final File[] files = dictDir.listFiles();
        final List<String> dictNames = new ArrayList<>();
        Arrays.sort(files);
        for (final File dictFile : files) {
            if (!dictFile.getName().endsWith("quickdic")) {
                continue;
            }
            System.out.println(dictFile.getPath());


            final RandomAccessFile raf = new RandomAccessFile(dictFile, "r");
            final Dictionary dict = new Dictionary(raf.getChannel());

            final DictionaryInfo dictionaryInfo = dict.getDictionaryInfo();

            String version_code = VERSION_CODE;
            File zipFile = new File(dictFile.getPath() + "." + version_code + ".zip");
            if (!zipFile.canRead()) {
                version_code = VERSION_CODE_OLD;
                zipFile = new File(dictFile.getPath() + "." + version_code + ".zip");
            }
            dictionaryInfo.uncompressedFilename = dictFile.getName();
            dictionaryInfo.downloadUrl = BASE_URL + dictFile.getName() + "." + version_code + ".zip";
            // TODO: zip it right here....
            dictionaryInfo.uncompressedBytes = dictFile.length();
            dictionaryInfo.zipBytes = zipFile.canRead() ? zipFile.length() : -1;

            // Print it.
//      final PrintWriter textOut = new PrintWriter(new BufferedWriter(new FileWriter(dictFile + ".text")));
//      final List<PairEntry> sorted = new ArrayList<PairEntry>(dict.pairEntries);
//      Collections.sort(sorted);
//      for (final PairEntry pairEntry : sorted) {
//        textOut.println(pairEntry.getRawText(false));
//      }
//      textOut.close();

            // Find the stats.
            System.out.println("Stats...");
            final List<String> indexNames = new ArrayList<>();
            for (final IndexInfo indexInfo : dictionaryInfo.indexInfos) {
                indexNames.add(indexInfo.shortName);
            }
            dictNames.add(CollectionUtil.join(indexNames, "-") + "\n");
            StringBuilder row = new StringBuilder();
            row.append(dictionaryInfo.uncompressedFilename);
            row.append("\t").append(dictionaryInfo.downloadUrl);
            row.append("\t").append(dictionaryInfo.creationMillis);
            row.append("\t").append(dictionaryInfo.uncompressedBytes);
            row.append("\t").append(dictionaryInfo.zipBytes);
            row.append("\t").append(dictionaryInfo.indexInfos.size());
            for (final IndexInfo indexInfo : dictionaryInfo.indexInfos) {
                row.append("\t").append(indexInfo.shortName);
                row.append("\t").append(indexInfo.allTokenCount);
                row.append("\t").append(indexInfo.mainTokenCount);
            }
            row.append("\t").append(dictionaryInfo.dictInfo.replace("\n", "\\\\n"));

            if (!zipFile.canRead()) {
                System.err.println("Couldn't read zipfile: " + zipFile);
            }
            System.out.println(row.toString() + "\n");


            dictionaryInfoOut.println(row.toString());
            dictionaryInfoOut.flush();

            raf.close();
        }

        Collections.sort(dictNames);
        System.out.println(dictNames.toString().replace(",", "  *"));

        dictionaryInfoOut.close();
    }

}

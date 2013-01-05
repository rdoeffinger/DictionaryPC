package com.hughes.android.dictionary.engine;

import com.hughes.android.dictionary.DictionaryInfo;
import com.hughes.android.dictionary.DictionaryInfo.IndexInfo;
import com.hughes.util.CollectionUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CheckDictionariesMain {
  
  static final String BASE_URL = "http://quickdic-dictionary.googlecode.com/files/";
  static final String VERSION_CODE = "v006";

  public static void main(String[] args) throws IOException {
    final File dictDir = new File(DictionaryBuilderMain.OUTPUTS);
    
    final PrintWriter dictionaryInfoOut = new PrintWriter(new File("../Dictionary/res/raw/dictionary_info.txt"));
//    dictionaryInfoOut.println("# LANG_1\t%LANG_2\tFILENAME\tVERSION_CODE\tFILESIZE\tNUM_MAIN_WORDS_1\tNUM_MAIN_WORDS_2\tNUM_ALL_WORDS_1\tNUM_ALL_WORDS_2");

    final File[] files = dictDir.listFiles();
    final List<String> dictNames = new ArrayList<String>();
    Arrays.sort(files);
    for (final File dictFile : files) {
      if (!dictFile.getName().endsWith("quickdic")) {
        continue;
      }
      System.out.println(dictFile.getPath());
      
      
      final RandomAccessFile raf = new RandomAccessFile(dictFile, "r");
      final Dictionary dict = new Dictionary(raf);

      final DictionaryInfo dictionaryInfo = dict.getDictionaryInfo();

      dictionaryInfo.uncompressedFilename = dictFile.getName();
      dictionaryInfo.downloadUrl = BASE_URL + dictFile.getName() + "." + VERSION_CODE + ".zip";
      // TODO: zip it right here....
      dictionaryInfo.uncompressedBytes = dictFile.length();
      final File zipFile = new File(dictFile.getPath() + "." + VERSION_CODE + ".zip");
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
      final List<String> indexNames = new ArrayList<String>();
      for (final IndexInfo indexInfo : dictionaryInfo.indexInfos) {
          indexNames.add(indexInfo.shortName);
      }
      dictNames.add(CollectionUtil.join(indexNames, "-") + "\n");
      final String row = dictionaryInfo.append(new StringBuilder()).toString();
      if (!zipFile.canRead()) {
        System.err.println("Couldn't read zipfile: " + zipFile);
      }
      System.out.println(row + "\n");
      
      
      dictionaryInfoOut.println(row);
      dictionaryInfoOut.flush();
      
      raf.close();
    }
    
    Collections.sort(dictNames);
    System.out.println(dictNames.toString().replaceAll(",", "  *"));
    
    dictionaryInfoOut.close();
  }

}

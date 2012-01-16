package com.hughes.android.dictionary.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hughes.android.dictionary.DictionaryInfo;
import com.hughes.android.dictionary.engine.Index.IndexEntry;

public class CheckDictionariesMain {
  
  static final String BASE_URL = "http://quickdic-dictionary.googlecode.com/files/";
  static final String VERSION_CODE = "v002";

  public static void main(String[] args) throws IOException {
    final File dictDir = new File(DictionaryBuilderMain.OUTPUTS);
    
    final PrintWriter dictionaryInfoOut = new PrintWriter(new File("../Dictionary/res/raw/dictionary_info.txt"));
    dictionaryInfoOut.println("# LANG_1\t%LANG_2\tFILENAME\tVERSION_CODE\tFILESIZE\tNUM_MAIN_WORDS_1\tNUM_MAIN_WORDS_2\tNUM_ALL_WORDS_1\tNUM_ALL_WORDS_2");

    final File[] files = dictDir.listFiles();
    Arrays.sort(files);
    for (final File dictFile : files) {
      if (!dictFile.getName().endsWith("quickdic")) {
        continue;
      }
      System.out.println(dictFile.getPath());
      
      final DictionaryInfo dictionaryInfo = new DictionaryInfo();
      
      final RandomAccessFile raf = new RandomAccessFile(dictFile, "r");
      final Dictionary dict = new Dictionary(raf);
      
      dictionaryInfo.uncompressedFilename = dictFile.getName();
      dictionaryInfo.downloadUrl = BASE_URL + dictFile.getName() + "." + VERSION_CODE + ".zip";
      // TODO: zip it right here....
      dictionaryInfo.creationMillis = dict.creationMillis;
      dictionaryInfo.uncompressedSize = dictFile.length();

      // Print it.
//      final PrintWriter textOut = new PrintWriter(new File(dictFile + ".text"));
//      final List<PairEntry> sorted = new ArrayList<PairEntry>(dict.pairEntries);
//      Collections.sort(sorted);
//      for (final PairEntry pairEntry : sorted) {
//        textOut.println(pairEntry.getRawText(false));
//      }
//      textOut.close();
      
      // Find the stats.
      System.out.println("Stats...");
      for (int i = 0; i < 2; ++i) {
        dictionaryInfo.langIsos[i] = dict.indices.get(i).sortLanguage.getIsoCode();
        final Index index = dict.indices.get(i);
        for (final IndexEntry indexEntry : index.sortedIndexEntries) {
          final TokenRow tokenRow = (TokenRow) index.rows.get(indexEntry.startRow);
          dictionaryInfo.allTokenCounts[i]++; 
          if (tokenRow.hasMainEntry) {
            dictionaryInfo.mainTokenCounts[i]++; 
          }
        }
      }
      
      raf.close();
      
      dictionaryInfoOut.println(dictionaryInfo.toTabSeparatedString());
      dictionaryInfoOut.flush();
      System.out.println(dictionaryInfo.toTabSeparatedString() + "\n");
    }
    
    dictionaryInfoOut.close();
  }

}

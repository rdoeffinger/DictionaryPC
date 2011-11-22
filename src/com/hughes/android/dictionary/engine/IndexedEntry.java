/**
 * 
 */
package com.hughes.android.dictionary.engine;

import com.hughes.util.IndexedObject;

public class IndexedEntry extends IndexedObject {
  public IndexedEntry(final AbstractEntry entry) {
    super(-1);
    this.entry = entry;
  }
  AbstractEntry entry;
  
  public void addToDictionary(Dictionary dictionary) {
    assert index == -1;
    index = entry.addToDictionary(dictionary);
  }
}
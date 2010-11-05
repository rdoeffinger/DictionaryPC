/**
 * 
 */
package com.hughes.android.dictionary.engine;

import com.hughes.util.IndexedObject;

public class EntryData extends IndexedObject {
  public EntryData(final int index, final Entry entry) {
    super(index);
    this.entry = entry;
  }
  Entry entry;
}
/**
 * 
 */
package com.hughes.android.dictionary.engine;

import com.hughes.util.IndexedObject;

class EntryData extends IndexedObject {
  EntryData(final int index, final Entry entry) {
    super(index);
    this.entry = entry;
  }
  Entry entry;
}
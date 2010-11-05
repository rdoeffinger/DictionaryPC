package com.hughes.android.dictionary.engine;

import java.util.Comparator;

import com.ibm.icu.text.Transliterator;

public class NormalizeComparator implements Comparator<String> {
  
  final Transliterator normalizer;
  final Comparator<Object> comparator;

  public NormalizeComparator(final Transliterator normalizer,
      final Comparator<Object> comparator) {
    this.normalizer = normalizer;
    this.comparator = comparator;
  }

  @Override
  public int compare(final String s1, final String s2) {
    final String n1 = normalizer.transform(s1);
    final String n2 = normalizer.transform(s2);
    final int cn = comparator.compare(n1, n2);
    if (cn != 0) {
      return cn;
    }
    return comparator.compare(s1, s2);
  }

}

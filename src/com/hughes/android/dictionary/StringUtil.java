package com.hughes.android.dictionary;

public final class StringUtil {

  public static String longestCommonSubstring(final String s1, final String s2) {
    for (int i = 0; i < s1.length() && i < s2.length(); i++) {
      if (s1.charAt(i) != s2.charAt(i)) {
        return s1.substring(0, i);
      }
    }
    return s1.length() < s2.length() ? s1 : s2;
  }

}

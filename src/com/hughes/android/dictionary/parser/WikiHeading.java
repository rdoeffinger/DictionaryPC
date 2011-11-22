package com.hughes.android.dictionary.parser;

public class WikiHeading {
  public final int depth;
  public final String name;
  public final String prefix;
  
  public WikiHeading(int depth, String name, String prefix) {
    this.depth = depth;
    this.name = name;
    this.prefix = prefix;
  }

  public static WikiHeading getHeading(String line) {
    line = line.trim();  
    if (!line.startsWith("=")) {
      return null;
    }
    int i = 0;
    for (; i < line.length() && line.charAt(i) == '='; ++i) {
    }
    final String prefix = line.substring(0, i);
    if (!line.substring(i).endsWith(prefix) || line.charAt(line.length() - i - 1) == '=') {
      System.err.println("Invalid heading: " + line);
      return null;
    }
    return new WikiHeading(i, line.substring(i, line.length() - i).trim(), prefix);
  }
  
}

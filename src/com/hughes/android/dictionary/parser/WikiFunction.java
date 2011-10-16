package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiFunction {
  
  public int start;
  public int end;
  public String name = "";
  public final List<String> args = new ArrayList<String>();;
  public final Map<String,String> namedArgs = new LinkedHashMap<String, String>();
  
  private static final Pattern functionEvent = Pattern.compile("\\{\\{|\\[\\[|\\}\\}|\\]\\]|=|\\|");

  public static WikiFunction getFunction(String line) {
    final int start = line.indexOf("{{"); 
    if (start == -1) {
      return null;
    }
    final WikiFunction result = new WikiFunction();
    result.start = start;
    
    final Matcher matcher = functionEvent.matcher(line);
    int depth = 1;
    int end = start + 2;
    int lastPipe = end;
    int lastEquals = -1;
    while (end < line.length() && matcher.find(end)) {
      end = matcher.end();
      if (matcher.group().equals("{{") || matcher.group().equals("[[")) {
        ++depth;
      } else if (matcher.group().equals("}}") || matcher.group().equals("]]")) {
        --depth;
        if (depth == 0) {
          break;
        }
      } else if (matcher.group().equals("|") && depth == 1) {
        if (lastEquals != -1) {
          result.namedArgs.put(line.substring(lastPipe, lastEquals), line.substring(lastEquals + 1, matcher.start()));
        } else {
          result.args.add(line.substring(lastPipe, matcher.start()));
        }
        lastPipe = matcher.end();
        lastEquals = -1;
      } else if (matcher.group().equals("=") && depth == 1) {
        lastEquals = matcher.start();
      }
    }
    if (depth > 0) {
      System.err.println("Invalid function: " + line);
      return null;
    }
    
    if (lastEquals != -1) {
      result.namedArgs.put(line.substring(lastPipe, lastEquals), line.substring(lastEquals + 1, matcher.start()));
    } else {
      result.args.add(line.substring(lastPipe, matcher.start()));
    }
    result.end = matcher.end();
    if (result.args.size() > 0) {
      result.name = result.args.remove(0);
    } else {
      System.err.println("Funnction unnamed: " + line);
    }
   
    return result;
  }
  
  public String getArg(final int pos) {
    return (pos < args.size()) ? args.get(pos) : null;
  }
  
  public String getNamedArg(final String name) {
    return namedArgs.get(name);
  }

  public String replaceWith(final String line, final String sub) {
    return line.substring(0, start) + sub + line.substring(end);
  }
  
  

}

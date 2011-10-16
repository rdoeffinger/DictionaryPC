package com.hughes.android.dictionary.parser;

import java.util.Arrays;

import junit.framework.TestCase;

public class WikiLineReaderTest extends TestCase {
  
  public void testSimple() {
    final String wikiText =
      "Hi" + "\n" +
      "Hello thad you're <!-- not --> '''pretty''' cool '''''over''''' there." + "\n" +
      "hi <!--" + "\n" +
      "multi-line" + "\n" +
      "# comment -->" + "\n" +
      "" + "\n" +
      "asdf\n" + 
      "# {{template_in_list}}" + "\n" +
      "[[wikitext]]:[[wikitext]]" + "\n" +  // don't want this to trigger a list
      "here's [[some blah|some]] wikitext." + "\n" +
      "here's a {{template|this has an = sign|blah=2|blah2=3|" + "\n" +
      "blah3=3,[[asdf]|[asdf asdf]|[asdf asdf asdf]],blah4=4}} and some more text." + "\n" +
      "== Header 2 ==" + "\n" +
      "{{some-func|blah={{nested-func|n2}}|blah2=asdf}}" + "\n" +
      "{{unterminated}" + "\n" +
      "[[unterminated]" + "\n" +
      "=== {{header-template}} ===" + "\n";
    
    final String[] expected = new String[] {
        "Hi",
        "Hello thad you're '''pretty''' cool '''''over''''' there.",
        "hi",
        "",
        "asdf",
        "# {{template_in_list}}",
        "[[wikitext]]:[[wikitext]]",
        "here's [[some blah|some]] wikitext.",
        "here's a {{template|this has an = sign|blah=2|blah2=3| blah3=3,[[asdf]|[asdf asdf]|[asdf asdf asdf]],blah4=4}} and some more text.",
        "== Header 2 ==",
        "{{some-func|blah={{nested-func|n2}}|blah2=asdf}}",
        "{{unterminated}",
        "[[unterminated]",
        "=== {{header-template}} ===",
    };
    
    final WikiLineReader wikiLineReader = new WikiLineReader(wikiText);
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], wikiLineReader.readLine());
    }
    final String end = wikiLineReader.readLine();
    if (end != null) {
      System.out.println(end);
    }
    assertNull(end);
  }
  
  public void testWikiHeading() {
    assertNull(WikiHeading.getHeading(""));
    assertNull(WikiHeading.getHeading("="));
    assertNull(WikiHeading.getHeading("=="));
    assertNull(WikiHeading.getHeading("=a"));
    assertNull(WikiHeading.getHeading("=a=="));
    assertNull(WikiHeading.getHeading("===a=="));
    assertNull(WikiHeading.getHeading("===a===="));
    assertNull(WikiHeading.getHeading("a="));
    assertEquals("a", WikiHeading.getHeading("=a=").name);
    assertEquals(1, WikiHeading.getHeading("=a=").depth);
    assertEquals("aa", WikiHeading.getHeading("==aa==").name);
    assertEquals(2, WikiHeading.getHeading("==aa==").depth);
  }

  
  public void testWikiFunction() {
    assertNull(WikiFunction.getFunction(""));
    assertNull(WikiFunction.getFunction("[[asdf]]"));
    assertNull(WikiFunction.getFunction("asd [[asdf]]asdf "));
    assertEquals("a", WikiFunction.getFunction("{{a}}").name);
    assertEquals("a", WikiFunction.getFunction("{{a|b}}").name);
    assertEquals("a", WikiFunction.getFunction("a{{a|b}}a").name);
    assertEquals("a[[a]]", WikiFunction.getFunction("a{{a[[a]]|b}}a").name);
    assertEquals("a", WikiFunction.getFunction("a{{a|b[[abc|def]]|[[fgh|jkl]]|qwer}}a").name);
    assertEquals(Arrays.asList("a", "b[[abc|d=f]]", "qwer", "[[fgh|jkl]]", "qwer"), WikiFunction.getFunction("a{{a|b[[abc|d=f]]|qwer|[[fgh|jkl]]|qwer}}a").args);
    assertEquals("[[abc|def]]", WikiFunction.getFunction("a{{a|b=[[abc|def]]|qwer|[[fgh|jkl]]|qwer={{asdf}}}}a").namedArgs.get("b"));
    assertEquals("{{asdf}}", WikiFunction.getFunction("a{{a|b=[[abc|def]]|qwer|[[fgh|jkl]]|qwer={{asdf}}}}a").namedArgs.get("qwer"));
  }

}

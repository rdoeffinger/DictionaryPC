package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class WikiTokenizerTest extends TestCase {
  
  public void testSimple() {
    final String wikiText =
      "Hi" + "\n" +
      "Hello thad you're <!-- not --> '''pretty''' cool '''''over''''' there." + "\n" +
      "hi <!--" + "\n" +
      "multi-line" + "\n" +
      "# comment -->" + "\n" +
      "" + "\n" +
      "asdf\n" +
      "{{template_not_in_list}}" + "\n" +
      "# {{template_in_list}}" + "\n" +
      "[[wikitext]]:[[wikitext]]" + "\n" +  // don't want this to trigger a list
      ": but this is a list!" + "\n" +
      "*:* and so is this :::" + "\n" +
      "here's [[some blah|some]] wikitext." + "\n" +
      "here's a {{template|this has an = sign|blah=2|blah2=3|" + "\n" +
      "blah3=3,[[asdf]|[asdf asdf]|[asdf asdf asdf]],blah4=4}} and some more text." + "\n" +
      "== Header 2 ==" + "\n" +
      "{{some-func|blah={{nested-func|n2}}|blah2=asdf}}" + "\n" +
      "{{mismatched]]" + "\n" +
      "[[mismatched}}" + "\n" +
      "{extraterminated}}" + "\n" +
      "[extraterminated]]" + "\n" +
      "=== {{header-template}} ===" + "\n";
    
    final String[] expectedTokens = new String[] {
        "Hi",
        "\n",
        "Hello thad you're ",
        "<!-- not -->",
        " ",
        "'''",
        "pretty",
        "'''",
        " cool ",
        "'''",
        "''",
        "over",
        "'''",
        "''",
        " there.",
        "\n",
        "hi ",
        "<!--\nmulti-line\n# comment -->",
        "\n",
        "\n",
        "asdf",
        "\n",
        "{{template_not_in_list}}",
        "\n",
        "# {{template_in_list}}",
        "\n",
        "[[wikitext]]",
        ":",
        "[[wikitext]]",
        "\n",
        ": but this is a list!",
        "\n",
        "*:* and so is this :::",
        "\n",
        "here's ",
        "[[some blah|some]]",
        " wikitext.",
        "\n",
        "here's a ",
        "{{template|this has an = sign|blah=2|blah2=3|\nblah3=3,[[asdf]|[asdf asdf]|[asdf asdf asdf]],blah4=4}}",
        " and some more text.",
        "\n",
        "== Header 2 ==",
        "\n",
        "{{some-func|blah={{nested-func|n2}}|blah2=asdf}}",
        "\n",
        "{{mismatched]]\n",
        "[[mismatched}}\n",
        "{extraterminated",
        "}}",
        "\n",
        "[extraterminated",
        "]]",
        "\n",
        "=== {{header-template}} ===",
        "\n",
        };
    
    final List<String> actualTokens = new ArrayList<String>();
    
    final WikiTokenizer wikiTokenizer = new WikiTokenizer(wikiText);
    WikiTokenizer token;
    int i = 0;
    while ((token = wikiTokenizer.nextToken()) != null) {
      actualTokens.add(token.token());
      System.out.println("\"" + token.token().replace("\n", "\\n") + "\",");
      assertEquals(expectedTokens[i++], token.token());
    }
    assertEquals(Arrays.asList(expectedTokens), actualTokens);
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
    assertEquals(Arrays.asList("b[[abc|d=f]]", "qwer", "[[fgh|jkl]]", "qwer"), WikiFunction.getFunction("a{{a|b[[abc|d=f]]|qwer|[[fgh|jkl]]|qwer}}a").args);
    assertEquals("[[abc|def]]", WikiFunction.getFunction("a{{a|b=[[abc|def]]|qwer|[[fgh|jkl]]|qwer={{asdf}}}}a").namedArgs.get("b"));
    assertEquals("{{asdf}}", WikiFunction.getFunction("a{{a|b=[[abc|def]]|qwer|[[fgh|jkl]]|qwer={{asdf}}}}a").namedArgs.get("qwer"));
  }

}

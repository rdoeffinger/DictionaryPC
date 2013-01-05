// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class WikiTokenizerTest extends TestCase {
    
  public void testWikiLink() {
    String wikiText;
    
    wikiText = "[[abc]]";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isWikiLink());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().wikiLinkText());
    assertEquals(null, new WikiTokenizer(wikiText).nextToken().wikiLinkDest());
    
    wikiText = "[[abc|def]]";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isWikiLink());
    assertEquals("def", new WikiTokenizer(wikiText).nextToken().wikiLinkText());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().wikiLinkDest());

    wikiText = "[[abc|def|ghi{{a|=2}}p]]";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isWikiLink());
    assertEquals("ghi{{a|=2}}p", new WikiTokenizer(wikiText).nextToken().wikiLinkText());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().wikiLinkDest());

    wikiText = "[[abc]][[def]]";
    assertEquals("[[abc]]", new WikiTokenizer(wikiText).nextToken().token());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().wikiLinkText());
    assertEquals("def", new WikiTokenizer(wikiText).nextToken().nextToken().wikiLinkText());

  }
  
  public void testWikiList() {
    String wikiText;

    wikiText = "* This is ''bold''' asdf.";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());

    wikiText = "* {{a|US}} {{IPA|[ˈfɔɹ.wɝd]]}}\nasdf\n";
    assertEquals("* {{a|US}} {{IPA|[ˈfɔɹ.wɝd]]}}", new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isListItem());
    assertEquals("\n", new WikiTokenizer(wikiText).nextToken().nextToken().token());

    
    wikiText = "* [[asdf|\u2028" +
    		"asdf]]";
    assertEquals("* [[asdf|\n" +
        "asdf]]", new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isListItem());

  }
  
  public void testFunction() {
    String wikiText;

    {
    WikiTokenizer wt = new WikiTokenizer("'''Προστατευόμενη Ονομασία Προέλευσης''', \"Protected Designation of Origin\" {{");
        while (wt.nextToken() != null) {
            if (wt.isFunction()) {
                assertEquals("", wt.functionName());
            }
        }
    }

    wikiText = "{{abc}}";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isFunction());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().functionName());
    assertEquals(0, new WikiTokenizer(wikiText).nextToken().functionPositionArgs().size());
    assertEquals(0, new WikiTokenizer(wikiText).nextToken().functionNamedArgs().size());

    wikiText = "{{abc|def}}";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isFunction());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().functionName());
    assertEquals(Arrays.asList("def"), new WikiTokenizer(wikiText).nextToken().functionPositionArgs());
    assertEquals(0, new WikiTokenizer(wikiText).nextToken().functionNamedArgs().size());

    wikiText = "{{abc|d[[|]]ef|ghi}}";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isFunction());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().functionName());
    assertEquals(Arrays.asList("d[[|]]ef", "ghi"), new WikiTokenizer(wikiText).nextToken().functionPositionArgs());
    assertEquals(0, new WikiTokenizer(wikiText).nextToken().functionNamedArgs().size());

    wikiText = "{{abc|arg1=101|ghi|arg2=202|arg3={{n1|n2=7|n3}}|{{d}}}}";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isFunction());
    assertEquals("abc", new WikiTokenizer(wikiText).nextToken().functionName());
    assertEquals(Arrays.asList("ghi", "{{d}}"), new WikiTokenizer(wikiText).nextToken().functionPositionArgs());
    assertEquals(3, new WikiTokenizer(wikiText).nextToken().functionNamedArgs().size());
    assertEquals("101", new WikiTokenizer(wikiText).nextToken().functionNamedArgs().get("arg1"));
    assertEquals("202", new WikiTokenizer(wikiText).nextToken().functionNamedArgs().get("arg2"));
    assertEquals("{{n1|n2=7|n3}}", new WikiTokenizer(wikiText).nextToken().functionNamedArgs().get("arg3"));

    wikiText = "{{gloss|asdf}\nAsdf\n\n";
    assertEquals("{{gloss|asdf}", new WikiTokenizer(wikiText).nextToken().token());

    wikiText = "#*{{quote-book|year=1960|author={{w|P. G. Wodehouse}}\n" +
    "|title={{w|Jeeves in the Offing}}\n" +
    "|section=chapter XI\n" +
    "|passage=“I'm sorely beset, Jeeves. Do you recall telling me once about someone who told somebody he could tell him something which would make him think a bit? Knitted socks and porcu\n" +
    "pines entered into it, I remember.” “I think you may be referring to the ghost of the father of Hamlet, Prince of Denmark, sir. Addressing his son, he said ‘I could a tale unfold whos\n" +
    "e lightest word would harrow up thy soul, freeze thy young blood, make thy two eyes, like stars, start from their spheres, thy knotted and combined locks to part and each particular h\n" +
    "air to stand on end like quills upon the fretful '''porpentine'''.’&nbsp;” “That's right. Locks, of course, not socks. Odd that he should have said '''porpentine''' when he meant porc\n" +
    "upine. Slip of the tongue, no doubt, as so often happens with ghosts.”}}";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());

    
  }
  
  public void testReturn() {
    String wikiText;

    wikiText = "hello\n=Heading=\nhello2";
    
    final WikiTokenizer tokenizer = new WikiTokenizer(wikiText);
    
    assertEquals("hello", tokenizer.nextToken().token());
    tokenizer.returnToLineStart();
    assertEquals("hello", tokenizer.nextToken().token());
    assertEquals("\n", tokenizer.nextToken().token());
    tokenizer.returnToLineStart();
    assertEquals("hello", tokenizer.nextToken().token());
    assertEquals("\n", tokenizer.nextToken().token());
    
    assertEquals("=Heading=", tokenizer.nextToken().token());
    tokenizer.returnToLineStart();
    assertEquals("=Heading=", tokenizer.nextToken().token());
    assertEquals("\n", tokenizer.nextToken().token());
    tokenizer.returnToLineStart();
    assertEquals("=Heading=", tokenizer.nextToken().token());
    assertEquals("\n", tokenizer.nextToken().token());

    assertEquals("hello2", tokenizer.nextToken().token());
    assertEquals(null, tokenizer.nextToken());
    tokenizer.returnToLineStart();
    assertEquals("hello2", tokenizer.nextToken().token());
    assertEquals(null, tokenizer.nextToken());
    
    
  }

  public void testWikiHeading() {
    String wikiText;

    wikiText = "==";
    assertEquals("==", new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isHeading());
    assertEquals(2, new WikiTokenizer(wikiText).nextToken().headingDepth());
    assertEquals("", new WikiTokenizer(wikiText).nextToken().headingWikiText());
    assertEquals(1, new WikiTokenizer(wikiText).nextToken().errors.size());

    
    wikiText = "=a";
    assertEquals("=a", new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isHeading());
    assertEquals(1, new WikiTokenizer(wikiText).nextToken().headingDepth());
    assertEquals("a", new WikiTokenizer(wikiText).nextToken().headingWikiText());
    assertEquals(2, new WikiTokenizer(wikiText).nextToken().errors.size());

    wikiText = "=a==";
    assertEquals("=a==", new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isHeading());
    assertEquals(1, new WikiTokenizer(wikiText).nextToken().headingDepth());
    assertEquals("a", new WikiTokenizer(wikiText).nextToken().headingWikiText());
    assertEquals(1, new WikiTokenizer(wikiText).nextToken().errors.size());

    wikiText = "a=";
    assertEquals("a", new WikiTokenizer(wikiText).nextToken().token());
    assertFalse(new WikiTokenizer(wikiText).nextToken().isHeading());

    wikiText = "=a=";
    assertEquals("=a=", new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isHeading());
    assertEquals(1, new WikiTokenizer(wikiText).nextToken().headingDepth());
    assertEquals("a", new WikiTokenizer(wikiText).nextToken().headingWikiText());
    assertEquals(0, new WikiTokenizer(wikiText).nextToken().errors.size());

    wikiText = "==aa[[|=]] {{|={{=}} }}==";
    assertEquals(wikiText, new WikiTokenizer(wikiText).nextToken().token());
    assertTrue(new WikiTokenizer(wikiText).nextToken().isHeading());
    assertEquals(2, new WikiTokenizer(wikiText).nextToken().headingDepth());
    assertEquals("aa[[|=]] {{|={{=}} }}", new WikiTokenizer(wikiText).nextToken().headingWikiText());
    assertEquals(0, new WikiTokenizer(wikiText).nextToken().errors.size());
    
  }

  

  public void testSimple() {
    final String wikiText =
      "Hi" + "\n" +
      "Hello =thad| you're <!-- not --> '''pretty''' cool '''''over''''' there." + "\n" +
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
        "Hello ",
        "=",
        "thad",
        "|",
        " you're ",
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
        "{{mismatched]]",
        "\n",
        "[[mismatched}}",
        "\n",
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
  
  public void testHtml() {
      String wikiText;

      {
      wikiText = " zz <pre> asdf </pre> ZZ <math> 1234 </math> XX ";
      final WikiTokenizer tokenizer = new WikiTokenizer(wikiText);
      assertEquals(" zz ", tokenizer.nextToken().token());
      assertEquals("<pre> asdf </pre>", tokenizer.nextToken().token());
      assertEquals(" ZZ ", tokenizer.nextToken().token());
      assertEquals("<math> 1234 </math>", tokenizer.nextToken().token());
      assertEquals(" XX ", tokenizer.nextToken().token());
      }
      {
      wikiText = "\n<math> 1234 </math>";
      final WikiTokenizer tokenizer = new WikiTokenizer(wikiText);
      assertEquals("<math> 1234 </math>", tokenizer.nextToken().nextToken().token());
      }

      {
      wikiText = "# z'' is the '''free''' variable in \"<math>\\forall x\\exists y:xy=z</math>\".''";
      final WikiTokenizer tokenizer = new WikiTokenizer(wikiText);
      assertEquals(wikiText, tokenizer.nextToken().token());
      }

      
  }
  
}

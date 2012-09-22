package com.hughes.android.dictionary.parser;

import static org.junit.Assert.*;

import com.hughes.util.StringUtil;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

public class GeneralTest {

    @Test
    public void testEscapeHtml() {
        // This isn't actually valid html:
        assertEquals("IPA|/dɛɪ̯/|lang=nds", StringEscapeUtils.escapeHtml3("IPA|/dɛɪ̯/|lang=nds"));
        // Hopefully this is:
        assertEquals("&#x49;&#x50;&#x41;&#x7c;&#x2f;&#x64;&#x25b;&#x26a;&#x32f;&#x2f;&#x7c;&#x6c;&#x61;&#x6e;&#x67;&#x3d;&#x6e;&#x64;&#x73;", StringUtil.escapeToPureHtmlUnicode("IPA|/dɛɪ̯/|lang=nds"));
    }
    
}

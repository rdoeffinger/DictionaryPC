package com.hughes.android.dictionary.parser;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

public class GeneralTest {

    @Test
    public void testEscapeHtml() {
        // Somehow need to escape IPA unicode specially :(
        assertEquals("", StringEscapeUtils.escapeXml("IPA|/dɛɪ̯/|lang=nds"));
    }

}

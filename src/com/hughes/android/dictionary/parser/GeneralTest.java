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
        assertEquals("IPA&#x7c;&#x2f;d&#x25b;&#x26a;&#x32f;&#x2f;&#x7c;lang&#x3d;nds", StringUtil.escapeUnicodeToPureHtml("IPA|/dɛɪ̯/|lang=nds"));
    }
    
}

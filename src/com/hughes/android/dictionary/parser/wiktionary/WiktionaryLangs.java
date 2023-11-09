// Copyright 2012 Google Inc. All Rights Reserved.
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

package com.hughes.android.dictionary.parser.wiktionary;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class WiktionaryLangs {

    public static final Map<String,String> isoCodeToEnWikiName = new LinkedHashMap<>();
    static {
        isoCodeToEnWikiName.put("AF", "Afrikaans");
        isoCodeToEnWikiName.put("SQ", "Albanian");
        isoCodeToEnWikiName.put("AR", "Arabic");
        isoCodeToEnWikiName.put("HY", "Armenian");
        isoCodeToEnWikiName.put("BE", "Belarusian");
        isoCodeToEnWikiName.put("BN", "Bengali");
        isoCodeToEnWikiName.put("BG", "Bulgarian");
        isoCodeToEnWikiName.put("CA", "Catalan");
        isoCodeToEnWikiName.put("SH", "Serbo-Croatian");
        isoCodeToEnWikiName.put("CS", "Czech");
        isoCodeToEnWikiName.put("ZH", "Chinese");
        isoCodeToEnWikiName.put("cmn", "Mandarin");
        isoCodeToEnWikiName.put("yue", "Cantonese");
        isoCodeToEnWikiName.put("DA", "Danish");
        isoCodeToEnWikiName.put("NL", "Dutch");
        isoCodeToEnWikiName.put("EN", "English");
        isoCodeToEnWikiName.put("EO", "Esperanto");
        isoCodeToEnWikiName.put("ET", "Estonian");
        isoCodeToEnWikiName.put("FI", "Finnish");
        isoCodeToEnWikiName.put("FR", "French");
        // Note: must be before German since matcher
        // simply takes first match instead of best.
        isoCodeToEnWikiName.put("nds", "Low German");
        isoCodeToEnWikiName.put("pdc", "Pennsylvania German");
        isoCodeToEnWikiName.put("DE", "German");
        isoCodeToEnWikiName.put("grc", "Ancient Greek");
        isoCodeToEnWikiName.put("EL", "Greek");
        isoCodeToEnWikiName.put("haw", "Hawaiian");
        isoCodeToEnWikiName.put("HE", "Hebrew");
        isoCodeToEnWikiName.put("HI", "Hindi");
        isoCodeToEnWikiName.put("HU", "Hungarian");
        isoCodeToEnWikiName.put("IS", "Icelandic");
        isoCodeToEnWikiName.put("ID", "Indonesian");
        isoCodeToEnWikiName.put("GA", "Irish");
        isoCodeToEnWikiName.put("GD", "Gaelic");
        isoCodeToEnWikiName.put("GV", "Manx");
        isoCodeToEnWikiName.put("IT", "Italian");
        isoCodeToEnWikiName.put("LA", "Latin");
        isoCodeToEnWikiName.put("LV", "Latvian");
        isoCodeToEnWikiName.put("LT", "Lithuanian");
        isoCodeToEnWikiName.put("JA", "Japanese");
        isoCodeToEnWikiName.put("KO", "Korean");
        isoCodeToEnWikiName.put("KU", "Kurdish");
        isoCodeToEnWikiName.put("LO", "Lao");
        isoCodeToEnWikiName.put("ML", "Malayalam");
        isoCodeToEnWikiName.put("MS", "Malay");
        isoCodeToEnWikiName.put("MI", "Maori");
        isoCodeToEnWikiName.put("MN", "Mongolian");
        isoCodeToEnWikiName.put("nci", "Classical Nahuatl");
        isoCodeToEnWikiName.put("NE", "Nepali");
        isoCodeToEnWikiName.put("NO", "Norwegian");
        isoCodeToEnWikiName.put("FA", "Persian");
        isoCodeToEnWikiName.put("PL", "Polish");
        isoCodeToEnWikiName.put("PT", "Portuguese");
        isoCodeToEnWikiName.put("PA", "Punjabi");
        isoCodeToEnWikiName.put("RO", "Romanian");
        isoCodeToEnWikiName.put("RU", "Russian");
        isoCodeToEnWikiName.put("SA", "Sanskrit");
        isoCodeToEnWikiName.put("SK", "Slovak");
        isoCodeToEnWikiName.put("SL", "Slovene|Slovenian");
        isoCodeToEnWikiName.put("SO", "Somali");
        isoCodeToEnWikiName.put("ES", "Spanish");
        isoCodeToEnWikiName.put("SW", "Swahili");
        isoCodeToEnWikiName.put("SV", "Swedish");
        isoCodeToEnWikiName.put("TL", "Tagalog");
        isoCodeToEnWikiName.put("TG", "Tajik");
        isoCodeToEnWikiName.put("TA", "Tamil");
        isoCodeToEnWikiName.put("TH", "Thai");
        isoCodeToEnWikiName.put("BO", "Tibetan");
        isoCodeToEnWikiName.put("TR", "Turkish");
        isoCodeToEnWikiName.put("UK", "Ukrainian");
        isoCodeToEnWikiName.put("UR", "Urdu");
        isoCodeToEnWikiName.put("VI", "Vietnamese");
        isoCodeToEnWikiName.put("CI", "Welsh");
        isoCodeToEnWikiName.put("YI", "Yiddish");
        isoCodeToEnWikiName.put("ZU", "Zulu");
        isoCodeToEnWikiName.put("AZ", "Azeri");
        isoCodeToEnWikiName.put("EU", "Basque");
        isoCodeToEnWikiName.put("BR", "Breton");
        isoCodeToEnWikiName.put("MR", "Marathi");
        isoCodeToEnWikiName.put("FO", "Faroese");
        isoCodeToEnWikiName.put("GL", "Galician");
        isoCodeToEnWikiName.put("KA", "Georgian");
        isoCodeToEnWikiName.put("HT", "Haitian Creole");
        isoCodeToEnWikiName.put("LB", "Luxembourgish");
        isoCodeToEnWikiName.put("MK", "Macedonian");
        isoCodeToEnWikiName.put("scn", "Sicilian");
        isoCodeToEnWikiName.put("cu", "Old Church Slavonic");
        isoCodeToEnWikiName.put("rom", "Romani");

        // No longer exists in EN:
        // isoCodeToEnWikiName.put("BS", "Bosnian");
        // isoCodeToEnWikiName.put("SR", "Serbian");
        // isoCodeToEnWikiName.put("HR", "Croatian");

        // Font doesn't work:
        //isoCodeToEnWikiName.put("MY", "Burmese");


        {
            //Set<String> missing = new LinkedHashSet<String>(isoCodeToEnWikiName.keySet());
            //missing.removeAll(Language.isoCodeToResources.keySet());
            //System.out.println(missing);
        }
        //assert Language.isoCodeToResources.keySet().containsAll(isoCodeToEnWikiName.keySet());
    }

    public static final Map<String,Map<String,String>> wikiCodeToIsoCodeToWikiName = new LinkedHashMap<>();
    static {
        Map<String,String> isoCodeToWikiName;

        // en
        wikiCodeToIsoCodeToWikiName.put("en", isoCodeToEnWikiName);

        // egrep -o '\{\{Wortart[^}]+\}\}' dewiktionary-pages-articles.xml | cut -d \| -f3 | sort | uniq -c | sort -nr
        isoCodeToWikiName = new LinkedHashMap<>();
        wikiCodeToIsoCodeToWikiName.put("de", isoCodeToWikiName);
        isoCodeToWikiName.put("nds", "Niederdeutsch");
        isoCodeToWikiName.put("DE", "Deutsch");
        isoCodeToWikiName.put("EN", "Englisch");
        isoCodeToWikiName.put("IT", "Italienisch");
        isoCodeToWikiName.put("PL", "Polnisch");
        isoCodeToWikiName.put("FR", "Französisch");
        isoCodeToWikiName.put("EO", "Esperanto");
        isoCodeToWikiName.put("CA", "Katalanisch");
        isoCodeToWikiName.put("LA", "Latein");
        isoCodeToWikiName.put("CS", "Tschechisch");
        isoCodeToWikiName.put("HU", "Ungarisch");
        isoCodeToWikiName.put("SV", "Schwedisch");
        isoCodeToWikiName.put("ES", "Spanisch");
        isoCodeToWikiName.put("RO", "Rumänisch");

        // egrep -o '== *\{\{langue\|[a-zA-Z]+\}\} *==' frwiktionary-pages-articles.xml | sort | uniq -c | sort -nr
        isoCodeToWikiName = new LinkedHashMap<>();
        wikiCodeToIsoCodeToWikiName.put("fr", isoCodeToWikiName);
        isoCodeToWikiName.put("FR", Pattern.quote("{{langue|fr}}"));
        isoCodeToWikiName.put("RU", Pattern.quote("{{langue|ru}}"));
        isoCodeToWikiName.put("AR", Pattern.quote("{{langue|ar}}"));  // Arabic
        isoCodeToWikiName.put("BG", Pattern.quote("{{langue|bg}}"));  // Bulgarian
        isoCodeToWikiName.put("EL", Pattern.quote("{{langue|el}}"));
        isoCodeToWikiName.put("EN", Pattern.quote("{{langue|en}}"));
        //isoCodeToWikiName.put("", Pattern.quote("{{langue|sl}}"));
        isoCodeToWikiName.put("LA", Pattern.quote("{{langue|la}}"));
        isoCodeToWikiName.put("IT", Pattern.quote("{{langue|it}}"));
        isoCodeToWikiName.put("EO", Pattern.quote("{{langue|eo}}"));
        isoCodeToWikiName.put("CS", Pattern.quote("{{langue|cs}}"));  // Czech
        isoCodeToWikiName.put("NL", Pattern.quote("{{langue|nl}}"));  // Dutch
        //isoCodeToWikiName.put("", Pattern.quote("{{langue|mg}}"));
        //isoCodeToWikiName.put("", Pattern.quote("{{langue|hsb}}"));
        isoCodeToWikiName.put("ZH", Pattern.quote("{{langue|zh}}"));
        isoCodeToWikiName.put("cmn", Pattern.quote("{{langue|cmn}}"));
        isoCodeToWikiName.put("yue", Pattern.quote("{{langue|yue}}"));
        isoCodeToWikiName.put("JA", Pattern.quote("{{langue|ja}}"));
        isoCodeToWikiName.put("DE", Pattern.quote("{{langue|de}}"));
        isoCodeToWikiName.put("IS", Pattern.quote("{{langue|is}}"));  // Icelandic
        isoCodeToWikiName.put("ES", Pattern.quote("{{langue|es}}"));
        isoCodeToWikiName.put("UK", Pattern.quote("{{langue|uk}}"));
        isoCodeToWikiName.put("PT", Pattern.quote("{{langue|pt}}"));
        isoCodeToWikiName.put("SV", Pattern.quote("{{langue|sv}}"));

        // egrep -o '= *\{\{-[a-z]+-\}\} *=' itwiktionary-pages-articles.xml | sort | uniq -c | sort -n
        isoCodeToWikiName = new LinkedHashMap<>();
        wikiCodeToIsoCodeToWikiName.put("it", isoCodeToWikiName);
        isoCodeToWikiName.put("IT", "\\{\\{-(it|scn|nap|cal|lmo)-\\}\\}");  // scn, nap, cal, lmo
        isoCodeToWikiName.put("EN", Pattern.quote("{{-en-}}"));
        isoCodeToWikiName.put("FR", Pattern.quote("{{-fr-}}"));
        isoCodeToWikiName.put("DE", Pattern.quote("{{-de-}}"));
        isoCodeToWikiName.put("ES", Pattern.quote("{{-es-}}"));
        isoCodeToWikiName.put("JA", Pattern.quote("{{-ja-}}"));
        isoCodeToWikiName.put("PL", Pattern.quote("{{-pl-}}"));
        isoCodeToWikiName.put("NL", Pattern.quote("{{-nl-}}"));
        isoCodeToWikiName.put("LV", Pattern.quote("{{-lv-}}"));
        isoCodeToWikiName.put("LA", Pattern.quote("{{-la-}}"));
        isoCodeToWikiName.put("HU", Pattern.quote("{{-hu-}}"));
        isoCodeToWikiName.put("EL", Pattern.quote("{{-grc-}}"));
        isoCodeToWikiName.put("SV", Pattern.quote("{{-sv-}}"));
        isoCodeToWikiName.put("RU", Pattern.quote("{{-ru-}}"));

        // egrep -o '== *\{\{lengua\|[a-zA-Z]+\}\} *==' frwiktionary-pages-articles.xml | sort | uniq -c | sort -nr
        isoCodeToWikiName = new LinkedHashMap<>();
        wikiCodeToIsoCodeToWikiName.put("es", isoCodeToWikiName);
        isoCodeToWikiName.put("AR", Pattern.quote("{{lengua|ar}}"));
        isoCodeToWikiName.put("ES", Pattern.quote("{{lengua|es}}"));
        isoCodeToWikiName.put("EN", Pattern.quote("{{lengua|en}}"));
        isoCodeToWikiName.put("FR", Pattern.quote("{{lengua|fr}}"));
        isoCodeToWikiName.put("IT", Pattern.quote("{{lengua|it}}"));
        isoCodeToWikiName.put("nci", Pattern.quote("{{lengua|nci}}"));

        // Pattern seems to match Italian one
        isoCodeToWikiName = new LinkedHashMap<>();
        wikiCodeToIsoCodeToWikiName.put("pt", isoCodeToWikiName);
        isoCodeToWikiName.put("PT", Pattern.quote("{{-pt-}}"));
        isoCodeToWikiName.put("EN", Pattern.quote("{{-en-}}"));
        isoCodeToWikiName.put("ES", Pattern.quote("{{-es-}}"));
    }
    public static String getEnglishName(String langCode) {
        String name = isoCodeToEnWikiName.get(langCode);
        if (name == null) {
            name = isoCodeToEnWikiName.get(langCode.toUpperCase());
        }
        if (name == null) {
            return null;
        }
        if (name.indexOf('|') != -1) {
            return name.substring(0, name.indexOf('|'));
        }
        if (name.indexOf('$') != -1) {
            return name.substring(0, name.indexOf('$'));
        }
        return name;  // can be null.
    }

}

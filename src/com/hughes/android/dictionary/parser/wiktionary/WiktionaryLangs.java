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
  
  public static final Map<String,String> isoCodeToWikiName = new LinkedHashMap<String,String>();
  static {
    isoCodeToWikiName.put("AF", "Afrikaans");
    isoCodeToWikiName.put("SQ", "Albanian");
    isoCodeToWikiName.put("AR", "Arabic");
    isoCodeToWikiName.put("HY", "Armenian");
    isoCodeToWikiName.put("BE", "Belarusian");
    isoCodeToWikiName.put("BN", "Bengali");
    isoCodeToWikiName.put("BS", "Bosnian");
    isoCodeToWikiName.put("BG", "Bulgarian");
    isoCodeToWikiName.put("CA", "Catalan");
    isoCodeToWikiName.put("HR", "Croatian");
    isoCodeToWikiName.put("CS", "Czech");
    isoCodeToWikiName.put("ZH", "Chinese|Mandarin|Cantonese");
    isoCodeToWikiName.put("DA", "Danish");
    isoCodeToWikiName.put("NL", "Dutch");
    isoCodeToWikiName.put("EN", "English");
    isoCodeToWikiName.put("EO", "Esperanto");
    isoCodeToWikiName.put("ET", "Estonian");
    isoCodeToWikiName.put("FI", "Finnish");
    isoCodeToWikiName.put("FR", "French");
    isoCodeToWikiName.put("DE", "German");
    isoCodeToWikiName.put("EL", "Greek");
    isoCodeToWikiName.put("haw", "Hawaiian");
    isoCodeToWikiName.put("HE", "Hebrew");
    isoCodeToWikiName.put("HI", "Hindi");
    isoCodeToWikiName.put("HU", "Hungarian");
    isoCodeToWikiName.put("IS", "Icelandic");
    isoCodeToWikiName.put("ID", "Indonesian");
    isoCodeToWikiName.put("GA", "Gaelic");
    isoCodeToWikiName.put("IT", "Italian");
    isoCodeToWikiName.put("LA", "Latin");
    isoCodeToWikiName.put("LV", "Latvian");
    isoCodeToWikiName.put("LT", "Lithuanian");
    isoCodeToWikiName.put("JA", "Japanese");
    isoCodeToWikiName.put("KO", "Korean");
    isoCodeToWikiName.put("KU", "Kurdish");
    isoCodeToWikiName.put("MS", "Malay");
    isoCodeToWikiName.put("MI", "Maori");
    isoCodeToWikiName.put("MN", "Mongolian");
    isoCodeToWikiName.put("NE", "Nepali");
    isoCodeToWikiName.put("NO", "Norwegian");
    isoCodeToWikiName.put("FA", "Persian");
    isoCodeToWikiName.put("PL", "Polish");
    isoCodeToWikiName.put("PT", "Portuguese");
    isoCodeToWikiName.put("PA", "Punjabi");
    isoCodeToWikiName.put("RO", "Romanian");
    isoCodeToWikiName.put("RU", "Russian");
    isoCodeToWikiName.put("SA", "Sanskrit");
    isoCodeToWikiName.put("SR", "Serbian");
    isoCodeToWikiName.put("SK", "Slovak");
    isoCodeToWikiName.put("SO", "Somali");
    isoCodeToWikiName.put("ES", "Spanish");
    isoCodeToWikiName.put("SW", "Swahili");
    isoCodeToWikiName.put("SV", "Swedish");
    isoCodeToWikiName.put("TL", "Tagalog");
    isoCodeToWikiName.put("TG", "Tajik");
    isoCodeToWikiName.put("TH", "Thai");
    isoCodeToWikiName.put("BO", "Tibetan");
    isoCodeToWikiName.put("TR", "Turkish");
    isoCodeToWikiName.put("UK", "Ukrainian");
    isoCodeToWikiName.put("UR", "Urdu");
    isoCodeToWikiName.put("VI", "Vietnamese");
    isoCodeToWikiName.put("CI", "Welsh");
    isoCodeToWikiName.put("YI", "Yiddish");
    isoCodeToWikiName.put("ZU", "Zulu");
    
    isoCodeToWikiName.put("AZ", "Azeri");
    isoCodeToWikiName.put("EU", "Basque");
    isoCodeToWikiName.put("BR", "Breton");
    isoCodeToWikiName.put("MR", "Burmese");
    isoCodeToWikiName.put("FO", "Faroese");
    isoCodeToWikiName.put("GL", "Galician");
    isoCodeToWikiName.put("KA", "Georgian");
    isoCodeToWikiName.put("HT", "Haitian Creole");
    isoCodeToWikiName.put("LB", "Luxembourgish");
    isoCodeToWikiName.put("MK", "Macedonian");
    
  }

  public static final Map<String,Map<String,String>> wikiCodeToIsoCodeToWikiName = new LinkedHashMap<String, Map<String,String>>();
  static {
    // en
    wikiCodeToIsoCodeToWikiName.put("en", isoCodeToWikiName);
    
    Map<String,String> isoCodeToWikiName;
    
    // egrep -o '\{\{Wortart[^}]+\}\}' dewiktionary-pages-articles.xml | cut -d \| -f3 | sort | uniq -c | sort -nr
    isoCodeToWikiName = new LinkedHashMap<String, String>();
    wikiCodeToIsoCodeToWikiName.put("de", isoCodeToWikiName);
    isoCodeToWikiName.put("DE", "Deutsch");
    isoCodeToWikiName.put("EN", "Englisch");
    isoCodeToWikiName.put("IT", "Italienisch");
    isoCodeToWikiName.put("PL", "Polnisch");
    isoCodeToWikiName.put("FR", "Französisch");
    isoCodeToWikiName.put("EO", "Esperanto");
    isoCodeToWikiName.put("CA", "Katalanisch");
    isoCodeToWikiName.put("LA", "Lateinisch");
    isoCodeToWikiName.put("CS", "Tschechisch");
    isoCodeToWikiName.put("HU", "Ungarisch");
    isoCodeToWikiName.put("SV", "Schwedisch");
    isoCodeToWikiName.put("ES", "Spanisch");

    // egrep -o '\{\{=[a-zA-Z]+=\}\}' frwiktionary-pages-articles.xml | sort | uniq -c | sort -nr
    isoCodeToWikiName = new LinkedHashMap<String, String>();
    wikiCodeToIsoCodeToWikiName.put("fr", isoCodeToWikiName);
    isoCodeToWikiName.put("FR", Pattern.quote("{{=fr=}}"));
    isoCodeToWikiName.put("RU", Pattern.quote("{{=ru=}}"));
    isoCodeToWikiName.put("BG", Pattern.quote("{{=bg=}}"));  // Bulgarian
    isoCodeToWikiName.put("EN", Pattern.quote("{{=en=}}"));
    //isoCodeToWikiName.put("", Pattern.quote("{{=sl=}}"));
    isoCodeToWikiName.put("LA", Pattern.quote("{{=la=}}"));
    isoCodeToWikiName.put("IT", Pattern.quote("{{=it=}}"));
    isoCodeToWikiName.put("EO", Pattern.quote("{{=eo=}}"));
    isoCodeToWikiName.put("CS", Pattern.quote("{{=cs=}}"));  // Czech
    isoCodeToWikiName.put("NL", Pattern.quote("{{=nl=}}"));  // Dutch
    //isoCodeToWikiName.put("", Pattern.quote("{{=mg=}}"));
    //isoCodeToWikiName.put("", Pattern.quote("{{=hsb=}}"));
    isoCodeToWikiName.put("ZH", Pattern.quote("{{=zh=}}"));
    isoCodeToWikiName.put("JA", Pattern.quote("{{=ja=}}"));
    isoCodeToWikiName.put("DE", Pattern.quote("{{=de=}}"));
    isoCodeToWikiName.put("IS", Pattern.quote("{{=is=}}"));  // Icelandic
    isoCodeToWikiName.put("ES", Pattern.quote("{{=es=}}"));
    isoCodeToWikiName.put("UK", Pattern.quote("{{=uk=}}"));

    // egrep -o '= *\{\{-[a-z]+-\}\} *=' itwiktionary-pages-articles.xml | sort | uniq -c | sort -n
    isoCodeToWikiName = new LinkedHashMap<String, String>();
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
    isoCodeToWikiName.put("LV", Pattern.quote("{{-la-}}"));
    isoCodeToWikiName.put("HU", Pattern.quote("{{-hu-}}"));
    isoCodeToWikiName.put("PL", Pattern.quote("{{-pl-}}"));
    isoCodeToWikiName.put("EL", Pattern.quote("{{-grc-}}"));
    isoCodeToWikiName.put("SV", Pattern.quote("{{-sv-}}"));

  }
  
}

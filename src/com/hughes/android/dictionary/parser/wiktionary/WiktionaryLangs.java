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

import com.hughes.android.dictionary.engine.Language;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class WiktionaryLangs {
  
  public static final Map<String,String> isoCodeToEnWikiName = new LinkedHashMap<String,String>();
  static {
    isoCodeToEnWikiName.put("AF", "Afrikaans");
    isoCodeToEnWikiName.put("SQ", "Albanian");
    isoCodeToEnWikiName.put("AR", "Arabic");
    isoCodeToEnWikiName.put("HY", "Armenian");
    isoCodeToEnWikiName.put("BE", "Belarusian");
    isoCodeToEnWikiName.put("BN", "Bengali");
    isoCodeToEnWikiName.put("BS", "Bosnian");
    isoCodeToEnWikiName.put("BG", "Bulgarian");
    isoCodeToEnWikiName.put("MY", "Burmese");
    isoCodeToEnWikiName.put("yue", "Cantonese");
    isoCodeToEnWikiName.put("CA", "Catalan");
    isoCodeToEnWikiName.put("HR", "Croatian");
    isoCodeToEnWikiName.put("CS", "Czech");
    isoCodeToEnWikiName.put("ZH", "Chinese|Mandarin");
    isoCodeToEnWikiName.put("DA", "Danish");
    isoCodeToEnWikiName.put("NL", "Dutch");
    isoCodeToEnWikiName.put("EN", "English");
    isoCodeToEnWikiName.put("EO", "Esperanto");
    isoCodeToEnWikiName.put("ET", "Estonian");
    isoCodeToEnWikiName.put("FI", "Finnish");
    isoCodeToEnWikiName.put("FR", "French");
    isoCodeToEnWikiName.put("DE", "German");
    isoCodeToEnWikiName.put("EL", "Greek");
    isoCodeToEnWikiName.put("grc", "Ancient Greek");
    isoCodeToEnWikiName.put("haw", "Hawaiian");
    isoCodeToEnWikiName.put("HE", "Hebrew");
    isoCodeToEnWikiName.put("HI", "Hindi");
    isoCodeToEnWikiName.put("HU", "Hungarian");
    isoCodeToEnWikiName.put("IS", "Icelandic");
    isoCodeToEnWikiName.put("ID", "Indonesian");
    isoCodeToEnWikiName.put("GA", "Irish");
    isoCodeToEnWikiName.put("GD", "Gaelic");
    isoCodeToEnWikiName.put("IT", "Italian");
    isoCodeToEnWikiName.put("LA", "Latin");
    isoCodeToEnWikiName.put("LV", "Latvian");
    isoCodeToEnWikiName.put("LT", "Lithuanian");
    isoCodeToEnWikiName.put("JA", "Japanese");
    isoCodeToEnWikiName.put("KO", "Korean");
    isoCodeToEnWikiName.put("KU", "Kurdish");
    isoCodeToEnWikiName.put("LO", "Lao");
    isoCodeToEnWikiName.put("MS", "Malay");
    isoCodeToEnWikiName.put("ML", "Malayalam");
    isoCodeToEnWikiName.put("MI", "Maori");
    isoCodeToEnWikiName.put("MN", "Mongolian");
    isoCodeToEnWikiName.put("NE", "Nepali");
    isoCodeToEnWikiName.put("NO", "Norwegian");
    isoCodeToEnWikiName.put("FA", "Persian");
    isoCodeToEnWikiName.put("PL", "Polish");
    isoCodeToEnWikiName.put("PT", "Portuguese");
    isoCodeToEnWikiName.put("PA", "Punjabi");
    isoCodeToEnWikiName.put("RO", "Romanian");
    isoCodeToEnWikiName.put("RU", "Russian");
    isoCodeToEnWikiName.put("SA", "Sanskrit");
    isoCodeToEnWikiName.put("SR", "Serbian");
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
    isoCodeToEnWikiName.put("MR", "Burmese");
    isoCodeToEnWikiName.put("FO", "Faroese");
    isoCodeToEnWikiName.put("GL", "Galician");
    isoCodeToEnWikiName.put("KA", "Georgian");
    isoCodeToEnWikiName.put("HT", "Haitian Creole");
    isoCodeToEnWikiName.put("LB", "Luxembourgish");
    isoCodeToEnWikiName.put("MK", "Macedonian");
    
    assert Language.isoCodeToResources.keySet().containsAll(isoCodeToEnWikiName.keySet());
  }

  public static final Map<String,Map<String,String>> wikiCodeToIsoCodeToWikiName = new LinkedHashMap<String, Map<String,String>>();
  static {
    // en
    wikiCodeToIsoCodeToWikiName.put("en", isoCodeToEnWikiName);
    
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
    isoCodeToWikiName.put("LA", Pattern.quote("{{-la-}}"));
    isoCodeToWikiName.put("HU", Pattern.quote("{{-hu-}}"));
    isoCodeToWikiName.put("EL", Pattern.quote("{{-grc-}}"));
    isoCodeToWikiName.put("SV", Pattern.quote("{{-sv-}}"));

  }
  
}

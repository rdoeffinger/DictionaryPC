#!/bin/sh
DE_DICTS=true
#DE_DICTS=false
EN_DICTS=true
#EN_DICTS=false
FR_DICTS=true
#FR_DICTS=false
IT_DICTS=true
#IT_DICTS=false
# Spanish is unfortunately not yet working
SINGLE_DICTS="en de fr it"
#SINGLE_DICTS=""

#./run.sh --lang1=EN --dictOut=test --dictInfo=test --input0=data/inputs/wikiSplit/en/EN.data  --input0Name=enwikitionary --input0Format=enwiktionary --input0LangPattern=English --input0LangCodePattern=en --input0EnIndex=1 --input0WiktionaryType=EnEnglish

if $EN_DICTS; then
# Note: using input1 seems to hang for ZH currently!
while read langcode langname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')
test "$lang" = "CY" && lang=CI
test "$lang" = "CMN" && lang=cmn
test "$lang" = "GRC" && lang=grc

reverse_dicts=""
if test "$lang" = "DE" -o "$lang" = "FR" -o "$lang" = "IT" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/EN.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
./run.sh --lang1=EN --lang2=$lang --lang1Stoplist=data/inputs/stoplists/en.txt $stoplist --dictOut=data/outputs/EN-${lang}.quickdic --dictInfo="(EN)Wiktionary-based EN-$lang dictionary." --input0=data/inputs/wikiSplit/en/${lang}.data  --input0Name=enwikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=enwiktionary --input1LangPattern=${langname} --input1LangCodePattern=${langcode} --input1EnIndex=1 --input1WiktionaryType=EnToTranslation --input2=data/inputs/wikiSplit/en/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=enwikitionary --input2WiktionaryLang=EN --input2TitleIndex=2 --input2WebUrlTemplate=http://en.wiktionary.org/wiki/%s $reverse_dicts
rm -f data/outputs/EN-${lang}.quickdic.v006.zip
7z a -mx=9 data/outputs/EN-${lang}.quickdic.v006.zip ./data/outputs/EN-${lang}.quickdic

done < EN-foreign-dictlist.txt
fi

# EnEnglish only makes the dictionary cluttered
#./run.sh --lang1=EN --lang1Stoplist=data/inputs/stoplists/en.txt --dictOut=data/outputs/EN.quickdic --dictInfo="Wiktionary-based EN dictionary." --input0=data/inputs/wikiSplit/en/EN.data  --input0Name=enwikitionary --input0Format=enwiktionary --input0LangPattern=English --input0LangCodePattern=en --input0EnIndex=1 --input0WiktionaryType=EnEnglish --input2=data/inputs/wikiSplit/en/EN.data --input2Format=WholeSectionToHtmlParser --input2Name=enwikitionary --input2WiktionaryLang=EN --input2TitleIndex=1 --input2WebUrlTemplate=http://en.wiktionary.org/wiki/%s
#rm -f data/outputs/EN.quickdic.v006.zip
#7z a -mx=9 data/outputs/EN.quickdic.v006.zip ./data/outputs/EN.quickdic

for langcode in $SINGLE_DICTS ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')
./run.sh --lang1=$lang --lang1Stoplist=data/inputs/stoplists/${langcode}.txt --dictOut=data/outputs/${lang}.quickdic --dictInfo="Wiktionary-based ${lang} dictionary." --input1=data/inputs/wikiSplit/${langcode}/${lang}.data --input1Format=WholeSectionToHtmlParser --input1Name=${langcode}wikitionary --input1WiktionaryLang=$lang --input1TitleIndex=1 --input1WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s
rm -f data/outputs/${lang}.quickdic.v006.zip
7z a -mx=9 data/outputs/${lang}.quickdic.v006.zip ./data/outputs/${lang}.quickdic

done

if $DE_DICTS; then
while read langcode langname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')

reverse_dicts=""
if test "$lang" = "FR" -o "$lang" = "IT" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/DE.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
test -e data/inputs/wikiSplit/de/${lang}.data && input0="--input0=data/inputs/wikiSplit/de/${lang}.data --input0Name=dewikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign"
test -e data/inputs/wikiSplit/de/${lang}.data && input2="--input2=data/inputs/wikiSplit/de/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=dewikitionary --input2WiktionaryLang=DE --input2TitleIndex=2 --input2WebUrlTemplate=http://de.wiktionary.org/wiki/%s"
./run.sh --lang1=DE --lang2=$lang --lang1Stoplist=data/inputs/stoplists/de.txt $stoplist --dictOut=data/outputs/DE-${lang}.quickdic --dictInfo="(DE)Wiktionary-based DE-$lang dictionary." $input0 --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=EnTranslationToTranslation --input1LangPattern1=de --input1LangPattern2=${langcode} $input2 $reverse_dicts
rm -f data/outputs/DE-${lang}.quickdic.v006.zip
7z a -mx=9 data/outputs/DE-${lang}.quickdic.v006.zip ./data/outputs/DE-${lang}.quickdic

done < DE-foreign-dictlist.txt
fi

if $FR_DICTS; then
while read langcode langname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')

reverse_dicts=""
if test "$lang" = "DE" -o "$lang" = "IT" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/FR.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
./run.sh --lang1=FR --lang2=$lang --lang1Stoplist=data/inputs/stoplists/fr.txt $stoplist --dictOut=data/outputs/FR-${lang}.quickdic --dictInfo="(FR)Wiktionary-based FR-$lang dictionary." --input0=data/inputs/wikiSplit/fr/${lang}.data --input0Name=frwikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=EnTranslationToTranslation --input1LangPattern1=fr --input1LangPattern2=${langcode} --input2=data/inputs/wikiSplit/fr/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=frwikitionary --input2WiktionaryLang=FR --input2TitleIndex=2 --input2WebUrlTemplate=http://fr.wiktionary.org/wiki/%s $reverse_dicts
rm -f data/outputs/FR-${lang}.quickdic.v006.zip
7z a -mx=9 data/outputs/FR-${lang}.quickdic.v006.zip ./data/outputs/FR-${lang}.quickdic

done < FR-foreign-dictlist.txt
fi

if $IT_DICTS; then
while read langcode langname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')

reverse_dicts=""
if test "$lang" = "FR" -o "$lang" = "DE" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/IT.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
./run.sh --lang1=IT --lang2=$lang --lang1Stoplist=data/inputs/stoplists/it.txt $stoplist --dictOut=data/outputs/IT-${lang}.quickdic --dictInfo="(IT)Wiktionary-based IT-$lang dictionary." --input0=data/inputs/wikiSplit/it/${lang}.data --input0Name=itwikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=EnTranslationToTranslation --input1LangPattern1=it --input1LangPattern2=${langcode} --input2=data/inputs/wikiSplit/it/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=itwikitionary --input2WiktionaryLang=IT --input2TitleIndex=2 --input2WebUrlTemplate=http://it.wiktionary.org/wiki/%s $reverse_dicts
rm -f data/outputs/IT-${lang}.quickdic.v006.zip
7z a -mx=9 data/outputs/IT-${lang}.quickdic.v006.zip ./data/outputs/IT-${lang}.quickdic

done < IT-foreign-dictlist.txt
fi

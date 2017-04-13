#!/bin/sh
DE_DICTS=true
#DE_DICTS=false
EN_DICTS=true
#EN_DICTS=false
FR_DICTS=true
#FR_DICTS=false
IT_DICTS=true
#IT_DICTS=false
EN_TRANS_DICTS=true
#EN_TRANS_DICTS=false
SINGLE_DICTS="en de fr it es pt"
#SINGLE_DICTS=""

VERSION=v007

#./run.sh --lang1=EN --dictOut=test --dictInfo=test --input0=data/inputs/wikiSplit/en/EN.data  --input0Name=enwikitionary --input0Format=enwiktionary --input0LangPattern=English --input0LangCodePattern=en --input0EnIndex=1 --input0WiktionaryType=EnEnglish

if $EN_DICTS; then
# Note: using input1 seems to hang for ZH currently!
while read langcode langname enlangname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')
test "$lang" = "CY" && lang=CI
test "$lang" = "CMN" && lang=cmn
test "$lang" = "GRC" && lang=grc
test "$lang" = "HAW" && lang=haw
test "$lang" = "SCN" && lang=scn
test "$lang" = "YUE" && lang=yue
test "$lang" = "PDC" && lang=pdc
test "$lang" = "CU" && lang=cu

reverse_dicts=""
if test "$lang" = "DE" -o "$lang" = "FR" -o "$lang" = "IT" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/EN.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
#reverse_dicts="$reverse_dicts --input4=data/inputs/wikiSplit/$langcode/EN.data --input4Name=${langcode}wikitionary --input4Format=enwiktionary --input4LangPattern=${enlangname} --input4LangCodePattern=en --input4EnIndex=1 --input4WiktionaryType=EnForeign"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
./run.sh --lang1=EN --lang2=$lang --lang1Stoplist=data/inputs/stoplists/en.txt $stoplist --dictOut=data/outputs/EN-${lang}.quickdic --dictInfo="(EN)Wiktionary-based EN-$lang dictionary." --input0=data/inputs/wikiSplit/en/${lang}.data  --input0Name=enwikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=enwiktionary --input1LangPattern=${langname} --input1LangCodePattern=${langcode} --input1EnIndex=1 --input1WiktionaryType=EnToTranslation --input2=data/inputs/wikiSplit/en/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=enwikitionary --input2WiktionaryLang=EN --input2TitleIndex=2 --input2WebUrlTemplate=http://en.wiktionary.org/wiki/%s $reverse_dicts
rm -f data/outputs/EN-${lang}.quickdic.${VERSION}.zip
7z a -mx=9 data/outputs/EN-${lang}.quickdic.${VERSION}.zip ./data/outputs/EN-${lang}.quickdic

done < EN-foreign-dictlist.txt
fi

# EnEnglish only makes the dictionary cluttered
#./run.sh --lang1=EN --lang1Stoplist=data/inputs/stoplists/en.txt --dictOut=data/outputs/EN.quickdic --dictInfo="Wiktionary-based EN dictionary." --input0=data/inputs/wikiSplit/en/EN.data  --input0Name=enwikitionary --input0Format=enwiktionary --input0LangPattern=English --input0LangCodePattern=en --input0EnIndex=1 --input0WiktionaryType=EnEnglish --input2=data/inputs/wikiSplit/en/EN.data --input2Format=WholeSectionToHtmlParser --input2Name=enwikitionary --input2WiktionaryLang=EN --input2TitleIndex=1 --input2WebUrlTemplate=http://en.wiktionary.org/wiki/%s
#rm -f data/outputs/EN.quickdic.${VERSION}.zip
#7z a -mx=9 data/outputs/EN.quickdic.${VERSION}.zip ./data/outputs/EN.quickdic

for langcode in $SINGLE_DICTS ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')
./run.sh --lang1=$lang --lang1Stoplist=data/inputs/stoplists/${langcode}.txt --dictOut=data/outputs/${lang}.quickdic --dictInfo="Wiktionary-based ${lang} dictionary." --input1=data/inputs/wikiSplit/${langcode}/${lang}.data --input1Format=WholeSectionToHtmlParser --input1Name=${langcode}wikitionary --input1WiktionaryLang=$lang --input1TitleIndex=1 --input1WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s
rm -f data/outputs/${lang}.quickdic.${VERSION}.zip
7z a -mx=9 data/outputs/${lang}.quickdic.${VERSION}.zip ./data/outputs/${lang}.quickdic

done

if $DE_DICTS; then
while read langcode langname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')
test "$lang" = "CY" && lang=CI
test "$lang" = "CMN" && lang=cmn
test "$lang" = "GRC" && lang=grc
test "$lang" = "HAW" && lang=haw
test "$lang" = "YUE" && lang=yue

reverse_dicts=""
if test "$lang" = "FR" -o "$lang" = "IT" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/DE.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
input0=""
test -e data/inputs/wikiSplit/de/${lang}.data && input0="--input0=data/inputs/wikiSplit/de/${lang}.data --input0Name=dewikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign"
input2=""
test -e data/inputs/wikiSplit/de/${lang}.data && input2="--input2=data/inputs/wikiSplit/de/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=dewikitionary --input2WiktionaryLang=DE --input2TitleIndex=2 --input2WebUrlTemplate=http://de.wiktionary.org/wiki/%s"
./run.sh --lang1=DE --lang2=$lang --lang1Stoplist=data/inputs/stoplists/de.txt $stoplist --dictOut=data/outputs/DE-${lang}.quickdic --dictInfo="(DE)Wiktionary-based DE-$lang dictionary." $input0 --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=EnTranslationToTranslation --input1LangPattern1=de --input1LangPattern2=${langcode} $input2 $reverse_dicts
rm -f data/outputs/DE-${lang}.quickdic.${VERSION}.zip
7z a -mx=9 data/outputs/DE-${lang}.quickdic.${VERSION}.zip ./data/outputs/DE-${lang}.quickdic

done < DE-foreign-dictlist.txt
fi

if $FR_DICTS; then
while read langcode langname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')
test "$lang" = "CY" && lang=CI
test "$lang" = "CMN" && lang=cmn
test "$lang" = "GRC" && lang=grc
test "$lang" = "HAW" && lang=haw
test "$lang" = "YUE" && lang=yue

reverse_dicts=""
if test "$lang" = "DE" -o "$lang" = "IT" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/FR.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
./run.sh --lang1=FR --lang2=$lang --lang1Stoplist=data/inputs/stoplists/fr.txt $stoplist --dictOut=data/outputs/FR-${lang}.quickdic --dictInfo="(FR)Wiktionary-based FR-$lang dictionary." --input0=data/inputs/wikiSplit/fr/${lang}.data --input0Name=frwikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=EnTranslationToTranslation --input1LangPattern1=fr --input1LangPattern2=${langcode} --input2=data/inputs/wikiSplit/fr/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=frwikitionary --input2WiktionaryLang=FR --input2TitleIndex=2 --input2WebUrlTemplate=http://fr.wiktionary.org/wiki/%s $reverse_dicts
rm -f data/outputs/FR-${lang}.quickdic.${VERSION}.zip
7z a -mx=9 data/outputs/FR-${lang}.quickdic.${VERSION}.zip ./data/outputs/FR-${lang}.quickdic

done < FR-foreign-dictlist.txt
fi

if $IT_DICTS; then
while read langcode langname ; do
lang=$(echo $langcode | tr '[a-z]' '[A-Z]')
test "$lang" = "CY" && lang=CI
test "$lang" = "CMN" && lang=cmn
test "$lang" = "GRC" && lang=grc
test "$lang" = "HAW" && lang=haw
test "$lang" = "YUE" && lang=yue

reverse_dicts=""
if test "$lang" = "FR" -o "$lang" = "DE" ; then
reverse_dicts="--input3=data/inputs/wikiSplit/$langcode/IT.data --input3Format=WholeSectionToHtmlParser --input3Name=${langcode}wikitionary --input3WiktionaryLang=$lang --input3TitleIndex=1 --input3WebUrlTemplate=http://${langcode}.wiktionary.org/wiki/%s"
fi

stoplist=""
test -e data/inputs/stoplists/${langcode}.txt && stoplist="--lang2Stoplist=data/inputs/stoplists/${langcode}.txt"
./run.sh --lang1=IT --lang2=$lang --lang1Stoplist=data/inputs/stoplists/it.txt $stoplist --dictOut=data/outputs/IT-${lang}.quickdic --dictInfo="(IT)Wiktionary-based IT-$lang dictionary." --input0=data/inputs/wikiSplit/it/${lang}.data --input0Name=itwikitionary --input0Format=enwiktionary --input0LangPattern=${langname} --input0LangCodePattern=${langcode} --input0EnIndex=1 --input0WiktionaryType=EnForeign --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=EnTranslationToTranslation --input1LangPattern1=it --input1LangPattern2=${langcode} --input2=data/inputs/wikiSplit/it/${lang}.data --input2Format=WholeSectionToHtmlParser --input2Name=itwikitionary --input2WiktionaryLang=IT --input2TitleIndex=2 --input2WebUrlTemplate=http://it.wiktionary.org/wiki/%s $reverse_dicts
rm -f data/outputs/IT-${lang}.quickdic.${VERSION}.zip
7z a -mx=9 data/outputs/IT-${lang}.quickdic.${VERSION}.zip ./data/outputs/IT-${lang}.quickdic

done < IT-foreign-dictlist.txt
fi

if $EN_TRANS_DICTS; then
while read langcode1 langname1 langcode2 langname2 ; do
lang1=$(echo $langcode1 | tr '[a-z]' '[A-Z]')
lang2=$(echo $langcode2 | tr '[a-z]' '[A-Z]')
test "$lang1" = "CY" && lang1=CI
test "$lang1" = "CMN" && lang1=cmn
test "$lang1" = "GRC" && lang1=grc
test "$lang1" = "HAW" && lang1=haw
test "$lang1" = "YUE" && lang1=yue
test "$lang2" = "CY" && lang2=CI
test "$lang2" = "CMN" && lang2=cmn
test "$lang2" = "GRC" && lang2=grc
test "$lang2" = "HAW" && lang2=haw
test "$lang2" = "YUE" && lang2=yue
stoplist1=""
stoplist2=""
test -e data/inputs/stoplists/${langcode1}.txt && stoplist1="--lang1Stoplist=data/inputs/stoplists/${langcode1}.txt"
test -e data/inputs/stoplists/${langcode2}.txt && stoplist2="--lang2Stoplist=data/inputs/stoplists/${langcode2}.txt"
./run.sh --lang1=$lang1 --lang2=$lang2 $stoplist1 $stoplist2 --dictOut=data/outputs/${lang1}-${lang2}.quickdic --dictInfo="(EN)Wiktionary-based ${lang1}-${lang2} dictionary." --input1=data/inputs/wikiSplit/en/EN.data --input1Name=enwikitionary --input1Format=EnTranslationToTranslation --input1LangPattern1=${langcode1} --input1LangPattern2=${langcode2}
rm -f data/outputs/${lang1}-${lang2}.quickdic.${VERSION}.zip
7z a -mx=9 data/outputs/${lang1}-${lang2}.quickdic.${VERSION}.zip ./data/outputs/${lang1}-${lang2}.quickdic
done < EN-trans-dictlist.txt
fi

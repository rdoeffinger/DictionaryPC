#!/bin/bash -e

echo "Downloading from: http://dumps.wikimedia.org/enwiktionary/"
WIKI=enwiktionary-20111224-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/enwiktionary/20111224/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/

echo "Downloading from: http://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/"
CHEMNITZ=de-en.txt
curl --remote-name http://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/${CHEMNITZ}.gz
gunzip ${CHEMNITZ}.gz
mv ${CHEMNITZ} inputs/

echo "Done.  Now run WiktionarySplitter to spit apart enwiktionary."
#!/bin/bash -e

OLD_DIR=`pwd`
DIR=`dirname $0`
cd $DIR

echo "Note that unzipping is slow."

L=en
echo "Downloading from: https://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-latest-pages-articles.xml
curl -L --remote-name https://dumps.wikimedia.org/${L}wiktionary/latest/${WIKI}.bz2
mv ${WIKI}.bz2 inputs/${L}wiktionary-pages-articles.xml.bz2

echo "Downloading from: https://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/"
CHEMNITZ=de-en.txt
curl -L --remote-name https://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/${CHEMNITZ}.gz
mv ${CHEMNITZ}.gz inputs/de-en_chemnitz.txt.gz

L=fr
echo "Downloading from: https://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-latest-pages-articles.xml
curl -L --remote-name https://dumps.wikimedia.org/${L}wiktionary/latest/${WIKI}.bz2
mv ${WIKI}.bz2 inputs/${L}wiktionary-pages-articles.xml.bz2

L=it
echo "Downloading from: https://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-latest-pages-articles.xml
curl -L --remote-name https://dumps.wikimedia.org/${L}wiktionary/latest/${WIKI}.bz2
mv ${WIKI}.bz2 inputs/${L}wiktionary-pages-articles.xml.bz2

L=de
echo "Downloading from: https://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-latest-pages-articles.xml
curl -L --remote-name https://dumps.wikimedia.org/${L}wiktionary/latest/${WIKI}.bz2
mv ${WIKI}.bz2 inputs/${L}wiktionary-pages-articles.xml.bz2

L=es
echo "Downloading from: https://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-latest-pages-articles.xml
curl -L --remote-name https://dumps.wikimedia.org/${L}wiktionary/latest/${WIKI}.bz2
mv ${WIKI}.bz2 inputs/${L}wiktionary-pages-articles.xml.bz2

L=pt
echo "Downloading from: https://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-latest-pages-articles.xml
curl -L --remote-name https://dumps.wikimedia.org/${L}wiktionary/latest/${WIKI}.bz2
mv ${WIKI}.bz2 inputs/${L}wiktionary-pages-articles.xml.bz2

echo "Done.  Now run WiktionarySplitter to split apart enwiktionary."

cd $OLD_DIR

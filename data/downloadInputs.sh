#!/bin/bash -e

OLD_DIR=`pwd`
DIR=`dirname $0`
cd $DIR

echo "Note that unzipping is slow."

L=en
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120930-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120930/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

echo "Downloading from: http://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/"
CHEMNITZ=de-en.txt
curl --remote-name http://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/${CHEMNITZ}.gz
gunzip ${CHEMNITZ}.gz
mv ${CHEMNITZ} inputs/

L=fr
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120926-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120926/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

L=it
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120926-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120926/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

L=de
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120928-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120928/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

L=es
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120924-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120924/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

echo "Done.  Now run WiktionarySplitter to spit apart enwiktionary."

cd $OLD_DIR

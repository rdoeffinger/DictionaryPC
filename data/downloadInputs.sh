#!/bin/bash -e

OLD_DIR=`pwd`
DIR=`dirname $0`

cd $DIR

echo "Downloading from: http://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/"
CHEMNITZ=de-en.txt
#curl --remote-name http://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/${CHEMNITZ}.gz
#gunzip ${CHEMNITZ}.gz
#mv ${CHEMNITZ} inputs/

echo "Note that unzipping is slow."

L=en
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120109-pages-articles.xml
#curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120109/${WIKI}.bz2
#bunzip2 ${WIKI}.bz2
#mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

L=fr
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120106-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120106/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

L=it
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120110-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120110/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

L=de
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120111-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120111/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

L=es
echo "Downloading from: http://dumps.wikimedia.org/${L}wiktionary/"
WIKI=${L}wiktionary-20120108-pages-articles.xml
curl --remote-name http://dumps.wikimedia.org/${L}wiktionary/20120108/${WIKI}.bz2
bunzip2 ${WIKI}.bz2
mv ${WIKI} inputs/${L}wiktionary-pages-articles.xml

echo "Done.  Now run WiktionarySplitter to spit apart enwiktionary."

cd $OLD_DIR

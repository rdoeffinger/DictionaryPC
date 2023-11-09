#!/bin/bash -e

this_dir=$(dirname "${0}")
input_dir="${this_dir}"/inputs

echo "Downloading dictionary files."

# Downloading wikimedia dump files
for l in en fr it de es pt; do
    download_url="http://dumps.wikimedia.org/${l}wiktionary/latest/${l}wiktionary-latest-pages-articles.xml.bz2"
    echo "Downloading data for language '${l}' from: ${download_url}"
    wget --no-verbose --show-progress "${download_url}" --output-document="${input_dir}/${l}wiktionary-pages-articles.xml.bz2"
done

# Downloading de-en from chemnitz.de
download_url='http://ftp.tu-chemnitz.de/pub/Local/urz/ding/de-en-devel/de-en.txt.gz'
echo "Downloading from: ${download_url}"
wget --no-verbose --show-progress "${download_url}" --output-document="${input_dir}/de-en_chemnitz.txt.gz"

echo "Done. Now run './WiktionarySplitter.sh' to split apart wiktionary dump files."

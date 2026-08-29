set -e
rm -rf data/outputsv6
mkdir data/outputsv6
# pigz -9 is about 20x faster but 2% larger than 7x -mx9
# pigz -11 is slightly smaller but only 60% faster and
# much, much higher CPU usage (multithreaded)
for i in data/outputs/*.quickdic ; do
    o=data/outputsv6/$(basename "$i")
    ./convert_to_v6.sh "$i" "$o"
    #7z a -mx=9 "$o".v006.zip ./"$o"
    pigz -v -K -9 -c "$o" > "$o".v006.zip
    rm "$o"
    # skipHtml makes no sense for single-language dictionaries
    if echo "$o" | grep -q '-' ; then
        if ./convert_to_v6.sh "$i" "$o" skipHtmlOpt ; then
            #7z a -mx=9 "$o".small.v006.zip ./"$o"
            pigz -v -K -9 -c "$o" > "$o".small.v006.zip
            rm "$o"
        elif [ $? -ne 3 ] ; then
            # Check for magic 3 indicating "no HTML entries in dictionary"
            echo "Converting dictionary failed!"
            exit 1
        fi
    fi
done

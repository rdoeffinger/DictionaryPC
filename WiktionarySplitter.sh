# Run after downloading (data/downloadInputs.sh) to generate
# per-language data files from enwiktionary.
/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java -classpath src:../Util/src/:../Dictionary/src/:/usr/share/java/com.ibm.icu.jar:/usr/share/java/xercesImpl.jar com.hughes.android.dictionary.engine.WiktionarySplitter "$@"

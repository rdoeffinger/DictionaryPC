# Run to update ..//Dictionary/res/raw/dictionary_info.txt to reference
# all dictionaries in /data/outputs (needs to contain both zip and uncompressed files).
CLASS=CheckDictionariesMain
JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
test -x "$JAVA" || JAVA=java
$JAVA -classpath bin/:/usr/share/java/com.ibm.icu.jar:/usr/share/java/xercesImpl.jar com.hughes.android.dictionary.engine.$CLASS "$@"

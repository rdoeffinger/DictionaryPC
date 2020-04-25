# Run to update ..//Dictionary/res/raw/dictionary_info.txt to reference
# all dictionaries in /data/outputs (needs to contain both zip and uncompressed files).
RUNNER=./DictionaryPC
if ! test -x "$RUNNER" ; then
  JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
  test -x "$JAVA" || JAVA=java
  RUNNER="$JAVA -classpath bin/:/usr/share/java/com.ibm.icu.jar com.hughes.android.dictionary.engine.Runner"
fi
$RUNNER CheckDictionariesMain "$@"

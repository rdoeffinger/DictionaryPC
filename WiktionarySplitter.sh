# Run after downloading (data/downloadInputs.sh) to generate
# per-language data files from enwiktionary.
RUNNER=./DictionaryPC
if ! test -x "$RUNNER" ; then
  ICU4J=/usr/share/java/icu4j-49.1.jar
  test -r "$ICU4J" || ICU4J=/usr/share/icu4j-55/lib/icu4j.jar
  COMMONS_COMPRESS=/usr/share/java/commons-compress.jar
  COMMONS_IO=/usr/share/java/commons-io.jar
  JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
  test -x "$JAVA" || JAVA=java
  RUNNER="$JAVA -Xmx4096m -Xverify:none -classpath bin/:$ICU4J:$COMMONS_COMPRESS:$COMMONS_IO com.hughes.android.dictionary.engine.Runner"
fi
$RUNNER WiktionarySplitter "$@"

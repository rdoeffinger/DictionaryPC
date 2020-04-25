RUNNER=./DictionaryPC
if ! test -x "$RUNNER" ; then
  # -agentlib:hprof=heap=sites,depth=20
  ICU4J=/usr/share/java/icu4j-49.1.jar
  test -r "$ICU4J" || ICU4J=/usr/share/icu4j-55/lib/icu4j.jar
  COMMONS_LANG3=/usr/share/java/commons-lang3.jar
  test -r "$COMMONS_LANG3" || COMMONS_LANG3=/usr/share/commons-lang-3.3/lib/commons-lang.jar
  COMMONS_TEXT=/usr/share/java/commons-text.jar
  COMMONS_COMPRESS=/usr/share/java/commons-compress.jar
  JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
  test -x "$JAVA" || JAVA=java
  RUNNER="$JAVA -Djava.util.logging.config.file=logging.properties -Xmx4096m -classpath bin/:$ICU4J:$COMMONS_LANG3:$COMMONS_TEXT:$COMMONS_COMPRESS com.hughes.android.dictionary.engine.Runner"
fi
$RUNNER DictionaryBuilder "$@"

RUNNER=./DictionaryPC
if ! test -x "$RUNNER" ; then
  JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
  test -x "$JAVA" || JAVA=java
  RUNNER="$JAVA -classpath bin/ com.hughes.android.dictionary.engine.Runner"
fi
$RUNNER ConvertToV6 "$@"

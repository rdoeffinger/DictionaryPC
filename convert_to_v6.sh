JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
test -x "$JAVA" || JAVA=java
"$JAVA" -classpath bin/ com.hughes.android.dictionary.engine.ConvertToV6 "$@"

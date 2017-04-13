# -agentlib:hprof=heap=sites,depth=20
ICU4J=/usr/share/java/icu4j-49.1.jar
test -r "$ICU4J" || ICU4J=/usr/share/icu4j-55/lib/icu4j.jar
XERCES=/usr/share/java/xercesImpl.jar
test -r "$XERCES" || XERCES=/usr/share/xerces-2/lib/xercesImpl.jar
COMMONS=/usr/share/java/commons-lang3.jar
test -r "$COMMONS" || COMMONS=/usr/share/commons-lang-3.3/lib/commons-lang.jar
JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
test -x "$JAVA" || JAVA=java
"$JAVA" -Djava.util.logging.config.file="logging.properties" -Xmx4096m -classpath src:../Util/src/:../Dictionary/src/:"$ICU4J":"$XERCES":"$COMMONS" com.hughes.android.dictionary.engine.DictionaryBuilder "$@"

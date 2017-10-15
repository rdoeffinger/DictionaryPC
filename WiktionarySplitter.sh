# Run after downloading (data/downloadInputs.sh) to generate
# per-language data files from enwiktionary.
ICU4J=/usr/share/java/icu4j-49.1.jar
test -r "$ICU4J" || ICU4J=/usr/share/icu4j-55/lib/icu4j.jar
XERCES=/usr/share/java/xercesImpl.jar
test -r "$XERCES" || XERCES=/usr/share/xerces-2/lib/xercesImpl.jar
COMMONS_COMPRESS=/usr/share/java/commons-compress-1.13.jar
JAVA=/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
test -x "$JAVA" || JAVA=java
"$JAVA" -Xverify:none -classpath src:../Util/src/:../Dictionary/src/:"$ICU4J":"$XERCES":"$COMMONS_COMPRESS" com.hughes.android.dictionary.engine.WiktionarySplitter "$@"

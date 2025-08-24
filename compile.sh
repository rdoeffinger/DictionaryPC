ICU4J=/usr/share/java/icu4j-49.1.jar
test -r "$ICU4J" || ICU4J=/usr/share/icu4j-55/lib/icu4j.jar
test -r "$ICU4J" || ICU4J=$(pwd)/icu4j-55.2.jar
JUNIT=/usr/share/java/junit.jar
test -r "$JUNIT" || JUNIT=/usr/share/junit/lib/junit.jar
test -r "$JUNIT" || JUNIT=$(pwd)/junit-4.13.2.jar
COMMONS=/usr/share/java/commons-text.jar
test -r "$COMMONS" || COMMONS=$(pwd)/commons-text-1.13.1.jar
COMMONS_COMPRESS=/usr/share/java/commons-compress.jar
test -r "$COMMONS_COMPRESS" || COMMONS_COMPRESS=$(pwd)/commons-compress-1.27.1.jar
if [ ! -x ../Dictionary ] ; then
    echo "You need to clone the Dictionary repository (including subprojects) into .., alongside this DictionaryPC clone"
    exit 1
fi
if [ ! -x ../Dictionary/Util ] ; then
    echo "Dictionary/Util directory is missing, did you clone the Dictionary repository without --recursive?"
    exit 1
fi
if [ ! -r "$ICU4J" ] ; then
    echo "ICU4J needs to be installed"
    echo "Download from https://repo1.maven.org/maven2/com/ibm/icu/icu4j/55.2/icu4j-55.2.jar"
    exit 1;
fi
if [ ! -r "$JUNIT" ] ; then
    echo "Junit needs to be installed"
    echo "Download from https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar"
    exit 1;
fi
if [ ! -r "$COMMONS" ] ; then
    echo "commons-lang needs to be installed"
    echo "Download from https://repo1.maven.org/maven2/org/apache/commons/commons-text/1.13.1/commons-text-1.13.1.jar"
    exit 1;
fi
if [ ! -r "$COMMONS_COMPRESS" ] ; then
    echo "commons-compress needs to be installed"
    echo "Download from https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.jar"
    exit 1;
fi
# https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar
# https://repo1.maven.org/maven2/org/tukaani/xz/1.10/xz-1.10.jar
# https://repo1.maven.org/maven2/com/github/luben/zstd-jni/1.5.7-4/zstd-jni-1.5.7-4.jar
mkdir -p bin
# -encoding is just a work around for user that still run systems
# with non-UTF8 locales
javac --release 21 --limit-modules java.xml,java.logging -Xlint:all -encoding UTF-8 -g -d bin/ ../Dictionary/Util/src/com/hughes/util/*.java ../Dictionary/Util/src/com/hughes/util/raf/*.java ../Dictionary/src/com/hughes/android/dictionary/engine/*.java src/com/hughes/util/*.java src/com/hughes/android/dictionary/*.java src/com/hughes/android/dictionary/*/*.java src/com/hughes/android/dictionary/*/*/*.java -classpath "$ICU4J:$JUNIT:$COMMONS:$COMMONS_COMPRESS"
if [ "$?" != "0" ] ; then
    echo "compilation failed, check output above for errors!"
    exit 1;
fi

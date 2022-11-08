ICU4J=/usr/share/java/icu4j-49.1.jar
test -r "$ICU4J" || ICU4J=/usr/share/icu4j-55/lib/icu4j.jar
JUNIT=/usr/share/java/junit.jar
test -r "$JUNIT" || JUNIT=/usr/share/junit/lib/junit.jar
COMMONS=/usr/share/java/commons-text.jar
COMMONS_COMPRESS=/usr/share/java/commons-compress.jar
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
    exit 1;
fi
if [ ! -r "$JUNIT" ] ; then
    echo "Junit needs to be installed"
    exit 1;
fi
if [ ! -r "$COMMONS" ] ; then
    echo "commons-lang needs to be installed"
    exit 1;
fi
if [ ! -r "$COMMONS_COMPRESS" ] ; then
    echo "commons-compress needs to be installed"
    exit 1;
fi
mkdir -p bin
# -encoding is just a work around for user that still run systems
# with non-UTF8 locales
# Limit to Java 11 for compatibility with native-image
javac --source 11 --target 11 --limit-modules java.xml,java.logging -Xlint:all -encoding UTF-8 -g -d bin/ ../Dictionary/Util/src/com/hughes/util/*.java ../Dictionary/Util/src/com/hughes/util/raf/*.java ../Dictionary/src/com/hughes/android/dictionary/DictionaryInfo.java ../Dictionary/src/com/hughes/android/dictionary/engine/*.java ../Dictionary/src/com/hughes/android/dictionary/C.java src/com/hughes/util/*.java src/com/hughes/android/dictionary/*.java src/com/hughes/android/dictionary/*/*.java src/com/hughes/android/dictionary/*/*/*.java -classpath "$ICU4J:$JUNIT:$COMMONS:$COMMONS_COMPRESS"
if [ "$?" != "0" ] ; then
    echo "compilation failed, check output above for errors!"
    exit 1;
fi

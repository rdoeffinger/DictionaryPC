ICU4J=/usr/share/java/icu4j-49.1.jar
test -r "$ICU4J" || ICU4J=/usr/share/icu4j-55/lib/icu4j.jar
JUNIT=/usr/share/java/junit.jar
test -r "$JUNIT" || JUNIT=/usr/share/junit/lib/junit.jar
XERCES=/usr/share/java/xercesImpl.jar
test -r "$XERCES" || XERCES=/usr/share/xerces-2/lib/xercesImpl.jar
COMMONS=/usr/share/java/commons-lang3.jar
test -r "$COMMONS" || COMMONS=/usr/share/commons-lang-3.3/lib/commons-lang.jar
COMMONS_COMPRESS=/usr/share/java/commons-compress.jar
if [ ! -x ../Dictionary ] ; then
    echo "You need to clone the Dictionary repository (including subprojects) into .."
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
if [ ! -r "$XERCES" ] ; then
    echo "Xerces needs to be installed"
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
# -encoding is just a work around for user that still run systems
# with non-UTF8 locales
javac -encoding UTF-8 -g ../Dictionary/Util/src/com/hughes/util/*.java ../Dictionary/Util/src/com/hughes/util/raf/*.java ../Dictionary/src/com/hughes/android/dictionary/DictionaryInfo.java ../Dictionary/src/com/hughes/android/dictionary/engine/*.java ../Dictionary/src/com/hughes/android/dictionary/C.java src/com/hughes/util/*.java src/com/hughes/android/dictionary/*.java src/com/hughes/android/dictionary/*/*.java src/com/hughes/android/dictionary/*/*/*.java -classpath "$ICU4J:$JUNIT:$XERCES:$COMMONS:$COMMONS_COMPRESS"

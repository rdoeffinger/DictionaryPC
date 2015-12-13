# -agentlib:hprof=heap=sites,depth=20
/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java -Xmx4096m -classpath src:../Util/src/:../Dictionary/src/:/usr/share/java/icu4j-49.1.jar:/usr/share/java/xercesImpl.jar:/usr/share/java/commons-lang3.jar com.hughes.android.dictionary.engine.DictionaryBuilder "$@"

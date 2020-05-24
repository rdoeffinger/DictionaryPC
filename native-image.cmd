REM --allow-incomplete-classpath due to missing XZ implementation
%GRAALVM_HOME%/bin/native-image --allow-incomplete-classpath --no-server -H:Name="DictionaryPC" com.hughes.android.dictionary.engine.Runner --no-fallback -cp bin/;commons-compress.jar;commons-text.jar;commons-lang3.jar;icu4j-49.1.jar -H:IncludeResources="com/ibm/icu/.*" -H:ReflectionConfigurationFiles=native-image-reflection.json

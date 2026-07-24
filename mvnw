#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Start Up Batch script
# ----------------------------------------------------------------------------

DIRNAME="$(dirname "$0")"
MAVEN_PROJECTBASEDIR="$DIRNAME"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    mkdir -p "$MAVEN_PROJECTBASEDIR/.mvn/wrapper"
    curl -sH "User-Agent: Mozilla/5.0" -o "$WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
fi

exec java -jar "$WRAPPER_JAR" "$@"

#!/usr/bin/env bash
# Minimal Gradle wrapper bootstrap for Lavallette Tides.
# Prefers official gradle-wrapper.jar when present; otherwise restores it then runs.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
if [[ ! -f "$JAR" ]]; then
  if [[ -x "$ROOT/scripts/fetch-wrapper-jar.sh" ]]; then
    "$ROOT/scripts/fetch-wrapper-jar.sh"
  else
    echo "Missing $JAR — run scripts/fetch-wrapper-jar.sh" >&2
    exit 1
  fi
fi
# Delegate to the standard Gradle wrapper main class using the local jar.
APP_HOME="$ROOT"
APP_BASE_NAME="gradlew"
CLASSPATH="$JAR"
if [[ -n "${JAVA_HOME:-}" ]]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
eval "set -- $DEFAULT_JVM_OPTS ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \"-Dorg.gradle.appname=$APP_BASE_NAME\" -classpath \"$CLASSPATH\" org.gradle.wrapper.GradleWrapperMain \"\$@\""
exec "$JAVACMD" "$@"

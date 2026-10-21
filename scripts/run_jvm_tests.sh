#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Local JVM verification harness for OmniCalc (sandbox-only).
#
# The sandbox cannot reach Maven Central / services.gradle.org, so Gradle
# cannot run here. This script compiles the pure-Kotlin modules (core:common,
# core:math) plus their unit tests with the locally provisioned kotlinc, using
# a minimal JUnit4-compatible shim (scripts/local_junit), then executes every
# @org.junit.Test method.
#
# The SAME test sources run against real JUnit 4 in Gradle/CI
# (see .github/workflows/ci.yml and docs/TESTING.md).
#
# Prerequisites (provisioned under /home/user/tools in this environment):
#   JAVA_HOME  -> a JDK (17+)
#   KOTLINC    -> path to kotlinc launcher (default: /home/user/tools/kotlin/bin/kotlinc)
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KOTLINC="${KOTLINC:-/home/user/tools/kotlin/bin/kotlinc}"
OUT="${ROOT}/build/local-tests"
rm -rf "$OUT"
mkdir -p "$OUT/main" "$OUT/tests" "$OUT/shim"

if [ ! -x "$KOTLINC" ] && [ ! -f "$KOTLINC" ]; then
  echo "kotlinc not found at $KOTLINC" >&2
  exit 2
fi

echo "== [1/4] Compiling shim + main sources =="
"$KOTLINC" -jvm-target 17 -d "$OUT/main" \
  "$ROOT"/scripts/local_junit/org/junit/*.kt \
  "$ROOT"/core/common/src/main/kotlin \
  "$ROOT"/core/math/src/main/kotlin

echo "== [2/4] Compiling test sources =="
"$KOTLINC" -jvm-target 17 -cp "$OUT/main" -d "$OUT/tests" \
  "$ROOT"/scripts/local_junit/LocalTestRunner.kt \
  "$ROOT"/core/common/src/test/kotlin \
  "$ROOT"/core/math/src/test/kotlin

echo "== [3/4] Discovering test classes =="
TEST_CLASSES=$(find "$ROOT/core" -path '*/src/test/kotlin/*' -name '*.kt' \
  | sed -E 's|.*/src/test/kotlin/||; s|/|.|g; s|\.kt$||' \
  | sort)

if [ -z "$TEST_CLASSES" ]; then
  echo "No test classes found." >&2
  exit 3
fi
echo "$TEST_CLASSES" | sed 's/^/  - /'

# kotlin-stdlib must be on the runtime classpath (kotlinc compiles against it
# but does not embed it in the output).
KOTLINC_HOME="$(cd "$(dirname "$KOTLINC")" && pwd)"
STDLIB="$KOTLINC_HOME/../lib/kotlin-stdlib.jar"
if [ ! -f "$STDLIB" ]; then
  STDLIB=$(find "$KOTLINC_HOME/../lib" -name 'kotlin-stdlib.jar' 2>/dev/null | head -1 || true)
fi
if [ -z "${STDLIB:-}" ] || [ ! -f "$STDLIB" ]; then
  echo "kotlin-stdlib.jar not found relative to $KOTLINC" >&2
  exit 2
fi

echo "== [4/4] Running tests =="
# Force UTF-8 console output even under POSIX locales (Bengali digits in
# assertion messages otherwise print as "?").
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dfile.encoding=UTF-8 \
  -cp "$OUT/main:$OUT/tests:$STDLIB" localtest.LocalTestRunnerKt $TEST_CLASSES

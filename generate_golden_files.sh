#!/bin/bash
# Script to generate golden test files for allOf modes

cd "$(dirname "$0")"

# Compile the test classes first
./gradlew compileTestKotlin compileKotlin

# Get the classpath
CLASSPATH="build/classes/kotlin/main:build/classes/kotlin/test"
for jar in $(find ~/.gradle/caches -name "*.jar" 2>/dev/null | grep -E "(gson|snakeyaml|kotlin-stdlib)" | head -20); do
    CLASSPATH="$CLASSPATH:$jar"
done

# Run the manual test to generate outputs
java -cp "$CLASSPATH" test_manual.TestAllOfModesKt > /tmp/allof_output.txt 2>&1

# Extract and save the outputs
echo "Generated outputs in /tmp/allof_output.txt"
cat /tmp/allof_output.txt


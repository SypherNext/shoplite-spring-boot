#!/usr/bin/env sh
# Builds and tests every step folder and every final folder, course by course.
#
# Usage (from anywhere):
#   scripts/verify-all.sh            build and test everything
#   scripts/verify-all.sh --clean    also delete build/ and .gradle/ after each project
#
# Needs JDK 25 (JAVA_HOME pointing at it, or java 25 on the PATH) and a running Docker,
# because the tests start PostgreSQL and Redis with Testcontainers.

set -u

cd "$(dirname "$0")/.." || exit 1

clean=0
if [ "${1:-}" = "--clean" ]; then
    clean=1
fi

passed=0
failed=0
failed_list=""

for course in fundamentals production-apis advanced; do
    for dir in "$course"/steps/*/ "$course"/final/; do
        [ -f "$dir/gradlew" ] || continue
        echo "==> $dir"
        if (cd "$dir" && sh ./gradlew build --console=plain -q); then
            echo "    PASS"
            passed=$((passed + 1))
        else
            echo "    FAIL"
            failed=$((failed + 1))
            failed_list="$failed_list $dir"
        fi
        if [ "$clean" -eq 1 ]; then
            rm -rf "$dir/build" "$dir/.gradle"
        fi
    done
done

echo
echo "Passed: $passed  Failed: $failed"
if [ "$failed" -gt 0 ]; then
    echo "Failed projects:$failed_list"
    exit 1
fi

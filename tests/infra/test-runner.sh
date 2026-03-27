#!/bin/bash
# ============================================
# Infrastructure Test Runner
# Usage: ./test-runner.sh [--quick|--full]
#   --quick: Skip container startup tests (default)
#   --full:  Run all tests including container tests
# ============================================

# Don't use set -e as test failures should not exit the script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# Test mode
TEST_MODE="${1:---quick}"

# ============================================
# Assertion Functions
# ============================================

assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="${3:-Values should be equal}"

    if [ "$expected" = "$actual" ]; then
        return 0
    else
        echo "  Expected: $expected"
        echo "  Actual:   $actual"
        return 1
    fi
}

assert_contains() {
    local haystack="$1"
    local needle="$2"
    local message="${3:-Should contain substring}"

    if [[ "$haystack" == *"$needle"* ]]; then
        return 0
    else
        echo "  String does not contain: $needle"
        return 1
    fi
}

assert_file_exists() {
    local file="$1"
    local message="${2:-File should exist}"

    if [ -f "$file" ]; then
        return 0
    else
        echo "  File not found: $file"
        return 1
    fi
}

assert_dir_exists() {
    local dir="$1"
    local message="${2:-Directory should exist}"

    if [ -d "$dir" ]; then
        return 0
    else
        echo "  Directory not found: $dir"
        return 1
    fi
}

assert_command_success() {
    local cmd="$1"
    local message="${2:-Command should succeed}"

    if eval "$cmd" > /dev/null 2>&1; then
        return 0
    else
        echo "  Command failed: $cmd"
        return 1
    fi
}

assert_command_output_contains() {
    local cmd="$1"
    local needle="$2"
    local message="${3:-Command output should contain string}"

    local output
    output=$(eval "$cmd" 2>&1) || true

    if [[ "$output" == *"$needle"* ]]; then
        return 0
    else
        echo "  Command: $cmd"
        echo "  Expected output to contain: $needle"
        echo "  Actual output: $output"
        return 1
    fi
}

# ============================================
# Test Runner Functions
# ============================================

run_test() {
    local test_name="$1"
    local test_func="$2"

    printf "  %-50s " "$test_name"

    # Run test function and capture result
    local result=0
    $test_func || result=$?

    if [ $result -eq 0 ]; then
        echo -e "${GREEN}PASS${NC}"
        ((TESTS_PASSED++)) || true
    else
        echo -e "${RED}FAIL${NC}"
        ((TESTS_FAILED++)) || true
    fi
}

skip_test() {
    local test_name="$1"
    local reason="${2:-Skipped}"

    printf "  %-50s " "$test_name"
    echo -e "${YELLOW}SKIP${NC} ($reason)"
    ((TESTS_SKIPPED++)) || true
}

run_test_suite() {
    local suite_name="$1"
    local suite_file="$2"

    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $suite_name${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    if [ -f "$suite_file" ]; then
        source "$suite_file"
    else
        echo -e "  ${RED}Suite file not found: $suite_file${NC}"
        ((TESTS_FAILED++)) || true
    fi
}

# Export functions for use in sourced test files
export -f run_test
export -f skip_test
export -f assert_equals
export -f assert_contains
export -f assert_file_exists
export -f assert_dir_exists
export -f assert_command_success
export -f assert_command_output_contains

# Export color variables
export RED GREEN YELLOW BLUE NC

# ============================================
# Main
# ============================================

echo ""
echo "============================================"
echo "  Infrastructure Test Suite"
echo "  Mode: $TEST_MODE"
echo "============================================"

# Export variables for test suites
export PROJECT_ROOT
export SCRIPT_DIR
export TEST_MODE

# Run test suites
run_test_suite "Docker Compose Validation" "$SCRIPT_DIR/test-docker-compose.sh"
run_test_suite "Shell Scripts Validation" "$SCRIPT_DIR/test-shell-scripts.sh"
run_test_suite "Environment Configuration" "$SCRIPT_DIR/test-env-config.sh"
run_test_suite "File Structure" "$SCRIPT_DIR/test-file-structure.sh"

if [ "$TEST_MODE" = "--full" ]; then
    run_test_suite "Container Startup" "$SCRIPT_DIR/test-containers.sh"
    run_test_suite "Service Connectivity" "$SCRIPT_DIR/test-connectivity.sh"
else
    echo ""
    echo -e "${YELLOW}Skipping container tests (use --full to run)${NC}"
fi

# Summary
echo ""
echo "============================================"
echo "  Test Results"
echo "============================================"
echo -e "  ${GREEN}Passed:${NC}  $TESTS_PASSED"
echo -e "  ${RED}Failed:${NC}  $TESTS_FAILED"
echo -e "  ${YELLOW}Skipped:${NC} $TESTS_SKIPPED"
echo ""

if [ $TESTS_FAILED -gt 0 ]; then
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi

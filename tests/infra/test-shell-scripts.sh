#!/bin/bash
# ============================================
# Shell Scripts Validation Tests
# ============================================

SCRIPTS_DIR="$PROJECT_ROOT/scripts"

# Test: start-infra.sh exists
test_start_script_exists() {
    assert_file_exists "$SCRIPTS_DIR/start-infra.sh"
}

# Test: stop-infra.sh exists
test_stop_script_exists() {
    assert_file_exists "$SCRIPTS_DIR/stop-infra.sh"
}

# Test: logs.sh exists
test_logs_script_exists() {
    assert_file_exists "$SCRIPTS_DIR/logs.sh"
}

# Test: start-infra.sh is executable
test_start_script_executable() {
    if [ -x "$SCRIPTS_DIR/start-infra.sh" ]; then
        return 0
    else
        echo "  start-infra.sh should be executable"
        return 1
    fi
}

# Test: stop-infra.sh is executable
test_stop_script_executable() {
    if [ -x "$SCRIPTS_DIR/stop-infra.sh" ]; then
        return 0
    else
        echo "  stop-infra.sh should be executable"
        return 1
    fi
}

# Test: logs.sh is executable
test_logs_script_executable() {
    if [ -x "$SCRIPTS_DIR/logs.sh" ]; then
        return 0
    else
        echo "  logs.sh should be executable"
        return 1
    fi
}

# Test: start-infra.sh has valid bash syntax
test_start_script_syntax() {
    if bash -n "$SCRIPTS_DIR/start-infra.sh" 2>/dev/null; then
        return 0
    else
        echo "  start-infra.sh has syntax errors"
        return 1
    fi
}

# Test: stop-infra.sh has valid bash syntax
test_stop_script_syntax() {
    if bash -n "$SCRIPTS_DIR/stop-infra.sh" 2>/dev/null; then
        return 0
    else
        echo "  stop-infra.sh has syntax errors"
        return 1
    fi
}

# Test: logs.sh has valid bash syntax
test_logs_script_syntax() {
    if bash -n "$SCRIPTS_DIR/logs.sh" 2>/dev/null; then
        return 0
    else
        echo "  logs.sh has syntax errors"
        return 1
    fi
}

# Test: start-infra.sh has shebang
test_start_script_shebang() {
    local first_line
    first_line=$(head -n 1 "$SCRIPTS_DIR/start-infra.sh")

    if [[ "$first_line" == "#!/bin/bash"* ]] || [[ "$first_line" == "#!/usr/bin/env bash"* ]]; then
        return 0
    else
        echo "  start-infra.sh should have bash shebang"
        return 1
    fi
}

# Test: stop-infra.sh has shebang
test_stop_script_shebang() {
    local first_line
    first_line=$(head -n 1 "$SCRIPTS_DIR/stop-infra.sh")

    if [[ "$first_line" == "#!/bin/bash"* ]] || [[ "$first_line" == "#!/usr/bin/env bash"* ]]; then
        return 0
    else
        echo "  stop-infra.sh should have bash shebang"
        return 1
    fi
}

# Test: logs.sh has shebang
test_logs_script_shebang() {
    local first_line
    first_line=$(head -n 1 "$SCRIPTS_DIR/logs.sh")

    if [[ "$first_line" == "#!/bin/bash"* ]] || [[ "$first_line" == "#!/usr/bin/env bash"* ]]; then
        return 0
    else
        echo "  logs.sh should have bash shebang"
        return 1
    fi
}

# Test: start-infra.sh uses set -e for error handling
test_start_script_error_handling() {
    if grep -q "set -e" "$SCRIPTS_DIR/start-infra.sh"; then
        return 0
    else
        echo "  start-infra.sh should use 'set -e' for error handling"
        return 1
    fi
}

# Test: stop-infra.sh uses set -e for error handling
test_stop_script_error_handling() {
    if grep -q "set -e" "$SCRIPTS_DIR/stop-infra.sh"; then
        return 0
    else
        echo "  stop-infra.sh should use 'set -e' for error handling"
        return 1
    fi
}

# Test: start-infra.sh checks for Docker
test_start_script_docker_check() {
    if grep -q "docker info" "$SCRIPTS_DIR/start-infra.sh" || grep -q "docker --version" "$SCRIPTS_DIR/start-infra.sh"; then
        return 0
    else
        echo "  start-infra.sh should check if Docker is running"
        return 1
    fi
}

# Test: start-infra.sh uses docker compose
test_start_script_uses_compose() {
    if grep -q "docker compose" "$SCRIPTS_DIR/start-infra.sh" || grep -q "docker-compose" "$SCRIPTS_DIR/start-infra.sh"; then
        return 0
    else
        echo "  start-infra.sh should use docker compose"
        return 1
    fi
}

# Test: stop-infra.sh uses docker compose
test_stop_script_uses_compose() {
    if grep -q "docker compose" "$SCRIPTS_DIR/stop-infra.sh" || grep -q "docker-compose" "$SCRIPTS_DIR/stop-infra.sh"; then
        return 0
    else
        echo "  stop-infra.sh should use docker compose"
        return 1
    fi
}

# Test: stop-infra.sh supports --clean flag
test_stop_script_clean_flag() {
    if grep -q "\-\-clean" "$SCRIPTS_DIR/stop-infra.sh"; then
        return 0
    else
        echo "  stop-infra.sh should support --clean flag"
        return 1
    fi
}

# Test: start-infra.sh supports --wait flag
test_start_script_wait_flag() {
    if grep -q "\-\-wait" "$SCRIPTS_DIR/start-infra.sh"; then
        return 0
    else
        echo "  start-infra.sh should support --wait flag"
        return 1
    fi
}

# Test: logs.sh uses docker compose logs
test_logs_script_uses_compose() {
    if grep -q "docker compose logs" "$SCRIPTS_DIR/logs.sh" || grep -q "docker-compose logs" "$SCRIPTS_DIR/logs.sh"; then
        return 0
    else
        echo "  logs.sh should use docker compose logs"
        return 1
    fi
}

# Run all tests
run_test "start-infra.sh exists" test_start_script_exists
run_test "stop-infra.sh exists" test_stop_script_exists
run_test "logs.sh exists" test_logs_script_exists
run_test "start-infra.sh is executable" test_start_script_executable
run_test "stop-infra.sh is executable" test_stop_script_executable
run_test "logs.sh is executable" test_logs_script_executable
run_test "start-infra.sh has valid syntax" test_start_script_syntax
run_test "stop-infra.sh has valid syntax" test_stop_script_syntax
run_test "logs.sh has valid syntax" test_logs_script_syntax
run_test "start-infra.sh has shebang" test_start_script_shebang
run_test "stop-infra.sh has shebang" test_stop_script_shebang
run_test "logs.sh has shebang" test_logs_script_shebang
run_test "start-infra.sh has error handling" test_start_script_error_handling
run_test "stop-infra.sh has error handling" test_stop_script_error_handling
run_test "start-infra.sh checks Docker status" test_start_script_docker_check
run_test "start-infra.sh uses docker compose" test_start_script_uses_compose
run_test "stop-infra.sh uses docker compose" test_stop_script_uses_compose
run_test "stop-infra.sh supports --clean flag" test_stop_script_clean_flag
run_test "start-infra.sh supports --wait flag" test_start_script_wait_flag
run_test "logs.sh uses docker compose logs" test_logs_script_uses_compose

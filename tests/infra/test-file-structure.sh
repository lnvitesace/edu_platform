#!/bin/bash
# ============================================
# File Structure Tests
# ============================================

DOCKER_DIR="$PROJECT_ROOT/docker"
SCRIPTS_DIR="$PROJECT_ROOT/scripts"

# Test: docker directory exists
test_docker_dir_exists() {
    assert_dir_exists "$DOCKER_DIR"
}

# Test: scripts directory exists
test_scripts_dir_exists() {
    assert_dir_exists "$SCRIPTS_DIR"
}

# Test: mysql init directory exists
test_mysql_init_dir_exists() {
    assert_dir_exists "$DOCKER_DIR/mysql/init"
}

# Test: MySQL init script exists
test_mysql_init_script_exists() {
    if ls "$DOCKER_DIR/mysql/init/"*.sql 1> /dev/null 2>&1; then
        return 0
    else
        echo "  No SQL init scripts found in docker/mysql/init/"
        return 1
    fi
}

# Test: MySQL init script creates databases
test_mysql_init_creates_databases() {
    local init_script
    init_script=$(ls "$DOCKER_DIR/mysql/init/"*.sql 2>/dev/null | head -n 1)

    if [ -z "$init_script" ]; then
        echo "  No init script found"
        return 1
    fi

    if grep -qi "CREATE DATABASE" "$init_script"; then
        return 0
    else
        echo "  Init script should create databases"
        return 1
    fi
}

# Test: MySQL init script uses utf8mb4
test_mysql_init_uses_utf8mb4() {
    local init_script
    init_script=$(ls "$DOCKER_DIR/mysql/init/"*.sql 2>/dev/null | head -n 1)

    if [ -z "$init_script" ]; then
        echo "  No init script found"
        return 1
    fi

    if grep -qi "utf8mb4" "$init_script"; then
        return 0
    else
        echo "  Init script should use utf8mb4 charset"
        return 1
    fi
}

# Test: docker/README.md exists
test_docker_readme_exists() {
    assert_file_exists "$DOCKER_DIR/README.md"
}

# Test: README has usage instructions
test_readme_has_usage() {
    if grep -qi "usage\|快速开始\|quick start" "$DOCKER_DIR/README.md"; then
        return 0
    else
        echo "  README should have usage instructions"
        return 1
    fi
}

# Test: README documents all services
test_readme_documents_services() {
    local services=("MySQL" "Redis" "Nacos" "RabbitMQ" "Elasticsearch" "Zipkin")
    for service in "${services[@]}"; do
        if ! grep -qi "$service" "$DOCKER_DIR/README.md"; then
            echo "  README should document $service"
            return 1
        fi
    done
    return 0
}

# Test: README has troubleshooting section
test_readme_has_troubleshooting() {
    if grep -qi "troubleshoot\|常见问题\|FAQ\|问题" "$DOCKER_DIR/README.md"; then
        return 0
    else
        echo "  README should have troubleshooting section"
        return 1
    fi
}

# Test: No sensitive data in tracked files
test_no_hardcoded_passwords() {
    local compose_file="$DOCKER_DIR/docker-compose.yml"

    # Check for password values that don't use ${VAR} env var syntax
    # Valid: MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:?set MYSQL_ROOT_PASSWORD in docker/.env}
    # Invalid: password: mysecretpassword
    local matches
    matches=$(grep -iE "(password|secret):\s+[a-zA-Z0-9]+" "$compose_file" 2>/dev/null | grep -v '\${' | grep -v '#' || true)

    if [ -n "$matches" ]; then
        echo "  docker-compose.yml may have hardcoded passwords:"
        echo "  $matches"
        return 1
    fi

    return 0
}

# Run all tests
run_test "docker/ directory exists" test_docker_dir_exists
run_test "scripts/ directory exists" test_scripts_dir_exists
run_test "mysql/init/ directory exists" test_mysql_init_dir_exists
run_test "MySQL init script exists" test_mysql_init_script_exists
run_test "MySQL init creates databases" test_mysql_init_creates_databases
run_test "MySQL init uses utf8mb4" test_mysql_init_uses_utf8mb4
run_test "docker/README.md exists" test_docker_readme_exists
run_test "README has usage instructions" test_readme_has_usage
run_test "README documents all services" test_readme_documents_services
run_test "README has troubleshooting section" test_readme_has_troubleshooting
run_test "No hardcoded passwords in compose" test_no_hardcoded_passwords

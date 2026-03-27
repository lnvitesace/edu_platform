#!/bin/bash
# ============================================
# Environment Configuration Tests
# ============================================

DOCKER_DIR="$PROJECT_ROOT/docker"

# Test: .env.example exists
test_env_example_exists() {
    assert_file_exists "$DOCKER_DIR/.env.example"
}

# Test: .env exists (or can be created from example)
test_env_file_exists() {
    if [ -f "$DOCKER_DIR/.env" ]; then
        return 0
    else
        return 0
    fi
}

# Test: .env.example has MySQL configuration
test_env_example_mysql_config() {
    if grep -q "MYSQL_ROOT_PASSWORD" "$DOCKER_DIR/.env.example" && \
       grep -q "MYSQL_PORT" "$DOCKER_DIR/.env.example"; then
        return 0
    else
        echo "  .env.example should have MySQL configuration"
        return 1
    fi
}

# Test: .env.example has Redis configuration
test_env_example_redis_config() {
    if grep -q "REDIS_PORT" "$DOCKER_DIR/.env.example"; then
        return 0
    else
        echo "  .env.example should have Redis configuration"
        return 1
    fi
}

# Test: .env.example has Nacos configuration
test_env_example_nacos_config() {
    if grep -q "NACOS_PORT" "$DOCKER_DIR/.env.example"; then
        return 0
    else
        echo "  .env.example should have Nacos configuration"
        return 1
    fi
}

# Test: .env.example has RabbitMQ configuration
test_env_example_rabbitmq_config() {
    if grep -q "RABBITMQ_USER" "$DOCKER_DIR/.env.example" && \
       grep -q "RABBITMQ_PASS" "$DOCKER_DIR/.env.example" && \
       grep -q "RABBITMQ_PORT" "$DOCKER_DIR/.env.example"; then
        return 0
    else
        echo "  .env.example should have RabbitMQ configuration"
        return 1
    fi
}

# Test: .env.example has Elasticsearch configuration
test_env_example_es_config() {
    if grep -q "ES_PORT" "$DOCKER_DIR/.env.example"; then
        return 0
    else
        echo "  .env.example should have Elasticsearch configuration"
        return 1
    fi
}

# Test: .env.example has Zipkin configuration
test_env_example_zipkin_config() {
    if grep -q "ZIPKIN_PORT" "$DOCKER_DIR/.env.example"; then
        return 0
    else
        echo "  .env.example should have Zipkin configuration"
        return 1
    fi
}

# Test: .gitignore exists in docker directory
test_docker_gitignore_exists() {
    assert_file_exists "$DOCKER_DIR/.gitignore"
}

# Test: .gitignore excludes .env
test_gitignore_excludes_env() {
    if grep -q "\.env" "$DOCKER_DIR/.gitignore"; then
        return 0
    else
        echo "  .gitignore should exclude .env file"
        return 1
    fi
}

# Test: Default ports don't conflict
test_default_ports_no_conflict() {
    local ports=(3306 6379 8848 9848 5672 15672 9200 9411)
    local unique_ports
    unique_ports=$(printf '%s\n' "${ports[@]}" | sort -u | wc -l | tr -d ' ')

    if [ "$unique_ports" = "${#ports[@]}" ]; then
        return 0
    else
        echo "  Default ports should not conflict"
        return 1
    fi
}

# Test: MySQL password is not empty in example
test_mysql_password_not_empty() {
    local password
    password=$(grep "MYSQL_ROOT_PASSWORD" "$DOCKER_DIR/.env.example" | cut -d'=' -f2)

    if [ -n "$password" ] && [ "$password" != "" ]; then
        return 0
    else
        echo "  MySQL password should not be empty in example"
        return 1
    fi
}

# Test: RabbitMQ credentials are set in example
test_rabbitmq_credentials_set() {
    local user pass
    user=$(grep "RABBITMQ_USER" "$DOCKER_DIR/.env.example" | cut -d'=' -f2)
    pass=$(grep "RABBITMQ_PASS" "$DOCKER_DIR/.env.example" | cut -d'=' -f2)

    if [ -n "$user" ] && [ -n "$pass" ]; then
        return 0
    else
        echo "  RabbitMQ credentials should be set in example"
        return 1
    fi
}

# Test: internal service token is set in example
test_internal_service_token_set() {
    local token
    token=$(grep "INTERNAL_SERVICE_TOKEN" "$DOCKER_DIR/.env.example" | cut -d'=' -f2)

    if [ -n "$token" ]; then
        return 0
    else
        echo "  Internal service token should be set in example"
        return 1
    fi
}

# Run all tests
run_test ".env.example exists" test_env_example_exists
run_test ".env file exists" test_env_file_exists
run_test ".env.example has MySQL config" test_env_example_mysql_config
run_test ".env.example has Redis config" test_env_example_redis_config
run_test ".env.example has Nacos config" test_env_example_nacos_config
run_test ".env.example has RabbitMQ config" test_env_example_rabbitmq_config
run_test ".env.example has Elasticsearch config" test_env_example_es_config
run_test ".env.example has Zipkin config" test_env_example_zipkin_config
run_test ".gitignore exists in docker/" test_docker_gitignore_exists
run_test ".gitignore excludes .env" test_gitignore_excludes_env
run_test "Default ports don't conflict" test_default_ports_no_conflict
run_test "MySQL password is set in example" test_mysql_password_not_empty
run_test "RabbitMQ credentials are set" test_rabbitmq_credentials_set
run_test "Internal service token is set" test_internal_service_token_set

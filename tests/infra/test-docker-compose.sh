#!/bin/bash
# ============================================
# Docker Compose Validation Tests
# ============================================

DOCKER_DIR="$PROJECT_ROOT/docker"

compose_config() {
    (cd "$DOCKER_DIR" && docker compose --env-file .env.example config 2>/dev/null)
}

# Test: docker-compose.yml exists
test_docker_compose_file_exists() {
    assert_file_exists "$DOCKER_DIR/docker-compose.yml"
}

# Test: docker-compose.yml is valid YAML
test_docker_compose_valid_yaml() {
    cd "$DOCKER_DIR"
    docker compose --env-file .env.example config > /dev/null 2>&1
}

# Test: All required services are defined
test_all_services_defined() {
    local config
    config=$(compose_config)

    local services=("mysql" "redis" "nacos" "rabbitmq" "elasticsearch" "zipkin")
    for service in "${services[@]}"; do
        if ! echo "$config" | grep -q "^  $service:"; then
            echo "  Missing service: $service"
            return 1
        fi
    done
    return 0
}

# Test: MySQL service configuration
test_mysql_service_config() {
    local config
    config=$(compose_config)

    # Check image
    if ! echo "$config" | grep -q "mysql:8"; then
        echo "  MySQL should use version 8.x"
        return 1
    fi

    # Check port mapping (docker compose config outputs in long format)
    if ! echo "$config" | grep -q "target: 3306"; then
        echo "  MySQL port 3306 should be exposed"
        return 1
    fi

    return 0
}

# Test: Redis service configuration
test_redis_service_config() {
    local config
    config=$(compose_config)

    # Check image
    if ! echo "$config" | grep -Eq "redis:(7|8)"; then
        echo "  Redis should use version 7.x or 8.x"
        return 1
    fi

    # Check port mapping
    if ! echo "$config" | grep -q "target: 6379"; then
        echo "  Redis port 6379 should be exposed"
        return 1
    fi

    return 0
}

# Test: Nacos service configuration
test_nacos_service_config() {
    local config
    config=$(compose_config)

    # Check image
    if ! echo "$config" | grep -q "nacos/nacos-server"; then
        echo "  Nacos image should be nacos/nacos-server"
        return 1
    fi

    # Check port mapping
    if ! echo "$config" | grep -q "target: 8848"; then
        echo "  Nacos port 8848 should be exposed"
        return 1
    fi

    return 0
}

# Test: RabbitMQ service configuration
test_rabbitmq_service_config() {
    local config
    config=$(compose_config)

    # Check image contains rabbitmq
    if ! echo "$config" | grep -q "rabbitmq:"; then
        echo "  RabbitMQ image should be specified"
        return 1
    fi

    # Check port mappings
    if ! echo "$config" | grep -q "target: 5672"; then
        echo "  RabbitMQ port 5672 should be exposed"
        return 1
    fi

    if ! echo "$config" | grep -q "target: 15672"; then
        echo "  RabbitMQ management port 15672 should be exposed"
        return 1
    fi

    return 0
}

# Test: Elasticsearch service configuration
test_elasticsearch_service_config() {
    local config
    config=$(compose_config)

    # Check image
    if ! echo "$config" | grep -q "elasticsearch"; then
        echo "  Elasticsearch image should be specified"
        return 1
    fi

    # Check port mapping
    if ! echo "$config" | grep -q "target: 9200"; then
        echo "  Elasticsearch port 9200 should be exposed"
        return 1
    fi

    return 0
}

# Test: Zipkin service configuration
test_zipkin_service_config() {
    local config
    config=$(compose_config)

    # Check image
    if ! echo "$config" | grep -q "zipkin"; then
        echo "  Zipkin image should be specified"
        return 1
    fi

    # Check port mapping
    if ! echo "$config" | grep -q "target: 9411"; then
        echo "  Zipkin port 9411 should be exposed"
        return 1
    fi

    return 0
}

# Test: Network is defined
test_network_defined() {
    local config
    config=$(compose_config)

    if ! echo "$config" | grep -q "edu-network"; then
        echo "  Network 'edu-network' should be defined"
        return 1
    fi

    return 0
}

# Test: Volumes are defined
test_volumes_defined() {
    local config
    config=$(compose_config)

    local volumes=("mysql-data" "redis-data" "elasticsearch-data")
    for vol in "${volumes[@]}"; do
        if ! echo "$config" | grep -q "$vol"; then
            echo "  Missing volume: $vol"
            return 1
        fi
    done

    return 0
}

# Test: All services have health checks
test_services_have_healthchecks() {
    local config
    config=$(compose_config)

    local services=("mysql" "redis" "nacos" "rabbitmq" "elasticsearch" "zipkin")
    for service in "${services[@]}"; do
        # Extract service block and check for healthcheck
        if ! echo "$config" | grep -A 50 "^  $service:" | grep -q "healthcheck:"; then
            echo "  Service '$service' should have healthcheck"
            return 1
        fi
    done

    return 0
}

# Test: All services have restart policy
test_services_have_restart_policy() {
    local config
    config=$(compose_config)

    if ! echo "$config" | grep -q "restart:"; then
        echo "  Services should have restart policy"
        return 1
    fi

    return 0
}

# Test: Compose does not ship placeholder secret fallbacks
test_no_placeholder_secret_fallbacks() {
    if grep -Eq '\$\{(MYSQL_ROOT_PASSWORD|MYSQL_PASSWORD|RABBITMQ_PASS|JWT_SECRET|GRAFANA_ADMIN_PASSWORD|INTERNAL_SERVICE_TOKEN):-' "$DOCKER_DIR/docker-compose.yml"; then
        echo "  Sensitive variables should not have insecure default fallbacks"
        return 1
    fi

    return 0
}

# Run all tests
run_test "docker-compose.yml exists" test_docker_compose_file_exists
run_test "docker-compose.yml is valid" test_docker_compose_valid_yaml
run_test "All required services defined" test_all_services_defined
run_test "MySQL service configuration" test_mysql_service_config
run_test "Redis service configuration" test_redis_service_config
run_test "Nacos service configuration" test_nacos_service_config
run_test "RabbitMQ service configuration" test_rabbitmq_service_config
run_test "Elasticsearch service configuration" test_elasticsearch_service_config
run_test "Zipkin service configuration" test_zipkin_service_config
run_test "Network is defined" test_network_defined
run_test "Volumes are defined" test_volumes_defined
run_test "Services have health checks" test_services_have_healthchecks
run_test "Services have restart policy" test_services_have_restart_policy
run_test "Sensitive env vars require explicit values" test_no_placeholder_secret_fallbacks

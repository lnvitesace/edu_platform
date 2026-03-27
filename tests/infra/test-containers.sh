#!/bin/bash
# ============================================
# Container Startup Tests
# Requires: Docker running, containers started
# ============================================

DOCKER_DIR="$PROJECT_ROOT/docker"

# Helper: Check if container exists and is running
container_is_running() {
    local name="$1"
    docker ps --format '{{.Names}}' | grep -q "^$name$"
}

# Helper: Check container health status
container_is_healthy() {
    local name="$1"
    local status
    status=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null)
    [ "$status" = "healthy" ]
}

# Helper: Wait for container to be healthy
wait_for_healthy() {
    local name="$1"
    local timeout="${2:-60}"
    local elapsed=0

    while [ $elapsed -lt $timeout ]; do
        if container_is_healthy "$name"; then
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    return 1
}

# Test: Docker is running
test_docker_running() {
    if docker info > /dev/null 2>&1; then
        return 0
    else
        echo "  Docker is not running"
        return 1
    fi
}

# Test: Can start containers
test_containers_can_start() {
    cd "$DOCKER_DIR"

    # Start containers if not already running
    if ! container_is_running "edu-mysql"; then
        docker compose up -d > /dev/null 2>&1
        sleep 5
    fi

    # Check if at least one container started
    if docker compose ps 2>/dev/null | grep -q "running\|Up"; then
        return 0
    else
        echo "  Failed to start containers"
        return 1
    fi
}

# Test: MySQL container is running
test_mysql_container_running() {
    if container_is_running "edu-mysql"; then
        return 0
    else
        echo "  MySQL container is not running"
        return 1
    fi
}

# Test: Redis container is running
test_redis_container_running() {
    if container_is_running "edu-redis"; then
        return 0
    else
        echo "  Redis container is not running"
        return 1
    fi
}

# Test: Nacos container is running
test_nacos_container_running() {
    if container_is_running "edu-nacos"; then
        return 0
    else
        echo "  Nacos container is not running"
        return 1
    fi
}

# Test: RabbitMQ container is running
test_rabbitmq_container_running() {
    if container_is_running "edu-rabbitmq"; then
        return 0
    else
        echo "  RabbitMQ container is not running"
        return 1
    fi
}

# Test: Elasticsearch container is running
test_elasticsearch_container_running() {
    if container_is_running "edu-elasticsearch"; then
        return 0
    else
        echo "  Elasticsearch container is not running"
        return 1
    fi
}

# Test: Zipkin container is running
test_zipkin_container_running() {
    if container_is_running "edu-zipkin"; then
        return 0
    else
        echo "  Zipkin container is not running"
        return 1
    fi
}

# Test: MySQL container is healthy
test_mysql_healthy() {
    if wait_for_healthy "edu-mysql" 120; then
        return 0
    else
        echo "  MySQL container is not healthy"
        return 1
    fi
}

# Test: Redis container is healthy
test_redis_healthy() {
    if wait_for_healthy "edu-redis" 30; then
        return 0
    else
        echo "  Redis container is not healthy"
        return 1
    fi
}

# Test: Containers use correct network
test_containers_on_network() {
    local network_name
    network_name=$(docker network ls --format '{{.Name}}' | grep "edu-network" | head -n 1)

    if [ -z "$network_name" ]; then
        echo "  edu-network not found"
        return 1
    fi

    local containers
    containers=$(docker network inspect "$network_name" --format='{{range .Containers}}{{.Name}} {{end}}' 2>/dev/null)

    if echo "$containers" | grep -q "edu-mysql"; then
        return 0
    else
        echo "  Containers not connected to edu-network"
        return 1
    fi
}

# Test: Data volumes are created
test_volumes_created() {
    local volumes
    volumes=$(docker volume ls --format '{{.Name}}')

    if echo "$volumes" | grep -q "mysql-data"; then
        return 0
    else
        echo "  Data volumes not created"
        return 1
    fi
}

# Run all tests
run_test "Docker is running" test_docker_running
run_test "Containers can start" test_containers_can_start
run_test "MySQL container running" test_mysql_container_running
run_test "Redis container running" test_redis_container_running
run_test "Nacos container running" test_nacos_container_running
run_test "RabbitMQ container running" test_rabbitmq_container_running
run_test "Elasticsearch container running" test_elasticsearch_container_running
run_test "Zipkin container running" test_zipkin_container_running
run_test "MySQL container healthy" test_mysql_healthy
run_test "Redis container healthy" test_redis_healthy
run_test "Containers on correct network" test_containers_on_network
run_test "Data volumes created" test_volumes_created

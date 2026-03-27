#!/bin/bash
# ============================================
# Service Connectivity Tests
# Requires: Containers running and healthy
# ============================================

DOCKER_DIR="$PROJECT_ROOT/docker"

# Load environment variables
if [ -f "$DOCKER_DIR/.env" ]; then
    set -a
    source "$DOCKER_DIR/.env"
    set +a
fi

# Default values
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_PORT="${REDIS_PORT:-6379}"
NACOS_PORT="${NACOS_PORT:-8848}"
RABBITMQ_PORT="${RABBITMQ_PORT:-5672}"
RABBITMQ_MGMT_PORT="${RABBITMQ_MGMT_PORT:-15672}"
ES_PORT="${ES_PORT:-9200}"
ZIPKIN_PORT="${ZIPKIN_PORT:-9411}"

# Helper: Check TCP port is open
check_port() {
    local host="$1"
    local port="$2"
    local timeout="${3:-5}"

    if command -v nc > /dev/null 2>&1; then
        nc -z -w "$timeout" "$host" "$port" 2>/dev/null
    elif command -v timeout > /dev/null 2>&1; then
        timeout "$timeout" bash -c "echo > /dev/tcp/$host/$port" 2>/dev/null
    else
        # Fallback using /dev/tcp
        (echo > /dev/tcp/"$host"/"$port") 2>/dev/null
    fi
}

# Helper: Check HTTP endpoint
check_http() {
    local url="$1"
    local timeout="${2:-10}"

    if command -v curl > /dev/null 2>&1; then
        curl -sf --max-time "$timeout" "$url" > /dev/null 2>&1
    elif command -v wget > /dev/null 2>&1; then
        wget -q --timeout="$timeout" -O /dev/null "$url" 2>/dev/null
    else
        echo "  Neither curl nor wget available"
        return 1
    fi
}

# Test: MySQL port is accessible
test_mysql_port_accessible() {
    if check_port "localhost" "$MYSQL_PORT" 5; then
        return 0
    else
        echo "  MySQL port $MYSQL_PORT is not accessible"
        return 1
    fi
}

# Test: Redis port is accessible
test_redis_port_accessible() {
    if check_port "localhost" "$REDIS_PORT" 5; then
        return 0
    else
        echo "  Redis port $REDIS_PORT is not accessible"
        return 1
    fi
}

# Test: Nacos port is accessible
test_nacos_port_accessible() {
    if check_port "localhost" "$NACOS_PORT" 5; then
        return 0
    else
        echo "  Nacos port $NACOS_PORT is not accessible"
        return 1
    fi
}

# Test: RabbitMQ port is accessible
test_rabbitmq_port_accessible() {
    if check_port "localhost" "$RABBITMQ_PORT" 5; then
        return 0
    else
        echo "  RabbitMQ port $RABBITMQ_PORT is not accessible"
        return 1
    fi
}

# Test: Elasticsearch port is accessible
test_elasticsearch_port_accessible() {
    if check_port "localhost" "$ES_PORT" 5; then
        return 0
    else
        echo "  Elasticsearch port $ES_PORT is not accessible"
        return 1
    fi
}

# Test: Zipkin port is accessible
test_zipkin_port_accessible() {
    if check_port "localhost" "$ZIPKIN_PORT" 5; then
        return 0
    else
        echo "  Zipkin port $ZIPKIN_PORT is not accessible"
        return 1
    fi
}

# Test: MySQL accepts connections
test_mysql_accepts_connection() {
    local password="${MYSQL_ROOT_PASSWORD:-}"

    if [ -z "$password" ]; then
        echo "  MYSQL_ROOT_PASSWORD is not set"
        return 1
    fi

    if docker exec edu-mysql mysqladmin ping -h localhost -u root -p"$password" > /dev/null 2>&1; then
        return 0
    else
        echo "  MySQL not accepting connections"
        return 1
    fi
}

# Test: Redis accepts connections
test_redis_accepts_connection() {
    local response
    response=$(docker exec edu-redis redis-cli ping 2>/dev/null)

    if [ "$response" = "PONG" ]; then
        return 0
    else
        echo "  Redis not responding to PING"
        return 1
    fi
}

# Test: Nacos health endpoint
test_nacos_health_endpoint() {
    local endpoints=(
        "http://localhost:$NACOS_PORT/nacos/v1/ns/operator/metrics"
        "http://localhost:$NACOS_PORT/nacos/v1/console/health/liveness"
        "http://localhost:$NACOS_PORT/nacos/v1/console/health/readiness"
    )
    local endpoint

    for endpoint in "${endpoints[@]}"; do
        if check_http "$endpoint" 10; then
            return 0
        fi
    done

    echo "  Nacos health endpoint not responding"
    return 1
}

# Test: RabbitMQ management UI accessible
test_rabbitmq_management_accessible() {
    if check_http "http://localhost:$RABBITMQ_MGMT_PORT" 10; then
        return 0
    else
        echo "  RabbitMQ management UI not accessible"
        return 1
    fi
}

# Test: Elasticsearch cluster health
test_elasticsearch_cluster_health() {
    local response
    response=$(curl -sf "http://localhost:$ES_PORT/_cluster/health" 2>/dev/null)

    if echo "$response" | grep -qE '"status"\s*:\s*"(green|yellow)"'; then
        return 0
    else
        echo "  Elasticsearch cluster not healthy"
        return 1
    fi
}

# Test: Zipkin health endpoint
test_zipkin_health_endpoint() {
    if check_http "http://localhost:$ZIPKIN_PORT/health" 10; then
        return 0
    else
        echo "  Zipkin health endpoint not responding"
        return 1
    fi
}

# Test: MySQL databases were created
test_mysql_databases_created() {
    local password="${MYSQL_ROOT_PASSWORD:-}"

    if [ -z "$password" ]; then
        echo "  MYSQL_ROOT_PASSWORD is not set"
        return 1
    fi

    local databases
    databases=$(docker exec edu-mysql mysql -u root -p"$password" -e "SHOW DATABASES LIKE 'edu_%';" 2>/dev/null)

    if echo "$databases" | grep -q "edu_user"; then
        return 0
    else
        echo "  Expected databases not found"
        return 1
    fi
}

# Test: Redis can store and retrieve data
test_redis_data_operations() {
    local set_result get_result

    set_result=$(docker exec edu-redis redis-cli SET test_key test_value 2>/dev/null)
    if [ "$set_result" != "OK" ]; then
        echo "  Redis SET operation failed"
        return 1
    fi

    get_result=$(docker exec edu-redis redis-cli GET test_key 2>/dev/null)
    if [ "$get_result" != "test_value" ]; then
        echo "  Redis GET operation failed"
        return 1
    fi

    # Cleanup
    docker exec edu-redis redis-cli DEL test_key > /dev/null 2>&1

    return 0
}

# Run all tests
run_test "MySQL port accessible" test_mysql_port_accessible
run_test "Redis port accessible" test_redis_port_accessible
run_test "Nacos port accessible" test_nacos_port_accessible
run_test "RabbitMQ port accessible" test_rabbitmq_port_accessible
run_test "Elasticsearch port accessible" test_elasticsearch_port_accessible
run_test "Zipkin port accessible" test_zipkin_port_accessible
run_test "MySQL accepts connections" test_mysql_accepts_connection
run_test "Redis accepts connections" test_redis_accepts_connection
run_test "Nacos health endpoint" test_nacos_health_endpoint
run_test "RabbitMQ management accessible" test_rabbitmq_management_accessible
run_test "Elasticsearch cluster healthy" test_elasticsearch_cluster_health
run_test "Zipkin health endpoint" test_zipkin_health_endpoint
run_test "MySQL databases created" test_mysql_databases_created
run_test "Redis data operations work" test_redis_data_operations

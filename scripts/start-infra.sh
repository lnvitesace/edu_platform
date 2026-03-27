#!/bin/bash
# Education Platform - Start Infrastructure Services
# Usage: ./start-infra.sh [--wait] [--build]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$SCRIPT_DIR/../docker"

cd "$DOCKER_DIR"

echo "=========================================="
echo "  Education Platform - Infrastructure"
echo "=========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker first."
    exit 1
fi

if [ ! -f ".env" ]; then
    echo "Error: docker/.env is missing. Copy docker/.env.example to docker/.env and set real credentials first."
    exit 1
fi

required_vars=(
    "MYSQL_ROOT_PASSWORD"
    "MYSQL_PASSWORD"
    "RABBITMQ_PASS"
    "JWT_SECRET"
    "INTERNAL_SERVICE_TOKEN"
    "GRAFANA_ADMIN_PASSWORD"
)

for var in "${required_vars[@]}"; do
    value=$(grep "^${var}=" .env | cut -d'=' -f2-)

    if [ -z "$value" ]; then
        echo "Error: ${var} is not set in docker/.env."
        exit 1
    fi

    if [[ "$value" == *change-me* ]] || [[ "$value" == *replace-me* ]]; then
        echo "Error: ${var} in docker/.env still uses a placeholder value."
        exit 1
    fi
done

# Start services
echo "[1/2] Starting infrastructure services..."

compose_args=(up -d)

for arg in "$@"; do
    case "$arg" in
        --build)
            compose_args+=(--build)
            ;;
    esac
done

docker compose "${compose_args[@]}"

echo ""
echo "[2/2] Waiting for services to be healthy..."

# Function to check service health
check_health() {
    local service=$1
    local max_attempts=${2:-30}
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        status=$(docker inspect --format='{{.State.Health.Status}}' "edu-$service" 2>/dev/null || echo "not_found")

        if [ "$status" = "healthy" ]; then
            echo "  ✓ $service is healthy"
            return 0
        elif [ "$status" = "not_found" ]; then
            echo "  ✗ $service container not found"
            return 1
        fi

        sleep 2
        attempt=$((attempt + 1))
    done

    echo "  ⚠ $service health check timed out (status: $status)"
    return 1
}

# Wait for critical services if --wait flag is provided
if printf '%s\n' "$@" | grep -qx -- '--wait'; then
    echo ""
    services=("mysql" "redis" "nacos" "rabbitmq" "elasticsearch" "zipkin")

    for service in "${services[@]}"; do
        check_health "$service" 60
    done
fi

echo ""
echo "=========================================="
echo "  Services Started!"
echo "=========================================="
echo ""
echo "Access URLs:"
echo "  • MySQL:         localhost:${MYSQL_PORT:-3306}"
echo "  • Redis:         localhost:${REDIS_PORT:-6379}"
echo "  • Nacos:         http://localhost:${NACOS_PORT:-8848}/nacos"
echo "  • RabbitMQ:      http://localhost:${RABBITMQ_MGMT_PORT:-15672}"
echo "  • Elasticsearch: http://localhost:${ES_PORT:-9200}"
echo "  • Zipkin:        http://localhost:${ZIPKIN_PORT:-9411}"
echo ""
echo "Credentials:"
echo "  • See docker/.env for MySQL, RabbitMQ and Grafana credentials"
echo "  • Replace example values from docker/.env.example before production use"
echo ""
echo "Commands:"
echo "  • View logs:  ./scripts/logs.sh [service]"
echo "  • Stop all:   ./scripts/stop-infra.sh"
echo ""

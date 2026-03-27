#!/bin/bash
# Education Platform - One-click Start All Services
# Usage: ./scripts/start-all.sh
#
# Starts: Docker infra → backend microservices → frontend → seed data
# Requirements: local java/mvn must target Java 25 for Maven-based services

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; }

require_cmd() {
    local cmd=$1
    if ! command -v "$cmd" >/dev/null 2>&1; then
        err "Required command not found: $cmd"
        exit 1
    fi
}

# ── Pre-checks ──────────────────────────────────────────────

if ! docker info > /dev/null 2>&1; then
    err "Docker is not running. Please start Docker first."
    exit 1
fi

for cmd in docker java mvn npm node python3 curl nc; do
    require_cmd "$cmd"
done

JAVA_MAJOR=$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')
if [ "$JAVA_MAJOR" != "25" ]; then
    err "Local Java 25 is required for ./scripts/start-all.sh. Current java major version: ${JAVA_MAJOR:-unknown}"
    err "Please point both 'java' and 'mvn' to a Java 25 installation, then retry."
    exit 1
fi

MVN_JAVA_MAJOR=$(mvn -version 2>/dev/null | awk -F ': ' '/Java version/ {split($2, parts, "."); print parts[1]; exit}')
if [ "$MVN_JAVA_MAJOR" != "25" ]; then
    err "Maven is not using Java 25. Current Maven Java major version: ${MVN_JAVA_MAJOR:-unknown}"
    err "Please update JAVA_HOME or Maven runtime configuration so 'mvn' also uses Java 25."
    exit 1
fi

if [ ! -f docker/.env ]; then
    err "docker/.env not found. Run: cp docker/.env.example docker/.env"
    exit 1
fi

# Stop local MySQL if running (port 3306 conflict with Docker MySQL)
if command -v brew &>/dev/null && brew services list 2>/dev/null | grep -q "mysql.*started"; then
    warn "Stopping local Homebrew MySQL to avoid port 3306 conflict..."
    brew services stop mysql 2>/dev/null || true
    sleep 2
fi

# ── Load .env ───────────────────────────────────────────────

export $(grep -v '^#' docker/.env | grep -v '^$' | xargs)

# Services use MYSQL_ROOT_PASSWORD as the actual connection password
# (they connect as root, and MYSQL_PASSWORD in .env is for a non-root user)
SVC_MYSQL_PW="$MYSQL_ROOT_PASSWORD"
SVC_JWT="$JWT_SECRET"
SVC_TOKEN="$INTERNAL_SERVICE_TOKEN"
SVC_RABBIT_PW="$RABBITMQ_PASS"

# docker/.env contains container-network hostnames such as `nacos` and `elasticsearch`.
# Local Maven services must talk to the host-published ports instead.
LOCAL_MYSQL_HOST="localhost"
LOCAL_REDIS_HOST="localhost"
LOCAL_RABBITMQ_HOST="localhost"
LOCAL_NACOS_ADDR="localhost:8848"
LOCAL_ELASTICSEARCH_URIS="http://localhost:9200"

# ── Step 1: Docker Infrastructure ───────────────────────────

echo ""
echo "════════════════════════════════════════════"
echo "  Education Platform - Starting All Services"
echo "════════════════════════════════════════════"
echo ""

echo "▶ [1/4] Docker infrastructure..."

# Stop Docker application containers first (we run services locally via Maven)
cd "$ROOT_DIR/docker"
docker compose stop gateway-service user-service course-service search-service 2>/dev/null || true
cd "$ROOT_DIR"

# Start only infra containers (not application containers)
cd "$ROOT_DIR/docker"
docker compose up -d --build mysql redis nacos rabbitmq elasticsearch zipkin prometheus grafana loki promtail 2>/dev/null
cd "$ROOT_DIR"

# Wait for critical infra
echo "  Waiting for infrastructure..."
check_docker_health() {
    local container=$1 max=$2
    for i in $(seq 1 "$max"); do
        s=$(docker inspect --format='{{.State.Health.Status}}' "edu-$container" 2>/dev/null || echo "")
        [ "$s" = "healthy" ] && return 0
        sleep 2
    done
    return 1
}

for svc in mysql redis nacos rabbitmq elasticsearch; do
    if check_docker_health "$svc" 60; then
        log "$svc is healthy"
    else
        err "$svc health check timed out"
        exit 1
    fi
done

# Wait for Nacos gRPC port (9848) to be reachable — services need it to register
echo "  Waiting for Nacos gRPC port 9848..."
for i in $(seq 1 30); do
    if nc -z localhost 9848 2>/dev/null; then
        log "Nacos gRPC ready"
        break
    fi
    sleep 2
done

# ── Step 2: Backend microservices ───────────────────────────

echo ""
echo "▶ [2/4] Backend microservices..."

PIDS=()
LOGS_DIR="/tmp/edu-platform-logs"
mkdir -p "$LOGS_DIR"

start_service() {
    local name=$1 dir=$2 port=$3
    shift 3
    # Pass remaining args as env vars
    (cd "$ROOT_DIR/$dir" && env "$@" mvn spring-boot:run -q) > "$LOGS_DIR/$name.log" 2>&1 &
    PIDS+=($!)
    echo "  Starting $name (PID $!, port $port)..."
}

start_service gateway-service gateway-service 8080 \
    REDIS_HOST="$LOCAL_REDIS_HOST" \
    NACOS_SERVER_ADDR="$LOCAL_NACOS_ADDR" \
    JWT_SECRET="$SVC_JWT"

start_service user-service user-service 8001 \
    MYSQL_HOST="$LOCAL_MYSQL_HOST" \
    REDIS_HOST="$LOCAL_REDIS_HOST" \
    NACOS_SERVER_ADDR="$LOCAL_NACOS_ADDR" \
    MYSQL_PASSWORD="$SVC_MYSQL_PW" \
    JWT_SECRET="$SVC_JWT" \
    INTERNAL_SERVICE_TOKEN="$SVC_TOKEN"

start_service course-service course-service 8002 \
    MYSQL_HOST="$LOCAL_MYSQL_HOST" \
    REDIS_HOST="$LOCAL_REDIS_HOST" \
    RABBITMQ_HOST="$LOCAL_RABBITMQ_HOST" \
    NACOS_SERVER_ADDR="$LOCAL_NACOS_ADDR" \
    MYSQL_PASSWORD="$SVC_MYSQL_PW" \
    MYSQL_DATABASE=edu_course \
    JWT_SECRET="$SVC_JWT" \
    INTERNAL_SERVICE_TOKEN="$SVC_TOKEN" \
    RABBITMQ_PASSWORD="$SVC_RABBIT_PW"

start_service search-service search-service 8005 \
    RABBITMQ_HOST="$LOCAL_RABBITMQ_HOST" \
    NACOS_SERVER_ADDR="$LOCAL_NACOS_ADDR" \
    ELASTICSEARCH_URIS="$LOCAL_ELASTICSEARCH_URIS" \
    RABBITMQ_PASSWORD="$SVC_RABBIT_PW"

# Wait for core services
echo ""
echo "  Waiting for services to be ready..."
READY=false
for i in $(seq 1 90); do
    gw=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null || echo "")
    us=$(curl -sf http://localhost:8001/actuator/health 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null || echo "")
    cs=$(curl -sf http://localhost:8002/actuator/health 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null || echo "")

    if [ "$gw" = "UP" ] && [ "$us" = "UP" ] && [ "$cs" = "UP" ]; then
        READY=true
        break
    fi

    sleep 2
done

if [ "$READY" = false ]; then
    err "Timeout waiting for core services (gateway/user/course). Check logs: $LOGS_DIR/"
    exit 1
fi

log "gateway-service  :8080"
log "user-service     :8001"
log "course-service   :8002"

# Check optional services (non-blocking)
sleep 5
curl -sf http://localhost:8005/actuator/health >/dev/null 2>&1 && log "search-service   :8005" || warn "search-service not ready (non-critical)"

# Wait for Nacos service registration (async, takes ~30-60s after service start)
echo ""
echo "  Waiting for Nacos service discovery..."
for i in $(seq 1 30); do
    registered=$(curl -sf 'http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=100' 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('count',0))" 2>/dev/null || echo "0")
    if [ "$registered" -ge 3 ] 2>/dev/null; then
        log "Nacos: $registered services registered"
        break
    fi
    sleep 3
done

# ── Step 3: Frontend ────────────────────────────────────────

echo ""
echo "▶ [3/4] Frontend dev server..."

(cd "$ROOT_DIR/web-app" && npm run dev) > "$LOGS_DIR/web-app.log" 2>&1 &
PIDS+=($!)

# Wait for Vite
for i in $(seq 1 20); do
    if curl -sf http://localhost:3000 >/dev/null 2>&1; then
        log "web-app          :3000"
        break
    fi
    sleep 1
done

# ── Step 4: Seed data ──────────────────────────────────────

echo ""
echo "▶ [4/4] Seeding demo data..."

if [ -d "$ROOT_DIR/e2e/node_modules" ]; then
    (cd "$ROOT_DIR/e2e" && node seed/seed.js 2>&1 | grep -E "✓|⊘|✗|Seed") || warn "Seed had errors (data may already exist)"
else
    warn "e2e/node_modules not found. Run: cd e2e && npm install"
    warn "Then seed manually: cd e2e && node seed/seed.js"
fi

# ── Done ────────────────────────────────────────────────────

echo ""
echo "════════════════════════════════════════════"
echo "  All Services Running!"
echo "════════════════════════════════════════════"
echo ""
echo "  Frontend:       http://localhost:3000"
echo "  API Gateway:    http://localhost:8080"
echo "  Nacos Console:  http://localhost:8848/nacos"
echo "  RabbitMQ:       http://localhost:15672"
echo "  Grafana:        http://localhost:3001"
echo ""
echo "  Logs:           $LOGS_DIR/"
echo "  Stop:           ./scripts/stop-all.sh"
echo "  E2E tests:      cd e2e && npx playwright test"
echo ""
echo "  Demo accounts:"
echo "    Student:    student_zhang / Study123456"
echo "    Instructor: instructor_wang / Teach123456"
echo "    Admin:      admin_user / Admin123456"
echo ""

# Keep script alive — Ctrl+C stops everything
cleanup() {
    echo ""
    echo "Stopping all services..."
    for pid in "${PIDS[@]}"; do
        kill "$pid" 2>/dev/null
    done
    wait 2>/dev/null
    echo "Backend services stopped. Docker infra still running."
    echo "Run ./scripts/stop-infra.sh to stop Docker containers."
}

trap cleanup EXIT INT TERM
wait

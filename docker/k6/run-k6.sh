#!/bin/bash
# k6 Load Testing Runner
# Usage: ./run-k6.sh [smoke|load|stress|spike]

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_TYPE="${1:-smoke}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

case "$TEST_TYPE" in
  smoke)
    echo "🔥 Running smoke test (basic functionality check)..."
    docker run --rm -i \
      --network docker_edu-network \
      -v "$SCRIPT_DIR/scripts:/scripts:ro" \
      grafana/k6:latest run \
      -e BASE_URL=http://host.docker.internal:8080 \
      /scripts/smoke-test.js
    ;;
  load)
    echo "📊 Running load test (normal production traffic)..."
    docker run --rm -i \
      --network docker_edu-network \
      -v "$SCRIPT_DIR/scripts:/scripts:ro" \
      grafana/k6:latest run \
      -e BASE_URL=http://host.docker.internal:8080 \
      /scripts/load-test.js
    ;;
  stress)
    echo "💪 Running stress test (find breaking point)..."
    docker run --rm -i \
      --network docker_edu-network \
      -v "$SCRIPT_DIR/scripts:/scripts:ro" \
      grafana/k6:latest run \
      -e BASE_URL=http://host.docker.internal:8080 \
      /scripts/stress-test.js
    ;;
  spike)
    echo "⚡ Running spike test (sudden traffic burst)..."
    docker run --rm -i \
      --network docker_edu-network \
      -v "$SCRIPT_DIR/scripts:/scripts:ro" \
      grafana/k6:latest run \
      -e BASE_URL=http://host.docker.internal:8080 \
      /scripts/spike-test.js
    ;;
  *)
    echo "Usage: $0 [smoke|load|stress|spike]"
    echo ""
    echo "Test types:"
    echo "  smoke   - Basic functionality check (1 user, 30s)"
    echo "  load    - Normal production traffic (20-50 users, 10min)"
    echo "  stress  - Find breaking point (up to 300 users)"
    echo "  spike   - Sudden traffic burst (500 users spike)"
    exit 1
    ;;
esac

echo "✅ Test completed!"

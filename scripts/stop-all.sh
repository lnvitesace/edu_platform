#!/bin/bash
# Education Platform - Stop All Services
# Usage: ./scripts/stop-all.sh

set -e

echo "Stopping backend Java services..."
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "com.edu." 2>/dev/null || true

echo "Stopping frontend dev server..."
pkill -f "vite" 2>/dev/null || true

echo "Stopping Docker infrastructure..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/stop-infra.sh"

echo ""
echo "All services stopped."

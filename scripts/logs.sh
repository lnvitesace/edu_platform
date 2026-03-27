#!/bin/bash
# Education Platform - View Service Logs
# Usage: ./logs.sh [service] [-f]
# Examples:
#   ./logs.sh           # Show all logs
#   ./logs.sh mysql     # Show MySQL logs
#   ./logs.sh nacos -f  # Follow Nacos logs

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$SCRIPT_DIR/../docker"

cd "$DOCKER_DIR"

if [ -z "$1" ]; then
    echo "Showing logs for all services (last 100 lines)..."
    docker compose logs --tail=100
elif [ "$1" = "-f" ]; then
    echo "Following logs for all services..."
    docker compose logs -f
else
    service=$1
    shift
    echo "Showing logs for $service..."
    docker compose logs $@ "$service"
fi

#!/bin/bash
# Education Platform - Stop Infrastructure Services
# Usage: ./stop-infra.sh [--clean]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$SCRIPT_DIR/../docker"

cd "$DOCKER_DIR"

echo "=========================================="
echo "  Education Platform - Stop Services"
echo "=========================================="
echo ""

if [ "$1" = "--clean" ]; then
    echo "Stopping and removing containers, networks, and volumes..."
    docker compose down -v
    echo ""
    echo "All data has been removed."
else
    echo "Stopping containers..."
    docker compose down
    echo ""
    echo "Containers stopped. Data volumes preserved."
    echo "Use './stop-infra.sh --clean' to remove all data."
fi

echo ""
echo "Done!"

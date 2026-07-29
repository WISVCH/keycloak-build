#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
mvn package
docker compose restart keycloak

echo "Provider rebuilt and Keycloak restarted."

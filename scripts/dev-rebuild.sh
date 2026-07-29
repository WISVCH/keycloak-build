#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
mvn clean package
docker compose up --detach --force-recreate keycloak

echo "Provider rebuilt and Keycloak recreated."

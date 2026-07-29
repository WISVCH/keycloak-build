#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "Missing .env. Create it once with: cp .env.example .env"
  exit 1
fi

mvn package
docker compose up --detach

echo "Keycloak is starting at http://localhost:8181"
echo "Follow startup logs with: docker compose logs --follow keycloak"

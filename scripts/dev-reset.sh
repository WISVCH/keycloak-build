#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
read -r -p "Delete the local Keycloak database and re-import ch-dev? [y/N] " answer
if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
  echo "Cancelled."
  exit 0
fi

docker compose down --volumes --remove-orphans
"$(dirname "$0")/dev-up.sh"

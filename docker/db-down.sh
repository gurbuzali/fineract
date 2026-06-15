#!/usr/bin/env bash
# Tear down the MySQL container AND its data volume (full clean slate).
set -euo pipefail
cd "$(dirname "$0")"
docker compose down -v
echo "fineract-mysql57 container + data volume removed."

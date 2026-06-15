#!/usr/bin/env bash
# Bring up the disposable MySQL 5.7 container and wait until it is healthy.
set -euo pipefail
cd "$(dirname "$0")"

docker compose up -d

echo "Waiting for MySQL (fineract-mysql57) to become healthy..."
for _ in $(seq 1 60); do
  status="$(docker inspect -f '{{.State.Health.Status}}' fineract-mysql57 2>/dev/null || echo starting)"
  if [ "$status" = "healthy" ]; then
    echo "MySQL is healthy and listening on 127.0.0.1:3306 (root/mysql)."
    exit 0
  fi
  sleep 3
done

echo "ERROR: MySQL did not become healthy in time. Recent logs:" >&2
docker compose logs --tail=60 mysql >&2
exit 1

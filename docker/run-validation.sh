#!/usr/bin/env bash
# End-to-end validation against a real database:
#   fresh MySQL 5.7  ->  migrate tenant DBs  ->  full unit suite  ->  HTTP integration suite.
#
# Logs land in build/validation-logs/. The DB is left running for inspection; run
# docker/db-down.sh afterwards to destroy it.
#
# Usage:  docker/run-validation.sh            # clean run (drops any existing DB volume first)
#         KEEP_DB=1 docker/run-validation.sh  # reuse an already-migrated DB (skip down + migrate)
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
LOG_DIR="$ROOT/build/validation-logs"
mkdir -p "$LOG_DIR"
cd "$ROOT"

if [ "${KEEP_DB:-0}" != "1" ]; then
  echo "[1/5] Fresh DB container (dropping any existing volume)..."
  "$HERE/db-down.sh" >/dev/null 2>&1 || true
  "$HERE/db-up.sh"
  echo "[2/5] Migrating tenant databases..."
  "$HERE/migrate.sh" 2>&1 | tee "$LOG_DIR/migrate.log"
else
  echo "[1-2/5] KEEP_DB=1 -> reusing existing DB (skipping teardown + migrate)."
  "$HERE/db-up.sh"
fi

echo "[3/5] Unit tests (full suite)..."
./gradlew -Penv=dev test --tests '*' -x rat -x licenseMain -x licenseTest 2>&1 | tee "$LOG_DIR/unit.log"
echo "unit gradle exit: ${PIPESTATUS[0]}"

echo "[4/5] Integration tests (starts embedded Tomcat; ~150 HTTP tests; slow)..."
./gradlew -Penv=dev integrationTest -x rat -x licenseMain -x licenseTest 2>&1 | tee "$LOG_DIR/integration.log"
echo "integration gradle exit: ${PIPESTATUS[0]}"

echo "[5/5] Done. Logs: $LOG_DIR/{migrate,unit,integration}.log"
echo "Tear down the DB with: docker/db-down.sh"

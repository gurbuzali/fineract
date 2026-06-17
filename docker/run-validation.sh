#!/usr/bin/env bash
# End-to-end validation against a real database:
#   fresh MySQL 5.7  ->  migrate tenant DBs  ->  full unit suite  ->  HTTP integration suite.
#
# Logs land in build/validation-logs/. The DB is left running for inspection; run
# docker/db-down.sh afterwards to destroy it.
#
# EXIT CODE POLICY (this is a real gate — it returns non-zero when validation fails):
#   * DB bring-up / migrate failure                         -> FAIL (exit 1)
#   * a Gradle stage fails for a non-test reason (compile/infra, i.e. no per-test
#     "Class > method FAILED" lines in its log)             -> FAIL (exit 1)
#   * any test failure OUTSIDE the documented allow-list    -> FAIL (exit 1)
#   * only allow-listed known-pre-existing failures present -> tolerated, stage PASS
#   * everything green                                      -> PASS (exit 0)
#
# Known pre-existing failures are tolerated EXPLICITLY (never blanket-swallowed). See
# .aura/artifacts/briefs/000-modernization-roadmap/preflight-baseline.md and the
# date-sensitivity note in project memory. To tolerate a newly-confirmed pre-existing
# failure, add it to ALLOW_RE below WITH a one-line justification - do not silence the gate.
#
# Usage:  docker/run-validation.sh            # clean run (drops any existing DB volume first)
#         KEEP_DB=1 docker/run-validation.sh  # reuse an already-migrated DB (skip down + migrate)
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
LOG_DIR="$ROOT/build/validation-logs"
mkdir -p "$LOG_DIR"
cd "$ROOT"

# --- Documented known-pre-existing failures tolerated by this gate (extended-regex, matched against "<fqcn> > <method>") ---
#   notification.TopicTest          : known-broken per CLAUDE.md - dereferences an unstubbed Mockito mock (NPE).
#   notification.StorageTest        : timing flake - refEq() compares a second-precision createdAt; straddling a
#                                     1-second boundary mismatches (passes ~3/5 reruns; not a regression).
#   ClientSavingsIntegrationTest > testSavingsAccountCharges : date-sensitive - 2016 relative-date logic on a 2026 clock.
ALLOW_RE='org\.apache\.fineract\.notification\.TopicTest|org\.apache\.fineract\.notification\.StorageTest|ClientSavingsIntegrationTest > testSavingsAccountCharges'

FAIL=0

# Per-test failures only: lines like "<fqcn> > <method> FAILED" (NOT ":test FAILED" / "BUILD FAILED").
fails_in() { grep -E ' > .+ FAILED$' "$1" 2>/dev/null | sed -E 's/ FAILED$//' | sort -u; }

# check_stage <label> <gradle_exit> <logfile>
check_stage() {
  local label="$1" rc="$2" log="$3" fails unexp
  if [ "$rc" -eq 0 ]; then
    echo "[$label] PASS (gradle exit 0)"
    return
  fi
  fails="$(fails_in "$log")"
  if [ -z "$fails" ]; then
    echo "[$label] FAIL - gradle exited $rc with no per-test FAILED lines (compile/infra error). See $log"
    FAIL=1
    return
  fi
  unexp="$(printf '%s\n' "$fails" | grep -vE "$ALLOW_RE" | grep -v '^$' || true)"
  if [ -n "$unexp" ]; then
    echo "[$label] FAIL - unexpected (non-allow-listed) test failures:"
    printf '%s\n' "$unexp" | sed 's/^/    /'
    FAIL=1
  else
    echo "[$label] PASS - only tolerated known-pre-existing failures:"
    printf '%s\n' "$fails" | sed 's/^/    /'
  fi
}

if [ "${KEEP_DB:-0}" != "1" ]; then
  echo "[1/5] Fresh DB container (dropping any existing volume)..."
  "$HERE/db-down.sh" >/dev/null 2>&1 || true
  if ! "$HERE/db-up.sh"; then echo "[db-up] FAIL - MySQL did not come up"; FAIL=1; fi
  echo "[2/5] Migrating tenant databases..."
  "$HERE/migrate.sh" 2>&1 | tee "$LOG_DIR/migrate.log"
  MIGRATE_RC=${PIPESTATUS[0]}
  if [ "$MIGRATE_RC" -ne 0 ]; then echo "[migrate] FAIL - exit $MIGRATE_RC (see $LOG_DIR/migrate.log)"; FAIL=1; fi
else
  echo "[1-2/5] KEEP_DB=1 -> reusing existing DB (skipping teardown + migrate)."
  if ! "$HERE/db-up.sh"; then echo "[db-up] FAIL - MySQL did not come up"; FAIL=1; fi
fi

echo "[3/5] Unit tests (full suite)..."
./gradlew --no-daemon -Penv=dev cleanTest test --tests '*' -x rat -x licenseMain -x licenseTest 2>&1 | tee "$LOG_DIR/unit.log"
UNIT_RC=${PIPESTATUS[0]}
check_stage "unit" "$UNIT_RC" "$LOG_DIR/unit.log"

echo "[4/5] Integration tests (starts embedded Tomcat; ~150 HTTP tests; slow)..."
./gradlew --no-daemon -Penv=dev integrationTest -x rat -x licenseMain -x licenseTest 2>&1 | tee "$LOG_DIR/integration.log"
INTEG_RC=${PIPESTATUS[0]}
check_stage "integration" "$INTEG_RC" "$LOG_DIR/integration.log"

echo "[5/5] Summary. Logs: $LOG_DIR/{migrate,unit,integration}.log"
echo "Tear down the DB with: docker/db-down.sh"
if [ "$FAIL" -ne 0 ]; then
  echo "VALIDATION FAILED"
  exit 1
fi
echo "VALIDATION PASSED (green, or only documented pre-existing failures tolerated)"
exit 0

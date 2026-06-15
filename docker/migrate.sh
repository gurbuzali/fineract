#!/usr/bin/env bash
# Migrate the Fineract databases against the running container (Flyway via the gradle tasks).
#   1. list_db  -> mifosplatform-tenants  (4 scripts: tenants registry + default tenant row)
#   2. core_db  -> mifostenant-default     (359 scripts: the full platform schema)
# Requires the DB container to be up (docker/db-up.sh) and JDK 8 active.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> migrateTenantListDB (mifosplatform-tenants)"
./gradlew -Penv=dev migrateTenantListDB -PdbName=mifosplatform-tenants -x rat -x licenseMain -x licenseTest "$@"

echo "==> migrateTenantDB (mifostenant-default) — 359 core_db migrations, takes a few minutes"
./gradlew -Penv=dev migrateTenantDB -PdbName=mifostenant-default -x rat -x licenseMain -x licenseTest "$@"

echo "Migrations complete."

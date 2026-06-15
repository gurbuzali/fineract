# Local database for Apache Fineract integration testing

A disposable **MySQL 5.7** container that stands in for the real database the Fineract integration suite
needs. Use it to run `./gradlew integrationTest` (and the platform itself) locally, then throw it away.

> **Why MySQL 5.7 and not 8.x?** This checkout is a 2016-era stack: Spring Boot 1.1.6, the
> `org.drizzle.jdbc:drizzle-jdbc:thin` 1.3 driver, and 363 Flyway migrations written for MySQL 5.x. The
> drizzle driver predates MySQL 8's `caching_sha2_password` auth and cannot connect to 8.x, and the 2016
> migrations rely on lenient SQL behaviour (zero-dates, non-`ONLY_FULL_GROUP_BY`, implicit `TIMESTAMP`
> defaults) that MySQL 8 rejects by default. 5.7 keeps `mysql_native_password` and tolerates the migrations.
> (The dev launcher uses embedded MariaDB4j ≈ MariaDB 10.1; MariaDB 10.x is an equally valid image here.)

## What's in here

| File | Purpose |
|------|---------|
| `docker-compose.yml` | MySQL 5.7 service: `root`/`mysql`, port 3306, `sql_mode=''`, utf8, native password. |
| `init/01-fineract-databases.sql` | Runs once on first boot — creates `mifosplatform-tenants` + `mifostenant-default` (and a `mifos`/`mysql` user for parity). |
| `db-up.sh` | Start the container and wait until healthy. |
| `db-down.sh` | Stop the container **and delete its data volume** (clean slate). |
| `migrate.sh` | Run the two Flyway migration tasks (list DB then tenant DB). |
| `run-validation.sh` | One-shot: fresh DB → migrate → full unit suite → integration suite. Logs to `build/validation-logs/`. |

## Credentials (fixed — matches the hard-coded build/app config)

| Who | User / Pass | Source |
|-----|-------------|--------|
| migrate tasks + bootstrap datasource + default tenant | `root` / `mysql` | `build.gradle` (`mysqlUser`/`mysqlPassword`), `src/test/resources/META-INF/context.xml`, `list_db/V1` |
| optional app/tenant user | `mifos` / `mysql` | `setBlankPassword` task / dev launcher |

Do **not** change these without also changing the build + `context.xml` — they are wired together.

## Prerequisites

- Docker Desktop running.
- JDK 8 active (`java -version` → `1.8.0_xxx`) — required by the Gradle 2.10 build. (`JAVA_HOME` is set via
  asdf in this checkout: `/Users/<you>/.asdf/installs/java/liberica-8u382+6`.)

## Quick start

```bash
# 1. Start the database
docker/db-up.sh

# 2. Migrate the tenant + core schemas (list_db: 4 scripts, core_db: 359 scripts)
docker/migrate.sh

# 3. Run the suites
./gradlew -Penv=dev test --tests '*' -x rat -x licenseMain -x licenseTest   # unit
./gradlew -Penv=dev integrationTest  -x rat -x licenseMain -x licenseTest   # integration (HTTP, ~150 tests)

# 4. Tear it all down
docker/db-down.sh
```

Or run everything in one go (clean DB → migrate → unit → integration, logs under `build/validation-logs/`):

```bash
docker/run-validation.sh
# reuse an already-migrated DB instead of rebuilding it:
KEEP_DB=1 docker/run-validation.sh
```

## Notes & gotchas

- **`-x rat -x licenseMain -x licenseTest`** is included in the scripts. The Apache RAT license-audit gate
  fails on this checkout (≈236 files without Apache headers — pre-existing, unrelated to tests); skipping it
  lets the build reach compile/package/test. (`licenseMain`/`licenseTest` are the same gate.)
- **First `migrate.sh` is one-shot.** Flyway records applied versions in each DB. Re-running against an
  already-migrated DB is a no-op (or errors if checksums drift). For a truly clean run, `db-down.sh` first
  (or use `run-validation.sh`, which does it for you).
- **`integrationTest` starts its own embedded Tomcat** (`tomcatRunWar`, daemon) on `:8080` / `:8443` and runs
  rest-assured tests against it. Make sure those ports are free. It is slow (tens of minutes).
- **Known pre-existing unit failures** (not regressions): `TopicTest` (×2, uninitialized Mockito) and the
  flaky `StorageTest` in the `notification` package.
- **Data volume** `fineract-mysql57-data` persists across `db-up`/`compose stop` so you can re-test without
  re-migrating; `db-down.sh` removes it.

# Fineract Dev Setup

Instructions for building, running, and testing this checkout locally on macOS.
The shipped `build.sh` is broken (references a missing `apps/community-app/`
sibling repo and a non-existent `fineract-provider/gradlew`); use the steps below
instead.

## Prerequisites

- **JDK 8** — required. The project sets `sourceCompatibility = 1.8` and uses
  Gradle 2.10, which won't run on newer JDKs. Example with asdf:
  ```bash
  export JAVA_HOME=/Users/ali/.asdf/installs/java/liberica-8u382+6
  export PATH=$JAVA_HOME/bin:$PATH
  java -version   # should print 1.8.0_xxx
  ```
- **Gradle 2.10** via the wrapper — `./gradlew` in the repo root must point at
  `gradle-2.10-bin.zip` (`gradle/wrapper/gradle-wrapper.properties`). Do **not**
  upgrade the wrapper; the `build.gradle` is from 2016 and uses APIs/plugins
  removed in modern Gradle.
- No external MySQL needed for `run` — the app uses embedded MariaDB4j when
  `-Penv=dev` is passed. Real MySQL is only needed for `integrationTest`.

## Build

```bash
./build.sh
# or directly:
./gradlew -Penv=dev build -x licenseMain -x licenseTest
```

- `-Penv=dev` is required everywhere — see the run section for why. Omitting it
  excludes top-level `Server*`/`MariaDB4j*` files from compilation.
- `-x licenseMain -x licenseTest` skips the Apache-header check (`license`
  plugin with `strictCheck true`). If you want the check, run
  `./gradlew -Penv=dev licenseFormatMain` first to auto-prepend missing headers.
- The output war lands at `<repo>/build/libs/` (note: `buildDir` is overridden
  to `../build` in `fineract-provider/build.gradle`, so it is **not** under
  `fineract-provider/build/`).

### Repository note

`build.gradle` originally used `jcenter()`, which JFrog shut down in 2022. Both
`buildscript` and project `repositories` blocks have been switched to the
Aliyun mirror (`https://maven.aliyun.com/repository/public`) so the legacy
plugins (`gradle-tomcat-plugin:1.0`, `license-gradle-plugin:0.11.0`,
`gradle-jrebel-plugin:1.1.2`, `gradle-openjpa:0.2.0`) still resolve.

## Run (embedded Tomcat + MariaDB4j)

```bash
./gradlew --no-daemon -Penv=dev run
```

- `-Penv=dev` is **required**. Without it, `sourceSets.main` excludes
  `**/Server*`, `**/MariaDB4j*`, and `EmbeddedTomcatWithSSLConfiguration.java`
  (see `build.gradle:332-340`), and `:run` fails with
  `Could not find or load main class org.apache.fineract.ServerWithMariaDB4jApplication`.
- `--no-daemon` keeps stdin attached so the `Hit Enter to quit...` prompt
  actually waits for input. The `run { standardInput = System.in }` block at
  the bottom of `build.gradle` is what makes this work.
- Server listens on **HTTP `:8080`** and **HTTPS `:8443`** at context path
  `/fineract-provider`. The cert is self-signed.
- The browser-launch logic in `ServerWithMariaDB4jApplication.main()` looks for
  the community-app UI under `apps/community-app/...` — that directory doesn't
  exist in this checkout, so the server logs an error and continues. API access
  still works.

### Smoke-test the running server

Default seed credentials are `mifos` / `password`. Every request needs the
`Fineract-Platform-TenantId` header.

```bash
curl -k -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://localhost:8443/fineract-provider/api/v1/offices
```

Expected: JSON array containing the seed "Head Office".

Pretty output:

```bash
curl -sk -u mifos:password \
  -H "Fineract-Platform-TenantId: default" \
  https://localhost:8443/fineract-provider/api/v1/offices | jq
```

Full API reference: <https://demo.openmf.org/api-docs/apiLive.htm>

## Tests

### Unit tests

```bash
./gradlew -Penv=dev test                                  # filtered to TemplateMergeServiceTest only
./gradlew -Penv=dev test --tests '*'                      # run everything under src/test/java
./gradlew -Penv=dev test --tests 'org.apache.fineract.template.TemplateMergeServiceTest'
```

`build.gradle:386-389` filters the `test` task to `TemplateMergeServiceTest` by
default. Override with `--tests`.

Known-broken tests when running unfiltered:

- `org.apache.fineract.notification.TopicTest` — uses `@Mock` fields but never
  initializes Mockito (no `@RunWith(MockitoJUnitRunner.class)` and no
  `MockitoAnnotations.initMocks(this)`). All mock fields are null, so both
  `testTopicStorage` and `testTopicSubscriberStorage` NPE. Skip or fix:
  ```bash
  ./gradlew -Penv=dev test --tests '*' --tests '!org.apache.fineract.notification.TopicTest'
  ```

HTML test report: `<repo>/build/reports/tests/test/index.html`

### Integration tests

Require a real MySQL instance on `root` / `mysql`:

```bash
mysql -u root -pmysql -e "CREATE DATABASE \`mifosplatform-tenants\`; CREATE DATABASE \`mifostenant-default\`;"
./gradlew -Penv=dev migrateTenantListDB -PdbName=mifosplatform-tenants
./gradlew -Penv=dev migrateTenantDB     -PdbName=mifostenant-default
./gradlew -Penv=dev integrationTest
```

`integrationTest` builds a war, starts Tomcat in daemon mode, and runs the
suite under `src/integrationTest/java`.

## Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| `Gradle requires JVM 17 or later to run` | Root wrapper was upgraded to Gradle 9. Restore the Gradle 2.10 wrapper (`distributionUrl=...gradle-2.10-bin.zip`). |
| `Could not find org.gradle.api.plugins:gradle-tomcat-plugin:1.0` (or other legacy plugin) | `jcenter()` is dead; ensure the Aliyun mirror is in both `buildscript.repositories` and the top-level `repositories` block. |
| `:licenseMain FAILED` | Run `./gradlew licenseFormatMain` to auto-add headers, or skip with `-x licenseMain -x licenseTest`. |
| `:run` fails with `Could not find or load main class …ServerWithMariaDB4jApplication` | Missing `-Penv=dev`. Top-level `Server*` and `MariaDB4j*` files are excluded from compilation otherwise. |
| Server prints `Hit Enter to quit...` then immediately shuts down | Gradle subprocess has no stdin attached. Use `--no-daemon` and ensure `run { standardInput = System.in }` is in `build.gradle`. |
| Browser fails to open community-app UI | Expected — `apps/community-app/` is a separate repo not present in this checkout. Use the API directly. |

# SCIP code-intelligence indexing (manual)

How to (re)generate the **SCIP** structural index for this checkout — a compiler-accurate
code-intelligence index over all Java sources, consumed by AURA's `aura-sense-compose`
(`structural-engine: scip`) for precise symbol/reference resolution during
`/aura.brief` → `/aura.plan` → `/aura.review`. SCIP is higher-fidelity than the default CRG
(tree-sitter) graph because it is emitted by the **actual compiler**.

> **TL;DR** — the automatic `aura scip-scan` / `scip-java index` **cannot** run here (it needs a
> modern Gradle). Produce the index manually by attaching the `semanticdb-javac` compiler plugin to
> the working `-Penv=dev` Gradle 2.10 build on JDK 8, then converting with `scip-java`. The result
> lands under `.aura/artifacts/codebase-intelligence/scip/` (gitignored / local-only).

## Why the automatic path doesn't work

`scip-java index` (and `aura scip-scan`, which wraps it) injects a Gradle build hook that calls
`TaskCollection.configureEach(Action)` — a **Gradle 4.9+** API. This project is pinned to
**Gradle 2.10** (the 2016 `build.gradle` uses APIs/plugins removed in modern Gradle and won't run on
a newer one — see `README-DEV.md`), so project configuration fails before any SemanticDB is produced:

```
FAILURE: Build failed with an exception.
* What went wrong:
A problem occurred configuring root project 'fineract-provider'.
> Failed to notify project evaluation listener.
   > org.gradle.api.tasks.TaskCollection.configureEach(Lorg/gradle/api/Action;)V
```

The Gradle version is the **only** blocker. **JDK 8 is fine** — scip-java 0.12.3 runs on JDK 8 and
its `semanticdb-javac` plugin is Java-8 bytecode that JDK 8 `javac` loads. The manual route below
bypasses scip-java's Gradle integration entirely by wiring the plugin into the build ourselves.

## How SCIP is produced (two stages)

```
compile WITH semanticdb-javac  →  *.semanticdb  →  scip-java index-semanticdb  →  index.scip  →  scip print --json
      (stage 1: javac plugin)     (intermediate)     (stage 2: pure conversion)      (index)        (json the consumer reads)
```

## Prerequisites

- **JDK 8 active.** The build is pinned to JDK 8 + Gradle 2.10. ⚠️ Selecting the asdf java version is
  not enough — **`JAVA_HOME` must point at the JDK 8 install**: the asdf shim resolves `java`→8 but
  `JAVA_HOME` can be stale (e.g. left pointing at a JDK 21), and **Gradle picks its JVM from
  `JAVA_HOME`** (Gradle 2.10 won't start on a modern JDK).
- **`scip-java` and `scip` CLIs** on `PATH` (`scip-java --version` → 0.12.3; `scip` → v0.7.1).
- **`semanticdb-javac-0.12.3.jar` at the repo root** — a self-contained fat jar (protobuf shaded in;
  registers `META-INF/services/com.sun.source.util.Plugin` = `SemanticdbPlugin`; Java-8 bytecode).
  Obtain with `coursier fetch com.sourcegraph:semanticdb-javac:0.12.3` (or extract from the scip-java
  coursier bootstrap) and copy it to the repo root. It is gitignored.
- **Full git history** — `git fetch --unshallow` (scip-java's preflight warns on shallow clones).

The outputs land under `.aura/artifacts/codebase-intelligence/scip/`, which is **gitignored**
(local-only); the jar and `.scip/` are gitignored / removable.

## Procedure

Run from the repo root. Safe on any branch — no Java source is modified; only a temporary
`build.gradle` block, reverted via `git checkout` in step 5.

### 1. Pin JDK 8

```bash
export JAVA_HOME="$(asdf where java liberica-8u382+6 2>/dev/null || echo "$HOME/.asdf/installs/java/liberica-8u382+6")"
export PATH="$JAVA_HOME/bin:$PATH"
java -version          # must print 1.8.0_xxx
```

### 2. Attach the plugin to every JavaCompile (temporary)

Append this block to `fineract-provider/build.gradle` (reverted in step 5).
`tasks.withType(JavaCompile)` covers **all** sourcesets — `compileJava`, `compileTestJava`,
`compileIntegrationTestJava`:

```groovy
// === TEMP: SemanticDB/SCIP instrumentation — DO NOT COMMIT. ===
def scipRepoRoot = rootProject.projectDir.parentFile
def scipPluginJar = new File(scipRepoRoot, "semanticdb-javac-0.12.3.jar")
def scipTargetroot = new File(scipRepoRoot, ".scip/targetroot")
tasks.withType(JavaCompile) {
    doFirst { scipTargetroot.mkdirs() }
    options.compilerArgs += [
        "-processorpath", scipPluginJar.absolutePath,
        "-Xplugin:semanticdb -sourceroot:" + scipRepoRoot.absolutePath + " -targetroot:" + scipTargetroot.absolutePath + " -text:on"
    ]
}
// === END TEMP ===
```

### 3. Compile all sourcesets (emits `*.semanticdb`)

```bash
rm -rf .scip/targetroot
./gradlew -Penv=dev clean compileJava compileTestJava compileIntegrationTestJava \
  -x rat -x licenseMain -x licenseTest --no-daemon --console=plain
```

- **`clean` is REQUIRED** — otherwise Gradle skips an up-to-date `compileJava`, so `javac` (and the
  plugin) never runs and **zero** `.semanticdb` are written.
- `-Penv=dev` keeps the dev sourceset (`Server*`/`MariaDB4j*` included); RAT/license checks skipped.
- Expect **3,041** `.semanticdb` under `.scip/targetroot` (2,865 main + 26 test + 150 integrationTest)
  = 100% of the repo's Java files. Drop `compileIntegrationTestJava` for a main+test-only index.

### 4. Convert SemanticDB → SCIP, dump JSON

```bash
OUT=.aura/artifacts/codebase-intelligence/scip
mkdir -p "$OUT"
scip-java index-semanticdb --output "$OUT/index.scip" .scip/targetroot
scip stats --from "$OUT/index.scip" | grep documents          # → "documents": 3041
scip print --json "$OUT/index.scip" > "$OUT/index.scip.json"
```

Then write `$OUT/status.json` with `state: "ok"` and `produced_at_commit` = `git rev-parse HEAD`
(plus `index_path`/`json_path`/`scip_java_version`/`scip_cli_version`) so `aura-sense-compose` trusts
it — copy the shape from the existing `status.json`.

### 5. Revert + clean up

```bash
git checkout -- fineract-provider/build.gradle    # remove the TEMP block (verify: grep -c 'TEMP: SemanticDB' returns 0)
rm -rf .scip/targetroot                            # ~100 MB intermediate
```

## Coverage / consistency notes

- Verified index: **3,041 documents = 100% of repo Java files**, produced on `e2e-448` @ `4b4aa3b5`.
  Java source there is identical to the CRG/aura-sense scan commit `b6c67da4` (only `.gitignore`
  differs) — so SCIP and CRG describe the same source and are mutually consistent.
- `scip-java index-semanticdb` only indexes what you compiled — to cover a sourceset, compile it.
- `status.json`'s `produced_at_commit` is what the `aura-sense-compose` freshness gate
  equality-checks; re-run (or re-stamp) when the indexed source moves to a new commit.

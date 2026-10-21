# OmniCalc — Phase 0: Initial Repository & Environment Audit

Audited: 2026-10-21 (UTC) · Branch: `arena/5066faab-omnicalc-android` · Base commit: `def2a4a410f0c768f729f7460fb878c5339d51e0`

All findings below were **verified by direct execution** in the build environment unless explicitly marked otherwise.

---

## 1. Repository state

| Item | Finding |
|---|---|
| Remote | `https://github.com/shohanbusinessmail-cmd/omnicalc-android.git` (public, push permission confirmed) |
| Default branch | `main` |
| Contents at audit time | `README.md` only (`# omnicalc-android`) — effectively an **empty repository** |
| Existing app ID / package | none |
| Existing Kotlin/AGP/Gradle config | none |
| Existing modules, screens, assets, tests | none |
| Existing CI/CD, signing, artifacts | none |
| Git status | clean working tree on `arena/5066faab-omnicalc-android` |
| GitHub Actions | enabled (public repo → unlimited CI minutes); `gh` CLI authenticated in sandbox |

**Conclusion:** Per master-instruction rule 3, the project must be initialized as a proper native Android project. There is no existing work to preserve or regress.

## 2. Host environment

| Item | Finding (verified) |
|---|---|
| OS | Debian GNU/Linux 12 (bookworm), x86_64 |
| CPU / RAM / disk | 2 cores · 3.8 GiB RAM · ~20 GiB free on `/` |
| Python | 3.11.2 (present) |
| Node/npm | present (yarn 1.22.22 present) |
| Java/JDK | **not installed** (`java: command not found`) |
| Gradle | **not installed** |
| Android SDK (`ANDROID_HOME`, sdkmanager, adb, avdmanager) | **not present anywhere on disk** |
| Emulator / devices | **none**; `/dev/kvm` absent → hardware acceleration unavailable |
| `sudo` | available, but `apt-get update` **fails** (see §3) |
| Outbound network | allow-listed (see §3) |

## 3. Network reachability (verified with direct requests)

| Host | Result | Needed for |
|---|---|---|
| `github.com`, `api.github.com`, `codeload.github.com` | ✅ reachable | git, source archives, GitHub API/Actions |
| `registry.npmjs.org` | ✅ reachable | npm packages |
| `pypi.org`, `files.pythonhosted.org` | ✅ reachable | pip packages |
| `dl.google.com` | ❌ blocked | **Android SDK command-line tools, platforms, build-tools** |
| `maven.google.com` / `google()` Maven | ❌ blocked | **AGP, AndroidX, Jetpack Compose, Room, DataStore** |
| `repo.maven.apache.org` / Maven Central | ❌ blocked | **Kotlin Gradle Plugin, coroutines, JUnit** |
| `services.gradle.org` | ❌ blocked | **Gradle distribution download** |
| `plugins.gradle.org` | ❌ blocked | Gradle plugin portal |
| `deb.debian.org` (apt) | ❌ blocked | installing packages via apt |

### Consequence (hard blocker)

**A full Android build (APK/AAB), Android Lint, instrumented tests and emulator runs cannot execute inside this sandbox**, because the Android SDK, Gradle distribution and every Maven dependency (AGP/Compose/AndroidX/Kotlin-GradlePlugin/JUnit) live on hosts that are not reachable. This is an infrastructure restriction, not a project defect. It is documented here as required by master-instruction rule 17; every feasible part of the build is still executed and verified (see §6 and `docs/TESTING.md`).

## 4. Toolchain recovered through allowed channels

Because PyPI/npm/GitHub are reachable, the following **verified-working** toolchain was provisioned locally (outside the repo, in `/home/user/tools`):

| Tool | Source | Verification |
|---|---|---|
| **OpenJDK 25.0.2** (Temurin LTS) | PyPI wheel `jdk4py==25.0.2.1` | `java -version` → `openjdk 25.0.2` |
| **Kotlin compiler 2.4.21** (`kotlinc`, JVM target, incl. `compose-compiler-plugin.jar`) | npm package `kotlin-compiler@2.4.21` | compiled and ran a test program successfully |
| **Gradle wrapper** (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) | fetched from `gradle/gradle` repository via GitHub API | wrapper jar archive validated |

With these, the **pure-JVM domain modules (`core:math`, `core:common`) are compiled and unit-tested locally for real**. The wrapper in the repo will download the pinned Gradle distribution in environments that can reach `services.gradle.org` (GitHub Actions, owner's machine).

## 5. Version selection (checked against reachable sources)

Chosen baseline (real, mutually compatible versions; kept conservative so CI resolves them reliably):

| Component | Version | Verification |
|---|---|---|
| Gradle | 8.9 (distribution URL pinned in wrapper) | `gradle/gradle` tag `v8.9.0` exists; AGP 8.7 requires Gradle ≥ 8.9 |
| Android Gradle Plugin | 8.7.3 | published on `maven.google.com` (resolved in CI) |
| Kotlin | 2.0.21 | release confirmed on `JetBrains/kotlin` |
| KSP (Room codegen) | 2.0.21-1.0.27 | **tag confirmed** on `google/ksp` releases |
| Compose BOM | 2024.10.01 | resolves from `maven.google.com` (CI) |
| Compose Compiler | `org.jetbrains.kotlin.plugin.compose` 2.0.21 | ships with Kotlin 2.0.21 |
| Room | 2.6.1 | resolves from `maven.google.com` (CI) |
| JDK for CI | Temurin 17 | `actions/setup-java` |
| Local verification JDK | Temurin 25 (jdk4py) | used only for `kotlinc` JVM runs |

No version numbers were invented for endpoints or APIs. Anything that could only be proven by a blocked host is verified by the CI run instead, and results are recorded in `docs/TESTING.md`.

## 6. What can and cannot run in this environment

### Runs here (executed and recorded)
- Full Kotlin compilation and unit-test execution of the pure-JVM domain modules via local `kotlinc` + a JUnit-compatible runner (same test sources are also wired to real JUnit 4 in Gradle).
- Static review of project configuration, manifest, permissions, resources.
- Git push and **GitHub Actions CI builds** (full Gradle build, Lint, unit tests, debug APK/AAB artifacts) — executed by pushing to the session branch; results are pulled back and inspected with `gh`.

### Cannot run here (documented, not faked)
- Local `./gradlew` execution (Gradle distribution host unreachable) → replaced by CI.
- Android SDK installation, local Lint, local APK/AAB assembly → replaced by CI.
- Emulator / instrumented UI tests (no SDK, no `/dev/kvm`) → JUnit/Robolectric-free unit coverage runs in CI; on-device tests deferred, see `docs/TESTING.md`.
- Release signing → no keystore/credentials exist or may be created without owner authorization (rule 15). CI produces an **unsigned** release build + debug APK; signing steps are documented for the owner in `docs/RELEASE.md`.

## 7. Risk & dependency review of existing state

- No existing dependencies to audit (empty repo).
- No secrets in the repository.
- No permissions declared yet (target: **zero** dangerous permissions; calculator tool works fully offline).

## 8. Baseline architecture (summary)

Native Android, Kotlin + Jetpack Compose + Material 3, MVVM with unidirectional data flow (`ViewModel` + `StateFlow`), offline-first.

```
:app                  → MainActivity, NavGraph (5 bottom destinations), global search, About, splash
:core:math            → pure-JVM math engine: tokenizer/Pratt parser, evaluator, percent semantics,
                        BigDecimal financial math, formatting, units registry, date math, bases
:core:common          → pure-JVM shared models & utilities (digits, grouping, results, preferences model)
:core:database         → Room history/favorites + DataStore settings + repositories (offline-first)
:core:designsystem    → Material 3 theme (light/dark/dynamic-free), colors, type, shared composables
:core:localization    → `en` + `bn` string resources (values/ and values-bn/)
:feature:calculator   → standard calculator (expression editing, memory, percent semantics)
:feature:scientific    → scientific mode (trig, logs, constants, DEG/RAD, matrices/complex subset)
:feature:formulas      → searchable formula catalog + step-by-step calculators
:feature:converter     → converter hub (SI/customary/land/currency-manual) with dimensional safety
:feature:finance      → BD-first money tools: VAT/tax engine, salary, loans, profit/discount
:feature:programmer    → base conversion + bitwise ops with defined overflow semantics
:feature:graph         → function grapher with adaptive sampling & discontinuity handling
:feature:library       → history, favorites, settings, language/theme/numeral preferences
```

All calculation logic lives in `:core:math` (and pure helpers in `:core:common`) — UI never contains business math.

## 9. Implementation sequence (prioritized checklist)

The living feature status is tracked in `docs/FEATURE_MATRIX.md`. Sequence:

1. **P0** Audit (this document). ✅
2. **P1** Project skeleton + version catalog + CI workflow (get a green `:app` build in Actions). 
3. **P2** `:core:math` engine + comprehensive unit tests (run locally with `kotlinc`).
4. **P3** Design system (light/dark, tokens from spec §2.1), 5-destination navigation shell, About screen with exact owner info.
5. **P4** Standard calculator (expression input, keypad, memory, percent semantics, copy/share).
6. **P5** Persistence: Room history/favorites, DataStore settings, theme/language/numeral preferences.
7. **P6** Bengali localization (`bn`) + numeral/grouping engine + Bangladesh defaults.
8. **P7** Scientific calculator.
9. **P8** Converter hub + land-unit profiles (configurable regional definitions).
10. **P9** Formula library (geometry/everyday/finance/stats catalog).
11. **P10** Finance: configurable VAT/tax engine, salary, loan/interest tools (assumptions exposed).
12. **P11** Programmer calculator; graph plotter; date/time tools.
13. **P12** Global search, favorites, export/share.
14. **P13** Accessibility pass, privacy doc, performance review.
15. **P14** Documentation set, release artifacts (CI), handoff report (`docs/HANDOFF.md`).

## 10. Approval-gated items (require owner authorization — not performed)

- Production keystore creation / signing (rule 14/15).
- Google Play submission (rule 15).
- Any paid service or API key provisioning (none needed for offline features).

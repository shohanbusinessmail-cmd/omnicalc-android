# OmniCalc

**All Your Math. One Place.** — an offline-first calculator suite for Android, Bangladesh-first and international.

| | |
|---|---|
| App name | OmniCalc |
| Tagline | All Your Math. One Place. |
| Developer / Owner | Shohan Khan |
| Email | helloiamshohan@gmail.com |
| Platform | Android 7.0+ (minSdk 24), target 35 |
| Stack | Kotlin · Jetpack Compose · Material 3 · MVVM · Room · DataStore |
| Permissions | **None** (offline-first, no network, no tracking) |

## What it does

- **Calculate** — standard calculator: editable expression, live preview, memory keys (MC/MR/M+/M−/MS), history with search, copy/share, Bengali/English UI, ০-৯ numeral option.
- **Scientific** — trig/log/power/roots/factorials with a persisted DEG·RAD·GRAD toggle; one shared engine, one shared ViewModel.
- **Formulas** — bilingual catalog of 18 real formulas (geometry, algebra, finance, physics) with variable inputs, computed live by the shared engine; favorites + history.
- **Tools**
  - Unit converter (8 categories incl. exact rational temperature conversion)
  - Programmer calculator (BIN/OCT/DEC/HEX readouts + AND/OR/XOR/NOT/shifts/arithmetic on BigInteger)
  - Date & time (day difference, ±days, age — pure proleptic-Gregorian math)
  - Land units (editable regional profiles with source + effective-date metadata)
  - Salary (user-defined earning/deduction rows → net + %)
  - VAT (inclusive/exclusive, configurable rate, provenance fields)
  - Currency (offline table; rates are **only** what you enter, each with source + date)
  - Graph plotter (y = f(x) sampled through the shared engine, gaps where undefined)
- **Library** — history (search/filter/clear), favorites (calculations + formulas), and complete settings: theme, language, numerals, grouping, precision, angle, percent mode, haptics, reset.

All calculation logic lives in the pure-JVM `core:math` engine (own tokenizer/parser/evaluator — `eval()` is never used) and is covered by a local JUnit harness plus CI.

## Build & test

```bash
# Local JVM tests (no Android SDK required) — 85 tests:
./scripts/run_jvm_tests.sh

# Full CI (GitHub Actions): lint + unit tests + assembleDebug
# — see .github/workflows/ci.yml
./gradlew test lint assembleDebug
```

Release artifacts (unsigned APK/AAB + SHA256SUMS.txt) are produced by the
`release-artifacts` CI job on `workflow_dispatch` or a `v*` tag.

## Honest status

See [docs/RELEASE.md](docs/RELEASE.md) and [docs/CHANGELOG.md](docs/CHANGELOG.md)
for verified status, known limitations (no emulator/UI-instrumentation in this
environment, unsigned artifacts, no publishing) and the roadmap. No feature is
claimed as done unless it is implemented, compiled and tested.

## Documentation

| Document | Purpose |
|---|---|
| [docs/INITIAL_AUDIT.md](docs/INITIAL_AUDIT.md) | Starting-point audit & constraints |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, DI, navigation, state |
| [docs/FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md) | Spec coverage with verification state |
| [docs/MATH_ENGINE.md](docs/MATH_ENGINE.md) | Engine contract, precedence, guards |
| [docs/FORMULA_CATALOG.md](docs/FORMULA_CATALOG.md) | Formula list & data model |
| [docs/REGIONAL_RULES.md](docs/REGIONAL_RULES.md) | Land/VAT/currency configurability policy |
| [docs/LOCALIZATION.md](docs/LOCALIZATION.md) | en/bn strategy, numerals, per-app locale |
| [docs/NETWORK_AND_DATA.md](docs/NETWORK_AND_DATA.md) | Offline-first analysis |
| [docs/PRIVACY.md](docs/PRIVACY.md) | Permissions, storage, data handling |
| [docs/TESTING.md](docs/TESTING.md) | Test strategy + how to run |
| [docs/RELEASE.md](docs/RELEASE.md) | Build types, artifacts, signing posture |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | Dated changelog |

Owner: Shohan Khan · helloiamshohan@gmail.com

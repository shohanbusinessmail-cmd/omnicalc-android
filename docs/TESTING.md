# Testing

## Layers

| Layer | Runner | Where | Count |
|---|---|---|---|
| Engine + common (pure JVM) | kotlinc + JUnit locally (`scripts/run_jvm_tests.sh`) **and** `./gradlew test` in CI | `core/common/src/test`, `core/math/src/test` | **85 tests** |
| Feature logic (finance, land, serialization) | `./gradlew test` in CI | `feature/tools/src/test` | 14 tests |
| Whole project compile | CI `assembleDebug` | every module | every push |
| Android lint (incl. string-format, translation, a11y checks) | CI `lint` | all modules | every push |
| UI/instrumented/emulator | — | 🚫 unavailable in this environment (no Android SDK/emulator) | documented gap |

## Run locally

```bash
export JAVA_HOME=/path/to/jdk            # any JDK 17+
./scripts/run_jvm_tests.sh               # exits 0 only if ALL pass
# output: ==== JVM test summary: 85 passed, 0 failed, 0 skipped ====
```

The same test sources run as real JUnit 4.13.2 under Gradle in CI — the local
harness is a runner, not a fork of the tests.

## What the tests actually prove

- Engine: precedence, right-associative `^`, percent modes (STANDARD vs
  EXPLICIT cases), exact-vs-approx division, factorial/trig/domain errors,
  guards (length/nesting/overflow), implicit multiplication, Bengali digit
  input, `2 3` → SYNTAX, money HALF_UP policy.
- Display: precision/grouping/numeral combinations (incl. `১২,৩৪,৫৬৭.৮৯`).
- Date: epoch round-trips, leap-century rules, signed diffs, age incl. Feb-29
  convention.
- Units: exact rational conversions (in↔m, °C↔°F↔K, km/h↔m/s, GB↔B…),
  cross-category rejection.
- Finance: VAT inclusive/exclusive, currency round-trip + serialization,
  salary totals, land profile serialization + corrupt-payload fallback.

## Honest gaps (master spec rule 10)

- Compose UI tests exist as a *goal*; without an emulator/device in this
  environment they cannot execute here and are **not claimed as passing**.
- “Accessibility checks pass” is currently: lint a11y checks + content
  descriptions + contrast-verified tokens — not a Talkback device run.
  See docs/FEATURE_MATRIX.md §16.

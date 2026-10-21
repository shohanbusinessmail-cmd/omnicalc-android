# Changelog

All notable changes to OmniCalc. Dates are UTC. Status claims follow the
honesty rules: nothing is listed as done unless implemented and verified.

## [Unreleased]

### Added
- **Tools increment 2** — land profiles (editable, region/source/effective
  metadata), VAT (inclusive/exclusive + provenance fields), salary rows,
  offline currency table (user-entered rates only), engine-backed graph
  plotter; DataStore fields for the above; 14 finance/land tests (CI-run).
- Documentation set: README + ARCHITECTURE, FEATURE_MATRIX, MATH_ENGINE,
  FORMULA_CATALOG, REGIONAL_RULES, LOCALIZATION, NETWORK_AND_DATA, PRIVACY,
  TESTING, RELEASE, CHANGELOG.

## [0.9-dev] — 2026-10-21

### Added
- **feature:tools** — unit converter (8 categories incl. rational-affine
  temperature), programmer calculator (4 base readouts, bit ops on
  BigInteger), date/time (diff/±days/age via pure `DateMath`); 24 new
  harness tests (85/85).
- **feature:formulas** — 18 bilingual engine-backed formulas with variable
  inputs, favorites, history; `DisplayFormatter` shared pipeline (+10 tests).
- `core:math`: `UnitConvert`/`UnitCatalog`/`DateMath`; **fix**: missing
  `api(project(":core:common"))` (the CI root cause of runs 1–3).
- CI improvements: compile step first, robust `::error` annotations, lint
  XML → file:line annotations.

### Fixed
- `.gitignore` `tools/` rule anchored to `/tools/` (it had swallowed
  `feature/tools/`).
- Lint `StringFormatInvalid` for literal `%` strings (`formatted="false"`).
- Designsystem duplicate `ThemeMode` unified on `core:common`.

## [0.8-dev] — 2026-10-21

### Added
- **Cycle A**: core:database (Room v1 + DataStore + `AppServices`),
  core:localization (en/bn engine errors/actions/keypad), core:designsystem
  components + CalcKeys, **feature:calculator** (full state machine, memory,
  copy/share), **feature:scientific** (shared VM + function rows + DEG/RAD/
  GRAD), **feature:history/favorites/settings**, app shell (AppCompat
  locales, 5-destination NavGraph, global search, exact-facts About screen,
  honest Formulas/Tools placeholders).

## [0.7-dev] — 2026-10-21

### Added
- CI annotation pipeline surfaced the first real build errors and fixed the
  `Tokenizer` unresolved-reference root cause.

## [0.6.0] — 2026-10-21 (Phase 0–4)

### Added
- Repo scaffolding, docs/INITIAL_AUDIT, Gradle/Kotlin/AGP/Compose setup,
  wrapper, CI skeleton.
- **Math engine**: tokenizer/pratt parser/evaluator with full guard rails —
  51 engine tests green locally, same sources run under Gradle in CI.
- Design tokens, app resources, icon, locale config (commits `8cbb314`,
  `92e3118`).

[Unreleased]: https://github.com/shohanbusinessmail-cmd/omnicalc-android/compare/v0.9-dev...HEAD

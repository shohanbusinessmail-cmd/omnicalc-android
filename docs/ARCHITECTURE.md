# OmniCalc — Architecture

## Principles

1. **Real logic only** — every visible control executes code from a module
   below; nothing is mocked, faked or hardcoded (master spec rule 7).
2. **Own math engine** — `core:math` tokenizes/parses/evaluates with a Pratt
   parser; `eval()`, `Runtime.exec` and script engines are never used.
3. **Offline-first** — no module declares an INTERNET permission; persistence
   is local (Room + DataStore).
4. **Modular, boring MVVM** — Compose UI → ViewModel → repositories/engine.
   DI is a single manual container (`AppServices`) — no Hilt, because it
   would add plugins/complexity without earning them at this scale.

## Module graph

```
                        ┌────────────── app ──────────────┐
                        │  MainActivity · NavGraph · About │
                        │  Search · OmniCalcApp (DI root)  │
                        └──┬────────┬────────┬────────┬───┘
            feature:calculator │  feature:scientific  feature:library  feature:formulas  feature:tools
                        └──┬────────┴────────┬┴───────────┴───────┘
        core:designsystem  │  core:localization  core:database
                           └──────────┬───────────┘
                                core:math ──► core:common
```

| Module | Contents |
|---|---|
| `core:common` | Pure JVM: numerals (০-৯ ⇄ 0-9), South Asian grouping, preference enums, `AppSettings`, `DisplayFormatter` |
| `core:math` | Pure JVM: tokenizer → parser → evaluator, `Engine` facade, `ErrorCode`/`CalcResult`, `UnitConvert` + `UnitCatalog`, `DateMath`, `NumericPolicy` (guards, rounding) |
| `core:localization` | en/bn string resources shared by features + `EngineErrorStrings` (ErrorCode → R.string) |
| `core:database` | Room v1 (`history`, `favorites`), DataStore `SettingsRepository`, repository classes, `AppServices` |
| `core:designsystem` | Theme tokens (contrast-checked), `CalcKey`/`CalcKeyOutlined`, shared components |
| `feature:*` | Screen + ViewModel pairs, one per spec §5/§6/§9/§11–§14/§15–§17 |
| `app` | Activity, Application (implements `AppServices`), Navigation Compose graph (5 tabs + search + about), About screen, search index |

## Dependency injection

```kotlin
interface AppServices {   // core:database
    val historyRepository: HistoryRepository
    val favoritesRepository: FavoritesRepository
    val settingsRepository: SettingsRepository
}
```

`OmniCalcApp : Application(), AppServices` provides lazy singletons. Feature
ViewModels recover their dependencies through CreationExtras:

```kotlin
viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as AppServices
        CalculatorViewModel(app, createSavedStateHandle(), errorMapper)
    }
}
```

## Navigation

Single-activity, Navigation Compose. Five `NavigationBar` destinations
(Calculate, Scientific, Formulas, Tools, Library) + `search` + `about` routes.
Reuse hand-offs (Library → owner screen) are shell-level `rememberSaveable`
state: `pendingExpression`, `pendingFormulaId`.

## State

- `MutableStateFlow<UiState>` in every ViewModel, collected with
  `collectAsStateWithLifecycle()`.
- Expression + cursor survive rotation via `SavedStateHandle`
  (`calc_expression`, `calc_cursor`).
- Settings/memory/land profiles/currency/VAT metadata persist via DataStore.

## Result formatting pipeline (single path)

```
BigDecimal ─► precision (AUTO strip / FIXED setScale HALF_UP)
          ─► NumberGrouping.group (international | South Asian)
          ─► Numerals.applyNumeralStyle (0-9 | ০-৯)
```

Implemented once in `core:common/DisplayFormatter` (unit-tested), used by the
calculator, formulas and every tool.

## Testing seams

- `core:common` and `core:math` are JVM-only → compiled and tested locally
  with kotlinc + JUnit (`scripts/run_jvm_tests.sh`, 85 tests).
- Android modules are compiled/linted/tested by GitHub Actions, the only
  builder available in this environment (see docs/TESTING.md).

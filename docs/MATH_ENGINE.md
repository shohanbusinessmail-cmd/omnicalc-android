# Math Engine (`core:math`)

Pure Kotlin/JVM. No Android, no network, no `eval()`. Facade:
`Engine.evaluate(input, context) → CalcResult`.

## Pipeline

```
input (user text)
  → Tokenizer      folds × → *, ÷ → /, −(U+2212) → -, ** → ^; normalizes ০-৯ digits
  → Parser         Pratt parser; known functions + KNOWN_CONSTANTS + supplied variable names
  → Evaluator      BigDecimal semantics with documented percent/angle modes
  → CalcResult.Success(value, exact) | CalcResult.Failure(CalcError(code, message, position))
```

Errors are typed (`ErrorCode`), localized via `core:localization/
EngineErrorStrings`, and never silently become `0`/`NaN`/`∞`.

## Operator precedence (lowest → highest)

`+ −` < `× ÷ mod`/implicit-multiplication < unary `−` < `^` (right-associative)
< postfix `!` `%` `°`

## Percent semantics

| Mode | `200 + 10%` | `200 × 10%` | `10%` alone |
|---|---|---|---|
| `STANDARD` (default) | 220 | 20 | 0.1 |
| `EXPLICIT` | 200.1 (percent = ÷100 always) | 20 | 0.1 |

`STANDARD` implements the everyday calculator rule: after `+ −`, percent is
relative to the left operand; after `× ÷` and standalone it is ÷100.
`%` also exists as an explicit ÷100 postfix anywhere.

## Angle & units

- `AngleMode.DEG/RAD/GRAD` from settings; literal `°` suffix converts its
  operand into the current angle mode (so `sin(90)` = 1 in DEG).
- Transcendentals compute in `Double`, then convert to BigDecimal with 15
  significant digits (HALF_UP) — documented in `NumericPolicy`.

## Constants

`π = 3.141592653589793` (Double→BigDecimal policy), `e = 2.718281828459045`.

## Division / precision policy

1. Exact-first: if the quotient terminates (or has ≤50 fraction digits) with
   scale ≤ 50, return the exact BigDecimal (`exact = true`).
2. Otherwise divide at `MathContext(50)` then trim (`exact = false`).
3. **Money**: `NumericPolicy.money(x)` uses `HALF_UP` at 2 dp — invoices
   round HALF_UP while a HALF_EVEN default would give 2.345 → 2.34 (tests
   document this choice).

## Guard rails (all tested)

| Guard | Limit |
|---|---|
| Input length | ≤ 1000 chars |
| Nesting depth | ≤ 64 |
| Factorial argument | ≤ 10000 |
| Exponent magnitude | ≤ 10⁶ (pre-check via `n·log₁₀|base| ≤ 308`, post-check `|result| ≤ 1E+308`) |
| Defensive net | unexpected exceptions → `ErrorCode.SYNTAX`, never a crash |

## Accepted syntax notes

- `2 3` → **SYNTAX** (number-after-number guard); `2 π`, `(2)(3)` → implicit
  multiplication.
- Unknown identifier before `(` → `UNKNOWN_FUNCTION`; bare unknown identifier
  → `UNKNOWN_VARIABLE`.
- Bengali/mixed digits anywhere are normalized to Western first.
- Variables come from `EvalContext.variables` (formulas, graph `x`, tools).

## Public companions

- `UnitConvert`/`UnitCatalog` — exact rational unit conversion (affine for
  temperature); regional land units deliberately excluded.
- `DateMath` — proleptic-Gregorian epoch-day arithmetic (diff/add/age).

## Tests

`core/math/src/test` runs in `scripts/run_jvm_tests.sh` locally **and** as
real JUnit under `./gradlew test` in CI (same sources, two runners).

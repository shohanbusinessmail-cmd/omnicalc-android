# Localization

## Languages

- `values/strings.xml` (en, default) + `values-bn/strings.xml` (bn) — parity
  is enforced in CI-adjacent checks during development; every string exists
  in both files (26 app + 100+ shared + 40 library strings at last check).
- Per-app language switch (spec §8.1): `LanguageMode.SYSTEM | ENGLISH |
  BENGALIC` persisted in DataStore; `MainActivity` maps it to
  `AppCompatDelegate.setApplicationLocales` (works on API 24+ through
  `AppLocalesMetadataHolderService` with `autoStoreLocales=true`; native on
  API 33+). DataStore is the single source of truth — reset also resets the
  locale.
- `android:localeConfig` declares en/bn for the system settings sheet.

## Where strings live

| Location | Scope |
|---|---|
| `app` | app name/tagline, nav labels, About facts, search, interim notes |
| `core:localization` | everything a feature needs to show: engine errors, actions, keypad a11y, settings labels, formula/tool chrome |
| `feature:library` | (strings moved up to core:localization — single shared home) |

Kotlin code references shared strings as
`import com.shohan.omnicalc.core.localization.R as LocR`.

## Engine errors

`EngineErrorStrings.messageRes(ErrorCode) → R.string.engine_error_*` with a
generic fallback; the English engine message remains available for logs/tests
but is never shown when a translation exists.

## Numerals & grouping

- `Numerals`: `toWesternDigits/toBengaliDigits/normalizeToWesternDigits/
  applyNumeralStyle`; Bengali digits are accepted anywhere input is read
  (calculator, converter fields, variables, search).
- `NumberGrouping`: international vs South Asian (last-3 then pairs).
- Applied through one pipeline — `DisplayFormatter` — so the calculator,
  formulas and tools behave identically (docs/ARCHITECTURE.md).

## Catalog text inside Kotlin

Formula/tool display names are bilingual `*En`/`*Bn` fields chosen at
runtime from `LocalConfiguration.current.locales` (strings.xml is not a fit
for structured catalog data with expressions).

## A11y localization

Keypad keys carry localized `contentDescription`s (`key_desc_*`), result
announcements use `label_result + value`, icon buttons all have descriptions.

# Regional Rules (Bangladesh-first, then international)

Master rules (spec §7, hard constraints): **never** hardcode variable tax
rates, exchange rates or land measurements as universal constants; everything
regional must be user-configurable and carry source/effective-date metadata.

## Land units (`feature:tools/LandProfiles.kt`)

- Base unit: **square foot** (exact definitions like 1 decimal = 43.56 sq ft
  are fixed by unit definition, not by region).
- Katha/Bigha/… differ by district — they exist only inside **profiles**:
  ```
  LandProfile(id, nameEn, nameBn, region, source, effectiveDate, units{→sq ft})
  ```
- Built-in profiles are **reference starting points**, visibly labeled:
  - *Bangladesh — common reference* (katha 720 sq ft, bigha variants, …)
  - *Dhaka Division — reference* (locally circulated figures)
  - both carry `source` text stating they must be verified locally and an
    `effectiveDate`.
- Users can edit any unit’s sq-ft factor, the source note and the date, then
  Save (persisted in DataStore; serialization round-trip tested). *Reset to
  built-ins* is one tap.
- History records store the profile id + source snippet in `contextPairs`.

## VAT / tax (`VatViewModel` + formula `vat-amount`)

- The **rate is always an input** (no “15%” baked in anywhere).
- Inclusive/exclusive modes with exact BigDecimal math (FinanceTools tests).
- Provenance fields — *Source* and *Effective date* — are first-class,
  persisted (`vat_source`, `vat_effective`), and stored with each history row.

## Currency (`FinanceTools.CurrencyRow`)

- Offline-first: **no network calls, ever** (docs/NETWORK_AND_DATA.md).
- The app **does not ship exchange rates**. The table lists currency codes
  and names; `rate`, `source`, `effectiveDate` start empty and are filled by
  the user from their own verified source. Conversion is disabled until a
  rate exists (clear on-screen prompt — no fabricated numbers).
- Editing a row persists all three fields; history captures them.

## Salary

- Entirely user-defined rows (earnings/deductions). No hardcoded grade
  structures, no assumed tax slabs. Totals + deduction % computed live.

## Number formats

- Grouping (international `1,234,567` vs South Asian `12,34,567`) and
  numerals (0-9 vs ০-৯) are **settings**, never forced by locale — a user can
  mix freely (docs/LOCALIZATION.md).

## Date conventions

- Proleptic Gregorian everywhere (docs/MATH_ENGINE.md §DateMath).
- Age: Feb-29 birthdays count on Mar 1 in common years (documented choice,
  tested in `DateMathTest`).

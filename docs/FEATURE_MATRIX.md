# Feature Matrix

Status vocabulary (honest, per master spec rules 10/17):
**✅ Done** = implemented + compiled + tests/lint pass in CI ·
**🟡 Partial** = working core, noted gap ·
**⏳ Next** = not yet built ·
**🚫 Blocked** = environment blocker (documented).

| Spec area | State | Verification |
|---|---|---|
| §2 Platform/language architecture | ✅ | Kotlin+Compose+M3+MVVM modular; CI `assembleDebug` green |
| §2.1 Design system | ✅ | `core:designsystem`, contrast notes in Theme.kt; lint clean |
| §2.2 Theme settings (system/light/dark) | ✅ | DataStore-backed, applied in MainActivity |
| §2.3 Number formatting | ✅ | `DisplayFormatter` + 10 harness tests (grouping/numerals/precision) |
| §2.4 Haptics setting | ✅ | Key handlers honor `hapticsEnabled` |
| §2.5 Five bottom-nav destinations | ✅ | NavGraph routes calculate/scientific/formulas/tools/library |
| §4 Standard calculator | ✅ | VM state machine; engine harness (51 engine tests); CI compile+lint |
| §4.7 History persistence | ✅ | Room DAO + repository dedup; wired from `=` |
| §4.6 Memory MC/MR/M+/M−/MS | ✅ | Persisted via `memoryValue` in DataStore |
| §5 Calculator UX (live preview, copy/share) | ✅ | Preview only on success; copy/share in status line; after-“=” keys use RAW result (wrap `sin(90)` etc.) |
| §6 Scientific functions | ✅ | sin/cos/tan + inverses, ln/log, roots, factorial, ^, abs, mod, DEG/RAD/GRAD toggle |
| §7 Bangladesh-first regional rules | ✅ | Land profiles/VAT/currency are editable with source+effective-date; nothing universal hardcoded (docs/REGIONAL_RULES.md) |
| §8 Localization en/bn | ✅ | AppCompat per-app locales (API 24+ via autoStoreLocales), bn strings parity checked |
| §8.2 Numerals ০-৯ / grouping | ✅ | Settings-driven; harness tests |
| §9 Formula library | ✅ | 31 bilingual formulas, engine-evaluated, favorites+history (tests in CI) |
| §10 Graph plotter | ✅ | Canvas plot of y=f(x) via engine samples; gaps at undefined x |
| §11.1 Unit converter | ✅ | 8 categories, exact rational affine conversion; 14 harness tests |
| §11.2 Programmer calculator | ✅ | 4 base readouts + AND/OR/XOR/NOT/shifts/±×÷ on BigInteger |
| §11.3 Land units | ✅ | Editable profiles w/ region+source+effective date; serialization tests (CI) |
| §11.4 Salary tools | ✅ | User-defined rows, net + deduction %, history |
| §11.5 VAT | ✅ | Inclusive/exclusive modes, configurable rate, provenance fields |
| §11.6 Quick percent/discount/split | ✅ | Percentage (X is what % of Y), discount with savings, bill split with optional tip — live BigDecimal results |
| §11 Currency converter | ➖ | **Removed on request** (user: “No need currency converter”) — code, strings and settings fields deleted, not hidden |
| §12 Date/time tools | ✅ | diff/±days/age on pure `DateMath` (10 harness tests) + M3 date picker |
| §13 History/favorites/settings | ✅ | Library tabs: search/clear/reuse/star + full settings screen |
| §14 Global search | ✅ | Local bilingual index (grows with features) |
| §15 About (exact facts) | ✅ | App name, tagline, Shohan Khan, helloiamshohan@gmail.com only |
| §16 Accessibility | 🟡 | Content descriptions on all icon controls, a11y labels on keypad, contrast-checked tokens; **automated Accessibility Checks beyond lint 🚫** (no emulator in this environment) |
| §17 Unit tests (logic) | ✅ | 85 local JVM tests + feature tests in CI (`./gradlew test` green) |
| §17 UI/instrumented tests | 🚫 | No emulator/device available here; Compose UI tests not run — documented in TESTING.md |
| §18 Release artifacts | 🟡 | CI `release-artifacts` job produces unsigned APK/AAB + SHA256SUMS on dispatch/tag; **no signing, no publishing** (credentials forbidden without authorization) |
| Docs set (12 files) | ✅ | This folder + README |

Known gaps tracked for later increments: instrumented/UI tests (environment),
Play-store metadata/signing (authorization), offline FX auto-refresh (would
require network — off by design until authorized).

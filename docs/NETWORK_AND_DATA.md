# Network & Data

## Network posture: none (by design)

- The manifest declares **zero permissions** — no INTERNET, no anything.
- No module contains Retrofit/OkHttp/Ktor clients, no endpoints, no telemetry,
  no ads, no crash reporters, no analytics SDKs.
- Currency conversion is an offline table the user fills in
  (docs/REGIONAL_RULES.md); there is no background refresh and none will be
  added without explicit authorization (master spec: no external resources
  or spending without permission).

## Data stores (all on-device)

| Store | Contents | API |
|---|---|---|
| `omnicalc.db` (Room v1) | `history` (expression, result, toolType, category, contextPairs, label, createdAt), `favorites` (type/title/payload/subtitle) | SQLite, exportSchema=false, no destructive migrations |
| `omnicalc_settings` (DataStore) | theme, language, numerals, grouping, precision, decimal places, angle, percent mode, haptics, memory, land profiles, currency table, VAT provenance | Preferences DataStore |

## Data flows

- Calculations are committed to history with exact/plain results;
  identical consecutive `=` presses are deduplicated in the repository.
- Search escapes SQL LIKE wildcards so user input can never act as a pattern.
- Clear History / Reset preferences are explicit user actions with
  confirmation dialogs; Favorites can be removed per item.
- Sharing uses the system share sheet (`ACTION_SEND` chooser) — the OS
  handles any network involvement; the app itself sends nothing.

## Corruption policy

Settings deserialize defensively: unknown enum values, out-of-range ints and
corrupt land/currency payloads fall back to defaults — settings always load,
never crash (tested by round-trip + garbage-input tests).

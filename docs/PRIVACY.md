# Privacy

**Product:** OmniCalc · **Owner:** Shohan Khan · helloiamshohan@gmail.com

## Summary

OmniCalc is an offline calculator. It requests **no permissions**, opens
**no network connections**, creates **no accounts**, and collects/shares
**no personal data**. Everything you type stays on your device.

## Details

| Topic | Practice |
|---|---|
| Permissions | None declared (no INTERNET, no storage, no location, …) |
| Data collected | Nothing — no analytics, no crash reporters, no ad IDs |
| Data stored locally | Your calculations (history), favorites, and preferences (see docs/NETWORK_AND_DATA.md) |
| Data leaving the device | Nothing automatic. "Share" opens the system chooser; you decide the target app |
| Backup | `allowBackup=true` with standard XML rules; restore is Android's normal device-to-device flow |
| Deleting data | Library → History → clear/delete; Settings → Reset all preferences (both confirmed) |
| Third-party SDKs | Only official Android/Google open-source libraries (AndroidX, Compose, Material, Room, DataStore) — libraries ≠ data sharing |
| Children | No special data features at all, so nothing to flag |
| Advertising | None |

## Engineering enforcement

- CI fails on any manifest permission change (review process) — current
  manifest intentionally comment-marked `Intentionally NO permissions`.
- No module imports networking libraries (dependency list in
  docs/ARCHITECTURE.md).
- No secrets, keystores or credentials exist in this repository; release
  artifacts are unsigned (docs/RELEASE.md).

Questions: helloiamshohan@gmail.com

# Release

## Versioning

- `versionName 1.0.0` · `versionCode 1` · `applicationId com.shohan.omnicalc`
- `minSdk 24` · `compileSdk/targetSdk 35` · Java 17 · Kotlin 2.0.21 · AGP 8.7.3

## Build types

| Type | Notes |
|---|---|
| debug | `applicationIdSuffix ".debug"`, installable alongside release |
| release | `minifyEnabled` + `shrinkResources` (R8), **unsigned** |

## Artifacts

CI job `release-artifacts` (`.github/workflows/ci.yml`) runs on
`workflow_dispatch` or any `v*` tag, after `build-test` passes:

```
./gradlew assembleRelease bundleRelease
→ dist/*.apk, dist/*.aab, dist/SHA256SUMS.txt   (uploaded as release-artifacts, 30-day retention)
```

The debug APK of every green push is also kept on the `build-test` job
(`ci-output` artifact, `**/build/outputs/**/*.apk`, 14-day retention).

## Signing posture (honest)

- **No keystore exists.** Creating/publishing credentials requires the
  owner's authorization (master spec rule: no credentials or publishing
  without permission) — so artifacts are explicitly *unsigned*.
- To sign later: create an upload keystore **outside the repo**, reference it
  via CI secrets (never in code/logs), then
  `./gradlew assembleRelease -Pandroid.injected.signing.*` or Gradle
  `signingConfig` reading env vars. The repo already gitignores `*.jks`,
  `keystore.properties`.

## Store readiness checklist (not yet done, not claimed)

- [ ] Upload keystore + Play App Signing (needs owner authorization)
- [ ] Play listing copy/screenshots/privacy form (docs/PRIVACY.md is the source)
- [ ] Device farm QA + accessibility walkthrough (environment blocker)
- [ ] `bundleRelease` upload via Play Console or `gh` (needs authorization)

## Verification before any release tag

1. `./scripts/run_jvm_tests.sh` → 85/85 locally.
2. CI green on the tag (test + lint + assembleDebug).
3. Run release-artifacts job; verify `sha256sum -c SHA256SUMS.txt`.
4. Install debug APK on a physical device and walk docs/FEATURE_MATRIX.md.

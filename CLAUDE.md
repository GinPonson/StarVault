# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

StarVault Android — minimalist 115 cloud-disk client. Kotlin + Jetpack Compose, single `:app` Gradle module. Phase 1 (M1) shipped UI skeleton across 9 screens, 1:1 pixel-aligned with `design/*.html` (Paparazzi 27-PNG baseline). OAuth 设备码流 (115 开放平台) wires real backend; UI is fully functional end-to-end on AVD.

## Build / run

JDK 21 (auto-provisioned by `foojay-resolver-convention`, no `org.gradle.java.home` needed) / Gradle 9.3.1 / AGP 9.1.0 / compileSdk 36 / minSdk 34 / JVM target 17.

```bash
./gradlew :app:installDebug              # build + push to connected device/emulator
adb shell am start -n com.starvault/.MainActivity
./gradlew :app:assembleDebug             # APK only
./gradlew :app:compileDebugKotlin        # type-check only (fast)
```

### Tests

```bash
# Non-Paparazzi JVM unit tests (Token401, OpenAuthManager, AuthRepository, ProfileViewModel)
./gradlew :app:testDebugUnitTest \
  --tests com.starvault.data.remote.cloud115.OpenAuthManagerTest \
  --tests com.starvault.data.remote.cloud115.Token401InterceptorTest \
  --tests com.starvault.data.repository.AuthRepositoryTest \
  --tests com.starvault.ui.profile.ProfileViewModelTest

# Paparazzi 9 屏 × 3 状态 = 27 PNG baseline
./gradlew :app:verifyPaparazziDebug                          # CI
./gradlew :app:recordPaparazziDebug                          # regenerate goldens
./gradlew :app:recordPaparazziDebug --tests com.starvault.screenshot.HomeScreenshotTest
```

Paparazzi currently SIGABRTs on `android/graphics/ColorSpace` in some environments — pre-existing JVM test issue, unrelated to source. `unitTests.isReturnDefaultValues = true` already in `app/build.gradle.kts`.

Design baseline (`design/*.html` → PNG via Playwright):
```bash
bash scripts/capture-design-baseline.sh
```

Paparazzi PNGs are Git LFS — `git lfs install && git lfs pull` on fresh clone.

## Architecture

### 5-file screen pattern (mandatory for every screen under `app/src/main/kotlin/com/starvault/ui/<screen>/`)

| File | Purpose |
|---|---|
| `<Screen>Screen.kt` | Pure `@Composable` UI — no VM access, no `nav.navigate` |
| `<Screen>ViewModel.kt` | State holder, exposes `StateFlow<UiState>`, takes repos via `ServiceLocator` |
| `<Screen>UiState.kt` | Sealed interface: `Idle / Loading / Success(...) / Error(...)` |
| `<Screen>Route.kt` | Wires `Screen` ↔ `NavController` + VM lifecycle (one-shot events via `Channel`) |
| `<Screen>ScreenPreview.kt` | Paparazzi `@Preview` composables (baseline = 412dp / 1133px @ 440dpi) |

Compose one-shot events use `Channel(UNLIMITED) + receiveAsFlow()` (not `SharedFlow` — queue semantics, no replay-0 drops).

### Network (`app/src/main/kotlin/com/starvault/data/remote/cloud115/`)

3 base URLs (defined in `Cloud115ApiClient`) — all serve the **115 开放平台 (open platform) OAuth API**, not the legacy webapi/Cookie endpoints. Every request path is prefixed `/open/...` (e.g. `open/authDeviceCode`, `open/ufile/files`, `open/user/info`):
- `qrcodeapi.115.com` — OAuth device code + status polling (`OpenAuthApiService`, `StatusPollApi`)
- `passportapi.115.com` — OAuth refresh + revoke (`OpenAuthApiService.refreshToken/revokeToken`)
- `proapi.115.com` — Bearer business calls (`OpenFileApiService`, `OpenUserApiService`, `OpenUploadApiService`)

3 OkHttpClient variants (timeout strategy):
- 30s regular — proapi + qrcodeapi POST
- 65s long-poll — `get/status/` (server holds 30~60s)
- 30s refresh — passportapi, no `Token401Interceptor` (prevents recursion on refresh API 401)

Interceptor chain (request order = add order):
1. `browserLikeHeaderInterceptor` — Referer/Origin/Android UA (downurl 签名必需)
2. `AuthHeaderInterceptor` — injects `Authorization: Bearer $token` from `OpenAuthStore.accessTokenBlocking()`
3. `Token401Interceptor` — on business-level 401 code, refresh + retry once (re-entry guarded by `X-Token-Retry: 1` header)

`Token401Interceptor` parses `code` field via kotlinx-serialization; matches `code == 99 || code.toString().startsWith("401")` (aligned with OpenList 115-sdk-go `Is401Started`).

`Response<T>.requireSuccessful()` (`ResponseExt.kt`) collapses HTTP+body check across 3 repositories.

### Upload pipeline (M2 spec §3)

Single-file upload (M2) routes through 3 endpoints under `proapi.115.com` (Bearer + 401 auto-refresh):
- `GET  /open/upload/get_token` — exchange for Aliyun OSS STS credentials (endpoint, AccessKeyId/Secret, SecurityToken, expiration)
- `POST /open/upload/init`     — SHA1 + target → `status` (1 = new / 2 = 秒传 / 6|7|8 = sign_check range)
- `POST /open/upload/resume`   — M3+ resume hook (M2 stub returns Unit)

Multipart PUT goes to **Aliyun OSS** (not 115 endpoints) — SDK `com.aliyun.dpa:oss-android-sdk:2.9.9`. STS credentials short-lived (typically 1h); `UploadExecutor` re-fetches `get_token` if 401. Status branches in `UploadStateMachine`:
- `1` → upload parts → DONE
- `2` → 秒传 → fail with ToastBus (M2 doesn't support)
- `6|7|8` → re-init with `sign_check` range SHA1, max 2 attempts
- else → fail with ToastBus

`target = "U_1_<cid>"` prefix kept 1:1 with OpenList; `topupload` field is **not sent** (115 docs "必填:否", Go SDK omits).

### DI

`core/ServiceLocator` — pure singleton, no Hilt/Koin. `MainActivity.onCreate` initializes once; all VMs read dependencies from there by default. Simpler than `Lazy`/`synchronized`.

### Auth flow

3-step OAuth device code:
1. `OpenAuthManager.requestDeviceCode()` — POST `authDeviceCode` → uid/time/qrcodeUrl; renders QR via zxing locally
2. `OpenAuthManager.pollForToken()` — long-polls `get/status/` until status=2
3. `exchangeForToken()` — POST `deviceCodeToToken` → accessToken + refreshToken

State machine: `Waiting(bitmap) → Scanned → Authorized | Denied | Expired | Error`. 5-min QR expiry driven by caller's deadline param (not re-timed inside `OpenAuthManager`).

`OpenAuthStore` (DataStore Preferences, name `cloud115_tokens`) persists access/refresh tokens + user info. `code_verifier` is hardcoded `"0"*64` (aligned with OpenList 115-sdk-go) — **never** stored.

### Navigation

`nav/Route.kt` — type-safe `@Serializable` routes via `androidx.navigation:navigation-compose` 2.8.5. `SavedStateHandle` provides typed route args to VMs.

## Conventions

- **No historical narrative in code/comments** — code reads as if OAuth was always the only auth method. Never use "替换 / 迁移 / 旧 / 替换历史 / 改为" or "Cookie 时代" / "老 webapi" / "替换 ScanStatus" in KDoc. Delete these words on sight, not just in narrative context.
- **Commit messages in English** — see `git log` history (e.g. `b94bef7`, `af06df0`, `0e6ec08`).
- **No `!!` in main src** — use `?.let`, `requireNotNull` with message, or smart-cast.
- **No `println(` in main src** — use `android.util.Log.w(TAG, ...)` + `companion object { private const val TAG = ... }`; tests need `mockkStatic(android.util.Log::class)`.
- **No "M3 MaterialTheme"** — project uses custom tokens (`StarVaultColors/Typography/Shapes/Dimens`) via `CompositionLocal`. Don't add `MaterialTheme { }` wrappers.
- **Inter variable font** — Paparazzi 2.0.0-alpha04 + layoutlib 16.x don't fully support `FontVariation.weight()`; keep only base `Font(R.font.inter_variable, weight = Medium)`.

## Reference

### Project
- `README.md` — environment, design decisions, design-baseline workflow
- `docs/superpowers/specs/2026-06-14-starvault-android-skeleton-design.md` — full M1 design spec
- `docs/superpowers/plans/2026-06-14-starvault-android-skeleton.md` — execution plan
- `design/*.html` — visual mockups (symlink to Open Design project)
- `app/src/test/snapshots/images/` — Paparazzi baselines (Git LFS)
- `gradle/libs.versions.toml` — version catalog

### External — 115 protocol references
- **OpenListTeam/OpenList** (https://github.com/OpenListTeam/OpenList) — Go-based 115 driver (`driver.go`, `internal/driver/115.go`). Source of truth for `request.go:33-50 authRequest` auto-refresh pattern, `utils.go:21-24 Is401Started`, `fs.go:309-319` downurl signing, `const.go:9-12` domain split (passportapi for auth, proapi for business).
- **ChenyangGao/p115client** (https://github.com/ChenyangGao/p115client) — Python 115 SDK. Source of truth for error-code table (40140123/40140124/40140126 = signature/format/verify failures that trigger refresh), field-shape mapping (e.g. `client.py:2573/4256/3100`), 401 → refresh semantics.
- **115 开放平台官方文档** (https://www.yuque.com/115yun/open) — official OAuth device-code flow spec, `authDeviceCode` / `deviceCodeToToken` request/response schemas, client_id, redirect_uri, code_verifier/challenge rules.

When in doubt on a 115 protocol question, check the three above in this order. Our `Token401Interceptor` and `OpenAuthManager` are ported from OpenList 115-sdk-go behavior (not p115client Python semantics).

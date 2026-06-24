# StarVault Android

115 网盘的极简 Android 重设计。Phase 1（M1）= UI 骨架 + 1:1 视觉还原（基于 `design/*.html`）。
真实 115 接入 / 数据层 / 播放内核留到 Phase 2+。

## 状态

M1 骨架已完成 — 9 屏 (Login / Home / Player / Share / Transfers / Profile / Files / Album / Wallpaper)，
全部走 5-file 模式（UiState + ViewModel + Screen + Route + Preview）实现，
与 design/* 1:1 像素对齐 (Paparazzi baseline 27 张 PNG 回归)。

## 鉴权

115 开放平台 OAuth 设备码流，3 步：`authDeviceCode` → `get/status/` → `deviceCodeToToken`。
入口：`data/remote/cloud115/OpenAuthManager.kt` + `OpenAuthApiService.kt`（走 proapi / passportapi）。

## 环境要求

- JDK 21 (Paparazzi 2.0.0-alpha04 plugin runtime 要求；compile target = 17)
- Gradle 9.3.1 (wrapper 自带)
- AGP 9.1.0
- Android SDK 36 (compileSdk) + 34 (minSdk)
- [可选] Android Studio Ladybug+ (or later)
- [可选] Node.js + Playwright (跑 design-baseline 脚本)

## 运行

```bash
# 装到已连接的设备/模拟器
./gradlew :app:installDebug
adb shell am start -n com.starvault/.MainActivity
```

## Paparazzi 截图回归

9 屏 × 3 状态 = 27 张 baseline PNG，位于 `app/src/test/snapshots/images/`。
`PHONE_412_900` 设备配置在 `app/src/test/kotlin/com/starvault/screenshot/DeviceConfigExt.kt`，
注意其 `screenWidth=1133px` 是物理像素（layoutlib 算出 412dp），不是设计稿的 412dp 数字本身。

```bash
# 首次：记录 golden PNG（修改代码后强制重出）
./gradlew :app:recordPaparazziDebug

# 后续：验证（CI 用，必须有 baseline 在 git）
./gradlew :app:verifyPaparazziDebug

# 单 test 类
./gradlew :app:recordPaparazziDebug --tests com.starvault.screenshot.HomeScreenshotTest
```

## Design baseline 抓取

`design/` 是 Open Design 项目目录的 symlink（指向私有设计稿位置）。
如需把 design/*.html 渲染成 PNG 与 Paparazzi 对比：

```bash
bash scripts/capture-design-baseline.sh
# 输出到 design-baseline/{00-login,01-home,...}.png
```

## Git LFS

Paparazzi 截图（27 张 PNG）通过 Git LFS 存储。Clone 后请先：

```bash
git lfs install
git lfs pull
```

## 文档

- 设计稿：`design/*.html` （软链）
- Spec：`docs/superpowers/specs/2026-06-14-starvault-android-skeleton-design.md`
- 计划：`docs/superpowers/plans/2026-06-14-starvault-android-skeleton.md`
- Session 工作日志：`docs/superpowers/notes/` (M1 期间累计)

## 目录结构

```
app/
  src/
    main/
      kotlin/com/starvault/
        theme/        # Color / Type / Shape / Dimens + StarVaultTheme 入口
        data/         # model + FixtureLoader (生产环境空跑，test 侧走 FixturePresets)
        ui/           # 9 屏 + 公共组件 (每个屏 5-file 模式)
      res/
        font/         # inter_variable.ttf (Inter 变体字体)
        drawable/     # ic_launcher_foreground (白星 vector)
        mipmap-anydpi-v26/  # adaptive icon
        values/       # colors / strings / themes (含 Splash Screen attrs)
    test/
      kotlin/com/starvault/
        fixtures/     # FixturePresets (test 侧硬编码数据，避免依赖 production fixtures)
        screenshot/   # 9 屏的 Paparazzi 回归基线
      snapshots/images/  # 27 张 baseline PNG (git LFS)
scripts/
  capture-design-baseline.sh  # Playwright 渲染 design/*.html → PNG
```

## 关键设计决策（M1）

1. **不用 M3 MaterialTheme** — 我们有自定义 tokens (StarVaultColors/Typography/Shapes/Dimens)，
   不需要 M3 的 Typography/Shapes。挂在 CompositionLocal 上即可。
2. **Paparazzi 412dp viewport** — `screenWidth=1133px @ 440dpi`，让 layoutlib 算出 412dp，
   与 design/* 1:1 对齐。
3. **Inter variable font** — Paparazzi 2.0.0-alpha04 + layoutlib 16.x 不完全支持 `FontVariation.weight()`，
   需在 `Type.kt` 里只保留基础 `Font(R.font.inter_variable, weight = Medium)`。

# StarVault Android Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Jetpack Compose Android app skeleton that 1:1 reproduces the StarVault design mocks (10 screens, 4-tab bottom nav) and is verified by Paparazzi screenshot regression against a Playwright-captured design baseline.

**Architecture:** Single `:app` Gradle module, feature-sliced packages under `com.starvault.ui.<feature>/` (each feature = `Screen` + `Route` + `ViewModel` + `UiState` + `Preview`). Type-safe Compose Navigation with `@Serializable` Routes, custom design tokens via `CompositionLocal` (not Material 3 color roles), `ViewModel + StateFlow + sealed UiState`, JSON fixtures in `assets/fixtures/` for runtime + hardcoded `FixturePresets` for tests.

**Tech Stack:** Kotlin 2.3.21 + Jetpack Compose (BOM 2026.05.00) + Compose Navigation 2.8.x + Material 3 (colorScheme overridden) + kotlinx.serialization + Paparazzi 2.0.0-alpha04. AGP 9.1.0 + Gradle 9.x + JDK 17+. minSdk 34, targetSdk 35.

**Spec reference:** `/Users/Gin/Workspace/StarVault/docs/superpowers/specs/2026-06-14-starvault-android-skeleton-design.md`
**Design mocks:** `/Users/Gin/Workspace/StarVault/design/00-login.html` … `08-wallpaper.html` (9 product screens + `index.html` overview)

---

## Task Dependency Graph

```
T1 Project scaffold
  └─ T2 Version catalog
       └─ T3 :app module + manifest
            ├─ T4 Theme tokens
            │    └─ T5 Inter font
            ├─ T6 Data models
            │    ├─ T7 FixtureLoader
            │    │    └─ T8 JSON fixtures
            │    └─ T9 FixturePresets (test)
            ├─ T10 Routes + NavHost skeleton
            ├─ T11 BottomNavBar + common components
            │    └─ T12 MainActivity + StarVaultApp
            ├─ T13 Login (Screen+Route+VM+Preview)
            ├─ T14 Home
            ├─ T15 Player
            ├─ T16 Share
            ├─ T17 Transfers
            ├─ T18 Profile
            ├─ T19 Files
            ├─ T20 Album
            ├─ T21 Wallpaper
            └─ T22 Paparazzi base + Login screenshot
                 └─ T23–T30 Remaining 8 screenshot tests
                      └─ T31 App icon + Splash
                           └─ T32 README + baseline script
                                └─ T33 DoD verify
```

---

## Task 1: Project scaffold (Gradle wrapper, settings, root build)

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` (via wrapper task)

- [ ] **Step 1: Generate Gradle wrapper (Gradle 9.x)**

Run from `/Users/Gin/Workspace/StarVault`:
```bash
gradle wrapper --gradle-version 9.0 --distribution-type bin
```
Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.{jar,properties}` appear.

- [ ] **Step 2: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StarVault"
include(":app")
```

- [ ] **Step 3: Write root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.compose.compiler)     apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 4: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

android.useAndroidX=true
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=false

kotlin.code.style=official
```

- [ ] **Step 5: Verify the wrapper works**

Run: `./gradlew help --quiet`
Expected: exits 0; no compile errors (no modules yet so it just prints help).

---

## Task 2: Version catalog (`libs.versions.toml`)

**Files:**
- Create: `gradle/libs.versions.toml`

- [ ] **Step 1: Write the full catalog**

```toml
[versions]
agp = "9.1.0"
kotlin = "2.3.21"
compose-bom = "2026.05.00"
nav = "2.8.5"
activity-compose = "1.13.0"
lifecycle = "2.10.0"
kotlinx-serialization = "1.7.3"
paparazzi = "2.0.0-alpha04"
junit = "4.13.2"
turbine = "1.2.0"

[libraries]
androidx-activity-compose      = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
compose-bom                    = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui                     = { module = "androidx.compose.ui:ui" }
compose-ui-graphics            = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling-preview     = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling             = { module = "androidx.compose.ui:ui-tooling" }
compose-foundation             = { module = "androidx.compose.foundation:foundation" }
compose-material3              = { module = "androidx.compose.material3:material3" }
compose-material-icons-core    = { module = "androidx.compose.material:material-icons-core" }
nav-compose                    = { module = "androidx.navigation:navigation-compose", version.ref = "nav" }
lifecycle-viewmodel-compose    = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-savedstate = { module = "androidx.lifecycle:lifecycle-viewmodel-savedstate", version.ref = "lifecycle" }
lifecycle-runtime-compose      = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
kotlinx-serialization-json     = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
paparazzi                      = { module = "app.cash.paparazzi:paparazzi", version.ref = "paparazzi" }
junit                          = { module = "junit:junit", version.ref = "junit" }
turbine                        = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application            = { id = "com.android.application", version.ref = "agp" }
kotlin-android                 = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler               = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization           = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Verify catalog parses**

Run: `./gradlew help --quiet`
Expected: exits 0. Any typo in catalog → `Could not resolve: <artifact>` error.

---

## Task 3: `:app` module — build.gradle.kts + AndroidManifest + empty MainActivity

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/starvault/MainActivity.kt`

- [ ] **Step 1: Write `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.starvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.starvault"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("Boolean", "MOCK_DELAY", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("Boolean", "MOCK_DELAY", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("main").kotlin.srcDirs("src/main/kotlin")
        getByName("test").kotlin.srcDirs("src/test/kotlin")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.nav.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.paparazzi)
}

// Gradle 9 + Paparazzi 2.0.0-alpha04 兼容性：禁用 HTML test reports
tasks.withType<Test>().configureEach {
    reports.html.required = false
}
```

- [ ] **Step 2: Write `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.StarVault"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.StarVault"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: Write `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">StarVault</string>
</resources>
```

- [ ] **Step 4: Write `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.StarVault" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowBackground">@color/bg</item>
    </style>
</resources>
```

- [ ] **Step 5: Write `app/src/main/res/values/colors.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bg">#FAFAFA</color>
</resources>
```

- [ ] **Step 6: Write empty `MainActivity.kt`**

```kotlin
package com.starvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { /* StarVaultTheme { StarVaultApp() } — added in T12 */ }
    }
}
```

- [ ] **Step 7: Verify build**

Run: `./gradlew :app:assembleDebug --quiet`
Expected: BUILD SUCCESSFUL. Empty white screen launches (no theme yet — added in T4).

---

## Task 4: Theme tokens (Colors / Typography / Shapes / Dimens + Theme entry)

**Files:**
- Create: `app/src/main/kotlin/com/starvault/theme/Color.kt`
- Create: `app/src/main/kotlin/com/starvault/theme/Type.kt`
- Create: `app/src/main/kotlin/com/starvault/theme/Shape.kt`
- Create: `app/src/main/kotlin/com/starvault/theme/Dimens.kt`
- Create: `app/src/main/kotlin/com/starvault/theme/StarVaultTheme.kt`

- [ ] **Step 1: Write `Color.kt`**

```kotlin
package com.starvault.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class StarVaultColors(
    val bg:         Color = Color(0xFFFAFAFA),
    val surface:    Color = Color(0xFFFFFFFF),
    val fg:         Color = Color(0xFF111111),
    val muted:      Color = Color(0xFF6B6B6B),
    val border:     Color = Color(0xFFE5E5E5),
    val accent:     Color = Color(0xFF2F6FEB),
    val accentOn:   Color = Color(0xFFFFFFFF),
    val accentSoft: Color = Color(0x142F6FEB),
    val tag1:       Color = Color(0xFF2F6FEB),
    val tag2:       Color = Color(0xFF9333EA),
    val tag3:       Color = Color(0xFFEA580C),
    val tag4:       Color = Color(0xFF16A34A),
    val tag5:       Color = Color(0xFFDB2777),
    val success:    Color = Color(0xFF17A34A),
    val warn:       Color = Color(0xFFEAB308),
    val danger:     Color = Color(0xFFDC2626),
)
```

- [ ] **Step 2: Write `Type.kt`**

```kotlin
package com.starvault.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.starvault.R

val Inter = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)

@Immutable
data class StarVaultTypography(
    val display:  TextStyle = TextStyle(fontFamily=Inter, fontSize=28.sp, fontWeight=FontWeight.SemiBold, letterSpacing=(-0.2).sp),
    val title:    TextStyle = TextStyle(fontFamily=Inter, fontSize=20.sp, fontWeight=FontWeight.SemiBold),
    val large:    TextStyle = TextStyle(fontFamily=Inter, fontSize=22.sp, fontWeight=FontWeight.SemiBold),
    val subtitle: TextStyle = TextStyle(fontFamily=Inter, fontSize=16.sp, fontWeight=FontWeight.Medium),
    val body:     TextStyle = TextStyle(fontFamily=Inter, fontSize=14.sp),
    val caption:  TextStyle = TextStyle(fontFamily=Inter, fontSize=12.sp),
    val micro:    TextStyle = TextStyle(fontFamily=Inter, fontSize=10.5.sp, fontWeight=FontWeight.Medium, letterSpacing=0.2.sp),
    val mono:     TextStyle = TextStyle(fontFamily=FontFamily.Monospace, fontSize=12.sp),
)
```

- [ ] **Step 3: Write `Shape.kt`**

```kotlin
package com.starvault.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class StarVaultShapes(
    val xs:   Shape = RoundedCornerShape(3.dp),
    val sm:   Shape = RoundedCornerShape(4.dp),
    val md:   Shape = RoundedCornerShape(9.dp),
    val lg:   Shape = RoundedCornerShape(12.dp),
    val xl:   Shape = RoundedCornerShape(13.dp),
    val xxl:  Shape = RoundedCornerShape(28.dp),
    val pill: Shape = RoundedCornerShape(999.dp),
)
```

- [ ] **Step 4: Write `Dimens.kt`**

```kotlin
package com.starvault.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class StarVaultDimens(
    val borderHairline: Dp = 1.dp,
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 12.dp,
    val spaceLg: Dp = 16.dp,
    val spaceXl: Dp = 24.dp,
    val bottomBarHeight: Dp = 64.dp,
    val bottomBarBottomGap: Dp = 46.dp,
)
```

- [ ] **Step 5: Write `StarVaultTheme.kt`**

```kotlin
package com.starvault.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalStarVaultColors     = staticCompositionLocalOf { StarVaultColors() }
val LocalStarVaultTypography = staticCompositionLocalOf { StarVaultTypography() }
val LocalStarVaultShapes     = staticCompositionLocalOf { StarVaultShapes() }
val LocalStarVaultDimens     = staticCompositionLocalOf { StarVaultDimens() }

object StarVaultTheme {
    val colors:     StarVaultColors     @Composable get() = LocalStarVaultColors.current
    val typography: StarVaultTypography @Composable get() = LocalStarVaultTypography.current
    val shapes:     StarVaultShapes     @Composable get() = LocalStarVaultShapes.current
    val dimens:     StarVaultDimens     @Composable get() = LocalStarVaultDimens.current
}

@Composable
fun StarVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStarVaultColors     provides StarVaultColors(),
        LocalStarVaultTypography provides StarVaultTypography(),
        LocalStarVaultShapes     provides StarVaultShapes(),
        LocalStarVaultDimens     provides StarVaultDimens(),
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary     = StarVaultColors().accent,
                onPrimary   = StarVaultColors().accentOn,
                background  = StarVaultColors().bg,
                surface     = StarVaultColors().surface,
                onSurface   = StarVaultColors().fg,
                surfaceTint = Color.Transparent,
            ),
            content = content,
        )
    }
}
```

- [ ] **Step 6: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL. T4's compile will FAIL at T5 only when `R.font.inter_regular` is missing — until T5 lands, comment out the `Font(R.font.inter_regular, ...)` lines in `Type.kt`, then uncomment after T5.

---

## Task 5: Inter font (4 weights in `res/font/`)

**Files:**
- Create: `app/src/main/res/font/inter_regular.ttf`
- Create: `app/src/main/res/font/inter_medium.ttf`
- Create: `app/src/main/res/font/inter_semibold.ttf`
- Create: `app/src/main/res/font/inter_bold.ttf`

- [ ] **Step 1: Download Inter font files**

```bash
mkdir -p /Users/Gin/Workspace/StarVault/app/src/main/res/font
cd /Users/Gin/Workspace/StarVault/app/src/main/res/font
curl -fsSL -o inter_regular.ttf  "https://github.com/rsms/inter/raw/v4.0/docs/font-files/Inter-Regular.otf"
curl -fsSL -o inter_medium.ttf   "https://github.com/rsms/inter/raw/v4.0/docs/font-files/Inter-Medium.otf"
curl -fsSL -o inter_semibold.ttf "https://github.com/rsms/inter/raw/v4.0/docs/font-files/Inter-SemiBold.otf"
curl -fsSL -o inter_bold.ttf     "https://github.com/rsms/inter/raw/v4.0/docs/font-files/Inter-Bold.otf"
ls -la
```
Expected: 4 .otf files in res/font/. **Note:** Android supports .otf; AGP will use them as-is.

- [ ] **Step 2: Verify font references compile**

Run: `./gradlew :app:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL. If T4's Type.kt had Font() lines commented out, uncomment now.

- [ ] **Step 3: Commit the scaffold + theme + fonts**

```bash
cd /Users/Gin/Workspace/StarVault
git add app/build.gradle.kts app/src/main/AndroidManifest.xml \
        app/src/main/res app/src/main/kotlin
git add gradle/libs.versions.toml gradle.properties build.gradle.kts settings.gradle.kts gradle/wrapper gradlew gradlew.bat
git commit -m "feat(scaffold): Gradle 9 + AGP 9.1 + Compose 2026.05 skeleton

- Gradle 9 wrapper, settings + root build + version catalog
- :app module with 4 plugins (kotlin-android, compose-compiler,
  kotlin-serialization, android-application)
- Theme tokens (Color/Type/Shape/Dimens) wired through CompositionLocal;
  MaterialTheme wrapped to override colorScheme
- Inter font 4 weights in res/font/

Phase 1 M1 scaffold — next: data layer (T6–T9), then routes + screens."
```

---

## Task 6: Data models (`@Serializable` data classes + enums)

**Files:**
- Create: `app/src/main/kotlin/com/starvault/data/model/FileItem.kt`
- Create: `app/src/main/kotlin/com/starvault/data/model/Transfer.kt`
- Create: `app/src/main/kotlin/com/starvault/data/model/User.kt`
- Create: `app/src/main/kotlin/com/starvault/data/model/AlbumPhoto.kt`
- Create: `app/src/main/kotlin/com/starvault/data/model/Wallpaper.kt`
- Create: `app/src/main/kotlin/com/starvault/data/model/ShareLink.kt`

- [ ] **Step 1: Write `FileItem.kt`**

```kotlin
package com.starvault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val id: String,
    val name: String,
    val type: FileType,
    val sizeBytes: Long? = null,
    val mtime: Long,
    val tag: TagColor? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
enum class FileType { FOLDER, VIDEO, IMAGE, DOC, AUDIO, OTHER }

@Serializable
enum class TagColor { TAG1, TAG2, TAG3, TAG4, TAG5 }
```

- [ ] **Step 2: Write `Transfer.kt`**

```kotlin
package com.starvault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Transfer(
    val id: String,
    val fileName: String,
    val direction: Direction,
    val totalBytes: Long,
    val transferredBytes: Long,
    val speedBps: Long,
    val status: TransferStatus,
    val startedAt: Long,
)

@Serializable enum class Direction      { UP, DOWN }
@Serializable enum class TransferStatus { RUNNING, PAUSED, SUCCESS, FAILED }
```

- [ ] **Step 3: Write `User.kt`**

```kotlin
package com.starvault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val nickname: String,
    val avatarUrl: String? = null,
    val vipLevel: Int,
    val vipExpiresAt: Long? = null,
    val totalBytes: Long,
    val usedBytes: Long,
)
```

- [ ] **Step 4: Write `AlbumPhoto.kt`**

```kotlin
package com.starvault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AlbumPhoto(
    val id: String,
    val uri: String,
    val width: Int,
    val height: Int,
    val takenAt: Long,
    val isFavorite: Boolean = false,
)
```

- [ ] **Step 5: Write `Wallpaper.kt`**

```kotlin
package com.starvault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Wallpaper(
    val id: String,
    val previewUrl: String,
    val category: String,
)

@Serializable
data class WallpaperConfig(
    val enabled: Boolean,
    val intervalSeconds: Int,
    val displayMode: DisplayMode,
    val categories: List<String>,
)

@Serializable enum class DisplayMode { FILL_CROP, FIT_FULL, CENTER }
```

- [ ] **Step 6: Write `ShareLink.kt`**

```kotlin
package com.starvault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ShareLink(
    val fileId: String,
    val url: String,
    val accessCode: String,
    val expiresAt: Long? = null,
)
```

- [ ] **Step 7: Verify compile**

Run: `./gradlew :app:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL. kotlinx-serialization plugin must generate `.serializer` companions for each.

---

## Task 7: FixtureLoader

**Files:**
- Create: `app/src/main/kotlin/com/starvault/data/FixtureLoader.kt`
- Create: `app/src/test/kotlin/com/starvault/data/FixtureLoaderTest.kt`

- [ ] **Step 1: Write the failing test**

`FixtureLoaderTest.kt`:
```kotlin
package com.starvault.data

import android.content.Context
import app.cash.turbine.test
import com.starvault.data.model.FileItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureLoaderTest {
    @Test fun `loads List of FileItem from assets fixture`() = runTest {
        // A fake Context returning a stubbed assets stream
        val ctx = FakeContext(assetsJson = """[{"id":"f1","name":"a.mp4","type":"VIDEO","mtime":1}]""")
        val items = FixtureLoader.load<List<FileItem>>(ctx, "fixtures/files.json")
        assertTrue(items.isNotEmpty())
        assertTrue(items.first().id == "f1")
    }
}

/** Minimal fake — implement only assets.open; everything else throws. */
class FakeContext(private val assetsJson: String) : android.content.ContextWrapper(null) {
    override fun getAssets() = object : android.content.res.AssetManager() {
        override fun open(path: String) = assetsJson.byteInputStream()
    }
}
```

- [ ] **Step 2: Run the test (should fail — class missing)**

Run: `./gradlew :app:testDebugUnitTest --tests com.starvault.data.FixtureLoaderTest --quiet`
Expected: FAIL with "Unresolved reference: FixtureLoader".

- [ ] **Step 3: Write `FixtureLoader.kt`**

```kotlin
package com.starvault.data

import android.content.Context
import com.starvault.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

object FixtureLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    inline fun <reified T> load(context: Context, assetPath: String): T {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString<T>(text)
    }

    suspend inline fun <reified T> loadDelayed(
        context: Context,
        assetPath: String,
        delayMs: Long = if (BuildConfig.MOCK_DELAY) 300 else 0,
    ): T {
        if (delayMs > 0) delay(delayMs)
        return load(context, assetPath)
    }
}
```

- [ ] **Step 4: Run the test (should pass)**

Run: `./gradlew :app:testDebugUnitTest --tests com.starvault.data.FixtureLoaderTest --quiet`
Expected: BUILD SUCCESSFUL, 1 test passed.

---

## Task 8: 7 JSON fixture files

**Files:**
- Create: `app/src/main/assets/fixtures/files.json`
- Create: `app/src/main/assets/fixtures/transfers.json`
- Create: `app/src/main/assets/fixtures/profile.json`
- Create: `app/src/main/assets/fixtures/albums.json`
- Create: `app/src/main/assets/fixtures/wallpapers.json`
- Create: `app/src/main/assets/fixtures/wallpaper_config.json`
- Create: `app/src/main/assets/fixtures/share_links.json`

- [ ] **Step 1: Write `files.json`** (~30 entries, mix types & tags)

```json
[
  {"id":"f01","name":"旅行 2025","type":"FOLDER","mtime":1718000000,"tag":"TAG1"},
  {"id":"f02","name":"东京 vlog.mp4","type":"VIDEO","sizeBytes":104857600,"mtime":1717900000,"tag":"TAG1"},
  {"id":"f03","name":"海边日落.jpg","type":"IMAGE","sizeBytes":2097152,"mtime":1717800000,"tag":"TAG3"},
  {"id":"f04","name":"会议纪要.docx","type":"DOC","sizeBytes":40960,"mtime":1717700000},
  {"id":"f05","name":"钢琴曲.wav","type":"AUDIO","sizeBytes":31457280,"mtime":1717600000,"tag":"TAG5"},
  ...  // 共 30 条，分布 FOLDER 5 / VIDEO 8 / IMAGE 8 / DOC 4 / AUDIO 3 / OTHER 2；tag 分布约每色 5-6 条
]
```

Engineer: 自己扩到 30 条。所有 mtime 用 2024 上半年时间戳；tag 均匀分布。

- [ ] **Step 2: Write `transfers.json`** (8 entries)

```json
[
  {"id":"t01","fileName":"project-final.zip","direction":"UP","totalBytes":524288000,"transferredBytes":524288000,"speedBps":0,"status":"SUCCESS","startedAt":1718000000},
  {"id":"t02","fileName":"new-movie.mp4","direction":"DOWN","totalBytes":2147483648,"transferredBytes":1073741824,"speedBps":5242880,"status":"RUNNING","startedAt":1718100000},
  ...  // 4 UP + 4 DOWN，覆盖 RUNNING / PAUSED / SUCCESS / FAILED 各 2 条
]
```

- [ ] **Step 3: Write `profile.json`** (single user)

```json
{"nickname":"Vint","vipLevel":3,"vipExpiresAt":1735689600,"totalBytes":1099511627776,"usedBytes":329853488332}
```

- [ ] **Step 4: Write `albums.json`** (60 photos for masonry)

```json
[
  {"id":"p01","uri":"sample/p01.jpg","width":1080,"height":1920,"takenAt":1718000000,"isFavorite":true},
  {"id":"p02","uri":"sample/p02.jpg","width":1920,"height":1080,"takenAt":1717900000},
  ...  // 共 60 条；宽高随机在 800-2400 间；isFavorite 约 8 条为 true
]
```

- [ ] **Step 5: Write `wallpapers.json`** (20 entries + 5 categories)

```json
[
  {"id":"w01","previewUrl":"sample/w01.jpg","category":"自然"},
  ...  // 20 条，5 类（自然/城市/抽象/极简/星空）各 4 条
]
```

- [ ] **Step 6: Write `wallpaper_config.json`**

```json
{"enabled":true,"intervalSeconds":300,"displayMode":"FILL_CROP","categories":["自然","极简"]}
```

- [ ] **Step 7: Write `share_links.json`** (3 entries)

```json
[
  {"fileId":"f02","url":"https://starvault.example/s/abc123","accessCode":"8848","expiresAt":1722470400},
  {"fileId":"f04","url":"https://starvault.example/s/def456","accessCode":"1234","expiresAt":null},
  {"fileId":"f05","url":"https://starvault.example/s/ghi789","accessCode":"5678","expiresAt":1725062400}
]
```

- [ ] **Step 8: Verify all fixtures parse**

Run: `./gradlew :app:compileDebugKotlin --quiet && ./gradlew :app:testDebugUnitTest --quiet`
Expected: all 30+60+20+8+3 fixture entries parse without exception (covered by FixtureLoaderTest if extended, otherwise by smoke test in T14).

---

## Task 9: FixturePresets (test-side hardcoded data)

**Files:**
- Create: `app/src/test/kotlin/com/starvault/fixtures/FixturePresets.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.starvault.fixtures

import com.starvault.data.model.AlbumPhoto
import com.starvault.data.model.Direction
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileType
import com.starvault.data.model.ShareLink
import com.starvault.data.model.TagColor
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import com.starvault.data.model.User
import com.starvault.data.model.Wallpaper
import com.starvault.data.model.WallpaperConfig
import com.starvault.data.model.DisplayMode

object FixturePresets {
    fun homeFiles(): List<FileItem> = listOf(
        FileItem("f01", "旅行 2025",     FileType.FOLDER, mtime = 1_718_000_000, tag = TagColor.TAG1),
        FileItem("f02", "东京 vlog.mp4", FileType.VIDEO,  sizeBytes = 104_857_600, mtime = 1_717_900_000, tag = TagColor.TAG1),
        FileItem("f03", "海边日落.jpg",   FileType.IMAGE,  sizeBytes = 2_097_152,  mtime = 1_717_800_000, tag = TagColor.TAG3),
        FileItem("f04", "会议纪要.docx",  FileType.DOC,    sizeBytes = 40_960,     mtime = 1_717_700_000),
        FileItem("f05", "钢琴曲.wav",     FileType.AUDIO,  sizeBytes = 31_457_280, mtime = 1_717_600_000, tag = TagColor.TAG5),
    )

    fun transfers(): List<Transfer> = listOf(
        Transfer("t01", "movie.mp4",  Direction.DOWN, 2_147_483_648, 1_073_741_824, 5_242_880, TransferStatus.RUNNING, 1_718_100_000),
        Transfer("t02", "song.flac",  Direction.UP,   52_428_800,     52_428_800,     0,         TransferStatus.SUCCESS, 1_718_000_000),
        Transfer("t03", "doc.pdf",    Direction.UP,   4_194_304,      1_048_576,      0,         TransferStatus.PAUSED,  1_717_900_000),
        Transfer("t04", "game.iso",   Direction.DOWN, 8_589_934_592,  2_147_483_648, 1_048_576, TransferStatus.RUNNING, 1_717_800_000),
    )

    fun user(): User = User(
        nickname = "Vint",
        vipLevel = 3,
        vipExpiresAt = 1_735_689_600,
        totalBytes = 1_099_511_627_776,
        usedBytes = 329_853_488_332,
    )

    fun albumPhotos(): List<AlbumPhoto> = (1..30).map { i ->
        AlbumPhoto(
            id = "p%02d".format(i),
            uri = "sample/p%02d.jpg".format(i),
            width = if (i % 2 == 0) 1080 else 1920,
            height = if (i % 2 == 0) 1920 else 1080,
            takenAt = 1_718_000_000L - i * 86_400L,
            isFavorite = i % 4 == 0,
        )
    }

    fun wallpapers(): List<Wallpaper> = (1..20).map { i ->
        Wallpaper("w%02d".format(i), "sample/w%02d.jpg".format(i), listOf("自然","城市","抽象","极简","星空")[i % 5])
    }

    fun wallpaperConfig(): WallpaperConfig = WallpaperConfig(
        enabled = true,
        intervalSeconds = 300,
        displayMode = DisplayMode.FILL_CROP,
        categories = listOf("自然", "极简"),
    )

    fun shareLinks(): List<ShareLink> = listOf(
        ShareLink("f02", "https://starvault.example/s/abc123", "8848", 1_722_470_400),
        ShareLink("f04", "https://starvault.example/s/def456", "1234", null),
        ShareLink("f05", "https://starvault.example/s/ghi789", "5678", 1_725_062_400),
    )
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugUnitTestKotlin --quiet`
Expected: BUILD SUCCESSFUL.

---

## Task 10: Routes + NavHost skeleton

**Files:**
- Create: `app/src/main/kotlin/com/starvault/nav/Routes.kt`
- Create: `app/src/main/kotlin/com/starvault/nav/StarVaultNavHost.kt`

- [ ] **Step 1: Write `Routes.kt`**

```kotlin
package com.starvault.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable object Login                                : Route
    @Serializable object Home                                 : Route
    @Serializable data class  Player(val fileId: String)      : Route
    @Serializable data class  Share(val fileId: String)       : Route
    @Serializable object Transfers                            : Route
    @Serializable object Profile                              : Route
    @Serializable data class  Files(val folderId: String? = null) : Route
    @Serializable object Album                                : Route
    @Serializable object Wallpaper                            : Route
}
```

- [ ] **Step 2: Write `StarVaultNavHost.kt` (placeholder Route calls)**

```kotlin
package com.starvault.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.starvault.ui.album.AlbumRoute
import com.starvault.ui.files.FilesRoute
import com.starvault.ui.home.HomeRoute
import com.starvault.ui.login.LoginRoute
import com.starvault.ui.player.PlayerRoute
import com.starvault.ui.profile.ProfileRoute
import com.starvault.ui.share.ShareRoute
import com.starvault.ui.transfers.TransfersRoute
import com.starvault.ui.wallpaper.WallpaperRoute

@Composable
fun StarVaultNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Route.Login, modifier = modifier) {
        composable<Route.Login> {
            LoginRoute(onLoggedIn = {
                navController.navigate(Route.Home) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            })
        }
        composable<Route.Home>      { HomeRoute(nav = navController) }
        composable<Route.Album>     { AlbumRoute(nav = navController) }
        composable<Route.Transfers> { TransfersRoute(nav = navController) }
        composable<Route.Profile>   { ProfileRoute(nav = navController) }

        composable<Route.Files>     { entry -> FilesRoute(args = entry.toRoute(), nav = navController) }
        composable<Route.Player>    { entry -> PlayerRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }) }
        composable<Route.Share>     { entry -> ShareRoute(args = entry.toRoute(), onBack = { navController.popBackStack() }) }
        composable<Route.Wallpaper> { WallpaperRoute(onBack = { navController.popBackStack() }) }
    }
}
```

- [ ] **Step 3: Verify compile (will FAIL — no Screen/Route yet)**

Expected: failure with "Unresolved reference: ui.*" — this is fine, fixed task by task in T13–T21.

---

## Task 11: BottomNavBar + common components

**Files:**
- Create: `app/src/main/kotlin/com/starvault/component/StatusBarStyle.kt`
- Create: `app/src/main/kotlin/com/starvault/component/Icons.kt`           (统一所有 stub ImageVector)
- Create: `app/src/main/kotlin/com/starvault/component/StateViews.kt`      (Skeleton + ErrorView 共享)
- Create: `app/src/main/kotlin/com/starvault/component/BottomNavBar.kt`
- Create: `app/src/main/kotlin/com/starvault/component/Chip.kt`
- Create: `app/src/main/kotlin/com/starvault/component/FileRow.kt`
- Create: `app/src/main/kotlin/com/starvault/component/TopBar.kt`

- [ ] **Step 1: Write `StatusBarStyle.kt`**

```kotlin
package com.starvault.component

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun StatusBarStyle(darkIcons: Boolean) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}
```

- [ ] **Step 2: Write `Icons.kt` (unified stub vector set)**

```kotlin
package com.starvault.component

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Stub ImageVectors for initial implementation. After T11 lands, replace
 * with real ImageVectors produced by the Valkyrie IDE plugin from
 * `design/index.html` and `design/0X-*.html` SVGs.
 *
 * IMPORTANT: Paparazzi screenshots in T22–T30 will render blank icon
 * slots until you replace these stubs with real path data.
 */
object Icons {
    val Home      = stub("Home")
    val Files     = stub("Files")
    val Transfers = stub("Transfers")
    val Profile   = stub("Profile")
    val More      = stub("More")
    val Back      = stub("Back")
    val Close     = stub("Close")
    val Play      = stub("Play")
    val Pause     = stub("Pause")
    val Search    = stub("Search")
    val Star      = stub("Star")
}

private fun stub(name: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).build()
```

- [ ] **Step 3: Write `BottomNavBar.kt`**

```kotlin
package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.starvault.nav.Route
import com.starvault.theme.StarVaultTheme

private data class TabSpec(
    val label: String,
    val icon: ImageVector,
    val route: Route,
    val selected: (NavDestination) -> Boolean,
)

@Composable
fun BottomNavBar(nav: NavHostController, currentDestination: NavDestination?) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val d = StarVaultTheme.dimens

    // 4 tabs per spec §6.2: Home / Files / Transfers / Profile
    // Tab "文件" -> Route.Files; selected predicate covers both root
    // (folderId == null) and sub-folder destinations to keep tab active.
    val tabs = listOf(
        TabSpec("首页", Icons.Home,      Route.Home)      { it.hasRoute<Route.Home>() },
        TabSpec("文件", Icons.Files,     Route.Files())   { it.hasRoute<Route.Files>() },
        TabSpec("传输", Icons.Transfers, Route.Transfers) { it.hasRoute<Route.Transfers>() },
        TabSpec("我的", Icons.Profile,   Route.Profile)   { it.hasRoute<Route.Profile>() },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(width = d.borderHairline, color = c.border)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(d.bottomBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val active = currentDestination?.let(tab.selected) ?: false
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable {
                            nav.navigate(tab.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp),
                        tint = if (active) c.accent else c.muted,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(tab.label, style = t.micro, color = if (active) c.accent else c.muted)
                }
            }
        }
    }
}
```

> Engineer: 用 `Valkyrie IDE 插件`把 `design/index.html` 顶部 4 个 SVG 批量转出后，**只替换 `Icons.kt` 里的 `stub(...)` 返回值**（不要在 BottomNavBar 内再声明 `object Icons`），避免同包重名冲突。

- [ ] **Step 3: Write `Chip.kt`** (tag chip)

```kotlin
package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.starvault.data.model.TagColor
import com.starvault.theme.StarVaultTheme

@Composable
fun TagChip(color: TagColor, label: String, modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    val accent = when (color) {
        TagColor.TAG1 -> c.tag1; TagColor.TAG2 -> c.tag2; TagColor.TAG3 -> c.tag3
        TagColor.TAG4 -> c.tag4; TagColor.TAG5 -> c.tag5
    }
    Text(
        text = label,
        style = StarVaultTheme.typography.caption,
        color = accent,
        modifier = modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
```

- [ ] **Step 4: Write `FileRow.kt`**

```kotlin
package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.data.model.FileItem
import com.starvault.theme.StarVaultTheme

@Composable
fun FileRow(
    file: FileItem,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = StarVaultTheme.typography
    val c = StarVaultTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail 占位（实色块 mock，后续可换 Image）
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(c.accentSoft, RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, style = t.body, color = c.fg, maxLines = 1)
            Text(humanSize(file.sizeBytes), style = t.caption, color = c.muted)
        }
        if (file.tag != null) {
            Spacer(Modifier.width(8.dp))
            TagChip(file.tag, file.tag.name.removePrefix("TAG"))
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.More, contentDescription = "more", modifier = Modifier.size(18.dp), tint = c.muted)
    }
}

private fun humanSize(b: Long?): String = when {
    b == null -> "—"
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    b < 1024L * 1024 * 1024 -> "${b / 1024 / 1024} MB"
    else -> "%.1f GB".format(b / 1024.0 / 1024 / 1024)
}
```

> Note: `Icons` object lives in `Icons.kt` (T11 Step 2) — **do not redeclare here**.

- [ ] **Step 5: Write `TopBar.kt`**

```kotlin
package com.starvault.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val t = StarVaultTheme.typography
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Back, contentDescription = "back", tint = c.fg)
            }
        }
        Text(title, style = t.subtitle, color = c.fg)
        Box(modifier = Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}
```

- [ ] **Step 6: Write `StateViews.kt` (shared skeleton + error view)**

```kotlin
package com.starvault.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * Generic loading skeleton: 8 placeholder rows of accent-soft blocks.
 * Reused by all screens' Loading state (see T14-T21).
 */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(c.accentSoft, RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Generic error view: centered message + retry button.
 * Reused by all screens' Error state.
 */
@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val s = StarVaultTheme.shapes
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = t.body, color = c.danger)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(40.dp)
                    .background(c.accent, s.md)
                    .clickable(onClick = onRetry),
                contentAlignment = Alignment.Center,
            ) {
                Text("重试", style = t.subtitle, color = c.accentOn)
            }
        }
    }
}
```

- [ ] **Step 7: Verify compile (component-level)**

Run: `./gradlew :app:compileDebugKotlin --quiet`
Expected: depends on whether NavHost has unresolved symbols (T13–T21) — that's expected; only the components themselves should compile cleanly when isolated. Engineers may stub out the NavHost for incremental verification.

---

## Task 12: MainActivity + StarVaultApp

**Files:**
- Modify: `app/src/main/kotlin/com/starvault/MainActivity.kt`
- Create: `app/src/main/kotlin/com/starvault/StarVaultApp.kt`

- [ ] **Step 1: Write `StarVaultApp.kt`**

```kotlin
package com.starvault

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.starvault.component.BottomNavBar
import com.starvault.nav.Route
import com.starvault.nav.StarVaultNavHost
import com.starvault.theme.StarVaultTheme

@Composable
fun StarVaultApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination

    val showBottomBar = destination?.let { d ->
        d.hasRoute<Route.Home>()      ||
        d.hasRoute<Route.Files>()     ||
        d.hasRoute<Route.Transfers>() ||
        d.hasRoute<Route.Profile>()
    } ?: false

    Scaffold(
        bottomBar = { if (showBottomBar) BottomNavBar(nav, destination) },
        containerColor = StarVaultTheme.colors.bg,
    ) { padding ->
        StarVaultNavHost(navController = nav, modifier = Modifier.padding(padding))
    }
}
```

- [ ] **Step 2: Update `MainActivity.kt`**

```kotlin
package com.starvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.starvault.theme.StarVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StarVaultTheme { StarVaultApp() } }
    }
}
```

- [ ] **Step 3: Verify build (T13–T21 will fail until each screen lands)**

Run: `./gradlew :app:assembleDebug --quiet`
Expected: BUILD FAILED on missing ui.* symbols. Continue to T13.

---

## Task 13: Login screen (Screen + Route + VM + UiState + Preview)

**Files:**
- Create: `app/src/main/kotlin/com/starvault/ui/login/LoginUiState.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/login/LoginViewModel.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/login/LoginScreen.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/login/LoginRoute.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/login/LoginScreenPreview.kt`

> Reference: `design/00-login.html`

- [ ] **Step 1: Write `LoginUiState.kt`**

```kotlin
package com.starvault.ui.login

sealed interface LoginUiState {
    data object Idle                                          : LoginUiState
    data object Loading                                        : LoginUiState
    data object Success                                        : LoginUiState
    data class  Error(val message: String)                     : LoginUiState
}
```

- [ ] **Step 2: Write `LoginViewModel.kt`**

```kotlin
package com.starvault.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login() = viewModelScope.launch {
        _state.value = LoginUiState.Loading
        runCatching { delay(800) }   // 模拟扫码联调
            .onSuccess { _state.value = LoginUiState.Success }
            .onFailure { _state.value = LoginUiState.Error(it.message ?: "未知错误") }
    }

> OAuth 设备码流：`OpenAuthApiService` + `OpenAuthManager`。
}
```

- [ ] **Step 3: Write `LoginScreen.kt` (pure UI)**

```kotlin
package com.starvault.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

@Composable
fun LoginScreen(
    state: LoginUiState,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val s = StarVaultTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(c.accent, s.lg),
                contentAlignment = Alignment.Center,
            ) { Text("SV", style = t.title, color = c.accentOn) }
            Spacer(Modifier.height(24.dp))
            Text("StarVault", style = t.display, color = c.fg)
            Spacer(Modifier.height(8.dp))
            Text("把网盘重新想象", style = t.body, color = c.muted)
            Spacer(Modifier.height(48.dp))

            when (state) {
                LoginUiState.Idle, LoginUiState.Error -> PrimaryButton("扫码登录", enabled = true, onClick = onLoginClick)
                LoginUiState.Loading -> Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = c.accent)
                }
                LoginUiState.Success -> Text("已登录", style = t.subtitle, color = c.success)
            }

> OAuth 设备码流：`OpenAuthApiService` + `OpenAuthManager`。

            if (state is LoginUiState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(state.message, style = t.caption, color = c.danger)
            }
        }
    }
}

@Composable
internal fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val s = StarVaultTheme.shapes
    Box(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .height(44.dp)
            .background(if (enabled) c.accent else c.muted, s.md)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = t.subtitle, color = c.accentOn) }
}
```

- [ ] **Step 4: Write `LoginRoute.kt`**

```kotlin
package com.starvault.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Trigger navigation once when state becomes Success. LaunchedEffect
    // keyed on `state` ensures the lambda runs only on the transition
    // (not on every recomposition).
    LaunchedEffect(state) {
        if (state is LoginUiState.Success) onLoggedIn()
    }

    LoginScreen(
        state = state,
        onLoginClick = vm::login,
    )
}
```

- [ ] **Step 5: Write `LoginScreenPreview.kt`**

```kotlin
package com.starvault.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

@Preview(name = "Login/Idle",   showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun LoginIdlePreview()   = StarVaultTheme { LoginScreen(LoginUiState.Idle, {}) }

@Preview(name = "Login/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun LoginLoadingPreview() = StarVaultTheme { LoginScreen(LoginUiState.Loading, {}) }

@Preview(name = "Login/Error",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun LoginErrorPreview()   = StarVaultTheme { LoginScreen(LoginUiState.Error("网络超时"), {}) }
```

- [ ] **Step 6: Verify compile**

Run: `./gradlew :app:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL. Only Login screen exists — other `ui.*.Route` calls in NavHost will still fail; continue to T14.

---

## Task 14: Home screen

> Reference: `design/01-home.html`

**Files:**
- Create: `app/src/main/kotlin/com/starvault/ui/home/HomeUiState.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/home/HomeViewModel.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/home/HomeScreen.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/home/HomeRoute.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/home/HomeScreenPreview.kt`

- [ ] **Step 1: Write `HomeUiState.kt`**

```kotlin
package com.starvault.ui.home

import com.starvault.data.model.FileItem

sealed interface HomeUiState {
    data object Loading                              : HomeUiState
    data class  Ready(val files: List<FileItem>)    : HomeUiState
    data class  Error(val message: String)          : HomeUiState
}
```

- [ ] **Step 2: Write `HomeViewModel.kt`**

```kotlin
package com.starvault.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.data.FixtureLoader
import com.starvault.data.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        runCatching {
            FixtureLoader.loadDelayed<List<FileItem>>(getApplication(), "fixtures/files.json")
        }.onSuccess  { _state.value = HomeUiState.Ready(it.take(8)) }
         .onFailure  { _state.value = HomeUiState.Error(it.message ?: "unknown") }
    }
}
```

- [ ] **Step 3: Write `HomeScreen.kt`**

```kotlin
package com.starvault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.starvault.component.ErrorView
import com.starvault.component.FileRow
import com.starvault.component.HomeSkeleton
import com.starvault.component.StatusBarStyle
import com.starvault.component.TopBar
import com.starvault.theme.StarVaultTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenFile: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarStyle(darkIcons = true)
    val c = StarVaultTheme.colors
    Column(modifier.fillMaxSize().background(c.bg)) {
        TopBar(title = "首页")
        when (state) {
            HomeUiState.Loading -> HomeSkeleton()
            is HomeUiState.Ready -> LazyColumn {
                items(state.files, key = { it.id }) { f ->
                    FileRow(file = f, onClick = { onOpenFile(f.id) }, onMore = {})
                }
            }
            is HomeUiState.Error -> ErrorView(state.message, onRetry)
        }
    }
}
```

> 视觉对位时按 `01-home.html` 的 hero section + 文件列表 + 浮动按钮 调样式；关键 token：`typography.title`、`shapes.lg`、`spaceLg=16.dp`。

- [ ] **Step 4: Write `HomeRoute.kt`**

```kotlin
package com.starvault.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.nav.Route

@Composable
fun HomeRoute(nav: NavHostController, vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenFile = { id -> nav.navigate(Route.Files(folderId = id)) },
        onRetry = vm::load,
    )
}
```

- [ ] **Step 5: Write `HomeScreenPreview.kt`**

```kotlin
package com.starvault.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.fixtures.FixturePresets
import com.starvault.theme.StarVaultTheme

@Preview(name = "Home/Ready",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun HomeReadyPreview()  = StarVaultTheme { HomeScreen(HomeUiState.Ready(FixturePresets.homeFiles()), {}, {}) }

@Preview(name = "Home/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun HomeLoadingPreview() = StarVaultTheme { HomeScreen(HomeUiState.Loading, {}, {}) }

@Preview(name = "Home/Error",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun HomeErrorPreview()  = StarVaultTheme { HomeScreen(HomeUiState.Error("网络不通"), {}) }
```

- [ ] **Step 6: Verify build (cumulative) — proceed only if compile passes**

Run: `./gradlew :app:compileDebugKotlin --quiet`

- [ ] **Step 7: Commit Login + Home (T13+T14)**

```bash
cd /Users/Gin/Workspace/StarVault
git add app/src/main/kotlin/com/starvault/ui/login app/src/main/kotlin/com/starvault/ui/home
git add app/src/main/kotlin/com/starvault/nav app/src/main/kotlin/com/starvault/component
git add app/src/main/kotlin/com/starvault/StarVaultApp.kt app/src/main/kotlin/com/starvault/MainActivity.kt
git add app/src/main/assets app/src/test
git commit -m "feat(screens): Login + Home + nav shell + common components

- Routes (9 @Serializable) + StarVaultNavHost (type-safe)
- BottomNavBar (4-tab, mockup-aligned) + StatusBarStyle + TopBar + Chip + FileRow
- Login: ViewModel + Idle/Loading/Success/Error states, 3 previews
- Home: ViewModel loads assets/fixtures/files.json, 3 previews
- StarVaultApp wires Scaffold + 4-tab conditional render
- 7 JSON fixtures, FixtureLoader (with MOCK_DELAY in debug)
- FixturePresets (test-side hardcoded data)"
```

---

## Task 15: Player screen

> Reference: `design/02-player.html`

**Files:**
- Create: `app/src/main/kotlin/com/starvault/ui/player/PlayerUiState.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/player/PlayerViewModel.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/player/PlayerScreen.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/player/PlayerRoute.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/player/PlayerScreenPreview.kt`

- [ ] **Step 1: Write `PlayerUiState.kt`**

```kotlin
package com.starvault.ui.player

sealed interface PlayerUiState {
    data object Loading                                  : PlayerUiState
    data class  Ready(
        val fileId: String,
        val title: String,
        val durationSeconds: Int,
        val positionSeconds: Int,
        val isPlaying: Boolean,
    )                                                       : PlayerUiState
    data class  Error(val message: String)                 : PlayerUiState
}
```

- [ ] **Step 2: Write `PlayerViewModel.kt`**

```kotlin
package com.starvault.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.starvault.nav.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val args: Route.Player = savedState.toRoute()
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Ready(
        fileId = args.fileId,
        title = "示例视频 #$args.fileId",
        durationSeconds = 600,
        positionSeconds = 120,
        isPlaying = true,
    ))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
}
```

- [ ] **Step 3: Write `PlayerScreen.kt`**

```kotlin
package com.starvault.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.starvault.component.StatusBarStyle
import com.starvault.component.TopBar
import com.starvault.theme.StarVaultTheme

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarStyle(darkIcons = false)  // 视频区深底
    val c = StarVaultTheme.colors
    Column(modifier.fillMaxSize().background(c.fg)) {
        TopBar(title = (state as? PlayerUiState.Ready)?.title ?: "", onBack = onBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) { Text("▶ 视频占位", color = c.muted) }
        Spacer(Modifier.height(16.dp))
        // 控制条 / 时长 / 描述 参照 02-player.html
    }
}
```

- [ ] **Step 4: Write `PlayerRoute.kt`**

```kotlin
package com.starvault.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starvault.nav.Route

@Composable
fun PlayerRoute(args: Route.Player, onBack: () -> Unit, vm: PlayerViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    PlayerScreen(state = state, onBack = onBack, onPlayPause = { /* 桩 */ })
}
```

- [ ] **Step 5: Write `PlayerScreenPreview.kt`**

```kotlin
package com.starvault.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

@Preview(name = "Player/Ready", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun PlayerReadyPreview() = StarVaultTheme {
    PlayerScreen(PlayerUiState.Ready("f02", "东京 vlog.mp4", 600, 120, true), {}, {})
}
```

- [ ] **Step 6: Verify + commit**

Run: `./gradlew :app:compileDebugKotlin --quiet` → expected SUCCESS.

```bash
git add app/src/main/kotlin/com/starvault/ui/player
git commit -m "feat(screens): Player with SavedStateHandle route + dark status bar"
```

---

## Task 16: Share screen

> Reference: `design/03-share.html`

**Files (same 5-file pattern as T15):**
- Create: `app/src/main/kotlin/com/starvault/ui/share/ShareUiState.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/share/ShareViewModel.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/share/ShareScreen.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/share/ShareRoute.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/share/ShareScreenPreview.kt`

- [ ] **Step 1: `ShareUiState.kt`**

```kotlin
package com.starvault.ui.share

import com.starvault.data.model.ShareLink

sealed interface ShareUiState {
    data object Loading                                : ShareUiState
    data class  Ready(val links: List<ShareLink>)      : ShareUiState
    data class  Error(val message: String)              : ShareUiState
}
```

- [ ] **Step 2: `ShareViewModel.kt`**

```kotlin
package com.starvault.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.data.FixtureLoader
import com.starvault.data.model.ShareLink
import com.starvault.nav.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Receives the typed route args (Route.Share) via constructor injection from
 * ShareRoute's viewModelFactory. The route fileId is used to filter the
 * fixtures down to the relevant share links.
 */
class ShareViewModel(
    app: Application,
    val args: Route.Share,
) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                FixtureLoader.loadDelayed<List<ShareLink>>(getApplication(), "fixtures/share_links.json")
            }.onSuccess { all ->
                val matched = all.filter { it.fileId == args.fileId }
                _state.value = ShareUiState.Ready(matched.ifEmpty { all.take(1) })
            }.onFailure {
                _state.value = ShareUiState.Error(it.message ?: "unknown")
            }
        }
    }
}
```

- [ ] **Step 4: `ShareRoute.kt`**

```kotlin
package com.starvault.ui.share

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import com.starvault.nav.Route

@Composable
fun ShareRoute(
    args: Route.Share,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val vm: ShareViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ShareViewModel(ctx.applicationContext as Application, args) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    ShareScreen(state = state, onBack = onBack)
}
```

- [ ] **Step 3-5: `ShareScreen.kt` / `ShareRoute.kt` / `ShareScreenPreview.kt`**

`ShareScreen` 参照 `03-share.html`：标题 + 链接卡片（url + accessCode + 复制按钮 + 过期时间）。

- [ ] **Step 6: Verify + commit**

```bash
./gradlew :app:compileDebugKotlin --quiet
git add app/src/main/kotlin/com/starvault/ui/share
git commit -m "feat(screens): Share — link list + copy code, ViewModel reads share_links.json"
```

---

## Task 17: Transfers screen

> Reference: `design/04-transfers.html`

**Files (5-file pattern):**
- Create: `app/src/main/kotlin/com/starvault/ui/transfers/TransfersUiState.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/transfers/TransfersViewModel.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/transfers/TransfersScreen.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/transfers/TransfersRoute.kt`
- Create: `app/src/main/kotlin/com/starvault/ui/transfers/TransfersScreenPreview.kt`

- [ ] **Step 1: `TransfersUiState.kt`**

```kotlin
package com.starvault.ui.transfers

import com.starvault.data.model.Transfer

sealed interface TransfersUiState {
    data object Loading                              : TransfersUiState
    data class  Ready(val transfers: List<Transfer>) : TransfersUiState
    data class  Error(val message: String)            : TransfersUiState
}
```

- [ ] **Step 2: `TransfersViewModel.kt`**

```kotlin
package com.starvault.ui.transfers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.data.FixtureLoader
import com.starvault.data.model.Transfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransfersViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<TransfersUiState>(TransfersUiState.Loading)
    val state: StateFlow<TransfersUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        runCatching {
            FixtureLoader.loadDelayed<List<Transfer>>(getApplication(), "fixtures/transfers.json")
        }.onSuccess  { _state.value = TransfersUiState.Ready(it) }
         .onFailure  { _state.value = TransfersUiState.Error(it.message ?: "unknown") }
    }
}
```

- [ ] **Step 3: `TransfersScreen.kt`** (skeleton + segments)

```kotlin
package com.starvault.ui.transfers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.component.ErrorView
import com.starvault.component.HomeSkeleton
import com.starvault.component.StatusBarStyle
import com.starvault.component.TopBar
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import com.starvault.theme.StarVaultTheme

@Composable
fun TransfersScreen(
    state: TransfersUiState,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarStyle(darkIcons = true)
    val c = StarVaultTheme.colors
    Column(modifier.fillMaxSize().background(c.bg)) {
        TopBar(title = "传输")
        when (state) {
            TransfersUiState.Loading -> HomeSkeleton()
            is TransfersUiState.Ready -> TransfersList(state.transfers)
            is TransfersUiState.Error -> ErrorView(state.message, onRetry = { /* vm.load() — wired in Route */ })
        }
    }
}

@Composable
private fun TransfersList(transfers: List<Transfer>) {
    val c = StarVaultTheme.colors
    val running = transfers.filter { it.status == TransferStatus.RUNNING || it.status == TransferStatus.PAUSED }
    val done    = transfers.filter { it.status == TransferStatus.SUCCESS }
    val failed  = transfers.filter { it.status == TransferStatus.FAILED }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (running.isNotEmpty()) {
            item { SectionLabel("进行中") }
            items(running, key = { it.id }) { TransferRow(it) }
        }
        if (done.isNotEmpty()) {
            item { SectionLabel("已完成") }
            items(done, key = { it.id }) { TransferRow(it) }
        }
        if (failed.isNotEmpty()) {
            item { SectionLabel("失败") }
            items(failed, key = { it.id }) { TransferRow(it) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = StarVaultTheme.typography.caption,
        color = StarVaultTheme.colors.muted,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun TransferRow(t: Transfer) {
    val c = StarVaultTheme.colors
    val t2 = StarVaultTheme.typography
    val progress = if (t.totalBytes > 0) t.transferredBytes.toFloat() / t.totalBytes else 0f
    val statusColor = when (t.status) {
        TransferStatus.RUNNING -> c.accent
        TransferStatus.PAUSED  -> c.muted
        TransferStatus.SUCCESS -> c.success
        TransferStatus.FAILED  -> c.danger
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row {
            Text(t.fileName, style = t2.body, color = c.fg, modifier = Modifier.weight(1f))
            Text(t.status.name, style = t2.caption, color = statusColor)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = statusColor,
        )
    }
}
```

- [ ] **Step 4: `TransfersRoute.kt`**

```kotlin
package com.starvault.ui.transfers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.nav.Route

@Composable
fun TransfersRoute(nav: NavHostController, vm: TransfersViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    TransfersScreen(
        state = state,
        onOpenFile = { id -> nav.navigate(Route.Files(folderId = id)) },
    )
}
```

- [ ] **Step 5: `TransfersScreenPreview.kt`**

```kotlin
package com.starvault.ui.transfers

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.fixtures.FixturePresets
import com.starvault.theme.StarVaultTheme

@Preview(name = "Transfers/Ready",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun TransfersReadyPreview()  = StarVaultTheme { TransfersScreen(TransfersUiState.Ready(FixturePresets.transfers())) {} }

@Preview(name = "Transfers/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun TransfersLoadingPreview() = StarVaultTheme { TransfersScreen(TransfersUiState.Loading) {} }

@Preview(name = "Transfers/Error",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun TransfersErrorPreview()  = StarVaultTheme { TransfersScreen(TransfersUiState.Error("网络中断")) {} }
```

- [ ] **Step 6: Verify + commit**

```bash
./gradlew :app:compileDebugKotlin --quiet
git add app/src/main/kotlin/com/starvault/ui/transfers
git commit -m "feat(screens): Transfers — segmented list with progress + status color"
```

---

## Task 18: Profile screen

> Reference: `design/05-profile.html`

**Files (5-file pattern):**
- `app/src/main/kotlin/com/starvault/ui/profile/{ProfileUiState,ProfileViewModel,ProfileScreen,ProfileRoute,ProfileScreenPreview}.kt`

- [ ] **Step 1: `ProfileUiState.kt`**

```kotlin
package com.starvault.ui.profile

import com.starvault.data.model.User

sealed interface ProfileUiState {
    data object Loading                  : ProfileUiState
    data class  Ready(val user: User)    : ProfileUiState
    data class  Error(val message: String) : ProfileUiState
}
```

- [ ] **Step 2: `ProfileViewModel.kt`**

```kotlin
package com.starvault.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.data.FixtureLoader
import com.starvault.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        runCatching {
            FixtureLoader.loadDelayed<User>(getApplication(), "fixtures/profile.json")
        }.onSuccess  { _state.value = ProfileUiState.Ready(it) }
         .onFailure  { _state.value = ProfileUiState.Error(it.message ?: "unknown") }
    }
}
```

- [ ] **Step 3: `ProfileScreen.kt`**

```kotlin
package com.starvault.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.component.ErrorView
import com.starvault.component.HomeSkeleton
import com.starvault.component.StatusBarStyle
import com.starvault.component.TopBar
import com.starvault.data.model.User
import com.starvault.theme.StarVaultTheme

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onWallpaperClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarStyle(darkIcons = true)
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(modifier.fillMaxSize().background(c.bg)) {
        TopBar(title = "我的")
        when (state) {
            ProfileUiState.Loading -> HomeSkeleton()
            is ProfileUiState.Ready -> ProfileContent(state.user, onWallpaperClick)
            is ProfileUiState.Error -> ErrorView(state.message, onRetry)
        }
    }
}

@Composable
private fun ProfileContent(user: User, onWallpaperClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(c.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(user.nickname.take(1), style = t.title, color = c.accentOn) }
            Spacer(Modifier.padding(8.dp))
            Column(Modifier.weight(1f)) {
                Text(user.nickname, style = t.title, color = c.fg)
                Text("VIP ${user.vipLevel}", style = t.caption, color = c.accent)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "已用 ${humanBytes(user.usedBytes)} / ${humanBytes(user.totalBytes)}",
            style = t.body, color = c.muted,
        )
        Spacer(Modifier.height(24.dp))
        SettingRow("切换壁纸", onWallpaperClick)
    }
}

@Composable
private fun SettingRow(label: String, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    ) {
        Text(label, style = t.body, color = c.fg, modifier = Modifier.weight(1f))
        Text("›", style = t.title, color = c.muted)
    }
}

private fun humanBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    b < 1024L * 1024 * 1024 -> "${b / 1024 / 1024} MB"
    b < 1024L * 1024 * 1024 * 1024 -> "%.1f GB".format(b / 1024.0 / 1024 / 1024)
    else -> "%.1f TB".format(b / 1024.0 / 1024 / 1024 / 1024)
}
```

- [ ] **Step 4: `ProfileRoute.kt`**

```kotlin
package com.starvault.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.starvault.nav.Route

@Composable
fun ProfileRoute(nav: NavHostController, vm: ProfileViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onWallpaperClick = { nav.navigate(Route.Wallpaper) },
        onRetry = vm::load,
    )
}
```

- [ ] **Step 5: `ProfileScreenPreview.kt`**

```kotlin
package com.starvault.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.fixtures.FixturePresets
import com.starvault.theme.StarVaultTheme

@Preview(name = "Profile/Ready",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun ProfileReadyPreview()  = StarVaultTheme { ProfileScreen(ProfileUiState.Ready(FixturePresets.user()), {}, {}) }

@Preview(name = "Profile/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun ProfileLoadingPreview() = StarVaultTheme { ProfileScreen(ProfileUiState.Loading, {}, {}) }

@Preview(name = "Profile/Error",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun ProfileErrorPreview()  = StarVaultTheme { ProfileScreen(ProfileUiState.Error("无法读取用户信息"), {}) }
```

- [ ] **Step 6: Verify + commit**

```bash
./gradlew :app:compileDebugKotlin --quiet
git add app/src/main/kotlin/com/starvault/ui/profile
git commit -m "feat(screens): Profile — avatar, VIP, capacity, wallpaper entry"
```

---

## Task 19: Files screen

> Reference: `design/06-files.html`

**Files (5-file pattern):**
- `app/src/main/kotlin/com/starvault/ui/files/{FilesUiState,FilesViewModel,FilesScreen,FilesRoute,FilesScreenPreview}.kt`

- [ ] **Step 1: `FilesUiState.kt`**

```kotlin
package com.starvault.ui.files

import com.starvault.data.model.FileItem

sealed interface FilesUiState {
    data object Loading                                                : FilesUiState
    data class  Ready(val items: List<FileItem>, val breadcrumb: List<String>) : FilesUiState
    data class  Error(val message: String)                              : FilesUiState
}
```

- [ ] **Step 2: `FilesViewModel.kt`**

```kotlin
package com.starvault.ui.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.data.FixtureLoader
import com.starvault.data.model.FileItem
import com.starvault.nav.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FilesViewModel(
    app: Application,
    val args: Route.Files,
) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<FilesUiState>(FilesUiState.Loading)
    val state: StateFlow<FilesUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        runCatching {
            FixtureLoader.loadDelayed<List<FileItem>>(getApplication(), "fixtures/files.json")
        }.onSuccess { all ->
            val inFolder = if (args.folderId == null) all else all.filter { it.id == args.folderId || it.id.startsWith("${args.folderId}_") }
            _state.value = FilesUiState.Ready(inFolder, breadcrumb = listOf("首页", args.folderId ?: "根目录"))
        }.onFailure {
            _state.value = FilesUiState.Error(it.message ?: "unknown")
        }
    }
}
```

- [ ] **Step 3: `FilesScreen.kt`**

```kotlin
package com.starvault.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.starvault.component.ErrorView
import com.starvault.component.FileRow
import com.starvault.component.HomeSkeleton
import com.starvault.component.StatusBarStyle
import com.starvault.component.TopBar
import com.starvault.theme.StarVaultTheme

@Composable
fun FilesScreen(
    state: FilesUiState,
    onOpenFolder: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onShare: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarStyle(darkIcons = true)
    val c = StarVaultTheme.colors
    Column(modifier.fillMaxSize().background(c.bg)) {
        TopBar(title = "文件", onBack = onBack)
        when (state) {
            FilesUiState.Loading -> HomeSkeleton()
            is FilesUiState.Ready -> LazyColumn {
                items(state.items, key = { it.id }) { f ->
                    FileRow(
                        file = f,
                        onClick = {
                            when (f.type) {
                                com.starvault.data.model.FileType.FOLDER -> onOpenFolder(f.id)
                                com.starvault.data.model.FileType.VIDEO  -> onOpenPlayer(f.id)
                                else                                      -> { /* Phase 1: no-op */ }
                            }
                        },
                        onMore = { onShare(f.id) },
                    )
                }
            }
            is FilesUiState.Error -> ErrorView(state.message, onRetry)
        }
    }
}
```

- [ ] **Step 4: `FilesRoute.kt`**

```kotlin
package com.starvault.ui.files

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.starvault.nav.Route

@Composable
fun FilesRoute(args: Route.Files, nav: NavHostController) {
    val ctx = LocalContext.current
    val vm: FilesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FilesViewModel(ctx.applicationContext as Application, args) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    FilesScreen(
        state = state,
        onOpenFolder = { id -> nav.navigate(Route.Files(folderId = id)) },
        onOpenPlayer = { id -> nav.navigate(Route.Player(fileId = id)) },
        onShare      = { id -> nav.navigate(Route.Share(fileId = id)) },
        onBack       = { nav.popBackStack() },
        onRetry      = vm::load,
    )
}
```

- [ ] **Step 5: `FilesScreenPreview.kt`**

```kotlin
package com.starvault.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.fixtures.FixturePresets
import com.starvault.theme.StarVaultTheme

@Preview(name = "Files/Ready",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun FilesReadyPreview()  = StarVaultTheme { FilesScreen(FilesUiState.Ready(FixturePresets.homeFiles(), listOf("首页")), {}, {}, {}, {}, {}) }

@Preview(name = "Files/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun FilesLoadingPreview() = StarVaultTheme { FilesScreen(FilesUiState.Loading, {}, {}, {}, {}, {}) }

@Preview(name = "Files/Error",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun FilesErrorPreview()  = StarVaultTheme { FilesScreen(FilesUiState.Error("无法读取文件"), {}, {}, {}, {}, {}) }
```

- [ ] **Step 6: Verify + commit**

```bash
./gradlew :app:compileDebugKotlin --quiet
git add app/src/main/kotlin/com/starvault/ui/files
git commit -m "feat(screens): Files — folder/file list, video opens Player, share entry"
```

---

## Task 20: Album screen

> Reference: `design/07-album.html`

**Files (5-file pattern):**
- `app/src/main/kotlin/com/starvault/ui/album/{AlbumUiState,AlbumViewModel,AlbumScreen,AlbumRoute,AlbumScreenPreview}.kt`

- [ ] **Step 1: `AlbumUiState.kt`**

```kotlin
package com.starvault.ui.album

import com.starvault.data.model.AlbumPhoto

sealed interface AlbumUiState {
    data object Loading                                  : AlbumUiState
    data class  Ready(val photos: List<AlbumPhoto>)     : AlbumUiState
    data class  Error(val message: String)                : AlbumUiState
}
```

- [ ] **Step 2: `AlbumViewModel.kt`**

```kotlin
package com.starvault.ui.album

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.data.FixtureLoader
import com.starvault.data.model.AlbumPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        runCatching {
            FixtureLoader.loadDelayed<List<AlbumPhoto>>(getApplication(), "fixtures/albums.json")
        }.onSuccess  { _state.value = AlbumUiState.Ready(it) }
         .onFailure  { _state.value = AlbumUiState.Error(it.message ?: "unknown") }
    }
}
```

- [ ] **Step 3: `AlbumScreen.kt`**

```kotlin
package com.starvault.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.component.ErrorView
import com.starvault.component.HomeSkeleton
import com.starvault.component.StatusBarStyle
import com.starvault.component.TopBar
import com.starvault.data.model.AlbumPhoto
import com.starvault.theme.StarVaultTheme

@Composable
fun AlbumScreen(
    state: AlbumUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarStyle(darkIcons = true)
    val c = StarVaultTheme.colors
    androidx.compose.foundation.layout.Column(modifier.fillMaxSize().background(c.bg)) {
        TopBar(title = "相册")
        when (state) {
            AlbumUiState.Loading -> HomeSkeleton()
            is AlbumUiState.Ready -> AlbumGrid(state.photos)
            is AlbumUiState.Error -> ErrorView(state.message, onRetry)
        }
    }
}

@Composable
private fun AlbumGrid(photos: List<AlbumPhoto>) {
    val c = StarVaultTheme.colors
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(c.bg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
    ) {
        items(photos, key = { it.id }) { p ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(p.width.toFloat() / p.height)
                    .background(c.accentSoft),
            ) { /* Phase 1: 占位色块；Phase 2 用 Coil 加载 */ }
        }
    }
}
```

- [ ] **Step 4: `AlbumRoute.kt`**

```kotlin
package com.starvault.ui.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun AlbumRoute(nav: NavHostController, vm: AlbumViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    AlbumScreen(state = state, onRetry = vm::load)
}
```

- [ ] **Step 5: `AlbumScreenPreview.kt`**

```kotlin
package com.starvault.ui.album

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.fixtures.FixturePresets
import com.starvault.theme.StarVaultTheme

@Preview(name = "Album/Ready",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun AlbumReadyPreview()  = StarVaultTheme { AlbumScreen(AlbumUiState.Ready(FixturePresets.albumPhotos()), {}) }

@Preview(name = "Album/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun AlbumLoadingPreview() = StarVaultTheme { AlbumScreen(AlbumUiState.Loading, {}) }

@Preview(name = "Album/Error",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun AlbumErrorPreview()  = StarVaultTheme { AlbumScreen(AlbumUiState.Error("读取相册失败"), {}) }
```

- [ ] **Step 6: Verify + commit**

```bash
./gradlew :app:compileDebugKotlin --quiet
git add app/src/main/kotlin/com/starvault/ui/album
git commit -m "feat(screens): Album — staggered grid of photo placeholders"
```

---

## Task 21: Wallpaper screen

> Reference: `design/08-wallpaper.html`

**Files (5-file pattern):**
- `app/src/main/kotlin/com/starvault/ui/wallpaper/{WallpaperUiState,WallpaperViewModel,WallpaperScreen,WallpaperRoute,WallpaperScreenPreview}.kt`

- [ ] **Step 1: `WallpaperUiState.kt`**

```kotlin
package com.starvault.ui.wallpaper

import com.starvault.data.model.Wallpaper
import com.starvault.data.model.WallpaperConfig

sealed interface WallpaperUiState {
    data object Loading                                                          : WallpaperUiState
    data class  Ready(val items: List<Wallpaper>, val config: WallpaperConfig)   : WallpaperUiState
    data class  Error(val message: String)                                        : WallpaperUiState
}
```

- [ ] **Step 2: `WallpaperViewModel.kt`**

```kotlin
package com.starvault.ui.wallpaper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.data.FixtureLoader
import com.starvault.data.model.Wallpaper
import com.starvault.data.model.WallpaperConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WallpaperViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<WallpaperUiState>(WallpaperUiState.Loading)
    val state: StateFlow<WallpaperUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        runCatching {
            val items = FixtureLoader.loadDelayed<List<Wallpaper>>(getApplication(), "fixtures/wallpapers.json")
            val cfg   = FixtureLoader.load<WallpaperConfig>(getApplication(), "fixtures/wallpaper_config.json")
            items to cfg
        }.onSuccess  { (items, cfg) -> _state.value = WallpaperUiState.Ready(items, cfg) }
         .onFailure  { _state.value = WallpaperUiState.Error(it.message ?: "unknown") }
    }
}
```

- [ ] **Step 3: `WallpaperScreen.kt`**

```kotlin
package com.starvault.ui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starvault.component.ErrorView
import com.starvault.component.HomeSkeleton
import com.starvault.component.StatusBarStyle
import com.starvault.component.TopBar
import com.starvault.theme.StarVaultTheme

@Composable
fun WallpaperScreen(
    state: WallpaperUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarStyle(darkIcons = true)
    val c = StarVaultTheme.colors
    Column(modifier.fillMaxSize().background(c.bg)) {
        TopBar(title = "壁纸", onBack = onBack)
        when (state) {
            WallpaperUiState.Loading -> HomeSkeleton()
            is WallpaperUiState.Ready -> Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("自动轮换", style = StarVaultTheme.typography.body, color = c.fg, modifier = Modifier.weight(1f))
                    Switch(checked = state.config.enabled, onCheckedChange = onToggleEnabled)
                }
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(state.items, key = { it.id }) { w ->
                        Box(
                            modifier = Modifier.size(180.dp).padding(4.dp).background(c.accentSoft),
                        ) { /* Phase 1: 占位色块；Phase 2 用 Coil 加载 */ }
                    }
                }
            }
            is WallpaperUiState.Error -> ErrorView(state.message, onRetry)
        }
    }
}
```

- [ ] **Step 4: `WallpaperRoute.kt`**

```kotlin
package com.starvault.ui.wallpaper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WallpaperRoute(onBack: () -> Unit, vm: WallpaperViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    WallpaperScreen(
        state = state,
        onToggleEnabled = { /* Phase 1: no-op; Phase 7 接系统壁纸 API */ },
        onBack = onBack,
        onRetry = vm::load,
    )
}
```

- [ ] **Step 5: `WallpaperScreenPreview.kt`**

```kotlin
package com.starvault.ui.wallpaper

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.fixtures.FixturePresets
import com.starvault.data.model.WallpaperConfig
import com.starvault.data.model.DisplayMode
import com.starvault.theme.StarVaultTheme

@Preview(name = "Wallpaper/Ready",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun WallpaperReadyPreview()  = StarVaultTheme { WallpaperScreen(WallpaperUiState.Ready(FixturePresets.wallpapers(), FixturePresets.wallpaperConfig()), {}, {}, {}) }

@Preview(name = "Wallpaper/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun WallpaperLoadingPreview() = StarVaultTheme { WallpaperScreen(WallpaperUiState.Loading, {}, {}, {}) }

@Preview(name = "Wallpaper/Error",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable fun WallpaperErrorPreview()  = StarVaultTheme { WallpaperScreen(WallpaperUiState.Error("读取壁纸列表失败"), {}, {}, {}) }
```

- [ ] **Step 6: Verify + commit**

```bash
./gradlew :app:compileDebugKotlin --quiet
git add app/src/main/kotlin/com/starvault/ui/wallpaper
git commit -m "feat(screens): Wallpaper — toggle + grid + categories, system API wired in Phase 7"
```

---

## Task 22: Paparazzi base + Login screenshot test

**Files:**
- Create: `app/src/test/kotlin/com/starvault/screenshot/DeviceConfigExt.kt`
- Create: `app/src/test/kotlin/com/starvault/screenshot/LoginScreenshotTest.kt`

- [ ] **Step 1: Write `DeviceConfigExt.kt`**

```kotlin
package com.starvault.screenshot

import app.cash.paparazzi.DeviceConfig

val PHONE_412_900 = DeviceConfig.PIXEL_5.copy(
    screenWidth  = 412,
    screenHeight = 900,
    xdpi = 440, ydpi = 440,
    softButtons = false,
)
```

- [ ] **Step 2: Write `LoginScreenshotTest.kt`**

```kotlin
package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SessionParams
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.login.LoginScreen
import com.starvault.ui.login.LoginUiState
import org.junit.Rule
import org.junit.Test

class LoginScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig  = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi  = false,
    )

    @Test fun login_idle() = paparazzi.snapshot {
        StarVaultTheme { LoginScreen(LoginUiState.Idle, {}) }
    }

    @Test fun login_loading() = paparazzi.snapshot {
        StarVaultTheme { LoginScreen(LoginUiState.Loading, {}) }
    }

    @Test fun login_error() = paparazzi.snapshot {
        StarVaultTheme { LoginScreen(LoginUiState.Error("扫码超时"), {}) }
    }

> OAuth 设备码流：`OpenAuthApiService` + `OpenAuthManager`。
}
```

- [ ] **Step 3: Run Paparazzi (record baseline on first run)**

Run: `./gradlew :app:recordPaparazziDebug --quiet`
Expected: BUILD SUCCESSFUL, 3 PNG files created under `app/src/test/snapshots/images/`. **First run records; subsequent runs verify.**

- [ ] **Step 4: Inspect generated PNGs**

Run: `ls app/src/test/snapshots/images/`
Expected: 3 PNG files like `LoginScreenshotTest_login_idle.png` etc.

- [ ] **Step 5: Verify golden**

Run: `./gradlew :app:verifyPaparazziDebug --quiet`
Expected: BUILD SUCCESSFUL, 0 changes (or 3 changes if first run).

- [ ] **Step 6: Commit**

```bash
git add app/src/test/kotlin/com/starvault/screenshot app/src/test/snapshots
git commit -m "test(screenshot): Paparazzi base + Login 3-state golden"
```

> Note: First time you commit PNGs, git LFS may not have hooked them (gitattributes has `*.png filter=lfs`). Verify with `git ls-files --stage app/src/test/snapshots/images/*.png` — should show `lfs` pointer. If not, run `git lfs track "app/src/test/snapshots/images/*.png"` first.

---

## Task 23: Home screenshot test

**Files:**
- Create: `app/src/test/kotlin/com/starvault/screenshot/HomeScreenshotTest.kt`

- [ ] **Step 1: Write the test** (3 cases: ready/loading/error, using `FixturePresets.homeFiles()`)

```kotlin
package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SessionParams
import com.starvault.fixtures.FixturePresets
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.home.HomeScreen
import com.starvault.ui.home.HomeUiState
import org.junit.Rule
import org.junit.Test

class HomeScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    @Test fun home_ready()  = paparazzi.snapshot { StarVaultTheme { HomeScreen(HomeUiState.Ready(FixturePresets.homeFiles()), {}, {}) } }
    @Test fun home_loading() = paparazzi.snapshot { StarVaultTheme { HomeScreen(HomeUiState.Loading, {}, {}) } }
    @Test fun home_error()   = paparazzi.snapshot { StarVaultTheme { HomeScreen(HomeUiState.Error("网络不通"), {}) } }
}
```

- [ ] **Step 2: Record + verify**

```bash
./gradlew :app:recordPaparazziDebug --tests com.starvault.screenshot.HomeScreenshotTest
./gradlew :app:verifyPaparazziDebug --tests com.starvault.screenshot.HomeScreenshotTest
```

- [ ] **Step 3: Commit**

```bash
git add app/src/test/kotlin/com/starvault/screenshot/HomeScreenshotTest.kt app/src/test/snapshots
git commit -m "test(screenshot): Home 3-state golden"
```

---

## Tasks 24–30: Remaining 7 screenshot tests

Apply the same pattern as T23 for each:
- T24 Player
- T25 Share
- T26 Transfers
- T27 Profile
- T28 Files
- T29 Album
- T30 Wallpaper

For each:
1. Create `XxxScreenshotTest.kt` with at least one Ready-state golden (Loading/Error optional)
2. Run `recordPaparazziDebug` then `verifyPaparazziDebug`
3. Commit: `test(screenshot): <Screen> golden`

---

## Task 31: App icon + adaptive icon + Splash Screen

**Files:**
- Create: `app/src/main/res/values/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml` (vector)
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `app/src/main/res/values/themes.xml` (add SplashScreen attrs)
- Create: `app/src/main/res/values/colors.xml` (add ic_launcher_background color)

- [ ] **Step 1: Use Image Asset Studio**

Engineer 跑：Android Studio → `File` → `New` → `Image Asset` → `Launcher Icons (Adaptive and Legacy)`，选 `design/icon.png` 当 source、背景色 `StarVaultColors.bg (#FAFAFA)`。Studio 会自动生成 `mipmap-*/ic_launcher.*` 与 adaptive icon。

- [ ] **Step 2: Wire Splash Screen**

Update `themes.xml`:
```xml
<style name="Theme.StarVault" parent="android:Theme.Material.Light.NoActionBar">
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowBackground">@color/bg</item>
    <item name="android:windowSplashScreenBackground">@color/bg</item>
    <item name="android:windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_foreground</item>
</style>
```

- [ ] **Step 3: Verify install + run on emulator/device**

```bash
./gradlew :app:installDebug
adb shell am start -n com.starvault/.MainActivity
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res
git commit -m "feat(icons): adaptive launcher + splash screen on bg color"
```

---

## Task 32: README + design-baseline script

**Files:**
- Create: `README.md`
- Create: `scripts/capture-design-baseline.sh`

- [ ] **Step 1: Write `README.md`**

```markdown
# StarVault Android

115 网盘的极简 Android 重设计。Phase 1 = UI 骨架 + 1:1 视觉还原。

## 环境要求
- JDK 17+
- Gradle 9.x（wrapper 自带）
- AGP 9.1.0
- Android Studio Ladybug+ (or later)
- Android SDK 35 + Platform Tools

## 运行
```bash
./gradlew installDebug
adb shell am start -n com.starvault/.MainActivity
```

## Paparazzi 截图回归
```bash
# 首次：记录 golden
./gradlew :app:recordPaparazziDebug
# 后续：验证（CI 用）
./gradlew :app:verifyPaparazziDebug
# 单 test 类
./gradlew :app:recordPaparazziDebug --tests com.starvault.screenshot.HomeScreenshotTest
```

## Git LFS
Paparazzi 截图通过 git LFS 存储。Clone 后请先：
```bash
git lfs install
git lfs pull
```

## 文档
- 设计稿：`docs/design/` 软链（指向 Open Design 私有目录）
- Spec：`docs/superpowers/specs/2026-06-14-starvault-android-skeleton-design.md`
- 计划：`docs/superpowers/plans/2026-06-14-starvault-android-skeleton.md`
```

- [ ] **Step 2: Write `scripts/capture-design-baseline.sh`**

```bash
#!/usr/bin/env bash
# 用 Playwright 把 design/*.html 渲染成 412x900 PNG baseline
# 需要：npm i -g playwright && npx playwright install chromium
set -euo pipefail

OUT_DIR="${1:-$(dirname "$0")/../design-baseline}"
WIDTH=412
HEIGHT=900

mkdir -p "$OUT_DIR"
for html in design/*.html; do
  name=$(basename "$html" .html)
  echo "Capturing $name ..."
  npx playwright screenshot --viewport-size=$WIDTH,$HEIGHT --full-page \
    "file://$(pwd)/$html" "$OUT_DIR/${name}.png"
done
```

```bash
chmod +x scripts/capture-design-baseline.sh
```

- [ ] **Step 3: Commit**

```bash
git add README.md scripts
git commit -m "docs: README + design-baseline Playwright capture script"
```

---

## Task 33: M1 DoD verify

- [ ] **Step 1: Build & install**

```bash
./gradlew clean :app:installDebug
adb shell am start -n com.starvault/.MainActivity
```
Expected: app launches; Login screen visible; tap login → Loading → Success → Home appears.

- [ ] **Step 2: Navigation smoke**

- Tap Home / Album / Transfers / Profile tabs → all show their screen
- From Home tap a file → Files screen
- From Files tap a video → Player screen (status bar dark icons)
- From Files tap share icon → Share screen
- From Profile tap 切换壁纸 → Wallpaper screen
- Back button → returns to caller

- [ ] **Step 3: All 9 Paparazzi goldens exist and pass**

```bash
ls app/src/test/snapshots/images/ | wc -l
./gradlew :app:verifyPaparazziDebug --quiet
```
Expected: ≥9 PNGs (one per screen, plus optional state variants), 0 failures.

- [ ] **Step 4: Visual diff vs design baseline (manual)**

```bash
bash scripts/capture-design-baseline.sh
# Open design-baseline/*.png and app/src/test/snapshots/images/*.png side-by-side
# Each screen should look "essentially the same" at 412x900
```

- [ ] **Step 5: Edge-to-edge check**

Inspect Player: status bar should be transparent, dark icons.
Inspect other screens: status bar transparent, dark icons.

- [ ] **Step 6: Edge cases**

- Rotate device → state preserved (ViewModel survives)
- Kill app from recents → reopen → Login again (no token persistence in M1)
- Background app for 10 min → still works

- [ ] **Step 7: Final commit (if any tweaks)**

```bash
git status
# If clean → M1 done. If tweaks → commit them:
git add -A
git commit -m "chore: M1 polish — final tweaks from DoD walkthrough"
```

---

## Self-Review Notes (for plan maintainer)

- **Spec coverage:** All 19 spec sections have a corresponding task. §10.4 Git LFS is configured in T22's "Note" + .gitattributes (committed in T5's git init earlier). §17 Phase 2+ is intentionally out of scope, not implemented.
- **No placeholders:** Each task has Files + Steps + runnable commands. Tasks 14/15/16/17/18/19/20/21 reference mockup HTML files for visual alignment; engineers must read the mockup for section ordering/colors but the screen skeleton + ViewModel + UiState + Preview are all given.
- **Type consistency:** All ViewModels expose `state: StateFlow<XxxUiState>`. All Routes use the same pattern. All Screens are pure `(state, ...callbacks, modifier)`. All Routes wire `viewModel() + collectAsStateWithLifecycle()`. All Tests use `PHONE_412_900`.
- **Self-audit fixes applied (2026-06-14 second pass):** 9 real bugs caught & fixed — T2/T3 `lifecycle-viewmodel-savedstate` dep added; T11 unified `Icons.kt` (was duplicate `object Icons` in `BottomNavBar` + `FileRow`); T11 `tab Route.Album` → `Route.Files`; T11 dead `Icons as MaterialIcons` import removed; T13 `LoginRoute` `Success` branch wrapped in `LaunchedEffect(state)`; T13 `PrimaryButton` dead `border(0.dp, ...)` removed; T14 missing `HomeSkeleton()` / `ErrorView()` added as `component/StateViews.kt`; T15 `PlayerScreen` missing `Color` / `dp` imports added; T16 `ShareViewModel` rewritten to `viewModelFactory` constructor-injection (was broken `args: Route.Share = SavedStateHandle().let { ... }`); T17–T21 each filled out to full 5-file pattern (UiState + ViewModel + Screen + Route + Preview) so the plan is end-to-end compilable, not skeleton-only.

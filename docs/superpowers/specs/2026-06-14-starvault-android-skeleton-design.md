# StarVault Android 骨架 — 设计文档（Phase 1）

| 字段 | 内容 |
|---|---|
| 文档版本 | v1.0 |
| 撰写日期 | 2026-06-14 |
| 状态 | 待用户复核 |
| 范围 | Phase 1：UI 骨架 + 1:1 还原视觉，不接真实 115 API |
| 实现目标 | 9 屏 Compose 实现可在 Android 14+ 设备运行，Paparazzi 视觉回归通过 |

---

## 1. 背景与定位

StarVault 是 115 网盘的 Android 原生重设计。设计稿（`design/` 目录下 10 个 HTML mockup）已经迭代多轮、用 Playwright 视觉验证终稿。本阶段（Phase 1）的目标是**把这些设计稿用 Jetpack Compose 1:1 实现成可运行的 Android 应用骨架**，业务功能（真实 115 接入、登录联调、下载、上传等）留到 Phase 2+。

**分发场景**：个人自用 / 小圈子，APK 侧载，不上架商店。

---

## 2. 范围

### 2.1 Phase 1 包含

- 9 个 Composable 屏：Login（00）、Home（01）、Player（02）、Share（03）、Transfers（04）、Profile（05）、Files（06）、Album（07）、Wallpaper（08）
- 自定义设计令牌系统（StarVaultTheme：Colors / Typography / Shapes / Dimens）
- 类型安全的 Compose Navigation 图（kotlinx.serialization 路由）
- 4-tab 全局底栏（Home / Album / Transfers / Profile），其他屏推到全屏堆栈
- 每屏一个 ViewModel + StateFlow + UiState sealed 类型
- JSON fixture 数据源（assets/fixtures/）+ FixtureLoader
- Paparazzi 截图回归测试（每屏至少一张 golden）
- 应用图标、Splash Screen、Edge-to-edge 默认开
- 简体中文文案，所有字符串走 strings.xml

### 2.2 Phase 1 不包含

- 真实 115 API 接入（任何形式：官方 / 逆向 / WebDAV / Alist）
- 登录扫码联调
- 网络层（OkHttp / Retrofit / Ktor）
- 本地数据库（Room / SQLDelight）
- 文件下载 / 上传引擎、传输任务持久化
- 推送 / 通知 / 后台任务（WorkManager）
- 多语言（仅中文）
- Dark Mode（仅 Light，与设计稿一致）
- 依赖注入框架（Hilt / Koin）
- UI 交互测试（Espresso / Compose Test）
- 设计稿之外的屏（设置详情页、空状态变体、错误页等）

---

## 3. 技术栈

| 维度 | 选择 | 备注 |
|---|---|---|
| 语言 | Kotlin 2.3.21 | 需 `kotlin.plugin.compose` 插件（与 Kotlin 同版本对齐）|
| UI 框架 | Jetpack Compose | BOM 2026.05.00 |
| 最低 SDK | 34 (Android 14) | 仅面向新设备 |
| 目标 SDK | 35 (Android 15) | |
| 编译 SDK | 35 | |
| 构建工具 | Gradle 9.x + Kotlin DSL + Version Catalog | AGP 9.1.0 要求 |
| AGP | 9.1.0 | 2026-03 发布；需 Gradle 9 |
| 项目结构 | 单 module（`:app`），按 feature 分包 | |
| 导航 | androidx.navigation:navigation-compose 2.8.x，类型安全路由 | 需 `kotlin.plugin.serialization` 插件 |
| 状态托管 | AndroidX ViewModel + StateFlow + sealed UiState | 不引入 Hilt |
| 数据序列化 | kotlinx.serialization | JSON fixture 反序列化 |
| 截图测试 | app.cash.paparazzi 2.0.0-alpha04+ | Gradle 9 兼容版；golden 进 git LFS |
| 字体 | Inter（4 字重打包进 res/font/） | 约 600KB |
| 图标 | SVG → ImageVector（Valkyrie IDE 插件批量转） | 落到 `ui/icons/` |

---

## 4. 项目结构

```
StarVault/
├── design/                                  # 现有设计稿软链，不动
├── docs/superpowers/specs/                  # 本文档所在
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml          # 单 Activity 入口
│       │   ├── kotlin/com/starvault/
│       │   │   ├── MainActivity.kt
│       │   │   ├── StarVaultApp.kt          # Scaffold + 4-tab 底栏 + NavHost
│       │   │   ├── nav/
│       │   │   │   ├── Routes.kt            # @Serializable sealed Route
│       │   │   │   └── StarVaultNavHost.kt
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt             # StarVaultColors
│       │   │   │   ├── Type.kt              # StarVaultTypography + Inter
│       │   │   │   ├── Shape.kt             # StarVaultShapes
│       │   │   │   ├── Dimens.kt            # StarVaultDimens
│       │   │   │   └── StarVaultTheme.kt    # CompositionLocal + Theme entry
│       │   │   ├── ui/
│       │   │   │   ├── login/
│       │   │   │   │   ├── LoginScreen.kt          # 纯 UI（state + lambdas）
│       │   │   │   │   ├── LoginRoute.kt           # 包 VM、订阅 state、注入回调
│       │   │   │   │   ├── LoginViewModel.kt
│       │   │   │   │   ├── LoginUiState.kt
│       │   │   │   │   └── LoginScreenPreview.kt
│       │   │   │   ├── home/        (Screen + Route + ViewModel + UiState + Preview)
│       │   │   │   ├── player/      (同)
│       │   │   │   ├── share/       (同)
│       │   │   │   ├── transfers/   (同)
│       │   │   │   ├── profile/     (同)
│       │   │   │   ├── files/       (同)
│       │   │   │   ├── album/       (同)
│       │   │   │   └── wallpaper/   (同)
│       │   │   ├── component/                # 跨屏公用组件
│       │   │   │   ├── TopBar.kt
│       │   │   │   ├── BottomNavBar.kt
│       │   │   │   ├── FileRow.kt
│       │   │   │   ├── Chip.kt
│       │   │   │   ├── StatusBarStyle.kt
│       │   │   │   └── ...
│       │   │   ├── icons/                    # ImageVector（SVG 批量转）
│       │   │   └── data/
│       │   │       ├── model/                # FileItem, Transfer, User, ...
│       │   │       └── FixtureLoader.kt
│       │   ├── assets/
│       │   │   ├── fixtures/                 # JSON 样本
│       │   │   └── sample/                   # 占位图（~5MB）
│       │   └── res/
│       │       ├── font/inter_*.ttf
│       │       ├── values/strings.xml
│       │       ├── values/colors.xml         # 仅必要的系统色
│       │       ├── drawable/                 # Splash 占位 / launcher 适配
│       │       └── mipmap-*/ic_launcher.*
│       └── test/
│           └── kotlin/com/starvault/screenshot/
│               ├── LoginScreenshotTest.kt
│               ├── HomeScreenshotTest.kt
│               ├── PlayerScreenshotTest.kt
│               ├── ShareScreenshotTest.kt
│               ├── TransfersScreenshotTest.kt
│               ├── ProfileScreenshotTest.kt
│               ├── FilesScreenshotTest.kt
│               ├── AlbumScreenshotTest.kt
│               └── WallpaperScreenshotTest.kt
├── gradle/libs.versions.toml                # Version Catalog
├── scripts/
│   └── capture-design-baseline.sh           # Playwright 截设计稿 baseline
├── .gitattributes                           # Paparazzi golden → LFS
├── .gitignore
├── build.gradle.kts
└── settings.gradle.kts
```

### 4.1 关键依赖（libs.versions.toml 摘录）

> ⚠️ 下列版本为撰写时的参考，**实施前应核对并升级到当时最新的稳定版**（Compose BOM、AGP、Kotlin、Nav 等都在快速迭代）。

```toml
[versions]
agp = "9.1.0"
kotlin = "2.3.21"            # 同时被 compose-compiler / kotlin-serialization 引用
compose-bom = "2026.05.00"
nav = "2.8.5"
activity-compose = "1.13.0"
lifecycle = "2.10.0"
kotlinx-serialization = "1.7.3"
paparazzi = "2.0.0-alpha04"   # Gradle 9 兼容起点
junit = "4.13.2"
turbine = "1.2.0"

[libraries]
androidx-activity-compose      = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
compose-bom                    = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui                     = { module = "androidx.compose.ui:ui" }
compose-ui-tooling-preview     = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling             = { module = "androidx.compose.ui:ui-tooling" }
compose-foundation             = { module = "androidx.compose.foundation:foundation" }
compose-material3              = { module = "androidx.compose.material3:material3" }
compose-material-icons-core    = { module = "androidx.compose.material:material-icons-core" }
nav-compose                    = { module = "androidx.navigation:navigation-compose", version.ref = "nav" }
lifecycle-viewmodel-compose    = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose      = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
kotlinx-serialization-json     = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
paparazzi                      = { module = "app.cash.paparazzi:paparazzi", version.ref = "paparazzi" }
junit                          = { module = "junit:junit", version.ref = "junit" }
turbine                        = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application            = { id = "com.android.application", version.ref = "agp" }
kotlin-android                 = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
# Compose Compiler 插件：版本必须与 Kotlin 对齐（同一 version.ref = "kotlin"）
compose-compiler               = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
# kotlinx.serialization 插件：生成 @Serializable 类的 serializer，类型安全路由的编译期前提
kotlin-serialization           = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

**插件对齐铁律**：
- `compose-compiler` 与 `kotlin-android` 必须**同一 Kotlin 版本**（都用 `version.ref = "kotlin"`），跨版本会编译失败
- `kotlin-serialization` 同样跟 Kotlin 版本对齐
- 三个 plugin 都在根 `build.gradle.kts` 用 `apply false` 声明；`:app` 模块 `apply` 进 plugins 块

---

## 5. Theme 与设计令牌

完整设计令牌系统，1:1 映射设计稿 `:root` CSS 变量。

### 5.1 Colors

```kotlin
@Immutable
data class StarVaultColors(
    val bg:         Color = Color(0xFFFAFAFA),  // --bg
    val surface:    Color = Color(0xFFFFFFFF),  // --surface
    val fg:         Color = Color(0xFF111111),  // --fg
    val muted:      Color = Color(0xFF6B6B6B),  // --muted
    val border:     Color = Color(0xFFE5E5E5),  // --border
    val accent:     Color = Color(0xFF2F6FEB),  // --accent
    val accentOn:   Color = Color(0xFFFFFFFF),  // --accent-on（accent 上的文字色）
    val accentSoft: Color = Color(0x142F6FEB),  // --accent-soft  rgba(47,111,235,0.08) → 0x14 = 20/255
    val tag1:       Color = Color(0xFF2F6FEB),  // --tag-1 蓝
    val tag2:       Color = Color(0xFF9333EA),  // --tag-2 紫
    val tag3:       Color = Color(0xFFEA580C),  // --tag-3 橙
    val tag4:       Color = Color(0xFF16A34A),  // --tag-4 绿
    val tag5:       Color = Color(0xFFDB2777),  // --tag-5 粉
    val success:    Color = Color(0xFF17A34A),  // --success
    val warn:       Color = Color(0xFFEAB308),  // --warn
    val danger:     Color = Color(0xFFDC2626),  // --danger
)
```

**主色取舍说明**：`design/index.html` 写的是 `#1F5BD0`，10 个 mockup 写的是 `#2F6FEB`。本设计取 `#2F6FEB`（mockup 票数占优，且 mockup 才是被还原对象）。`design/index.html` 与 mockup 的色差归为「设计稿内部不一致」，后续需要在设计源头对齐，本工程不调整。

### 5.2 Typography

```kotlin
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
    val large:    TextStyle = TextStyle(fontFamily=Inter, fontSize=22.sp, fontWeight=FontWeight.SemiBold),  // 数字/容量等大字号
    val subtitle: TextStyle = TextStyle(fontFamily=Inter, fontSize=16.sp, fontWeight=FontWeight.Medium),
    val body:     TextStyle = TextStyle(fontFamily=Inter, fontSize=14.sp),
    val caption:  TextStyle = TextStyle(fontFamily=Inter, fontSize=12.sp),
    val micro:    TextStyle = TextStyle(fontFamily=Inter, fontSize=10.5.sp, fontWeight=FontWeight.Medium, letterSpacing=0.2.sp),  // 4-tab 底栏
    val mono:     TextStyle = TextStyle(fontFamily=FontFamily.Monospace, fontSize=12.sp),
)
```

### 5.3 Shapes / Dimens

```kotlin
@Immutable data class StarVaultShapes(
    val xs:   Shape = RoundedCornerShape(3.dp),    // 小标签/内嵌元素
    val sm:   Shape = RoundedCornerShape(4.dp),    // 紧凑按钮
    val md:   Shape = RoundedCornerShape(9.dp),    // 中型 chip
    val lg:   Shape = RoundedCornerShape(12.dp),   // 卡片/输入框
    val xl:   Shape = RoundedCornerShape(13.dp),   // 大卡片
    val xxl:  Shape = RoundedCornerShape(28.dp),   // 模态底部 sheet
    val pill: Shape = RoundedCornerShape(999.dp),  // 头像/药丸按钮
)

@Immutable data class StarVaultDimens(
    val borderHairline: Dp = 1.dp,
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 12.dp,
    val spaceLg: Dp = 16.dp,
    val spaceXl: Dp = 24.dp,
    val bottomBarHeight: Dp = 64.dp,               // 4-tab 底栏高度（与 mockup .bottom-tab 对齐）
    val bottomBarBottomGap: Dp = 46.dp,            // 屏内容与底栏的间距（mockup 用 absolute 46px）
)
```

### 5.4 Theme 入口

```kotlin
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
                background  = StarVaultColors().bg,
                surface     = StarVaultColors().surface,
                onSurface   = StarVaultColors().fg,
                surfaceTint = Color.Transparent,  // 禁用 M3 tonal elevation 染色
            ),
            content = content
        )
    }
}
```

**为什么内包 MaterialTheme**：Compose Material 3 的 ripple / TextField / 部分基础组件依赖 LocalContentColor 等 MaterialTheme 提供的 CompositionLocal。直接裸用会出问题。包一层并把 colorScheme 全部染成我们的值，避免泄漏 M3 默认色。

---

## 6. 导航

### 6.1 路由

```kotlin
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

**路由参数语义**：
- `Player.fileId`：必须从一个具体文件进入，承载视频源信息
- `Share.fileId`：分享链接绑定一个文件
- `Files.folderId`：可选；null 表示根目录，复用 FilesScreen 渲染子目录

### 6.2 主结构：Scaffold + 4-tab 底栏

```kotlin
import androidx.navigation.NavDestination.Companion.hasRoute

@Composable
fun StarVaultApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination

    // 4 个底栏 tab 用 hasRoute<T>() 判定，避开 destination.route 字符串的 brittle 比对
    val showBottomBar = destination?.let { d ->
        d.hasRoute<Route.Home>()      ||
        d.hasRoute<Route.Album>()     ||
        d.hasRoute<Route.Transfers>() ||
        d.hasRoute<Route.Profile>()
    } ?: false

    Scaffold(
        bottomBar = { if (showBottomBar) BottomNavBar(nav, destination) },
        containerColor = StarVaultTheme.colors.bg,
    ) { padding ->
        StarVaultNavHost(
            navController = nav,
            modifier = Modifier.padding(padding),
        )
    }
}
```

**4 个 tab**：Home / Album / Transfers / Profile（依次对应设计稿 01/07/04/05）。
**全屏堆栈页**：Login（无底栏，且 popUpTo 清空回退栈进入 Home）、Player、Share、Files、Wallpaper。

**4-tab 底栏样式（直接对位 mockup CSS `.bottom-tab` / `.bt-item`）**：
- 位置：`position: absolute`（在 Scaffold 内 `bottomBar` 槽），底部留 `46.dp` 间隔（设备区与底栏之间）；高度 `64.dp`；白色背景；顶部 `1.dp` 实线 `colors.border`
- 单 tab：垂直 column，图标 `22x22`、文字 `typography.micro`（10.5.sp / Medium / letterSpacing 0.2）
- 颜色：未选中 `colors.muted`、悬停/按下 `colors.fg`、**选中 `colors.accent`**
- 选中态可加底部 2dp accent 短杠（视觉补完，spec 不强制）

### 6.3 NavHost

```kotlin
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

> NavHost 调用的是 `XxxRoute`（包 VM 的版本），不是 `XxxScreen`（纯 UI 版本）。详见 §7。

---

## 7. 状态层

### 7.1 Screen + Route 分层模式

**关键架构选择**：每屏拆为 `XxxScreen`（纯 UI，只接 state + lambdas）和 `XxxRoute`（包 VM、订阅 state、注入回调）。这样 Preview / Paparazzi 都不依赖 ViewModel，预览只需要传一个 UiState 即可。

```kotlin
sealed interface HomeUiState {
    data object Loading                              : HomeUiState
    data class  Ready(val files: List<FileItem>)    : HomeUiState
    data class  Error(val message: String)          : HomeUiState
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        runCatching {
            FixtureLoader.loadDelayed<List<FileItem>>(getApplication(), "fixtures/files.json")
        }.onSuccess  { _state.value = HomeUiState.Ready(it) }
         .onFailure  { _state.value = HomeUiState.Error(it.message ?: "unknown error") }
    }
}
```

### 7.2 Screen（纯 UI）

```kotlin
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenFile: (FileItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeUiState.Loading  -> HomeSkeleton(modifier)
        is HomeUiState.Ready -> HomeContent(state.files, onOpenFile, modifier)
        is HomeUiState.Error -> HomeError(state.message, onRetry, modifier)
    }
}
```

### 7.3 Route（VM + 导航绑定）

```kotlin
@Composable
fun HomeRoute(nav: NavHostController, vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    HomeScreen(
        state       = state,
        onOpenFile  = { nav.navigate(Route.Files(it.id)) },
        onRetry     = vm::load,
    )
}
```

NavHost（§6.3）中调用的是 `HomeRoute`，不是 `HomeScreen`。

### 7.4 状态保存
- ViewModel 跨配置变更（旋转）自动存活
- `rememberSaveable` 保存 UI 局部状态（滚动位置、当前 tab、文件夹路径栈）
- 进程被回收恢复时 ViewModel 重新 `load()`，从 JSON 重新读取

---

## 8. 数据层

### 8.1 数据模型

```kotlin
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

@Serializable enum class FileType  { FOLDER, VIDEO, IMAGE, DOC, AUDIO, OTHER }
@Serializable enum class TagColor  { TAG1, TAG2, TAG3, TAG4, TAG5 }

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

@Serializable
data class User(
    val nickname: String,
    val avatarUrl: String? = null,
    val vipLevel: Int,
    val vipExpiresAt: Long? = null,
    val totalBytes: Long,
    val usedBytes: Long,
)

@Serializable
data class AlbumPhoto(
    val id: String,
    val uri: String,
    val width: Int,
    val height: Int,
    val takenAt: Long,
    val isFavorite: Boolean = false,
)

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

@Serializable
data class ShareLink(
    val fileId: String,
    val url: String,
    val accessCode: String,
    val expiresAt: Long? = null,
)
```

### 8.2 Fixture 文件清单

| 文件 | 服务的屏 | 内容描述 |
|---|---|---|
| `assets/fixtures/files.json` | Home / Files | ~30 条混合类型条目，含 2~3 层文件夹 |
| `assets/fixtures/transfers.json` | Transfers | 8 条：4 上传 4 下载，覆盖 4 种状态 |
| `assets/fixtures/profile.json` | Profile | 1 个 User，容量数据 |
| `assets/fixtures/albums.json` | Album | 60 条 AlbumPhoto（masonry 测试需要数量） |
| `assets/fixtures/wallpapers.json` | Wallpaper | 20 条 Wallpaper + 5 个分类 |
| `assets/fixtures/wallpaper_config.json` | Wallpaper | 1 个 WallpaperConfig |
| `assets/fixtures/share_links.json` | Share | 3 条示例链接 |

### 8.3 占位图

- `assets/sample/*.jpg`：80 张约 60KB 平均的 JPG，预先下载到 assets/，运行时不联网
- 来源：picsum.photos 或类似图源，提前用 `scripts/fetch-sample-images.sh` 拉
- 预估打包体积：~5MB

### 8.4 FixtureLoader

```kotlin
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

**Loading 态可见性**：`loadDelayed` 在 debug 包注入 300ms 延迟，让 Loading / Skeleton 在开发期可见、被 Paparazzi 截图覆盖。Release 包延迟为 0。

---

## 9. 资源处理

### 9.1 图标

- 设计稿全部内联 SVG（估计 30~50 个），用 **Valkyrie IDE 插件** 批量转 ImageVector
- 转出物以 Kotlin 函数形式落到 `ui/icons/`（如 `Icons.Home`、`Icons.Player.Play`）
- 通用 Material 图标走 `androidx.compose.material:material-icons-core`（不引 extended，避免包体爆炸）

### 9.2 字体

- Inter 4 个字重（Regular / Medium / SemiBold / Bold），文件放 `res/font/inter_*.ttf`
- 在 `StarVaultTypography.Inter` 处一次声明，全应用复用
- 不用 Downloadable Fonts（避免首启依赖网络）

### 9.3 应用图标 / Splash

- 源图：`design/icon.png`（1.7MB）
- 用 Android Studio **Image Asset Studio** 生成 `mipmap-*/ic_launcher.*` 与 adaptive icon 前后景
- Splash Screen API（minSdk 34 原生支持）：纯色背景（`StarVaultColors.bg`）+ 居中 launcher icon

---

## 10. 视觉验证（Paparazzi）

### 10.1 设备配置

```kotlin
val PHONE_412_900 = DeviceConfig.PIXEL_5.copy(
    screenWidth  = 412,
    screenHeight = 900,
    xdpi = 440, ydpi = 440,
    softButtons = false,
)
```

### 10.2 测试模式

利用 §7 的 Screen + Route 分层，截图测试直接调用纯 `HomeScreen(state = ...)`，**完全不依赖 ViewModel / Application / assets**。

```kotlin
class HomeScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig  = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi  = false,
    )

    @Test fun home_ready() = paparazzi.snapshot {
        StarVaultTheme {
            HomeScreen(
                state      = HomeUiState.Ready(FixturePresets.homeFiles()),
                onOpenFile = {},
                onRetry    = {},
            )
        }
    }

    @Test fun home_loading() = paparazzi.snapshot {
        StarVaultTheme { HomeScreen(HomeUiState.Loading, {}, {}) }
    }

    @Test fun home_error() = paparazzi.snapshot {
        StarVaultTheme { HomeScreen(HomeUiState.Error("network unreachable"), {}, {}) }
    }
}
```

`FixturePresets` 放在 `app/src/test/.../fixtures/FixturePresets.kt`：测试期硬编码的样本对象（直接 `listOf(FileItem(...), ...)`），不读 assets，让截图测试与 runtime 数据完全解耦。

### 10.3 Baseline 与 Diff 流程

1. **设计稿 baseline**：`scripts/capture-design-baseline.sh` 用 Playwright 把 `design/*.html` 渲染成 412×900 PNG，存到 `design-baseline/`
2. **Compose golden**：Paparazzi 生成的截图存 `app/src/test/snapshots/images/`（进 git LFS）
3. **差异判定**：CI 步骤跑 `verifyPaparazziDebug`，golden 与上一次 commit 的对比；视觉差异阈值 **5%**
4. **设计稿 vs 实现**：人工或脚本比对 `design-baseline/` 与 Paparazzi golden，差异超过 5% 视为不达标，需要调整 Compose 实现

### 10.4 Git LFS 配置

`.gitattributes`：
```
app/src/test/snapshots/**/*.png filter=lfs diff=lfs merge=lfs -text
design-baseline/**/*.png        filter=lfs diff=lfs merge=lfs -text
```

---

## 11. 系统集成

### 11.1 Edge-to-edge

```xml
<!-- AndroidManifest.xml —— Activity 入口 -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.StarVault"
    android:windowSoftInputMode="adjustResize">  <!-- 必备：edge-to-edge 下让 IME inset 正确派发到 Compose -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Android 15 起强制；老版本自动适配透明 / 半透明系统栏
        setContent {
            StarVaultTheme { StarVaultApp() }
        }
    }
}
```

**关键配置说明**：
- `windowSoftInputMode="adjustResize"`：**不可省略**。edge-to-edge 下软键盘弹出时，必须靠这个属性把 IME inset 派发到 Compose 的 `WindowInsets.ime`，否则输入框会被键盘遮住
- `enableEdgeToEdge()` 默认把 status/nav bar 设为透明；3-button 模式下 nav bar 走半透明 scrim；如要 nav bar 也全透明，加 `window.isNavigationBarContrastEnforced = false`
- minSdk 34（Android 14）走的是 `WindowCompat.enableEdgeToEdge(window)` 兼容路径，无需 `Build.VERSION.SDK_INT` 分支

### 11.2 状态栏样式

```kotlin
@Composable
fun StatusBarStyle(darkIcons: Boolean) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}
```

- 其他 8 屏：`StatusBarStyle(darkIcons = true)`（浅底深字）
- PlayerScreen：`StatusBarStyle(darkIcons = false)`（深底浅字）

### 11.3 安全区（Insets）

- 屏幕级用 `Scaffold` 自动处理 statusBars + navigationBars
- 局部需要自定义时用 `Modifier.windowInsetsPadding(WindowInsets.systemBars)` / `WindowInsets.ime`

---

## 12. 构建配置

```kotlin
// 根 build.gradle.kts —— 声明所有 plugin（apply false），子模块按需 apply
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.compose.compiler)     apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

```kotlin
// :app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)      // Kotlin 2.0+ 必备；让 @Composable 走新编译器
    alias(libs.plugins.kotlin.serialization)  // @Serializable 类的 serializer 生成器；类型安全路由的编译期前提
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
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Paparazzi 2.0.0-alpha04 + Gradle 9 兼容性：必须禁用 HTML test reports
// （Paparazzi 用了 Gradle 内部 API，HTML 报告依赖会破坏）
tasks.withType<Test>().configureEach {
    reports.html.required = false
}
```

**plugin 应用铁律**：
- 4 个 plugin 都得加在 `:app` 模块（`compose-compiler` / `kotlin-serialization` 不能只在根 `apply false`）
- `compose-compiler` 的 version 必须 = Kotlin version（已在 `libs.versions.toml` 用同一 `version.ref` 强制对齐）
- `kotlin-serialization` 同样对齐 Kotlin version
- Gradle 9 + Paparazzi 2.0.0-alpha04 当前已知唯一 workaround：禁用 HTML 报告（见 tasks.withType 块）

---

## 13. 国际化

Phase 1 仅简体中文。所有文案集中在 `res/values/strings.xml`，不建 `values-en/`。后续若加英文，由 ViewModel 提供的 String 也要走 `stringResource()`，避免硬编码。

---

## 14. 测试金字塔

| 层级 | 范围 | 工具 |
|---|---|---|
| **截图回归** | 9 屏 × ≥1 态 = ≥9 个 golden | Paparazzi + JUnit 4 |
| **单元测试** | FixtureLoader 反序列化、ViewModel 状态机 | JUnit 4 + Turbine |
| **手动验证** | 真机/模拟器跑一遍 + 与设计稿浏览器并排对照 | 肉眼 |

UI 交互测试（Espresso / Compose UI Test）**Phase 1 不做** —— 屏内交互极少（多数是静态展示）、ROI 低。

---

## 15. 错误处理

| 场景 | 处理 |
|---|---|
| JSON 解析失败 | catch → `XxxUiState.Error(message)` → 屏显错误内容 + 重试按钮 |
| assets 文件缺失 | 不静默；抛 IOException，Logcat 一眼可见（开发期失误必须显形）|
| 字段缺失 / 类型错 | `coerceInputValues = true` 容错，未知字段忽略，避免格式微调即崩 |
| 导航参数缺失 | 由 kotlinx.serialization 路由层保证类型；缺失字段编译期即报错 |
| 字体缺失 | Compose 自动降级到 system default，应用不崩 |
| 占位图缺失 | 显示占位色块（StarVaultColors.muted alpha 30%）|

Phase 1 不引入全局异常上报（Crashlytics 等）。Logcat 已经够用。

---

## 16. M1 完成定义（Definition of Done）

- [ ] `./gradlew installDebug` 在 Android 14 真机 / 模拟器上能装能开
- [ ] Login → Home 流程通；Home/Album/Transfers/Profile 4 tab 切换正常
- [ ] Home → Files 进入文件浏览；Files → Player / Share 可达；Profile → Wallpaper 可达
- [ ] Paparazzi golden 9 张全部生成，与 design-baseline 视觉差异 < 5%
- [ ] Edge-to-edge 正确，PlayerScreen 状态栏自动转浅字
- [ ] 应用图标 / Splash Screen 配置完成
- [ ] 每屏的 Screen + Route 分层完成（Screen 收纯状态，Route 包 VM）
- [ ] README 写明：环境要求（JDK 21+、Gradle 9.x、Android Studio Ladybug+）、运行步骤、Paparazzi 命令、Git LFS 安装提示

---

## 17. Phase 2+ 规划（仅提纲，本次不实施）

| 阶段 | 主题 | 关键产物 |
|---|---|---|
| Phase 2 | 数据层抽象 | Repository 接口 + Fake 实现保持，新增 Real115Repository（决定走 p115client / 自研逆向 / Alist 代理 之一） |
| Phase 3 | 登录与会话 | 扫码登录联调，Token / Cookie 持久化（EncryptedSharedPreferences）|
| Phase 4 | 真实文件浏览 | 文件树、缩略图、缓存（Coil 网络图片加载） |
| Phase 5 | 下载 / 上传 | WorkManager 任务、断点续传、Transfers 屏接真实任务状态 |
| Phase 6 | 播放器 / 分享 | ExoPlayer 接入；分享链接生成 |
| Phase 7 | 相册 / 壁纸 | 媒体扫描、壁纸引擎落地（系统壁纸 API）|

---

## 18. 待定项与风险

| # | 项 | 状态 | 风险 |
|---|---|---|---|
| 1 | 主色 `#2F6FEB` vs `#1F5BD0` | 已决（取 #2F6FEB） | 设计源头 index.html 仍是 #1F5BD0，需要在设计稿那边对齐 |
| 2 | Inter 字体打包 ~600KB | 已决 | 包体增加，但避免运行时依赖 |
| 3 | 占位图打包 ~5MB | 已决 | 包体增加；如以后真接 API，这部分可删 |
| 4 | 4-tab 底栏是对设计稿的补完 | 已决（用户确认） | 设计稿没明确画 tab，实现是合理推断 |
| 5 | 项目根目录无 git 仓库 | 未决 | 需要在落 spec 后初始化 git，并确认 `.gitignore` / `.gitattributes` 配置 |
| 6 | Paparazzi 与设计稿渲染引擎不同 | 风险 | 字体抗锯齿 / 子像素渲染差异可能让 5% 阈值偏紧，必要时放宽到 8% |
| 7 | minSdk 34 限制用户群 | 已决 | 自用场景可接受 |
| 8 | `kotlin.plugin.serialization` 必须 apply | 已决 | 没它 `@Serializable` 不生成 serializer，类型安全路由整个挂 |
| 9 | `android:windowSoftInputMode="adjustResize"` 必须配 | 已决 | edge-to-edge 下缺这个属性，软键盘 inset 不派发，输入框被键盘遮住 |
| 10 | `kotlin.plugin.compose` 必须与 Kotlin 同版本 | 已决 | `libs.versions.toml` 用同一 `version.ref = "kotlin"` 强制对齐，跨版本编译失败 |
| 11 | Gradle 9 + Paparazzi 2.0.0-alpha04 兼容性 | 已知 | 必须 `tasks.withType<Test> { reports.html.required = false }`，否则 HTML 报告依赖 Gradle 内部 API 报错 |
| 12 | 未来可迁官方「Compose Preview Screenshot Testing」 | 候选 | 当前 alpha15 实验性；现用 Paparazzi，迁官方后可省工具链、得 IDE 集成 |

---

## 19. 关联文档

- 设计稿：`design/index.html` 起跳
- 实现计划（下一步产物）：本 spec 通过复核后由 `superpowers:writing-plans` 生成

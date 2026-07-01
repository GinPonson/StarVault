package com.starvault.component

import androidx.compose.material.icons.Icons as MaterialIcons
// Outlined icons (绝大多数)
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
// AutoMirrored
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
// Filled（实心 icon + 文件类型缩略图）
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.starvault.component.icons.*

/**
 * StarVault icon library — 两套图标源,按视觉权重分工:
 *
 *  • **Solar Bold (41 个)**:BottomNav 4 + File thumbs 8 + AppBar 8 + Player 11 +
 *    Files toolbar 10 — App 内视觉权重最高 / 最显眼的 icon 区域,统一走 Solar Bold
 *    Solid Fill 系列 (480 Design, CC BY 4.0),通过 s2c 从官方 SVG 生成到
 *    `com.starvault.component.icons.Solar*`。跨屏视觉一致。
 *
 *  • **Material Icons Extended (17 个)**:Profile 行 / Transfers / Login /
 *    Home Quick/Section/Album / WallpaperCard — 次要 icon 维持 Material Outlined
 *    (Apache 2.0),减少 s2c 生成工作量。
 *
 * Material 变体约定(只对 46 个 Material icon 适用):
 *  - `Outlined`              — 描边 icon(绝大多数导航/操作类)
 *  - `Filled`                — 实心 icon(Play / Pause / Star / Favorite / CheckCircle)
 *  - `AutoMirrored.Outlined` — 方向敏感 icon(Back / Help,RTL 自动镜像)
 *
 * 文件类型缩略图(**浅底背景 + 深彩色 icon**):Solar Bold 变体,见底部 [File thumbs] 分组。
 * 工具栏 / 透明背景 icon:Material Outlined 变体,视觉重量匹配透明背景。
 */
object Icons {

    /* ─────────────────── BottomNav (4, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0): Home / Folder / Gallery / User.
    // 与 File thumbs 8 个 icon 统一视觉语言 (Solid fill, 圆润几何). Files / Album
    // 复用现有 SolarFolder / SolarGallery (与 file thumb 同 shape, 靠尺寸/底色区分).
    val Home: ImageVector    get() = SolarHome
    val Files: ImageVector   get() = SolarFolder
    val Album: ImageVector   get() = SolarGallery
    val Profile: ImageVector get() = SolarUser

    /* ─────────────────── Common AppBar (8, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0) — 跨屏可见度最高的工具栏 icon.
    // 与 BottomNav + File thumbs 共 20 个 Solar Bold 统一视觉语言 (Solid fill, 圆润几何).
    //
    // Back 特殊性:Solar Bold 无 AutoMirrored 概念 (Alt Arrow Left 是固定朝向左),
    // call site 需在 RTL locale 下用 `Modifier.graphicsLayer(scaleX = -1f)` 镜像。
    val Scan: ImageVector     get() = SolarScan
    val Bell: ImageVector     get() = SolarBell
    val More: ImageVector     get() = SolarMore
    val Search: ImageVector   get() = SolarSearch
    val Settings: ImageVector get() = SolarSettings
    val Wallet: ImageVector   get() = SolarWallet
    val Close: ImageVector    get() = SolarClose
    val Back: ImageVector     get() = SolarBack

    /* ─────────────────── Player (11, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0). Filled→Bold weight
    // upgrade (Play / Pause / Star / Favorites):视觉重量比 Material Filled 重
    // ~5–10%,应用 site 可通过 `Icons.Play` 24dp + tint 控制视觉比例;若觉过重
    // 可改为 Linear 变体(同一 SVG 在 Linear/ 仓库)。
    val Subtitle: ImageVector     get() = SolarSubtitle
    val Cast: ImageVector         get() = SolarCast
    val Clock: ImageVector        get() = SolarClock
    val DownloadInto: ImageVector get() = SolarDownloadInto
    val Fullscreen: ImageVector   get() = SolarFullscreen
    val FullscreenExit: ImageVector get() = SolarFullscreenExit
    val Prev: ImageVector         get() = SolarPrev
    val Next: ImageVector         get() = SolarNext
    val Play: ImageVector         get() = SolarPlay
    val Pause: ImageVector        get() = SolarPause
    /** 播放列表(≡+♪);Audio 屏"文件列表"按钮。 */
    val Playlist: ImageVector     get() = SolarPlaylist

    /* ─────────────────── Transfers / common (5) ─────────────────── */
    val Minimize: ImageVector get() = MaterialIcons.Outlined.Remove
    val Refresh: ImageVector  get() = MaterialIcons.Outlined.Refresh
    val Cancel: ImageVector   get() = MaterialIcons.Outlined.Close
    val Trash: ImageVector    get() = MaterialIcons.Outlined.Delete
    val Retry: ImageVector    get() = MaterialIcons.Outlined.Refresh

    /* ─────────────────── Files toolbar (10, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0) — Files 屏工具栏 toggle /
    // 行操作。GridView → Solar Gallery (4 缩略图网格),BrokenImage → Solar Gallery Remove
    // (图库 + 减号,表示加载失败) — Solar 无 "view-module 3×3 grid" / "broken-image"
    // 概念,这 2 个 MEDIUM confidence;其余 icon 语义 1:1 还原。
    val Transfers: ImageVector   get() = SolarTransfers
    val ListView: ImageVector    get() = SolarListView
    val GridView: ImageVector    get() = SolarGridView
    val ChevronDown: ImageVector get() = SolarChevronDown
    val ChevronRight: ImageVector get() = SolarChevronRight
    val Download: ImageVector    get() = SolarDownload
    val Share: ImageVector       get() = SolarShare
    val Move: ImageVector        get() = SolarMove
    val Rename: ImageVector      get() = SolarRename
    val BrokenImage: ImageVector get() = SolarBrokenImage

    /* ─────────────────── Profile rows (6) ─────────────────── */
    val ShareOut: ImageVector   get() = MaterialIcons.Outlined.IosShare
    val ShareAlt: ImageVector   get() = MaterialIcons.Outlined.Share
    val Device: ImageVector     get() = MaterialIcons.Outlined.Devices
    val Privacy: ImageVector    get() = MaterialIcons.Outlined.PrivacyTip
    val Appearance: ImageVector get() = MaterialIcons.Outlined.Palette
    val Help: ImageVector       get() = MaterialIcons.AutoMirrored.Outlined.HelpOutline

    /* ─────────────────── Home Quick (3) ─────────────────── */
    val Favorites: ImageVector get() = MaterialIcons.Filled.Star
    val Upload: ImageVector    get() = MaterialIcons.Outlined.FileUpload
    val Recycle: ImageVector   get() = MaterialIcons.Outlined.RestoreFromTrash

    /* ─────────────────── Home section (2) ─────────────────── */
    val Check: ImageVector get() = MaterialIcons.Outlined.Check
    val Plus: ImageVector  get() = MaterialIcons.Outlined.Add

    /* ─────────────────── Album (3) ─────────────────── */
    val Camera: ImageVector    get() = MaterialIcons.Outlined.CameraAlt
    val PlaySmall: ImageVector get() = MaterialIcons.Filled.PlayArrow
    val Heart: ImageVector     get() = MaterialIcons.Filled.Favorite
    /** 实心 ❤️；Preview 屏 star toggle 用,区别于 Icons.Heart(相册喜欢)。 */
    val HeartFilled: ImageVector  get() = MaterialIcons.Filled.Favorite
    /** 空心 ♡；Preview 屏 star 未收藏态。 */
    val HeartOutline: ImageVector get() = MaterialIcons.Outlined.FavoriteBorder

    /* ─────────────────── Login (2) ─────────────────── */
    val CheckBold: ImageVector  get() = MaterialIcons.Filled.CheckCircle
    val RefreshAlt: ImageVector get() = MaterialIcons.Outlined.Refresh

    /* ─────────────────── WallpaperCard (1) ─────────────────── */
    val Storage: ImageVector get() = MaterialIcons.Outlined.Wallpaper

    /* ─────────────────── File thumbs (8, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0) — 24x24 flat-fill 单色 icon.
    // 视觉用法:浅色背景(#F4F4F5 / #EFF6FF / … Tailwind 50 系)+ 深彩色 icon(Tailwind 600 系)
    // + 24dp 居中,见 FileRow.FileThumb / FilesScreen.FileThumb。
    // 选 Solar Bold 而非 Phosphor Fill:FOLDER/AUDIO/VIDEO 在 24dp 下 Solar 几何比例更饱满、
    // 拐角更圆润,与 iOS Files / OneDrive / Dropbox 的浅底风格契合。
    val Folder: ImageVector    get() = SolarFolder
    val NewFolder: ImageVector get() = SolarFolderAdd
    val Image: ImageVector     get() = SolarGallery
    val Music: ImageVector     get() = SolarMusicNotes
    val Doc: ImageVector       get() = SolarDocumentText
    val Archive: ImageVector   get() = SolarZipFile
    val Video: ImageVector     get() = SolarClapperboardPlay
    /** 通用文件 icon(无 URL / FileType.OTHER 兜底),Solar Bold "File" — 带折角的纸。 */
    val GenericFile: ImageVector get() = SolarFile
}

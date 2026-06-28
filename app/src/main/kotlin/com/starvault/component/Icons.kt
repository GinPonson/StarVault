package com.starvault.component

import androidx.compose.material.icons.Icons as MaterialIcons
// Outlined icons (绝大多数)
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Wallpaper
// AutoMirrored
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
// Filled（实心 icon + 文件类型缩略图）
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder as FolderFilled
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image as ImageFilled
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * StarVault icon library — 全部使用 Material Icons Extended。
 *
 * 没有任何手写 SVG 路径：所有 52 个 icon 都映射到 Material 的 7000+ icon 之一。
 * 增删 icon 只需在对应 `MaterialIcons.<变体>.<name>` 位置加一行即可。
 *
 * Material 变体约定：
 *  - `Outlined`              — 描边 icon（绝大多数导航/操作类）
 *  - `Filled`                — 实心 icon（Play / Pause / Star / Favorite / CheckCircle +
 *                              文件类型缩略图 Folder / Image / MusicNote / Description / FolderZip / PlayCircle）
 *  - `AutoMirrored.Outlined` — 方向敏感 icon（Back / Help，RTL 自动镜像）
 *
 * 文件类型缩略图（带色块背景 + 白色实心 icon）：用 Filled 变体，视觉重量跟带色背景匹配。
 * 工具栏 / 透明背景 icon：用 Outlined 变体，视觉重量匹配透明背景。
 */
object Icons {

    /* ─────────────────── BottomNav (4) ─────────────────── */
    val Home: ImageVector    get() = MaterialIcons.Outlined.Home
    val Files: ImageVector   get() = MaterialIcons.Outlined.Folder
    val Album: ImageVector   get() = MaterialIcons.Outlined.PhotoLibrary
    val Profile: ImageVector get() = MaterialIcons.Outlined.Person

    /* ─────────────────── Common AppBar (8) ─────────────────── */
    val Scan: ImageVector     get() = MaterialIcons.Outlined.QrCodeScanner
    val Bell: ImageVector     get() = MaterialIcons.Outlined.Notifications
    val More: ImageVector     get() = MaterialIcons.Outlined.MoreHoriz
    val Search: ImageVector   get() = MaterialIcons.Outlined.Search
    val Settings: ImageVector get() = MaterialIcons.Outlined.Settings
    val Wallet: ImageVector   get() = MaterialIcons.Outlined.AccountBalanceWallet
    val Close: ImageVector    get() = MaterialIcons.Outlined.Close
    val Back: ImageVector     get() = MaterialIcons.AutoMirrored.Outlined.ArrowBack

    /* ─────────────────── Player (10) ─────────────────── */
    val Subtitle: ImageVector     get() = MaterialIcons.Outlined.ClosedCaption
    val Cast: ImageVector         get() = MaterialIcons.Outlined.Cast
    val Clock: ImageVector        get() = MaterialIcons.Outlined.Schedule
    val DownloadInto: ImageVector get() = MaterialIcons.Outlined.Download
    val Fullscreen: ImageVector   get() = MaterialIcons.Outlined.Fullscreen
    val FullscreenExit: ImageVector get() = MaterialIcons.Outlined.FullscreenExit
    val Prev: ImageVector         get() = MaterialIcons.Outlined.SkipPrevious
    val Next: ImageVector         get() = MaterialIcons.Outlined.SkipNext
    val Play: ImageVector         get() = MaterialIcons.Filled.PlayArrow
    val Pause: ImageVector        get() = MaterialIcons.Filled.Pause
    /** 播放列表(≡+♪);Audio 屏"文件列表"按钮。 */
    val Playlist: ImageVector     get() = MaterialIcons.Outlined.QueueMusic

    /* ─────────────────── Transfers / common (5) ─────────────────── */
    val Minimize: ImageVector get() = MaterialIcons.Outlined.Remove
    val Refresh: ImageVector  get() = MaterialIcons.Outlined.Refresh
    val Cancel: ImageVector   get() = MaterialIcons.Outlined.Close
    val Trash: ImageVector    get() = MaterialIcons.Outlined.Delete
    val Retry: ImageVector    get() = MaterialIcons.Outlined.Refresh

    /* ─────────────────── Files toolbar (9) ─────────────────── */
    val Transfers: ImageVector   get() = MaterialIcons.Outlined.SwapVert
    val ListView: ImageVector    get() = MaterialIcons.Outlined.ViewList
    val GridView: ImageVector    get() = MaterialIcons.Outlined.ViewModule
    val ChevronDown: ImageVector get() = MaterialIcons.Outlined.KeyboardArrowDown
    val ChevronRight: ImageVector get() = MaterialIcons.Outlined.ChevronRight
    val Download: ImageVector    get() = MaterialIcons.Outlined.Download
    val Share: ImageVector       get() = MaterialIcons.Outlined.Share
    val Move: ImageVector        get() = MaterialIcons.Outlined.DriveFileMove
    val Rename: ImageVector      get() = MaterialIcons.Outlined.Edit
    val BrokenImage: ImageVector get() = MaterialIcons.Outlined.ImageNotSupported

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

    /* ─────────────────── File thumbs (7, Material Filled) ─────────────────── */
    // 全部用 Filled 变体以匹配 FileRow 32dp 色块背景 + 居中白色 icon 的视觉重量
    val Folder: ImageVector    get() = MaterialIcons.Filled.FolderFilled
    val NewFolder: ImageVector get() = MaterialIcons.Outlined.CreateNewFolder
    val Image: ImageVector     get() = MaterialIcons.Filled.ImageFilled
    val Music: ImageVector     get() = MaterialIcons.Filled.MusicNote
    val Doc: ImageVector       get() = MaterialIcons.Filled.Description
    val Archive: ImageVector   get() = MaterialIcons.Filled.FolderZip
    val Video: ImageVector     get() = MaterialIcons.Filled.PlayCircle
}

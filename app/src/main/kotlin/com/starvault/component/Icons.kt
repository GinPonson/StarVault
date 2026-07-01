package com.starvault.component

import androidx.compose.ui.graphics.vector.ImageVector
import com.starvault.component.icons.*

/**
 * StarVault icon library — 两套图标源,按视觉权重分工:
 *
 *  • **Solar Bold (61 个)**:BottomNav 4 + File thumbs 8 + AppBar 8 + Player 11 +
 *    Files toolbar 10 + Profile 5 + Home Quick/Section/Album 9 + Transfers/Login/
 *    WallpaperCard 6 — App 内视觉权重最高 / 最显眼的 icon 区域,统一走 Solar Bold
 *    Solid Fill 系列 (480 Design, CC BY 4.0),通过 s2c 从官方 SVG 生成到
 *    `com.starvault.component.icons.Solar*`。跨屏视觉一致。
 *
 *  • **Solar Linear (1 个)**:Heart 三态(Heart / HeartFilled / HeartOutline 都指向
 *    `SolarHeart.kt`,Solar Bold 无 outline heart 形态,用 Linear 描边 + tint 切换
 *    实心/未填)。这是本仓库目前唯一的 Linear icon。
 *
 *  • **Material Icons Extended (0 个)**:**全部 53 个 wrapper 已迁移**。仅留
 *    `androidx.compose.material.icons.filled.PlayArrow` 静态 import 给 Album
 *    PlaySmall 提供 fallback(view 已切到 SolarPlay 但 import 暂留,后续清理)。
 *
 * Material 变体约定(只对 12 个 Material icon 适用):
 *  - `Outlined`              — 描边 icon(绝大多数导航/操作类)
 *  - `Filled`                — 实心 icon(Play / Pause / Star / Favorite / CheckCircle)
 *  - `AutoMirrored.Outlined` — 已废弃:Back / Help 都已迁移到 Solar Bold,Solar 无
 *    RTL 自动镜像,RTL 翻转为后续 task。
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

    /* ─────────────────── Transfers / common (5, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0). Cancel 复用 SolarClose (Batch 1);
    // Retry 用 SolarRecycle (Batch 5 的 Solar Restart 同样适用 retry 语义)。
    val Minimize: ImageVector get() = SolarMinimize
    val Refresh: ImageVector  get() = SolarRefresh
    val Cancel: ImageVector   get() = SolarClose
    val Trash: ImageVector    get() = SolarTrash
    val Retry: ImageVector    get() = SolarRecycle

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

    /* ─────────────────── Profile rows (5, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0). ShareOut →
    // Solar Export (Solar 无 iOS up-arrow,用箭头出框表示 share-out),MEDIUM confidence;
    // 其它 icon 语义 1:1 还原。
    // 备注:Batch 4 前存在 ShareAlt wrapper,grep 后 0 production caller,已随
    // 本次 swap 一并删除。
    val ShareOut: ImageVector   get() = SolarShareOut
    val Device: ImageVector     get() = SolarDevice
    val Privacy: ImageVector    get() = SolarPrivacy
    val Appearance: ImageVector get() = SolarAppearance
    val Help: ImageVector       get() = SolarHelp

    /* ─────────────────── Home Quick (3, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0). Recycle →
    // Solar Restart (Solar 无 "restore from trash" 概念,用循环箭头表示重做),
    // MEDIUM confidence;其它 icon 语义 1:1 还原。
    val Favorites: ImageVector get() = SolarFavorites
    val Upload: ImageVector    get() = SolarUpload
    val Recycle: ImageVector   get() = SolarRecycle

    /* ─────────────────── Home section (2, Solar Bold) ─────────────────── */
    val Check: ImageVector get() = SolarCheck
    val Plus: ImageVector  get() = SolarPlus

    /* ─────────────────── Album (4, Solar Bold + Linear) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0). PlaySmall 复用
    // SolarPlay (Batch 2)。三个 heart (Heart / HeartFilled / HeartOutline) 全用
    // **Solar Linear** Heart (Solar Bold 没有 outline heart 形态),call site 用
    // tint 切换填充 ↔ 未填。
    val Camera: ImageVector    get() = SolarCamera
    val PlaySmall: ImageVector get() = SolarPlay
    /** 相册喜欢(实心);Preview 屏 HeartFilled 的旧语义并入这里。 */
    val Heart: ImageVector     get() = SolarHeart
    /** 同 [Heart];为兼容 Preview 屏 toggle 接口保留旧名字(都指向 Solar Heart)。 */
    val HeartFilled: ImageVector  get() = SolarHeart
    /** 同 [Heart](Solar Linear);call site 用 `tint = c.muted` 区分未填态。 */
    val HeartOutline: ImageVector get() = SolarHeart

    /* ─────────────────── Login (2, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0). RefreshAlt → Solar
    // Refresh Circle (与 Refresh 同 Refresh 圆形版,Login 错误态专用)。
    val CheckBold: ImageVector  get() = SolarCheckBold
    val RefreshAlt: ImageVector get() = SolarRefreshAlt

    /* ─────────────────── WallpaperCard (1, Solar Bold) ─────────────────── */
    // Solar Bold from 480-Design/Solar-Icon-Set (CC BY 4.0)。原 Material Outlined.Wallpaper
    // 误用作存储卡 icon,Solar 仓库恰好有 Wallpaper.svg,语义比 Material 的更准确
    // (Material 的 Outlined.Wallpaper 实际是纹理填充样式,Solar 是真正的壁纸/显示器概念)。
    val Storage: ImageVector get() = SolarStorage

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

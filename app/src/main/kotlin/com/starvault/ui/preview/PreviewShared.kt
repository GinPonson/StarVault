package com.starvault.ui.preview

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.starvault.component.Icons
import com.starvault.data.repository.MediaMetadata
import com.starvault.data.repository.VideoM3u8
import com.starvault.theme.StarVaultTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Preview 屏的 UI 状态机(IMAGE / VIDEO 共用同一套 sealed)。
 *
 *  两态:
 *  - Loading : 初始 / 拉 metadata 或 url 中
 *  - Success : 拿到可播放/可看的资源
 *
 *  失败时 `_state` 不动(保持 Loading),错误经 [com.starvault.core.ToastBus] 投递,
 *  由全局 ToastHost 渲染为底部 Snackbar —— 屏不显示 Error 占位。
 *
 *  为什么不让 VM 各持一套:两边业务流程同构(getInfo → 拿 URL → 渲染),抽到 shared 让
 *  Route / Screen 都能复用 isLoading 的逻辑分支判断。
 */
sealed interface PreviewUiState {
    val isLoading: Boolean get() = this is Loading

    data object Loading : PreviewUiState

    data class Success(
        /** 从 /files/get_info 拿到的文件 metadata(name / size / pickCode / thumbnailUrl)。 */
        val metadata: MediaMetadata,
        /**
         * 拿到后可用的资源 URL:
         *  - IMAGE:图片原图 file_url(Coil 直接 GET)
         *  - VIDEO:m3u8 video_url(Media3 直接播放)
         *  - AUDIO:downurl 5min 签名直链 mp3/flac(Media3 ProgressiveMediaSource 播放)
         */
        val mediaUrl: String,
        /**
         * 清晰度名(VIDEO 才有,image/audio 屏空串)。
         * 115 proapi `/open/video/play` 响应 `video_url[0].desc`,
         * 取值如 "1080P"/"720P"/"4K"/"原画"。空 → UI fallback "原画"
         * (单档文件 115 一般不返 desc,UI 用 "原画" 表明"原始/最高清晰度")。
         */
        val qualityChip: String = "",
        /**
         * 全部可用清晰度(VIDEO 才有,image/audio 屏空 list)。
         * 来自 `/open/video/play` 响应的 `data.video_url[]`,
         * 每项 = 一个 (url, desc) 配对;给 PreviewVideoScreen 画质切换弹层用。
         * 5 分钟签名失效前可任意切换,过期需要重新 fetch(目前未做自动刷新)。
         */
        val qualityOptions: List<VideoM3u8> = emptyList(),
        /**
         * 进程内恢复播放位置(AUDIO 才有,image/video 屏默认 0)。
         * 由 [com.starvault.data.local.playback.AudioPositionStore.load] 在
         * PreviewAudioViewModel.load() 第一次 emit Success 前同步读出;>0 时 Screen
         * 在 player ready 后立刻 seekTo,实现"杀进程重进恢复上次听到的位置"。
         */
        val resumePositionMs: Int = 0,
    ) : PreviewUiState
}

/* ─────────────────── 跨 Screen 共用的格式化工具 ─────────────────── */

/**
 * 字节 → 人类可读(B / KB / MB / GB)。对齐 Lumen `ImagePreviewScreen.kt:formatFileSize`。
 *
 * @param sizeBytes 字节数;<=0 → "未知大小"
 */
internal fun formatFileSize(sizeBytes: Long): String {
    return when {
        sizeBytes <= 0 -> "未知大小"
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        sizeBytes < 1024 * 1024 * 1024 ->
            String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        else ->
            String.format(Locale.getDefault(), "%.1f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * unix 秒 → "yyyy-MM-dd HH:mm"。对齐 Lumen `ImagePreviewScreen.kt:formatDate`。
 *
 * @param timestampSec unix 秒;<=0 → "未知日期"
 */
internal fun formatDate(timestampSec: Long): String {
    return if (timestampSec <= 0) {
        "未知日期"
    } else {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        fmt.format(Date(timestampSec * 1000L))
    }
}

/**
 * 毫秒 → "mm:ss" 或 "h:mm:ss"(超过 1 小时加 h)。
 * Preview 屏族共享(视频/音频时间文本对齐)。
 */
internal fun millisToClock(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/* ─────────────────── 屏幕常亮 ─────────────────── */

/**
 * 进入 Composable 时给当前 Activity window 加 [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON],
 * 离屏时 onDispose 清掉。
 *
 * 必须 onDispose 清 — 否则回到 Files 屏后屏幕不会自动灭(常见副作用:忘了 onDispose)。
 * PreviewVideoScreen / PreviewAudioScreen 都在 root Composable 第一行挂这个。
 */
@Composable
internal fun KeepScreenOnEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/* ─────────────────── 10 Hz 状态同步 ─────────────────── */

/**
 * ExoPlayer 状态快照(每 100ms 刷新一次,供 Canvas / Controls 共用同一 source)。
 *
 *  - positionMs : 当前位置(playhead),从 player.currentPosition 读
 *  - durationMs : 总时长,coerceAtLeast(0) 防 UNKNOWN_TIME(-9223372036854775807)
 *  - bufferedMs : 已缓冲位置,从 player.bufferedPosition 读(给 seek 进度条画灰条用)
 *  - isPlaying  : 当前是否播放中,player.isPlaying 同步 playWhenReady + STATE_READY
 */
internal data class PlayerSnapshot(
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val bufferedMs: Int = 0,
    val isPlaying: Boolean = false,
)

/**
 * 10 Hz LaunchedEffect 协程循环读 4 个字段,写到一个 [mutableStateOf]。
 *
 * 为什么用 LaunchedEffect 不用 produceState:一次 snapshot 写入覆盖 4 字段,
 * 所有 chip / progress / play 按钮从同一 source 读,一次重组全更新。
 * 比 4 个独立 state 干净。
 *
 * 为什么不用 Player.Listener:
 *  - Media3 1.4.1 没有 progress callback
 *  - media3-ui-compose 1.6+ 才有,1.9+ 才有 rememberProgressStateWithTickInterval
 *  - listener 没有 onProgressChanged,只有 onIsPlayingChanged 等事件
 */
@Composable
internal fun rememberPlayerSnapshot(player: ExoPlayer): State<PlayerSnapshot> {
    val snapshot = remember(player) { mutableStateOf(PlayerSnapshot()) }
    LaunchedEffect(player) {
        while (true) {
            snapshot.value = PlayerSnapshot(
                positionMs = player.currentPosition.toInt(),
                durationMs = player.duration.coerceAtLeast(0L).toInt(),
                bufferedMs = player.bufferedPosition.toInt(),
                isPlaying = player.isPlaying,
            )
            delay(100L)
        }
    }
    return snapshot
}

/* ─────────────────── 倍速 4 档循环 ─────────────────── */

/** VLC / Media3 PlayerView 默认档(不是 B 站式 1.0/1.25/1.5/2.0)。 */
internal val speedSteps = listOf(1.0f, 1.5f, 2.0f, 0.5f)

/**
 * 4 档循环 + 应用到 ExoPlayer.playbackParameters。
 *
 *  返回 (label, cycleFn):
 *  - label : 当前档位显示文本("1.0×"/"1.5×"/"2.0×"/"0.5×")
 *  - cycleFn : 切下一档 + 立即 apply
 *
 *  LaunchedEffect(player) 初始 apply 一次(1.0× 起步)。
 */
@Composable
internal fun rememberSpeedCycler(player: ExoPlayer): Pair<String, () -> Unit> {
    var idx by remember(player) { mutableIntStateOf(0) }
    val apply = { player.playbackParameters = PlaybackParameters(speedSteps[idx]) }
    LaunchedEffect(player) { apply() }
    val label = speedLabel(speedSteps[idx])
    return label to {
        idx = (idx + 1) % speedSteps.size
        apply()
    }
}

/** 整数倍(1×/2×)显示 "Nx";小数倍(1.5×/0.5×)显示 "N.N×"。 */
internal fun speedLabel(v: Float): String =
    if (v == v.toInt().toFloat()) "${v.toInt()}×" else "${v}×"

/* ─────────────────── 可拖动 seek 进度条 ─────────────────── */

/**
 * 可拖动 seek 进度条。
 *
 *  - 3dp 灰底 + accent 蓝填充 + 12dp 白 thumb
 *  - 拖动([detectDragGestures])→ onSeek(ms)→ ExoPlayer.seekTo
 *  - 单击([detectTapGestures])→ onSeek(对应 frac 的 ms)→ ExoPlayer.seekTo
 *  - [BoxWithConstraints] 拿父宽推 thumb offset(Media3 PlayerView seekbar 标准写法)
 *
 *  为什么不用 Material3 Slider:Slider 的 track / thumb / 拖动手柄都不可定制到
 *  "3dp 灰底 + 12dp 白 thumb"细颗粒;改 SliderColors 只能换色,改不了尺寸/形状。
 *
 *  Tap + Drag 并存:Compose 多个 pointerInput modifier 独立处理手势——
 *  - detectTapGestures 等到 up 才触发,中间无 move
 *  - detectDragGestures 在 move > touchSlop 时立刻触发
 *  - 单纯 click: 无 move,drag 不触发,tap 在 up 时 seek 到点击位置
 *  - 拖动: 有 move,drag 触发 onDragStart 立即 seek + 持续更新,tap 不触发
 */
@Composable
internal fun PreviewSeekBar(
    positionMs: Int,
    durationMs: Int,
    bufferedMs: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .height(18.dp)
            .pointerInput(durationMs) {
                detectTapGestures(
                    onTap = { off ->
                        val frac = (off.x / size.width).coerceIn(0f, 1f)
                        onSeek((frac * durationMs).toInt())
                    },
                )
            }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { off ->
                        val frac = (off.x / size.width).coerceIn(0f, 1f)
                        onSeek((frac * durationMs).toInt())
                    },
                    onDrag = { ch, _ ->
                        val frac = (ch.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((frac * durationMs).toInt())
                        ch.consume()
                    },
                )
            },
    ) {
        val trackW = maxWidth
        // buffered 灰条(占满;当前用 positionMs 蓝条叠加显示进度,buffered 信息
        // 不再通过 Canvas 文本展示,这里保留 bufferedMs 字段作后续扩展)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .align(Alignment.Center),
        )
        // progress 蓝条(按 frac 填充)
        val frac = (positionMs.toFloat() / durationMs.coerceAtLeast(1))
            .coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth(frac)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.accent)
                .align(Alignment.CenterStart),
        )
        // thumb(12dp 白圆)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (trackW - 12.dp) * frac)
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/* ─────────────────── 控制行圆按钮 ─────────────────── */

/** 36dp 圆按钮(primary 44dp 白底 + 黑 icon)。 */
@Composable
internal fun PreviewCtrlBtn(
    icon: ImageVector,
    contentDescription: String,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    val size = if (isPrimary) 44.dp else 36.dp
    val alpha = if (enabled) 1f else 0.35f
    val resolvedTint = tint ?: if (isPrimary) Color(0xFF111111) else Color.White
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPrimary) Color.White.copy(alpha = alpha) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint.copy(alpha = alpha),
            modifier = Modifier.size(if (isPrimary) 22.dp else 20.dp),
        )
    }
}

/* ─────────────────── 通用"更多"菜单(6 项 dark DropdownMenu)─────────────────── */

/**
 * Preview 屏族通用"更多"菜单(下载/重命名/移动/删除/分享/属性)。
 * VIDEO / AUDIO 屏 TopBar 都挂这个。
 *
 *  视觉:黑底半透 + 白文字(Material3 DropdownMenu 默认浅色 → 这里改用
 *  DarkDropdownMenu 配色方案,跟黑底顶栏融合)。
 *
 *  下载项与其他 5 项间有 HorizontalDivider(0.12 alpha 白色)— 视觉强调"下载是主操作,
 *  其他是次要管理动作"。跟 MoreMenu 在 Files 屏的 Row dropdown 保持一致。
 */
@Composable
internal fun MoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onProperties: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF1A1A1A)),
    ) {
        MoreMenuItem(Icons.DownloadInto, "下载", onDownload)
        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
        MoreMenuItem(Icons.Rename, "重命名", onRename)
        MoreMenuItem(Icons.Move, "移动", onMove)
        MoreMenuItem(Icons.Trash, "删除", onDelete)
        MoreMenuItem(Icons.Share, "分享", onShare)
        MoreMenuItem(Icons.Settings, "属性", onProperties)
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(text = text, color = Color.White, style = StarVaultTheme.typography.body)
            }
        },
        onClick = onClick,
    )
}

/* ─────────────────── 播放列表 sheet(VIDEO / AUDIO 屏族共用) ─────────────────── */

/**
 * 播放列表底部弹层,显示同父目录的全部 media 项(AUDIO / VIDEO),
 * 点击某项触发 [onPicked](fid),Route 切到对应 Preview 屏。
 *
 *  与 [PreviewAudioScreen] / [PreviewVideoScreen] 的"上下首/集"按钮互补:
 *  - 按钮适合顺序浏览(连续看),sheet 适合跳看(看第 5 集不必 4 次 Next)
 *  - sheet 显示当前 fid 高亮 + 初始自动滚到该行(避免 50 首列表从第 1 开始)
 *
 *  实现选型 — 不用 material3 的 ModalBottomSheet:
 *  - 项目约定用 SortSheet / FolderSheet 的手写 pattern(scrim Box + 贴底 Column + 圆角顶);
 *    material3 的 ModalBottomSheet 默认 M3 动效,与现有 SortSheet 风格不一致
 *  - scrim 用 `Color.Black.copy(alpha = 0.4f)` + clickable dismiss,匹配 SortSheet
 *
 *  复用注意事项:
 *  - 当前 fid 黄色 accent 高亮(`c.accent.copy(alpha = 0.08f)`)+ 右侧 ≡♪ Icons.Playlist
 *  - 标题固定"播放列表 共 {N} 首"(N=items.size,空时显示"暂无播放列表")
 *  - 空态(Search 入口 / 拉失败):居中 muted 文字"暂无播放列表"
 *  - [placeholderIcon] 给 thumbnailUrl 为空时的 fallback:
 *    audio 传 [Icons.Music],video 传 [Icons.Video]
 *
 *  @param currentFileId  当前播放 fid,用于高亮 + 初始滚动定位
 *  @param items          父目录 media 列表(已过滤 audio/video,已按 name asc)
 *  @param placeholderIcon 无 thumbnailUrl 时显示的 fallback icon(Icons.Music / Icons.Video)
 *  @param onPicked       用户点某项(fileId)→ 关 sheet + Route 切歌
 *  @param onDismiss      点 scrim / 系统返回 → 关 sheet
 */
@Composable
internal fun PlaylistSheet(
    currentFileId: String,
    mediaItems: List<com.starvault.data.remote.cloud115.ParsedFileItem>,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val listState = rememberLazyListState()

    val initialIndex = remember(mediaItems) { mediaItems.indexOfFirst { it.id == currentFileId }.coerceAtLeast(0) }
    LaunchedEffect(mediaItems) {
        if (mediaItems.isNotEmpty()) listState.scrollToItem(initialIndex)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(540.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(c.surface)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFC4C4C4)),
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "播放列表",
                    color = c.fg,
                    style = t.title,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "共 ${mediaItems.size} 首",
                    color = c.muted,
                    style = t.micro,
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = c.border, thickness = 1.dp)

            if (mediaItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无播放列表",
                        color = c.muted,
                        style = t.body,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(mediaItems, key = { it.id }) { item ->
                        PlaylistRow(
                            item = item,
                            isCurrent = item.id == currentFileId,
                            placeholderIcon = placeholderIcon,
                            onClick = { onPicked(item.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 播放列表单行:左侧 thumbnail 36dp(无则 [placeholderIcon] fallback) + 文件名 +
 * 副标题(size · duration)+ 右侧 ≡♪ 当前标记。
 */
@Composable
internal fun PlaylistRow(
    item: com.starvault.data.remote.cloud115.ParsedFileItem,
    isCurrent: Boolean,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val duration = formatPlayLong(item.playLong)
    val subtitle = buildString {
        append(formatFileSize(item.sizeBytes))
        if (duration.isNotEmpty()) {
            append(" · ")
            append(duration)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) c.accent.copy(alpha = 0.08f) else c.surface)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.thumbnailUrl.isNotBlank()) {
            coil3.compose.AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1A1A1A)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = c.fg,
                style = t.body,
                fontWeight = if (isCurrent) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = c.muted,
                style = t.micro,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Playlist,
                contentDescription = "正在播放",
                tint = c.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** playLong(秒) → "04:23" 或 "01:30:08";<=0 → ""(空字符串,subtitle 拼接自动跳过)。 */
internal fun formatPlayLong(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

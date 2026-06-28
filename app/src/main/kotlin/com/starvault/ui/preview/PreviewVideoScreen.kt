package com.starvault.ui.preview

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.starvault.component.Icons
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.repository.VideoM3u8
import com.starvault.theme.StarVaultTheme
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

/**
 * PreviewVideo 全屏屏幕（黑底 Media3 风格播放器 chrome）。
 *
 *  结构（垂直 Column 堆叠）：
 *  ┌─ PreviewTopBar     56dp 黑底: ← 返回 / 字幕 / 投屏 / 更多
 *  ├─ PreviewCanvas     weight=1f 黑底 ExoPlayer + overlays
 *  │   ├─ AndroidView PlayerView(useController=false)
 *  │   ├─ 右上 3 chip (qualityChip / 倍速 / 原声)
 *  │   └─ 中心 76dp play 圆(play/pause 双向)
 *  └─ PreviewControls   黑底: seek 进度条 + 时间 + 6 圆按钮
 *
 *  Media3 + 115 Bearer 桥接：
 *  - 用 [OkHttpDataSource.Factory] 包装 [ServiceLocator.okHttpClient]，
 *    拦截器自动注入 115 Bearer + 浏览器伪装头(Media3 拉的 m3u8 + segment 都自动带 Bearer)
 *  - [HlsMediaSource.Factory] 解析 m3u8 manifest + 段地址
 *  - ExoPlayer 由 Composable 持有，dispose 时 release 防泄漏
 *
 *  状态机：
 *  - Loading  → 全屏 spinner(c.bg 浅色)
 *  - Success  → 上面 3 段布局
 *  - Error    → 不显示屏占位,ToastBus 一次性 Snackbar(DisposableEffect 内 send)
 *
 *  状态同步：
 *  - [rememberPlayerSnapshot] 用 10 Hz LaunchedEffect 协程读
 *    player.currentPosition / duration / bufferedPosition / isPlaying
 *  - 不用 Player.Listener:Media3 1.4.1 没有 progress callback,
 *    media3-ui-compose 是 1.6+ 才有,1.9+ 才有 rememberProgressStateWithTickInterval
 *  - 中心 play 圆 + 控制行 play ↔ ExoPlayer.playWhenReady 双向:
 *    player.playWhenReady = !player.playWhenReady 切一次,
 *    下次 100ms 轮询 player.isPlaying 同步 icon
 *
 *  倍速：4 档循环(1.0× → 1.5× → 2.0× → 0.5× → 1.0×),对齐 VLC / Media3 PlayerView 默认档。
 *  不是 B 站式 1.0/1.25/1.5/2.0(1.25× 不是业内常见档)。
 *
 *  下载：构造合成 [ParsedFileItem](从 [com.starvault.data.repository.MediaMetadata])
 *  → [com.starvault.data.repository.DownloadRepository.enqueue]。
 *  MediaMetadata.fileCategory 是 String "1" 而 ParsedFileItem.fileCategory 是 Int 2,
 *  同名不同义,合成处显式注释。
 *
 *  Noop + ToastBus:字幕 / 投屏 / 更多 / 上一集 / 下一集(全屏已接,见 VideoContent.isFullscreen)。
 *
 *  @param state VM 暴露的 [PreviewUiState]
 *  @param onBack 返回(BackHandler + 顶部"返回"按钮都调它)
 *  @param onSelectQuality 切档(VM 收到后改 Success.mediaUrl,Screen 侧 remember 重建 player)
 */
@Composable
fun PreviewVideoScreen(
    state: PreviewUiState,
    onBack: () -> Unit,
    onSelectQuality: (VideoM3u8) -> Unit = {},
) {
    val c = StarVaultTheme.colors
    BackHandler(enabled = true) { onBack() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        when (state) {
            is PreviewUiState.Loading -> LoadingBlock()
            is PreviewUiState.Success -> VideoContent(state, onBack, onSelectQuality)
        }
    }
}

/* ─────────────────── 视频内容 ─────────────────── */

@Composable
private fun VideoContent(
    state: PreviewUiState.Success,
    onBack: () -> Unit,
    onSelectQuality: (VideoM3u8) -> Unit,
) {
    val context = LocalContext.current

    // 1. ExoPlayer 实例(URL 变 → 重新 prepare,同一 Preview 屏被复用也能正确切源)
    val player = remember(state.mediaUrl) { buildPlayer(context, state.mediaUrl) }

    // 2. 错误监听(Media3 拉流失败 → ToastBus 一次性提示,不显示屏占位)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                ToastBus.error(error.message ?: "视频播放失败")
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // 3. 释放(Composable 离屏时 release ExoPlayer 防泄漏)
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // 4. 10 Hz 状态同步(单 source 写多字段)
    val snapshot by rememberPlayerSnapshot(player)

    // 5. 倍速循环(返回 (label, cycleFn))
    val (speedLabel, onCycleSpeed) = rememberSpeedCycler(player)

    // 6. play/pause toggle(playWhenReady 双向)
    val onTogglePlay: () -> Unit = { player.playWhenReady = !player.playWhenReady }

    // 7. seek
    val onSeek: (Int) -> Unit = { ms -> player.seekTo(ms.toLong()) }

    // 8. 下载触发(合成 ParsedFileItem)
    //    MediaMetadata.fileCategory 是 String "1"(proapi),ParsedFileItem.fileCategory
    //    是 Int 2(115 webapi 约定 video),同名不同义。
    val onDownload: () -> Unit = remember(state) {
        {
            val m = state.metadata
            val item = ParsedFileItem(
                id = m.fid,
                parentId = "",                      // Preview 没拿 parent cid,DownloadWorker 不读
                name = m.name,
                ico = m.ico,                        // "mp4" / "mkv"
                sizeBytes = m.sizeBytes,
                mtimeSec = m.mtimeSec,
                pickCode = m.pickCode,
                isFolder = false,
                playLong = 0,
                sha1 = m.sha1,
                fileCategory = 2,                   // 115 webapi fc 约定:2=video
                thumbnailUrl = m.thumbnailUrl,
            )
            ServiceLocator.downloadRepository.enqueue(item)
                .onSuccess { ToastBus.info("已加入下载队列") }
                .onFailure { ToastBus.error(it.message ?: "下载失败") }
        }
    }

    // 9. 全屏切换:
    //   - 横屏(landscape) + immersive sticky 隐藏系统栏
    //   - 顶栏整条收起(对齐 YouTube/B 站,横屏纯视频模式)
    //   - orientation 改变默认会重建 Activity;ExoPlayer 由 composition remember 持有,
    //     重建后会丢。预先把当前位置存到 rememberSaveable,新 player 起来后 seekTo 恢复。
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var resumePositionMs by rememberSaveable { mutableIntStateOf(0) }
    val localCtx = LocalContext.current
    DisposableEffect(isFullscreen) {
        val activity = localCtx as? Activity
        if (activity != null) {
            activity.requestedOrientation = if (isFullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { /* 状态翻转时由下一个 effect 接管 */ }
    }
    val onToggleFullscreen: () -> Unit = {
        // 切换前抓拍位置(无论进入还是退出):横竖屏切换都会重建 Activity,
        // 新 player 起来后 LaunchedEffect(player) 会 seekTo 到这里
        resumePositionMs = snapshot.positionMs
        isFullscreen = !isFullscreen
    }
    // 新 player 起来 → 恢复到原位置
    LaunchedEffect(player) {
        if (resumePositionMs > 0) {
            player.seekTo(resumePositionMs.toLong())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 全屏时隐藏顶栏:横屏下返回用 BackHandler / 系统手势即可,
        // 留顶栏反而挤压视频画布(对齐 YouTube/B 站视频横屏体验)
        if (!isFullscreen) {
            PreviewTopBar(name = state.metadata.name, onBack = onBack)
        }

        PreviewCanvas(
            player = player,
            snapshot = snapshot,
            qualityChip = state.qualityChip,
            qualityOptions = state.qualityOptions,
            currentQualityUrl = state.mediaUrl,
            speedChip = speedLabel,
            onTogglePlay = onTogglePlay,
            onSelectQuality = onSelectQuality,
            modifier = Modifier.weight(1f),
        )

        PreviewControls(
            snapshot = snapshot,
            speedLabel = speedLabel,
            isFullscreen = isFullscreen,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onCycleSpeed = onCycleSpeed,
            onDownload = onDownload,
            onToggleFullscreen = onToggleFullscreen,
        )
    }
}

/**
 * 构造 ExoPlayer + 注入 OkHttpDataSource.Factory(115 Bearer 拦截)+ HlsMediaSource。
 * 提到顶层函数避免 remember block 太长。
 */
private fun buildPlayer(context: Context, mediaUrl: String): ExoPlayer {
    val okHttpClient: OkHttpClient = ServiceLocator.okHttpClient
    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0")
    val mediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            playWhenReady = true
            prepare()
        }
}

/* ─────────────────── Snapshot 状态同步 ─────────────────── */

/**
 * ExoPlayer 状态快照(每 100ms 刷新一次,供 Canvas / Controls 共用同一 source)。
 *
 * 字段:
 *  - positionMs : 当前位置(playhead),从 player.currentPosition 读
 *  - durationMs : 总时长,coerceAtLeast(0) 防 UNKNOWN_TIME(-9223372036854775807)
 *  - bufferedMs : 已缓冲位置,从 player.bufferedPosition 读(给 seek 进度条画灰条用)
 *  - isPlaying  : 当前是否播放中,player.isPlaying 同步 playWhenReady + STATE_READY
 */
private data class PlayerSnapshot(
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
private fun rememberPlayerSnapshot(player: ExoPlayer): State<PlayerSnapshot> {
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
private val speedSteps = listOf(1.0f, 1.5f, 2.0f, 0.5f)

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
private fun rememberSpeedCycler(player: ExoPlayer): Pair<String, () -> Unit> {
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
private fun speedLabel(v: Float): String =
    if (v == v.toInt().toFloat()) "${v.toInt()}×" else "${v}×"

/* ─────────────────── 顶部 toolbar ─────────────────── */

/**
 * 顶部工具栏(56dp 黑底):
 *  - 左侧 ← 返回 图标
 *  - 中间 文件名(titleMedium,maxLines=1,weight=1f)
 *  - 右侧 字幕 / 投屏 / 更多 3 图标
 *  - 后 3 个是 noop + ToastBus 提示
 */
@Composable
private fun PreviewTopBar(name: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewIconBtn(Icons.Back, "返回", onBack)
        Text(
            text = name,
            color = Color.White,
            style = StarVaultTheme.typography.subtitle,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        PreviewIconBtn(Icons.Subtitle, "字幕") { ToastBus.info("字幕切换即将上线") }
        PreviewIconBtn(Icons.Cast, "投屏") { ToastBus.info("投屏功能即将上线") }
        PreviewIconBtn(Icons.More, "更多") { ToastBus.info("更多操作即将上线") }
    }
}

@Composable
private fun PreviewIconBtn(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ─────────────────── 视频画布 ─────────────────── */

/**
 * 黑底视频画布:
 *  - AndroidView PlayerView 占满(useController=false,我们自己画控件)
 *  - 右上 3 chip(qualityChip 蓝底 / 倍速 / 原声)
 *  - 中心 76dp 大圆 play 按钮(play/pause 双向同步)
 */
@Composable
private fun PreviewCanvas(
    player: ExoPlayer,
    snapshot: PlayerSnapshot,
    qualityChip: String,
    qualityOptions: List<VideoM3u8>,
    currentQualityUrl: String,
    speedChip: String,
    onTogglePlay: () -> Unit,
    onSelectQuality: (VideoM3u8) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = StarVaultTheme.typography
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
    ) {
        // ExoPlayer 全屏画布
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    useController = false             // 不显示 Media3 自带控件,我们自己画
                    setShowSubtitleButton(false)      // 防御:字幕切换由 URL 参数控制
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
            },
        )

        // 左上原本是 ● pulse + "在线播放 · 已缓冲 X MB" 状态行,
        // 用户认为冗余,删掉。视频画布纯展示 + 右上 3 chip(quality/speed/原声)已够。


        // 右上:3 chip。quality chip 可点(有 options 时)→ DropdownMenu 切档;
        // 单档 / 列表为空时降级为只读,避免点开空菜单。
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QualityChipWithMenu(
                currentDesc = qualityChip.ifBlank { "原画" },
                options = qualityOptions,
                currentUrl = currentQualityUrl,
                onSelect = onSelectQuality,
            )
            PreviewChip(text = speedChip, accent = false)
            PreviewChip(text = "原声", accent = false)
        }

        // 中心:76dp 大圆 play 圆
        // 播放时常驻的"播放"按钮会和底部暂停按钮冗余，所以播放时整块隐藏。
        // 暂停时显示，给用户一个最显眼的恢复入口。
        if (!snapshot.isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Play,
                    contentDescription = "播放",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** 半透明 chip:accent=true 蓝底(qualityChip),其它 50% 黑底。 */
@Composable
private fun PreviewChip(text: String, accent: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (accent) Color(0xD92F6FEB)   // rgba(47,111,235,0.85)
                else Color.Black.copy(alpha = 0.5f),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = StarVaultTheme.typography.micro,
            color = Color.White,
        )
    }
}

/**
 * 清晰度 chip + DropdownMenu 切档。
 *
 *  - [currentDesc]: 蓝底 chip 显示当前 desc(如 "1080P",空 → UI fallback "—")
 *  - [options]: 全部可用清晰度(来自 /open/video/play 的 video_url[])
 *  - [currentUrl]: 当前正在播放的 url,用于菜单项打勾
 *  - [onSelect]: 选中后调 → VM 改 Success.mediaUrl → Screen 重建 player
 *
 *  当 [options] 只有一个或为空时(降级到只 1 档 / 元数据缺失),整个 chip 不可点;
 *  只显示,不开菜单(避免点开"1 项菜单"这种空操作)。
 */
@Composable
private fun QualityChipWithMenu(
    currentDesc: String,
    options: List<VideoM3u8>,
    currentUrl: String,
    onSelect: (VideoM3u8) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val isInteractive = options.size > 1

    Box {
        // chip 本体:有 ≥2 档才加 clickable;否则纯展示
        val chipModifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xD92F6FEB))
            .then(
                if (isInteractive) Modifier.clickable { expanded = true }
                else Modifier,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        Box(modifier = chipModifier) {
            Text(
                text = currentDesc,
                style = StarVaultTheme.typography.micro,
                color = Color.White,
            )
        }
        // DropdownMenu:挂在 chip 父 Box 上,系统会自动定位到 anchor 下方
        DropdownMenu(
            expanded = expanded && isInteractive,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { opt ->
                val isCurrent = opt.url == currentUrl
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt.qualityDesc.ifBlank { "未知清晰度" },
                            color = if (isCurrent) Color(0xFF2F6FEB) else Color.White,
                            style = StarVaultTheme.typography.body,
                        )
                    },
                    onClick = {
                        expanded = false
                        if (!isCurrent) onSelect(opt)
                    },
                )
            }
        }
    }
}

/* ─────────────────── 底部控制条 ─────────────────── */


/**
 * 底部控制条(黑底):
 *  - 进度条([PreviewSeekBar]:3dp 灰底 + accent 蓝填充 + 12dp 白 thumb)
 *  - 时间文本(32:14 / 1:42:08)
 *  - 6 圆按钮:上一集 / 播放(白底实心) / 下一集 | 倍速(label 可点) / 下载 / 全屏
 */
@Composable
private fun PreviewControls(
    snapshot: PlayerSnapshot,
    speedLabel: String,
    isFullscreen: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onCycleSpeed: () -> Unit,
    onDownload: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 进度条 + 时间
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewSeekBar(
                positionMs = snapshot.positionMs,
                durationMs = snapshot.durationMs,
                bufferedMs = snapshot.bufferedMs,
                onSeek = onSeek,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${millisToClock(snapshot.positionMs)} / ${millisToClock(snapshot.durationMs)}",
                style = t.micro,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(8.dp))
        // 6 圆按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 左:上一集 / 播放 / 下一集
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewCtrlBtn(Icons.Prev, "上一集") { ToastBus.info("已是单视频") }
                PreviewCtrlBtn(
                    icon = if (snapshot.isPlaying) Icons.Pause else Icons.Play,
                    contentDescription = "播放",
                    onClick = onTogglePlay,
                    isPrimary = true,
                )
                PreviewCtrlBtn(Icons.Next, "下一集") { ToastBus.info("已是单视频") }
            }
            // 右:倍速(label 可点循环) / 下载 / 全屏
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCycleSpeed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = speedLabel,
                        color = Color.White,
                        style = t.micro,
                    )
                }
                PreviewCtrlBtn(Icons.DownloadInto, "下载", onClick = onDownload)
                // 全屏 / 退出全屏 二态:portrait 显示 Fullscreen(放大),landscape 显示 FullscreenExit(收缩)
                PreviewCtrlBtn(
                    icon = if (isFullscreen) Icons.FullscreenExit else Icons.Fullscreen,
                    contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                    onClick = onToggleFullscreen,
                )
            }
        }
    }
}

/**
 * 可拖动 seek 进度条。
 *
 *  - 3dp 灰底 + accent 蓝填充 + 12dp 白 thumb
 *  - 拖动([detectDragGestures])→ onSeek(ms)→ ExoPlayer.seekTo
 *  - [BoxWithConstraints] 拿父宽推 thumb offset(Media3 PlayerView seekbar 标准写法)
 *
 *  为什么不用 Material3 Slider:Slider 的 track / thumb / 拖动手柄都不可定制到
 *  "3dp 灰底 + 12dp 白 thumb"细颗粒;改 SliderColors 只能换色,改不了尺寸/形状。
 */
@Composable
private fun PreviewSeekBar(
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

/** 36dp 圆按钮(primary 44dp 白底 + 黑 icon)。 */
@Composable
private fun PreviewCtrlBtn(
    icon: ImageVector,
    contentDescription: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    val size = if (isPrimary) 44.dp else 36.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPrimary) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPrimary) Color(0xFF111111) else Color.White,
            modifier = Modifier.size(if (isPrimary) 22.dp else 20.dp),
        )
    }
}

/* ─────────────────── Loading ─────────────────── */

/** 全屏 spinner(c.bg 浅色,跟视频屏黑底过渡)。 */
@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = StarVaultTheme.colors.accent)
    }
}

/* ─────────────────── 工具 ─────────────────── */

/** 毫秒 → "mm:ss" 或 "h:mm:ss"(超过 1 小时加 h)。 */
private fun millisToClock(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

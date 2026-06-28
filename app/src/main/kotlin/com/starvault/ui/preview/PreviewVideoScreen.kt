package com.starvault.ui.preview

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
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
import okhttp3.OkHttpClient
import kotlinx.coroutines.delay

/**
 * PreviewVideo 全屏屏幕（黑底 Media3 风格播放器 chrome）。
 *
 *  结构（垂直 Column 堆叠）：
 *  ┌─ PreviewTopBar     56dp 黑底: ← 返回 / 投屏 / 更多(DropdownMenu 收纳 6 项)
 *  ├─ PreviewCanvas     weight=1f 黑底 ExoPlayer + overlays
 *  │   ├─ AndroidView PlayerView(useController=false)
 *  │   ├─ 右上 3 chip(qualityChip / 倍速 / 原声 | 字幕),音轨/字幕有 ≥2 条时 chip 可点开 DropdownMenu
 *  │   └─ 单击画布切换 play/pause(对齐 ExoPlayer PlayerView 默认行为)
 *  └─ PreviewControls   黑底: seek 进度条 + 时间 + 5 圆按钮(Prev/Play/Next | 倍速/全屏)
 *
 *  屏幕常亮([KeepScreenOnEffect]):进入 PreviewVideo 即给当前 window 加 [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON],
 *  离开 Composable 时清掉,避免回到 Files 屏后屏幕不再灭(常见副作用:忘了加 onDispose)。
 *
 *  媒体 3 + 115 Bearer 桥接:
 *  - 用 [OkHttpDataSource.Factory] 包装 [ServiceLocator.okHttpClient],
 *    拦截器自动注入 115 Bearer + 浏览器伪装头(Media3 拉的 m3u8 + segment 都自动带 Bearer)
 *  - [HlsMediaSource.Factory] 解析 m3u8 manifest + 段地址
 *  - ExoPlayer 由 Composable 持有,dispose 时 release 防泄漏
 *
 *  状态机:
 *  - Loading  → 全屏 spinner(c.bg 浅色)
 *  - Success  → 上面 3 段布局
 *  - Error    → 不显示屏占位,ToastBus 一次性 Snackbar(DisposableEffect 内 send)
 *
 *  状态同步:
 *  - [rememberPlayerSnapshot] 用 10 Hz LaunchedEffect 协程读
 *    player.currentPosition / duration / bufferedPosition / isPlaying
 *  - [rememberTracksSnapshot] 通过 Player.Listener.onTracksChanged 监听 currentTracks,
 *    解出 audio/text 各 track 列表(用于原声 / 字幕 chip + DropdownMenu 切轨)
 *
 *  倍速:4 档循环(1.0× → 1.5× → 2.0× → 0.5× → 1.0×),对齐 VLC / Media3 PlayerView 默认档。
 *  不是 B 站式 1.0/1.25/1.5/2.0(1.25× 不是业内常见档)。
 *
 *  下载:由 [PreviewTopBar] 的"更多" DropdownMenu 触发(原位从底部控制栏下移),构造
 *  合成 [ParsedFileItem] → [com.starvault.data.repository.DownloadRepository.enqueue]。
 *  MediaMetadata.fileCategory 是 String "1" 而 ParsedFileItem.fileCategory 是 Int 2,
 *  同名不同义,合成处显式注释。
 *
 *  上一集/下一集:接 [PreviewVideoViewModel.Siblings];prev/next fid 由 Route 解析到 nav 跳转;
 *  无父目录(Route.parentCid == null)或没拉到 → 按钮降级为 noop + ToastBus。
 *
 *  Noop + ToastBus:投屏 / 重命名 / 移动 / 删除 / 分享 / 属性(更多菜单内 5 项 + 顶栏投屏)。
 *
 *  星标(❤️/♡):最左位置在播放控制行的左侧(用户指示:和播放按钮同一行最左);
 *  PreviewCtrlBtn 36dp 默认白底透明 + 白 icon(视频黑底屏面下统一白色 tint);
 *  fill/outline 由 isStarred 决定。星标和 prev/play/next / 倍速 / 全屏 并存于一行,
 *  用 SpaceBetween 分布:左 4 按钮(❤️ / Prev / Play / Next),右 2 按钮(倍速 / 全屏)。
 *
 *  @param state VM 暴露的 [PreviewUiState]
 *  @param siblings 上一集/下一集 fid(从 [PreviewVideoViewModel.siblings] 来,null = 不可点)
 *  @param isStarred 当前星标状态(VM.isStarred StateFlow 注入)
 *  @param onBack 返回(BackHandler + 顶部"返回"按钮都调它)
 *  @param onSelectQuality 切档(VM 收到后改 Success.mediaUrl,Screen 侧 remember 重建 player)
 *  @param onPrev 上一集(Screen 只在 prevId 非空时调;Route 负责 nav 跳转)
 *  @param onNext 下一集(同 onPrev)
 *  @param onToggleStar 点 ❤️ 触发(VM.toggleStar:乐观更新 + 失败回滚)
 *  @param onSavePosition 5s 节流 + onDispose 兜底,把 player.currentPosition 写盘
 */
@Composable
fun PreviewVideoScreen(
    state: PreviewUiState,
    siblings: PreviewVideoViewModel.Siblings = PreviewVideoViewModel.Siblings(),
    isStarred: Boolean = false,
    onBack: () -> Unit,
    onSelectQuality: (VideoM3u8) -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onToggleStar: () -> Unit = {},
    onSavePosition: (Long) -> Unit = {},
) {
    val c = StarVaultTheme.colors
    KeepScreenOnEffect()
    BackHandler(enabled = true) { onBack() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        when (state) {
            is PreviewUiState.Loading -> LoadingBlock()
            is PreviewUiState.Success -> VideoContent(
                state = state,
                siblings = siblings,
                isStarred = isStarred,
                onBack = onBack,
                onSelectQuality = onSelectQuality,
                onPrev = onPrev,
                onNext = onNext,
                onToggleStar = onToggleStar,
                onSavePosition = onSavePosition,
            )
        }
    }
}

/**
 * 屏幕常亮:进入 Composable 时给当前 window 加 [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON],
 * 离开(onDispose)时清回默认。
 *
 *  - 实现:用 [LocalView] 拿到 host view,沿 view tree 找到 Activity(window 持有者),
 *    直接改 [Activity.getWindow] 的 flag。这是 Compose 推荐做法,比 remember 一个
 *    Activity 引用更安全(LocalView 在 Composition 销毁时自动失效)。
 *  - 生命周期:DisposableEffect onDispose 由 Composable 离屏触发(包括 nav pop / 屏销毁)。
 *    不会泄漏到其他屏。
 */
// KeepScreenOnEffect 已抽到 PreviewShared.kt(VIDEO / AUDIO 屏族共享)。

/* ─────────────────── 视频内容 ─────────────────── */

@Composable
private fun VideoContent(
    state: PreviewUiState.Success,
    siblings: PreviewVideoViewModel.Siblings,
    isStarred: Boolean,
    onBack: () -> Unit,
    onSelectQuality: (VideoM3u8) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleStar: () -> Unit,
    onSavePosition: (Long) -> Unit,
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

    // 3. 释放(Composable 离屏时 release ExoPlayer 防泄漏) + 兜底保存播放位置
    //    两个 onDispose 顺序执行:先 savePosition 读 player.currentPosition(此时 player
    //    还没 release),然后 release。DataStore.edit 是 IO,毫秒级不影响 release 时序。
    DisposableEffect(player) {
        onDispose {
            onSavePosition(player.currentPosition)
            player.release()
        }
    }

    // 4. 恢复播放位置：player ready 后立刻 seekTo 到上次的 positionMs
    //    state.resumePositionMs 由 VM 在 emit Success 之前从 MediaPositionStore 读出
    //    注:跟旋转屏 rememberSaveable resumePositionMs 是独立两份 — DataStore 跨进程,
    //    rememberSaveable 只跨 Activity 重建。VideoContent 重组 / player 切 URL 重建时
    //    优先用 state.resumePositionMs,横竖屏切换由下面"全屏切换"另处理。
    val savedResumeMs = state.resumePositionMs
    LaunchedEffect(player) {
        if (savedResumeMs > 0) {
            player.seekTo(savedResumeMs.toLong())
        }
    }

    // 5. 节流保存播放位置(5s 一次):player.currentPosition 写到 MediaPositionStore
    LaunchedEffect(player) {
        while (true) {
            delay(5_000L)
            onSavePosition(player.currentPosition)
        }
    }

    // 4. 10 Hz 状态同步(单 source 写多字段)
    val snapshot by rememberPlayerSnapshot(player)

    // 5. tracks 监听(audio/text chip + 切轨用)
    val tracks by rememberTracksSnapshot(player)

    // 6. 倍速循环(返回 (label, cycleFn))
    val (speedLabel, onCycleSpeed) = rememberSpeedCycler(player)

    // 7. play/pause toggle(playWhenReady 双向)
    val onTogglePlay: () -> Unit = { player.playWhenReady = !player.playWhenReady }

    // 8. seek
    val onSeek: (Int) -> Unit = { ms -> player.seekTo(ms.toLong()) }

    // 9. 下载触发(合成 ParsedFileItem,移动到"更多"菜单后这里依然保留为回调)
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

    // 10. 音轨选择(audio chip 点击 + DropdownMenu 选中 → TrackSelectionOverride)
    val onSelectAudioTrack: (TrackOption) -> Unit = { opt ->
        val params: TrackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .addOverride(TrackSelectionOverride(opt.trackGroup, opt.trackIndex))
            .build()
        player.trackSelectionParameters = params
    }

    // 11. 字幕轨选择(同 audio,类型 TRACK_TYPE_TEXT)
    val onSelectTextTrack: (TrackOption) -> Unit = { opt ->
        val params: TrackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .addOverride(TrackSelectionOverride(opt.trackGroup, opt.trackIndex))
            .build()
        player.trackSelectionParameters = params
    }

    // 12. 关闭字幕(清 override + 标记 TEXT 类型 disabled)
    val onDisableText: () -> Unit = {
        val params = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        player.trackSelectionParameters = params
    }

    // 13. 全屏切换:
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
            PreviewTopBar(name = state.metadata.name, onBack = onBack, onDownload = onDownload)
        }

        PreviewCanvas(
            player = player,
            snapshot = snapshot,
            qualityChip = state.qualityChip,
            qualityOptions = state.qualityOptions,
            currentQualityUrl = state.mediaUrl,
            speedChip = speedLabel,
            tracks = tracks,
            onTogglePlay = onTogglePlay,
            onSelectQuality = onSelectQuality,
            onSelectAudioTrack = onSelectAudioTrack,
            onSelectTextTrack = onSelectTextTrack,
            onDisableText = onDisableText,
            modifier = Modifier.weight(1f),
        )

        PreviewControls(
            snapshot = snapshot,
            speedLabel = speedLabel,
            isFullscreen = isFullscreen,
            hasPrev = siblings.prevId != null,
            hasNext = siblings.nextId != null,
            isStarred = isStarred,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onCycleSpeed = onCycleSpeed,
            onToggleFullscreen = onToggleFullscreen,
            onPrev = onPrev,
            onNext = onNext,
            onToggleStar = onToggleStar,
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
            // 进屏不自动播放(Spotify / Apple Music 风格):用户点 Play 按钮开始
            // resumePositionMs 由 LaunchedEffect(player) seekTo,但 playWhenReady=false
            // 让 player 准备好后停在暂停态,seek 后用户点 Play 才真正播
            playWhenReady = false
            prepare()
        }
}

/* ─────────────────── Snapshot 状态同步 ─────────────────── */
// PlayerSnapshot + rememberPlayerSnapshot 已抽到 PreviewShared.kt。

/* ─────────────────── Tracks 监听(原声 / 字幕 chip 用)─────────────────── */

/**
 * 一条可被用户选择的音轨 / 字幕轨。
 *
 *  - [trackGroup]:ExoPlayer 的 TrackGroup,用于构造 [TrackSelectionOverride](不可为 null)
 *  - [trackIndex]:Group 内第几条
 *  - [label]:UI 显示文案,优先 [androidx.media3.common.Format.label],否则 language code,否则 "音轨 N"
 */
data class TrackOption(
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val label: String,
)

/**
 * 播放器当前可用音轨 + 字幕轨快照(由 Player.Listener.onTracksChanged 触发更新)。
 *
 *  - [audioTracks]:当前音轨列表(可能为空 — 某些流只有 1 条且 isSelected=true,仍列出来)
 *  - [selectedAudioIndex]:当前选中 index,用于 DropdownMenu 项打勾
 *  - [textTracks]:字幕轨列表;空 = 该媒体无字幕轨
 *  - [selectedTextIndex]:当前选中字幕;null = 用户已手动关闭字幕
 *  - [textDisabled]:用户禁用了字幕(TrackSelectionParameters.setTrackTypeDisabled TEXT true)
 *
 * 解码约定:每个 [Tracks.Group] 表示一种"轨类型 + 轨集合"——同一 Group 内 N 条互斥可选
 * (原声 vs 粤语);不同 Group 一般不能跨选(类型不同,如一条 audio + 一条 text 是两个 Group)。
 * 这里 UI 层只列 Group 内的所有 track,用首个 Group 暴露给用户。
 */
data class TracksSnapshot(
    val audioTracks: List<TrackOption> = emptyList(),
    val selectedAudioIndex: Int? = null,
    val textTracks: List<TrackOption> = emptyList(),
    val selectedTextIndex: Int? = null,
    val textDisabled: Boolean = false,
)

/**
 * 监听 [Player.Listener.onTracksChanged] + 初始 poll 一次,解出 [TracksSnapshot]。
 *
 * 为什么初始要主动 poll:listener 在第一次 onTracksChanged 触发前 player.currentTracks 已经是
 * 已知值(115 单音轨 m3u8 立即 ready,没第二个 trigger)。不 poll 的话 UI 永远看不到 1 条音轨。
 */
@Composable
private fun rememberTracksSnapshot(player: ExoPlayer): State<TracksSnapshot> {
    val state = remember(player) { mutableStateOf(parseTracks(player)) }
    LaunchedEffect(player) { state.value = parseTracks(player) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                state.value = parseTracksFromTracks(tracks, player)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return state
}

/**
 * 主动 poll 一份 snapshot(进入 Composable 时调,或 listener 第一帧前).
 * 比 parseTracksFromTracks 多读一次 player,但语义最简单。
 */
private fun parseTracks(player: ExoPlayer): TracksSnapshot {
    val tracks = player.currentTracks
    return parseTracksFromTracks(tracks, player)
}

/**
 * 核心解码:[Tracks] → [TracksSnapshot]。
 *
 *  - 按 Group.type 分 audio / text
 *  - Group.length=0(空轨)跳过
 *  - 选中态:group.isSelected && isTrackSelected(index)
 *  - label:Format.label → "音轨 N"(兜底)
 *  - textDisabled:从 TrackSelectionParameters 读 TRACK_TYPE_TEXT disabled 态
 */
private fun parseTracksFromTracks(tracks: Tracks, player: ExoPlayer): TracksSnapshot {
    val audios = mutableListOf<TrackOption>()
    var selectedAudio: Int? = null
    val texts = mutableListOf<TrackOption>()
    var selectedText: Int? = null

    tracks.groups.forEach { group ->
        if (group.length == 0) return@forEach
        when (group.type) {
            C.TRACK_TYPE_AUDIO -> {
                val mediaGroup = group.mediaTrackGroup
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    audios += TrackOption(
                        trackGroup = mediaGroup,
                        trackIndex = i,
                        label = fmt.label?.takeIf { it.isNotBlank() }
                            ?: "音轨 ${audios.size + 1}",
                    )
                    if (group.isTrackSelected(i)) selectedAudio = audios.lastIndex
                }
            }
            C.TRACK_TYPE_TEXT -> {
                val mediaGroup = group.mediaTrackGroup
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    texts += TrackOption(
                        trackGroup = mediaGroup,
                        trackIndex = i,
                        label = fmt.label?.takeIf { it.isNotBlank() }
                            ?: fmt.language?.takeIf { it.isNotBlank() }
                            ?: "字幕 ${texts.size + 1}",
                    )
                    if (group.isTrackSelected(i)) selectedText = texts.lastIndex
                }
            }
        }
    }
    val textDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
    return TracksSnapshot(
        audioTracks = audios,
        selectedAudioIndex = selectedAudio,
        textTracks = texts,
        selectedTextIndex = if (textDisabled) null else selectedText,
        textDisabled = textDisabled,
    )
}

/* ─────────────────── 倍速 4 档循环 ─────────────────── */
// speedSteps + rememberSpeedCycler + speedLabel 已抽到 PreviewShared.kt。

/* ─────────────────── 顶部 toolbar ─────────────────── */

/**
 * 顶部工具栏(56dp 黑底):
 *  - 左侧 ← 返回 图标
 *  - 中间 文件名(titleMedium,maxLines=1,weight=1f)
 *  - 右侧 投屏(noop) / 更多(DropdownMenu,6 项:下载/重命名/移动/删除/分享/属性)
 *
 *  字幕入口已下沉到画布 chip 行的"字幕" chip([TextChipWithMenu],接 ExoPlayer 字幕轨真切换),
 *  不再在顶栏放 CC icon——避免两个"字幕"入口冲突(顶栏 noop vs chip 真功能)。
 *  "下载" 从原底部控制栏下移到更多菜单(用户偏好:用下拉收纳一次性操作,不放主控制条);
 *  重命名/移动/删除/分享/属性 本期 noop + ToastBus 提示。
 */
@Composable
private fun PreviewTopBar(
    name: String,
    onBack: () -> Unit,
    onDownload: () -> Unit,
) {
    var moreExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewIconBtn(Icons.Back, "返回", onClick = onBack)
        Text(
            text = name,
            color = Color.White,
            style = StarVaultTheme.typography.subtitle,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        PreviewIconBtn(Icons.Cast, "投屏") { ToastBus.info("投屏功能即将上线") }
        Box {
            PreviewIconBtn(Icons.More, "更多", onClick = { moreExpanded = true })
            // 6 项 DropdownMenu,深色背景,白文字,带分隔线
            MoreMenu(
                expanded = moreExpanded,
                onDismiss = { moreExpanded = false },
                onDownload = {
                    moreExpanded = false
                    onDownload()
                },
                onRename = { moreExpanded = false; ToastBus.info("重命名即将上线") },
                onMove = { moreExpanded = false; ToastBus.info("移动即将上线") },
                onDelete = { moreExpanded = false; ToastBus.info("删除即将上线") },
                onShare = { moreExpanded = false; ToastBus.info("分享即将上线") },
                onProperties = { moreExpanded = false; ToastBus.info("查看属性即将上线") },
            )
        }
    }
}

/**
 * "更多" DropdownMenu:6 项 + 第 1/2 项间分隔线。
 *
 *  - 下载:从原底部控制栏下移;真接 [com.starvault.data.repository.DownloadRepository.enqueue]
 *  - 重命名 / 移动 / 删除 / 分享 / 属性:本期 noop,均 ToastBus 提示
 *
 *  MoreMenu + MoreMenuItem 已抽到 PreviewShared.kt(VIDEO / AUDIO 屏族共享)。
 */
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
 *  - 右上 4 chip(qualityChip / 倍速 / 原声 | 字幕,均 50% 黑底统一)
 *    字幕/清晰度 ≥2 档时 chip 可点开 DropdownMenu 切轨或切档)
 *  - 单击画布 = 切换 play/pause(对齐 ExoPlayer PlayerView 默认行为)
 *
 *  原声 / 字幕 chip 行为:
 *  - 原声:1 条音轨 → 不可点(只展示当前 label);≥2 条 → 点开 DropdownMenu 切轨
 *  - 字幕:无字幕轨 → 不可点(只展示 "字幕");有字幕轨 → 点开 DropdownMenu 切轨或"关闭字幕"
 */
@Composable
private fun PreviewCanvas(
    player: ExoPlayer,
    snapshot: PlayerSnapshot,
    qualityChip: String,
    qualityOptions: List<VideoM3u8>,
    currentQualityUrl: String,
    speedChip: String,
    tracks: TracksSnapshot,
    onTogglePlay: () -> Unit,
    onSelectQuality: (VideoM3u8) -> Unit,
    onSelectAudioTrack: (TrackOption) -> Unit,
    onSelectTextTrack: (TrackOption) -> Unit,
    onDisableText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            // 单击视频画布(非 chip 行)= 切换播放/暂停
            // 对齐 ExoPlayer PlayerView 默认行为。子元素(chip 行)的 clickable
            // 会先消费 pointer event,不会冒泡到这里误触发。
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTogglePlay() })
            },
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

        // 右上:chip 行。quality + speed(固定) + audio chip + text chip
        // 每行水平排列,横向 6dp 间隔,顶部 12dp + 右侧 12dp 边距
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
            AudioChipWithMenu(
                tracks = tracks.audioTracks,
                selectedIndex = tracks.selectedAudioIndex,
                onSelect = onSelectAudioTrack,
            )
            TextChipWithMenu(
                tracks = tracks.textTracks,
                selectedIndex = tracks.selectedTextIndex,
                textDisabled = tracks.textDisabled,
                onSelect = onSelectTextTrack,
                onDisable = onDisableText,
            )
        }

        // 中心 play 圆已移除:画布整体 .pointerInput { detectTapGestures(onTap) }
        // 已接管播放/暂停切换(对齐 ExoPlayer PlayerView 默认行为),center 圆冗余
    }
}

/** 半透明 chip:所有 chip 统一 50% 黑底(用户反馈:清晰度不再用蓝底强调,4 chip 视觉一致)。 */
@Composable
private fun PreviewChip(text: String, accent: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
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
 *  - [currentDesc]: 50% 黑底 chip 显示当前 desc(如 "1080P",空 → UI fallback "—")
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
        // 跟其他 chip 一致用 50% 黑底(用户反馈:清晰度 chip 不再强调,跟 倍速/原声/字幕 一致)
        val chipModifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
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
        // 显式 modifier.background 覆盖 M3 默认白底 surface,跟黑底 Preview 屏融合
        DropdownMenu(
            expanded = expanded && isInteractive,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1A1A)),
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

/**
 * 原声 / 音轨 chip + DropdownMenu 切轨。
 *
 *  - 0 条音轨 → 不可点 + 显示 "原声"(理论上不会有,ExoPlayer 必有 audio;纯防御)
 *  - 1 条音轨 → 不可点 + 显示 "原声"(没切换意义,不展示"音轨 1"这种技术性 label)
 *  - ≥2 条 → 点开 DropdownMenu 切换,选中态打勾,chip 显示当前选中轨 label
 *
 *  chip 配色:4 chip(清晰度/倍速/原声/字幕)统一 50% 黑底,无视觉等级差异。
 */
@Composable
private fun AudioChipWithMenu(
    tracks: List<TrackOption>,
    selectedIndex: Int?,
    onSelect: (TrackOption) -> Unit,
) {
    val currentLabel = if (tracks.size > 1) {
        selectedIndex?.let { tracks.getOrNull(it)?.label } ?: "原声"
    } else {
        "原声"
    }
    var expanded by remember { mutableStateOf(false) }
    val isInteractive = tracks.size > 1

    Box {
        val chipModifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .then(
                if (isInteractive) Modifier.clickable { expanded = true }
                else Modifier,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        Box(modifier = chipModifier) {
            Text(
                text = currentLabel,
                style = StarVaultTheme.typography.micro,
                color = Color.White,
            )
        }
        if (isInteractive) {
            // 显式 modifier.background 覆盖 M3 默认白底 surface,跟黑底 Preview 屏融合
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1A1A1A)),
            ) {
                tracks.forEachIndexed { idx, opt ->
                    val isCurrent = idx == selectedIndex
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt.label,
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
}

/**
 * 字幕 chip + DropdownMenu 切轨。
 *
 *  - 0 字幕轨 → 显示 "字幕",不可点(媒体本身没字幕,菜单没意义)
 *  - 有字幕轨但用户主动关闭 → 显示 "字幕",菜单里有"开启"项
 *  - 有字幕轨 + 选中态 → 显示当前字幕 label,菜单有"关闭字幕"项
 *
 *  设计取舍:即使 0 字幕轨也保留 chip,给用户视觉一致性(顶栏 4 chip 数量稳定);
 *  不可点只是不响应 click,不影响显示。
 */
@Composable
private fun TextChipWithMenu(
    tracks: List<TrackOption>,
    selectedIndex: Int?,
    textDisabled: Boolean,
    onSelect: (TrackOption) -> Unit,
    onDisable: () -> Unit,
) {
    val currentLabel: String = when {
        tracks.isEmpty() -> "字幕"
        textDisabled -> "字幕"
        else -> selectedIndex?.let { tracks.getOrNull(it)?.label } ?: "字幕"
    }
    var expanded by remember { mutableStateOf(false) }
    val isInteractive = tracks.isNotEmpty()

    Box {
        val chipModifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .then(
                if (isInteractive) Modifier.clickable { expanded = true }
                else Modifier,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        Box(modifier = chipModifier) {
            Text(
                text = currentLabel,
                style = StarVaultTheme.typography.micro,
                color = Color.White,
            )
        }
        if (isInteractive) {
            // 显式 modifier.background 覆盖 M3 默认白底 surface,跟黑底 Preview 屏融合
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1A1A1A)),
            ) {
                tracks.forEachIndexed { idx, opt ->
                    val isCurrent = idx == selectedIndex && !textDisabled
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt.label,
                                color = if (isCurrent) Color(0xFF2F6FEB) else Color.White,
                                style = StarVaultTheme.typography.body,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(opt)
                        },
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (textDisabled) "开启字幕" else "关闭字幕",
                            color = Color.White,
                            style = StarVaultTheme.typography.body,
                        )
                    },
                    onClick = {
                        expanded = false
                        onDisable()
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
 *  - 6 圆按钮:❤️ / 上一集 / 播放(白底实心) / 下一集 | 倍速(label 可点) / 全屏
 *
 *  之前这里有 5 圆按钮;新增 ❤️ 在最左(用户指示:和播放按钮同一行最左)。
 *  上一集/下一集:有 prev/next 时真接 [onPrev]/[onNext](Route 跳到兄弟文件);
 *  没 prev/next 时(单文件/无 parentCid/已是首尾集) → ToastBus.info 提示。
 *  ❤️ 始终可点:星标不影响视频播放,只通过 [onToggleStar] 回调 VM.toggleStar。
 */
@Composable
private fun PreviewControls(
    snapshot: PlayerSnapshot,
    speedLabel: String,
    isFullscreen: Boolean,
    hasPrev: Boolean,
    hasNext: Boolean,
    isStarred: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleStar: () -> Unit,
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
        // 6 圆按钮(❤️ + 上一集/播放/下一集 | 倍速/全屏)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 左:❤️ / 上一集 / 播放 / 下一集 — ❤️ 在最左,跟用户指示"和播放按钮同一行最左"对齐
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewCtrlBtn(
                    icon = if (isStarred) Icons.HeartFilled else Icons.HeartOutline,
                    contentDescription = if (isStarred) "已收藏" else "收藏",
                    onClick = onToggleStar,
                )
                PreviewCtrlBtn(
                    icon = Icons.Prev,
                    contentDescription = "上一集",
                    enabled = hasPrev,
                    onClick = {
                        if (hasPrev) onPrev() else ToastBus.info("已是第一集")
                    },
                )
                PreviewCtrlBtn(
                    icon = if (snapshot.isPlaying) Icons.Pause else Icons.Play,
                    contentDescription = "播放",
                    onClick = onTogglePlay,
                    isPrimary = true,
                )
                PreviewCtrlBtn(
                    icon = Icons.Next,
                    contentDescription = "下一集",
                    enabled = hasNext,
                    onClick = {
                        if (hasNext) onNext() else ToastBus.info("已是最后一集")
                    },
                )
            }
            // 右:倍速(label 可点循环) / 全屏
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
 * 可拖动 seek 进度条 + 控制行圆按钮:已抽到 PreviewShared.kt(PreviewSeekBar / PreviewCtrlBtn)。
 */
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
// millisToClock / formatFileSize / formatDate 已抽到 PreviewShared.kt。

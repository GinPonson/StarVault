package com.starvault.ui.preview

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil3.compose.AsyncImage
import com.starvault.component.Icons
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.theme.StarVaultTheme
import okhttp3.OkHttpClient

/**
 * PreviewAudio 全屏屏幕（白底黑字音乐播放器，唱碟旋转）。
 *
 *  结构（垂直 Column 堆叠，白底黑字）：
 *  ┌─ PreviewTopBarLite   56dp 白底: ← 返回 / 文件名 / 更多
 *  ├─ VinylDisc           中段圆形唱碟（缩略图或音乐图标占位，播放时无限旋转 8s/圈）
 *  ├─ 标题 + 副标题        黑文件名 / 灰色 size · mtime
 *  ├─ PreviewSeekBar      PreviewShared 复用
 *  ├─ 时间文本             黑字 mm:ss / mm:ss
 *  └─ AudioControls       5 圆按钮：❤️ / Prev / Play(中央 72dp) / Next / Playlist
 *
 *  设计取舍：
 *  - 不接倍速/全屏：音频是听感优先，倍速/全屏语义弱，删 2 按钮让控制栏聚焦核心 4 动作 + ❤️
 *  - 5 按钮 SpaceEvenly 横向均分,Play 在第 3 位 → 视觉居中(用户指示)
 *  - 播放键居中 72dp：黑底白 ▶ icon，与左右 36dp Prev/Next/❤️/Playlist 形成"中间大、两侧小"层级
 *  - 唱碟圆形 + 旋转动效：唱碟旋转是 vinyl 物理直觉，旋转速度对应播放中状态（暂停则停转）
 *  - 整屏白底黑字：跟项目 Files/Search 等亮色屏一致,PreviewVideo 黑底专属于"视频沉浸式"
 *  - ❤️ 在最左：跟视频屏"播放按钮同一行最左"对齐(用户指示)；
 *    切换用 PreviewCtrlBtn 36dp + tint=c.fg(白屏黑 icon),fill/outline 由 isStarred 决定
 *
 *  @param state         PreviewUiState.Loading / Success(metadata, mediaUrl, "", [])
 *  @param siblings      上一首/下一首（VM.loadSiblings 异步拉取）
 *  @param playlist      同父目录 audio 完整列表(VM.playlist StateFlow),供"文件列表"ModalBottomSheet 用
 *  @param isStarred     当前星标状态,用于 ❤️/♡ 切换(VM.isStarred StateFlow 注入)
 *  @param onBack        NavHost popBackStack
 *  @param onPrev        上一首(fileId 由 Route 注入 → nav.navigate 新 PreviewAudio)
 *  @param onNext        下一首
 *  @param onToggleStar  点 ❤️ 触发(VM.toggleStar:乐观更新 + 失败回滚)
 *  @param onSelectFromPlaylist 从播放列表点选(fileId 由 Route 注入 → nav.navigate 新 PreviewAudio)
 *  @param onSavePosition 5s 节流 + onDispose 兜底,把 player.currentPosition 写盘
 */
@Composable
fun PreviewAudioScreen(
    state: PreviewUiState,
    siblings: PreviewAudioViewModel.Siblings = PreviewAudioViewModel.Siblings(),
    playlist: List<ParsedFileItem> = emptyList(),
    isStarred: Boolean = false,
    onBack: () -> Unit,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onToggleStar: () -> Unit = {},
    onSelectFromPlaylist: (String) -> Unit = {},
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
            is PreviewUiState.Loading -> AudioLoadingBlock()
            is PreviewUiState.Success -> AudioContent(
                state = state,
                siblings = siblings,
                playlist = playlist,
                isStarred = isStarred,
                onBack = onBack,
                onPrev = onPrev,
                onNext = onNext,
                onToggleStar = onToggleStar,
                onSelectFromPlaylist = onSelectFromPlaylist,
                onSavePosition = onSavePosition,
            )
        }
    }
}

/**
 * Audio Content 主屏（Success 分支）。
 *
 *  - ExoPlayer 实例 `remember(state.mediaUrl)`：URL 变 → 重新 prepare
 *  - 错误监听：onPlayerError → ToastBus 一次性提示
 *  - 释放：onDispose { player.release() } 防泄漏
 *  - 10Hz snapshot + seek + play/pause toggle 全部复用 PreviewShared
 */
@Composable
private fun AudioContent(
    state: PreviewUiState.Success,
    siblings: PreviewAudioViewModel.Siblings,
    playlist: List<ParsedFileItem>,
    isStarred: Boolean,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleStar: () -> Unit,
    onSelectFromPlaylist: (String) -> Unit,
    onSavePosition: (Long) -> Unit,
) {
    val context = LocalContext.current
    val t = StarVaultTheme.typography
    val c = StarVaultTheme.colors

    val player = remember(state.mediaUrl) {
        if (state.mediaUrl.isBlank()) null else buildAudioPlayer(context, state.mediaUrl)
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                ToastBus.error(error.message ?: "音频播放失败")
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    // 5. 恢复播放位置：player ready 后立刻 seekTo 到上次的 positionMs
    //    state.resumePositionMs 由 VM 在 emit Success 之前从 AudioPositionStore 读出
    //    只在第一次 emit 时 seek 一次：LaunchedEffect(player, state.resumePositionMs)
    //    player 重建（如切 URL）后会重走,resumePositionMs 仍是 VM 当前 Success 里的值
    val resumePositionMs = state.resumePositionMs
    LaunchedEffect(player) {
        if (player != null && resumePositionMs > 0) {
            player.seekTo(resumePositionMs.toLong())
        }
    }

    // 6. 节流保存播放位置（5s 一次）：player.currentPosition 写到 AudioPositionStore
    //    通过 onSavePosition 回调给 VM（VM 内 viewModelScope.launch 调 positionStore.save）
    //    暂停/退出屏时 player.currentPosition 仍可读,所以节流协程能持续跑
    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            delay(5_000L)
            onSavePosition(player.currentPosition)
        }
    }

    // 7. 离屏兜底：onDispose 时 player 还在,last position 写一次（防快速返回丢进度）
    //    必须放在 DisposableEffect(player) 之后,onDispose 顺序由 Compose 倒序执行
    DisposableEffect(player) {
        onDispose {
            player?.let { onSavePosition(it.currentPosition) }
        }
    }

    val snapshot: PlayerSnapshot = if (player != null) {
        val s by rememberPlayerSnapshot(player)
        s
    } else {
        PlayerSnapshot()
    }

    val onTogglePlay: () -> Unit = {
        if (player != null) player.playWhenReady = !player.playWhenReady
    }

    val onSeek: (Int) -> Unit = { ms -> player?.seekTo(ms.toLong()) }

    // 8. 播放列表 ModalBottomSheet 显隐:AudioControls 第 5 按钮(Playlist)切换
    //    用 rememberSaveable 让 sheet 状态在配置变更(旋转屏/进程死)后保留
    var playlistVisible by rememberSaveable { mutableStateOf(false) }

    val onDownload: () -> Unit = remember(state) {
        {
            val m = state.metadata
            val item = ParsedFileItem(
                id = m.fid,
                parentId = "",
                name = m.name,
                ico = m.ico,
                sizeBytes = m.sizeBytes,
                mtimeSec = m.mtimeSec,
                pickCode = m.pickCode,
                isFolder = false,
                playLong = 0,
                sha1 = m.sha1,
                fileCategory = 3,                    // 115 webapi fc 约定：3=audio
                thumbnailUrl = m.thumbnailUrl,
            )
            ServiceLocator.downloadRepository.enqueue(item)
                .onSuccess { ToastBus.info("已加入下载队列") }
                .onFailure { ToastBus.error(it.message ?: "下载失败") }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PreviewTopBarLite(
            name = state.metadata.name,
            onBack = onBack,
            onDownload = onDownload,
        )

        // 中段圆形唱碟（aspectRatio 1f 强制正方形后再 clip 圆）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onTogglePlay() })
                },
            contentAlignment = Alignment.Center,
        ) {
            VinylDisc(
                thumbnailUrl = state.metadata.thumbnailUrl.ifBlank { null },
                isPlaying = snapshot.isPlaying,
            )
        }

        // 标题 + 副标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = state.metadata.name,
                color = c.fg,
                style = t.title,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatFileSize(state.metadata.sizeBytes)} · ${formatDate(state.metadata.mtimeSec)}",
                color = c.muted,
                style = t.micro,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))

        AudioControls(
            snapshot = snapshot,
            hasPrev = siblings.prevId != null,
            hasNext = siblings.nextId != null,
            isStarred = isStarred,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onPrev = onPrev,
            onNext = onNext,
            onToggleStar = onToggleStar,
            onShowPlaylist = { playlistVisible = true },
        )
    }

    // 9. 播放列表 ModalBottomSheet(手搓风格,跟 SortSheet / FolderSheet 对齐)
    //    渲染在 Column 外、Box 内,这样 sheet 上方的 scrim 不会盖住 AudioControls
    if (playlistVisible) {
        PlaylistSheet(
            currentFileId = state.metadata.fid,
            items = playlist,
            onPicked = { fid ->
                playlistVisible = false
                onSelectFromPlaylist(fid)
            },
            onDismiss = { playlistVisible = false },
        )
    }
}

/* ─────────────────── 顶栏 + 唱碟 ─────────────────── */

/**
 * 轻量顶栏：← 返回 / 文件名 / 更多（6 项 DropdownMenu 复用 PreviewShared.MoreMenu）。
 *
 *  白底黑字：跟 PreviewVideo 黑底区分（视频暗色沉浸，音频亮色日常）。
 *  DropdownMenu 仍走 PreviewShared.MoreMenu 黑底半透（dark surface 在亮色顶栏上更聚焦）。
 */
@Composable
private fun PreviewTopBarLite(
    name: String,
    onBack: () -> Unit,
    onDownload: () -> Unit,
) {
    val c = StarVaultTheme.colors
    var moreExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarIconBtn(Icons.Back, "返回", tint = c.fg, onClick = onBack)
        Text(
            text = name,
            color = c.fg,
            style = StarVaultTheme.typography.subtitle,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Box {
            TopBarIconBtn(Icons.More, "更多", tint = c.fg, onClick = { moreExpanded = true })
            MoreMenu(
                expanded = moreExpanded,
                onDismiss = { moreExpanded = false },
                onDownload = { moreExpanded = false; onDownload() },
                onRename = { moreExpanded = false; ToastBus.info("重命名即将上线") },
                onMove = { moreExpanded = false; ToastBus.info("移动即将上线") },
                onDelete = { moreExpanded = false; ToastBus.info("删除即将上线") },
                onShare = { moreExpanded = false; ToastBus.info("分享即将上线") },
                onProperties = { moreExpanded = false; ToastBus.info("查看属性即将上线") },
            )
        }
    }
}

/** 顶栏 40dp 圆按钮（无背景 + tint 色 icon），跟 PreviewVideoScreen.PreviewIconBtn 形态一致但 tint 由调用方控制。 */
@Composable
private fun TopBarIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
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
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 黑胶唱碟（圆形 + 播放时无限旋转 8s/圈）。
 *
 *  视觉分层（从外到内）：
 *  - 12dp shadow + 圆形 clip：让唱碟在白底屏上有立体感
 *  - 外圈深灰底 0xFF111111：黑胶颜色
 *  - 中心圆形 thumbnail（padding 28dp 留出黑胶外圈）或音乐图标占位
 *  - 中心 16dp 黑色小圆：模拟唱片中心孔
 *
 *  旋转动效（仅 [isPlaying] 时启动）：
 *  - rememberInfiniteTransition + animateFloat 0°→360°
 *  - tween 8000ms LinearEasing：1 圈 8 秒，跟 33⅓ RPM 黑胶近似（真黑胶 1.8s/圈,这里放慢让旋转有视觉缓冲不晕)
 *  - 暂停时通过 transition.targetValue 切到 0f(不旋转)：
 *      实现方式：把 isPlaying 用 key 区分 transition —— 不行,会重建。
 *      实际写法:用 animateFloat 的初始值 vs 目标值都设 0f,然后 isPlaying 时让 progress 走完;
 *      或者:外部用 modifier.graphicsLayer { rotationZ = if (isPlaying) animatedDeg else 0f }。
 *      这里采用第二种,不重建 transition,只决定是否应用动画值。
 */
@Composable
private fun VinylDisc(
    thumbnailUrl: String?,
    isPlaying: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "vinyl-spin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinyl-rotation",
    )
    val angle = if (isPlaying) rotation else 0f

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF111111))
            .graphicsLayer { rotationZ = angle },
    ) {
        // 中心圆形 thumbnail（占满 padding 28dp → 中心圆形图）
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Music,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        // 中心孔（16dp 黑圆，叠在 thumbnail 中心；不旋转单独叠加避免 graphicsLayer 把它一起转）
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF000000)),
        )
    }
}

/* ─────────────────── 控制行（3 圆按钮，播放键居中）─────────────────── */

/**
 * 进度条 + 时间 + 5 圆按钮(点赞 / 上一首 / 暂停·播放 / 下一首 / 文件列表, 播放键居中且最大)。
 *
 *  - 点赞 / 上一首 / 下一首 / 文件列表 用 PreviewCtrlBtn 36dp 默认尺寸,黑 icon 透明底
 *  - 播放/暂停 用自定义 BigPlayButton 72dp：黑底 + 白 ▶ icon(亮色屏面下视觉重量更强)
 *  - 进度条 PreviewShared 复用：黑灰底 + accent 蓝填充 + 白 thumb
 *  - 时间文本用 StarVaultTheme.typography.micro + c.muted
 *  - 5 按钮用 SpaceEvenly 横向均分;播放/暂停在 Row 顺序第 3 位 → 视觉居中(用户指示)
 *  - ❤️ fill/outline 由 isStarred 决定:已星标=实心填充,未星标=空心描边(用 c.fg 黑)
 *  - 文件列表(≡+♪)MVP 阶段 noop,ToastBus.info("即将上线") 兜底 —— 真要展开播放列表需另起
 *    bottom sheet 容器,不在本任务范围
 */
@Composable
private fun AudioControls(
    snapshot: PlayerSnapshot,
    hasPrev: Boolean,
    hasNext: Boolean,
    isStarred: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleStar: () -> Unit,
    onShowPlaylist: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
    ) {
        PreviewSeekBar(
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
            bufferedMs = snapshot.bufferedMs,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = millisToClock(snapshot.positionMs),
                style = t.micro,
                color = c.muted,
            )
            Text(
                text = millisToClock(snapshot.durationMs),
                style = t.micro,
                color = c.muted,
            )
        }
        Spacer(Modifier.height(28.dp))
        // 5 圆按钮：❤️ / Prev(36dp 黑 icon) / Play(72dp 黑底白 icon 中央) / Next(36dp 黑 icon) / Playlist(36dp)
        // 顺序严格按用户指示:点赞 | 上一首 | 暂停/播放 | 下一首 | 文件列表
        // SpaceEvenly 让 Play 在 Row 第 3 位 → 视觉居中
        // Play 用黑底白 icon 而非 PreviewCtrlBtn 的白底黑 icon —— 亮色屏面下黑底白 icon 视觉重量
        // 比白底黑 icon 强,中央层级更突出
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PreviewCtrlBtn(
                icon = if (isStarred) Icons.HeartFilled else Icons.HeartOutline,
                contentDescription = if (isStarred) "已收藏" else "收藏",
                tint = c.fg,
                onClick = onToggleStar,
            )
            PreviewCtrlBtn(
                icon = Icons.Prev,
                contentDescription = "上一首",
                enabled = hasPrev,
                tint = c.fg,
                onClick = {
                    if (hasPrev) onPrev() else ToastBus.info("已是单文件")
                },
            )
            BigPlayButton(
                isPlaying = snapshot.isPlaying,
                onClick = onTogglePlay,
            )
            PreviewCtrlBtn(
                icon = Icons.Next,
                contentDescription = "下一首",
                enabled = hasNext,
                tint = c.fg,
                onClick = {
                    if (hasNext) onNext() else ToastBus.info("已是单文件")
                },
            )
            PreviewCtrlBtn(
                icon = Icons.Playlist,
                contentDescription = "文件列表",
                tint = c.fg,
                onClick = onShowPlaylist,
            )
        }
    }
}

/**
 * 中央 72dp 播放键（黑底 + 白 icon）。
 *
 *  不复用 PreviewShared.PreviewCtrlBtn(isPrimary=true) —— 后者是白底黑 icon（为黑底屏设计），
 *  亮色屏面下视觉重量不够。这里独立实现：黑底 + 白 icon + 轻微 shadow 让按钮在白屏上有立体感。
 */
@Composable
private fun BigPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF111111))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Pause else Icons.Play,
            contentDescription = "播放",
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

/* ─────────────────── Loading ─────────────────── */

/** 全屏 spinner（白底，accent 色）。 */
@Composable
private fun AudioLoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarVaultTheme.colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = StarVaultTheme.colors.accent)
    }
}

/* ─────────────────── ExoPlayer 工厂 ─────────────────── */

/**
 * 构造 ExoPlayer + OkHttpDataSource.Factory（115 Bearer 拦截）+ ProgressiveMediaSource。
 *
 *  PreviewVideo 用 HlsMediaSource（m3u8 段）；PreviewAudio 用 ProgressiveMediaSource（mp3/flac 单文件）。
 */
private fun buildAudioPlayer(context: Context, mediaUrl: String): ExoPlayer {
    val okHttpClient: OkHttpClient = ServiceLocator.okHttpClient
    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0")
    val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
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

/* ─────────────────── 播放列表 ModalBottomSheet ─────────────────── */

/**
 * 音频播放列表 BottomSheet（手搓 ModalBottomSheet 风格，对齐 [SortSheet] / `FolderSheet`）。
 *
 *  - 半透明 scrim(黑 0.4 alpha)+ 圆角顶 28dp 主体，跟项目其他 sheet 视觉一致
 *  - LazyColumn 列同父目录 audio 文件；当前 fid 用 `▶` 前缀 + 加粗高亮
 *  - 点选 → 调 [onPicked](fid) → Route 走 `nav.navigate(Route.PreviewAudio(...))` 切换
 *  - 空 list(Search 入口 / 拉失败) → 显示"暂无播放列表"占位
 *  - 标题显示 {N} 首,跟 FilesScreen type tab 数量徽标风格统一
 *
 *  用 [rememberLazyListState] 让 sheet 滚动位置在配置变更后保留
 *  初次显示时 [LaunchedEffect] 滚到当前 fid,避免 50 首歌从第 1 首开始要滑很久
 *
 *  @param currentFileId  当前正在播放的 fid,用于高亮 + 初始滚动定位
 *  @param items          父目录 audio 完整列表(已按 name asc,已过滤 audio)
 *  @param onPicked       用户点某项(fileId)→ 关 sheet + Route 切歌
 *  @param onDismiss      点 scrim / 系统返回 → 关 sheet
 */
@Composable
private fun PlaylistSheet(
    currentFileId: String,
    items: List<ParsedFileItem>,
    onPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    val listState = rememberLazyListState()

    // 初次进入 sheet:滚到当前 fid 所在行,避免从顶部 1 开始
    val initialIndex = remember(items) { items.indexOfFirst { it.id == currentFileId }.coerceAtLeast(0) }
    LaunchedEffect(items) {
        if (items.isNotEmpty()) listState.scrollToItem(initialIndex)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明 scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
        )

        // Sheet 主体(贴底,圆角顶,半屏高以容纳 5-7 行 + 标题)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(540.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(c.surface)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            // grabber
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFC4C4C4)),
            )
            Spacer(Modifier.height(20.dp))

            // 标题:"播放列表" + 数量
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
                    text = "共 ${items.size} 首",
                    color = c.muted,
                    style = t.micro,
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = c.border, thickness = 1.dp)

            if (items.isEmpty()) {
                // 空态(Search 入口 / 拉取失败)
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
                    items(items, key = { it.id }) { item ->
                        PlaylistRow(
                            item = item,
                            isCurrent = item.id == currentFileId,
                            onClick = { onPicked(item.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 播放列表单行:左侧 thumbnail 36dp(无则显示 Music icon)+ 文件名 + 副标题(size · duration)+ 右侧 ▶ 当前标记。
 */
@Composable
private fun PlaylistRow(
    item: ParsedFileItem,
    isCurrent: Boolean,
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
        // 左侧 thumbnail / fallback icon
        if (item.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1A1A1A)),
                contentScale = ContentScale.Crop,
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
                    imageVector = Icons.Music,
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
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = c.muted,
                style = t.micro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Playlist, // ≡♪ 复用,小尺寸表示"在列表中"
                contentDescription = "正在播放",
                tint = c.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** playLong(秒) → "04:23" 或 "01:30:08";<=0 → ""(空字符串,subtitle 拼接自动跳过)。 */
private fun formatPlayLong(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
package com.starvault.ui.preview

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.starvault.core.ServiceLocator
import com.starvault.theme.StarVaultTheme
import okhttp3.OkHttpClient

/**
 * PreviewVideo 全屏屏幕（浅色主题 + 浅色顶/底栏）。
 *
 *  视觉：
 *  - 屏背景：`StarVaultTheme.colors.bg` (#FAFAFA) — 不用黑色
 *  - 视频区：Media3 PlayerView 自带黑底（这是 ExoPlayer 控件系统配色，不可改，
 *    行业标准 letterbox；不放大到顶 / 底栏区域时也不会被用户看到，所以视觉仍是浅色）
 *  - 顶部信息栏：surface 白底（不透明）+ 1dp `border` 分隔线 + 文件名 + 元数据
 *  - 底部操作栏：surface 白底 + 1dp `border` 分隔线 + 三等宽 OutlinedButton
 *  - 顶 / 底栏共用 [uiVisible]，[AnimatedVisibility] fadeIn/fadeOut 切换
 *
 *  Media3 + 115 Bearer 桥接：
 *  - 用 [OkHttpDataSource.Factory] 包装 [ServiceLocator.okHttpClient]，由拦截器自动注入 115 Bearer + 浏览器伪装头
 *  - Media3 拉 m3u8 主索引 + 分片都自动带 115 Bearer
 *  - ExoPlayer 实例由 Composable 持有，dispose 时 [Player.release] 防泄漏
 *
 *  状态机：错误回调 → 自定义 error slot 显示。
 *
 *  @param state VM 暴露的 PreviewUiState
 *  @param onBack 返回（屏内 BackHandler + 顶部"返回"按钮都调它）
 */
@Composable
fun PreviewVideoScreen(
    state: PreviewUiState,
    onBack: () -> Unit,
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
            is PreviewUiState.Error   -> ErrorBlock(message = state.message, onBack = onBack)
            is PreviewUiState.Success -> VideoContent(
                state = state,
                onBack = onBack,
            )
        }
    }
}

/* ─────────────────── 视频内容 ─────────────────── */

/**
 * 视频播放器。ExoPlayer 由 Composable 持有，dispose 时 release。
 *
 *  Player 创建：每次 [state.metadata.fid] 或 [state.mediaUrl] 变化重新 prepare
 *  - 这样同一 Preview 屏被复用（compose 重组但 PlayerView 不重建）时也能正确切源
 *
 *  关键依赖：
 *  - [OkHttpDataSource.Factory] 复用 ServiceLocator 的 [OkHttpClient]，让 Media3 拉的
 *    m3u8 主索引 / 分片都带 115 Bearer（115 m3u8 签名 URL 必须登录态）
 *
 *  顶 / 底栏通过 BoxScope 扩展函数（[TopInfoBar] / [BottomActionBar]）挂在外层 Box 的
 *  TopCenter / BottomCenter，这是 [BoxScope.align] 唯一能用的调用场景。
 */
@Composable
private fun VideoContent(
    state: PreviewUiState.Success,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiVisible = remember { mutableStateOf(true) }

    // 1. ExoPlayer 实例：依赖 ServiceLocator.okHttpClient，由 115 Bearer 拦截器自动注入 token
    val player = remember(state.mediaUrl) {
        val okHttpClient: OkHttpClient = ServiceLocator.okHttpClient
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0")
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(state.mediaUrl))
                playWhenReady = true
                prepare()
            }
    }

    // 2. 错误监听：Media3 拉流失败时回 VM 通知（这里只用本地状态显示）
    var playerError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.message ?: "视频播放失败"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    // 3. 释放：Composable 离屏时 release ExoPlayer（防泄漏）
    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    useController = true          // 显示自带控件（play/pause/seekbar/10s skip）
                    controllerHideOnTouch = true   // 点击后自动隐藏
                    setShowSubtitleButton(false)   // 不要字幕切换（115 m3u8 多音轨由 URL 参数控制）
                    setShowFastForwardButton(true) // 显式开 10s skip
                    setShowNextButton(false)       // 单视频，不要下一首
                    setShowPreviousButton(false)
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
            },
        )

        // 错误覆盖层（盖在 PlayerView 上面）
        if (playerError != null) {
            ErrorBlock(message = playerError!!, onBack = onBack)
        }

        // 顶 / 底信息栏：参考 Google Photos 风格（白底 vs 视频黑底强对比）
        TopInfoBar(
            visible = uiVisible.value,
            fileName = state.metadata.name,
            onBack = onBack,
        )

        BottomActionBar(
            visible = uiVisible.value,
            onShare = { /* TODO: 调 AndroidSharesheet 分享视频 */ },
            onFavorite = { /* TODO: 调收藏 toggle */ },
            onDelete = { /* TODO: 调 115 files/delete */ },
        )
    }
}

/* ─────────────────── 顶 / 底 浮层（与 PreviewImage 完全一致） ─────────────────── */

/**
 * 顶部信息栏（参考 Google Photos lightbox 风格）。
 *
 *  - surface 白底（不透明），与下方黑底视频形成"白底 vs 黑底"强对比（明显分割）
 *  - 不用 1dp 灰线 —— 靠"白底栏 vs 黑底视频"的色差自然分割（Photos 同款）
 *  - 左侧 ← 返回 icon + 中间文件名 `titleMedium` 居中（maxLines=1）
 *  - 右侧留白占位
 *  - 8dp 横向 padding + 8dp 纵向 padding（更紧凑，Photos 同款）
 *  - 作为 BoxScope 扩展，钉到外层 Box 的 [Alignment.TopCenter]
 */
@Composable
private fun BoxScope.TopInfoBar(
    visible: Boolean,
    fileName: String,
    onBack: () -> Unit,
) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(c.surface),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .pointerInput(Unit) { detectTapGestures(onTap = { onBack() }) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_revert),
                        contentDescription = "返回",
                        tint = c.fg,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = fileName,
                    color = c.fg,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
                Box(modifier = Modifier.size(40.dp))
            }
        }
    }
}

/**
 * 底部操作栏（参考 Google Photos lightbox 风格）。
 *
 *  - surface 白底（不透明），与上方黑底视频形成"白底 vs 黑底"强对比（明显分割）
 *  - 不用 1dp 灰线 —— 靠色差自然分割（Photos 同款）
 *  - 16dp padding
 *  - 三等宽 [OutlinedButton]（`weight(1f)`），border 边 + fg 文字 + 透明容器
 *  - 按钮间 8dp 间距
 *  - 作为 BoxScope 扩展，钉到外层 Box 的 [Alignment.BottomCenter]
 */
@Composable
private fun BoxScope.BottomActionBar(
    visible: Boolean,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(c.surface),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedActionButton(
                    iconRes = android.R.drawable.ic_menu_share,
                    label = "分享",
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                )
                OutlinedActionButton(
                    iconRes = android.R.drawable.btn_star_big_off,
                    label = "收藏",
                    onClick = onFavorite,
                    modifier = Modifier.weight(1f),
                )
                OutlinedActionButton(
                    iconRes = android.R.drawable.ic_menu_delete,
                    label = "删除",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 浅色 OutlinedButton 风格（border 边 + fg 文字 + 透明容器）。
 * 视频屏与图片屏共用，所以定义在 PreviewVideoScreen 里。
 */
@Composable
private fun OutlinedActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, c.border),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.fg),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = c.fg,
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, color = c.fg)
    }
}

/* ─────────────────── Loading / Error 共享块 ─────────────────── */

@Composable
private fun LoadingBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = StarVaultTheme.colors.accent)
    }
}

/**
 * 错误块（浅色主题：muted 灰 icon + muted 灰文字 + accent 实心按钮）。
 */
@Composable
private fun ErrorBlock(message: String, onBack: () -> Unit) {
    val c = StarVaultTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_report_image),
            contentDescription = null,
            tint = c.muted,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = c.muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .height(40.dp)
                .background(c.accent)
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = { onBack() }) },
            contentAlignment = Alignment.Center,
        ) {
            Text("返回", color = c.accentOn, fontSize = 14.sp)
        }
    }
}
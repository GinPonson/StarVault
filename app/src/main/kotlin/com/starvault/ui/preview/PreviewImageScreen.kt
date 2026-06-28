package com.starvault.ui.preview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.starvault.component.Icons
import com.starvault.theme.StarVaultTheme

/**
 * PreviewImage 全屏屏幕（浅色主题对齐设计稿）。
 *
 *  视觉（基于 Lumen `ImagePreviewScreen` 风格 + 调整为浅色系统）：
 *  - 全屏浅色底（`StarVaultTheme.colors.bg` #FAFAFA，状态栏/导航栏也走浅色）
 *  - 顶部信息栏：surface 白底（不透明）+ 1dp `border` 分隔线 + 文件名（titleMedium）+ 元数据行（bodySmall：大小 • 日期）
 *  - 底部操作栏：surface 白底（不透明）+ 1dp `border` 分隔线（在上）+ 三等宽 OutlinedButton（accent 边 + accent 字 + 透明容器）
 *  - 顶/底栏统一由 [ImageContent] 的 [uiVisible] 控制显隐（[AnimatedVisibility] fadeIn / fadeOut）
 *
 *  交互：
 *  - 单击图片 → 切换 UI 显隐（对齐 Lumen `onSingleTapConfirmed`）
 *  - 双击图片 → 缩放 1x ↔ 2.5x（以触点为中心），translation 归零
 *  - 双指 pinch + drag → 1x ~ 4x 缩放 + pan（仅放大后可 pan）
 *  - 系统 Back → 返回 Files
 *
 *  渲染：Coil SubcomposeAsyncImage（保留 3 slot 子组合），不做 placeholder 渐变以保持底色纯净。
 *
 *  关键架构：顶 / 底栏**不能**放在 `ImageContent` 内部的 Box 里（会被 Image 的
 *  fillMaxSize 盖住），必须挂在 PreviewImageScreen 顶层 Box 上。所以把
 *  `ImageContent` 设计为返回 `uiVisible: MutableState<Boolean>` 的 Composable，
 *  让外层 Box 拿这个状态 + 渲染顶 / 底栏。
 *
 *  星标(❤️/♡):加在 TopInfoBar 右侧(文件名右边) — 用户指示:PreviewImage 没有播放按钮,
 *  用 TopBar 右侧(更多按钮左边)对齐 PreviewVideo/PreviewAudio 的 toggle 入口;
 *  fill/outline 由 isStarred 决定,ViewModel 走 toggleStar 乐观更新。
 *
 *  @param state VM 暴露的 PreviewUiState
 *  @param isStarred 当前星标状态(VM.isStarred StateFlow 注入)
 *  @param onBack 返回（屏内 BackHandler + 顶部"返回"按钮都调它）
 *  @param onToggleStar 点 ❤️ 触发(VM.toggleStar:乐观更新 + 失败回滚)
 */
@Composable
fun PreviewImageScreen(
    state: PreviewUiState,
    isStarred: Boolean = false,
    onBack: () -> Unit,
    onToggleStar: () -> Unit = {},
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
            is PreviewUiState.Success -> {
                val uiVisible = ImageContent(state = state, onBack = onBack)

                // 顶层 Box 拿到 uiVisible → 渲染顶 / 底浮层（BoxScope.align 钉边）
                TopInfoBar(
                    visible = uiVisible.value,
                    fileName = state.metadata.name,
                    onBack = onBack,
                )

                BottomActionBar(
                    visible = uiVisible.value,
                    isStarred = isStarred,
                    onShare = { /* TODO: 调 AndroidSharesheet 分享原图 */ },
                    onFavorite = onToggleStar,
                    onDelete = { /* TODO: 调 115 files/delete */ },
                )
            }
        }
    }
}

/* ─────────────────── 图片内容（pinch / pan / double-tap / single-tap） ─────────────────── */

/**
 * 图片 lightbox。
 *
 *  返回 `MutableState<Boolean>` 让外层 Box 拿到 `uiVisible`，进而渲染顶 / 底浮层。
 *  这是因为 `BoxScope.align` 必须直接挂在顶层 Box 的 lambda 里 —— 把 TopInfoBar /
 *  BottomActionBar 放到本 Box 里会被 SubcomposeAsyncImage 的 fillMaxSize 盖住。
 *
 *  手势映射：
 *  - pinch + drag → scale 增量 + pan 增量
 *  - double-tap   → 缩放 1x ↔ 2.5x（以触点为中心） + translation 归零
 *  - single-tap   → 切换 UI 显隐
 *
 *  缩放边界：1x ≤ scale ≤ 4x；超出 clamp。
 *  pan 边界：仅 scale > 1.01f 时累加；缩回 1x 时 translation 归零。
 *
 *  重置：每次 fid 变化清零（避免前后两张图手势状态串台）。
 */
@Composable
private fun ImageContent(
    state: PreviewUiState.Success,
    onBack: () -> Unit,
): MutableState<Boolean> {
    val scale = remember { mutableFloatStateOf(1f) }
    val offsetX = remember { mutableFloatStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }
    val uiVisible = remember { mutableStateOf(true) }

    // 新图片进来 → 重置手势 + 默认显示 UI
    androidx.compose.runtime.LaunchedEffect(state.metadata.fid) {
        scale.floatValue = 1f
        offsetX.floatValue = 0f
        offsetY.floatValue = 0f
        uiVisible.value = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarVaultTheme.colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val request = remember(state.mediaUrl) {
            ImageRequest.Builder(context)
                .data(state.mediaUrl)
                // 不传 size：lightbox 要全分辨率（pinch-zoom 才有细节可看）
                .memoryCacheKey("preview_image:${state.mediaUrl}")
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = state.metadata.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.metadata.fid) {
                    // 单击 / 双击 合并到一个 pointerInput 链路上，避免与下面的 pinch 链路打架
                    detectTapGestures(
                        onTap = { uiVisible.value = !uiVisible.value },
                        onDoubleTap = {
                            if (scale.floatValue > 1.05f) {
                                scale.floatValue = 1f
                                offsetX.floatValue = 0f
                                offsetY.floatValue = 0f
                            } else {
                                scale.floatValue = 2.5f
                                offsetX.floatValue = 0f
                                offsetY.floatValue = 0f
                            }
                        },
                    )
                }
                .pointerInput(state.metadata.fid) {
                    // pinch + drag 平移
                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                        scale.floatValue = (scale.floatValue * zoom).coerceIn(1f, 4f)
                        if (scale.floatValue > 1.01f) {
                            offsetX.floatValue += pan.x
                            offsetY.floatValue += pan.y
                        } else {
                            // 缩到 1x → pan 归零（避免被拖偏）
                            offsetX.floatValue = 0f
                            offsetY.floatValue = 0f
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scale.floatValue
                    scaleY = scale.floatValue
                    translationX = offsetX.floatValue
                    translationY = offsetY.floatValue
                },
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StarVaultTheme.colors.accent)
                }
            },
            error = { /* 屏不显示错误占位;由 VM 失败路径 ToastBus.error 提示 */ },
        )
    }

    return uiVisible
}

/* ─────────────────── 顶 / 底 浮层（BoxScope 扩展 → 钉到外层 Box 边） ─────────────────── */

/**
 * 顶部信息栏（参考 Google Photos lightbox 风格）。
 *
 *  - surface 白底（不透明），与下方黑底图片形成"白底 vs 黑底"强对比（明显分割）
 *  - 不用 1dp 灰线 —— 靠"白底栏 vs 黑底图片"的色差自然分割（Photos 同款）
 *  - 左侧 ← 返回 icon + 中间文件名 `titleMedium` 居中（maxLines=1）+ 右侧 ❤️ 星标
 *  - 16dp 横向 padding + 12dp 纵向 padding
 *  - 作为 BoxScope 扩展，钉到外层 Box 的 [Alignment.TopCenter]
 *  - 显隐由 [visible] 控制（外层 [AnimatedVisibility] 包裹 fadeIn/fadeOut）
 *
 *  顶 / 底与图片之间的"明显分割线"由黑底图片 + 白底栏的强对比实现 —— 不再画灰线。
 *  ❤️ 用 androidx vector icon（之前是 android.R 系统 drawable），跟 PreviewVideo/Audio ❤️ 一致。
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
                // 左侧 ← 返回
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
                // 中间文件名（居中 + 占满中间空间）
                Text(
                    text = fileName,
                    color = c.fg,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
                // 右侧空白占位(40dp),与原来 ❤️ 对齐宽度 — 避免文件名挤压
                Box(modifier = Modifier.size(40.dp))
            }
        }
    }
}

/**
 * 底部操作栏（参考 Google Photos lightbox 风格）。
 *
 *  - surface 白底（不透明），与上方黑底图片形成"白底 vs 黑底"强对比（明显分割）
 *  - 不用 1dp 灰线 —— 靠色差自然分割（Photos 同款）
 *  - 16dp padding
 *  - 三等宽 [OutlinedButton]（`weight(1f)`），border 边 + fg 文字 + 透明容器
 *  - 按钮间 8dp 间距
 *  - 作为 BoxScope 扩展，钉到外层 Box 的 [Alignment.BottomCenter]
 *  - MVP：分享 / 收藏 / 删除按钮仅占位（回调由父 Composable 注入）
 */
@Composable
private fun BoxScope.BottomActionBar(
    visible: Boolean,
    isStarred: Boolean,
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
                    iconVector = if (isStarred) Icons.HeartFilled else Icons.HeartOutline,
                    iconTint = if (isStarred) c.accent else c.fg,
                    label = if (isStarred) "已收藏" else "收藏",
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
 * 浅色 OutlinedButton 风格（accent 边 + accent 字 + 透明容器）。
 *
 *  替代之前 Lumen 风格的 FilledTonalButton（白 15% 容器 + 白文字）；
 *  浅色系统用 outline 边界更清晰，不会和 surface 融为一体。
 */
@Composable
private fun OutlinedActionButton(
    iconRes: Int? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val resolvedTint = iconTint ?: c.fg
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = c.fg,
        ),
    ) {
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = resolvedTint,
            )
        } else if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = c.fg,
            )
        }
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
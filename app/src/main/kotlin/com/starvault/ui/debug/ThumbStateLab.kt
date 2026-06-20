package com.starvault.ui.debug

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.starvault.component.Icons
import com.starvault.theme.StarVaultTheme

/**
 * 缩略图四态对照屏（DEBUG 专用）。
 *
 * 每格 120dp 大尺寸 + 黑色 1dp 边框，方便肉眼对比四种渲染路径：
 *
 *  A) **真实 URL + 正常加载** → success → 真图
 *  B) **真实 URL + 故意失败** → error   → 深灰底 + 裂图 icon
 *  C) **thumbnailUrl = null** → design 渐变 fallback（橙色 IMAGE）
 *  D) **真实 URL + 慢速 URL** → loading → 极浅灰底（透出外层 Box）
 *
 * ⚠️ 当前默认 1×40dp 的 FileThumb 太小，肉眼难辨 loading/error；本屏放大到 120dp
 *    仅用于开发者对照，**不在生产中暴露**（仅 BuildConfig.DEBUG 引用）。
 *
 * 注：URL 都用 picsum.photos（公共测试图，无签名过期问题）。
 */
@Composable
fun ThumbStateLab() {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "缩略图四态对照（DEBUG）",
            style = t.title,
            color = c.fg,
        )
        Text(
            "每格 120dp。黑色 1dp 边框便于对比背景色。",
            style = t.caption,
            color = c.muted,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // A：picsum.photos 200×200 稳定图，模拟「成功加载」
            LabCell(label = "A) 成功\npicsum 200") {
                SubcomposeAsyncImage(
                    model = "https://picsum.photos/seed/labA/200/200",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFE4E4E7)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.BrokenImage,
                                contentDescription = null,
                                tint = Color(0xFF52525B),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                    success = { SubcomposeAsyncImageContent() },
                )
            }
            // B：使用无效 TLD（.invalid）触发 DNS 失败，必然走 error
            LabCell(label = "B) 失败\ninvalid 域名") {
                SubcomposeAsyncImage(
                    model = "https://not-exist-abc-xyz.invalid/xxx.jpg",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFE4E4E7)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.BrokenImage,
                                contentDescription = null,
                                tint = Color(0xFF52525B),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                    success = { SubcomposeAsyncImageContent() },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // C：完整模拟 FileThumb 的「无 URL 走 design 渐变」分支
            LabCell(label = "C) 无 URL\nthumbnailUrl=null", bgBrush = designBrushForImage()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            // D：先强制 loading slot 持续 2 秒显示占位环，再切到真实图（不受网络快慢影响）
            LabCell(label = "D) 加载中\n占位环 (2s)") {
                var showLoading by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showLoading = false
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    // 持续 2 秒显示 loading 占位（与生产空 slot 不同 — 这里用于对照）
                    if (showLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp,
                                color = Color(0xFFA1A1AA),
                            )
                        }
                    } else {
                        SubcomposeAsyncImage(
                            model = "https://picsum.photos/seed/labD/200/200",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            success = { SubcomposeAsyncImageContent() },
                            error = {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFFE4E4E7)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.BrokenImage,
                                        contentDescription = null,
                                        tint = Color(0xFF52525B),
                                        modifier = Modifier.size(48.dp),
                                    )
                                }
                            },
                            loading = {},
                        )
                    }
                }
            }
        }

        Spacer(Modifier.size(12.dp))

        Text(
            "关键观察：",
            style = t.body,
            color = c.fg,
        )
        Text(
            "• A 成功：缩略图正确显示\n" +
                    "• B 失败：深灰底 + 裂图 icon，与 loading 灰底明显区分\n" +
                    "• C 无 URL：design 渐变（橙色 IMAGE）+ 白 icon，跟 B 视觉权重接近，" +
                    "用户容易把「没源」当成「加载失败」\n" +
                    "• D 加载中：当前生产是空 slot（#FAFAFA，几乎无感），本屏用转圈环展示「如果加了占位是什么样」",
            style = t.caption,
            color = c.muted,
        )
    }
}

/**
 * LabCell — 120dp 圆角盒，1dp 黑边框 + 自定义背景色（默认 #FAFAFA）。
 *
 * @param bgBrush 自定义背景画刷；默认纯色 #FAFAFA（loading 灰底）。传 null = 透明
 *                让 outer 黑边透出（用于 C 的渐变场景，但 C 现在传 designBrushForImage）。
 */
@Composable
private fun LabCell(
    label: String,
    bgBrush: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFFAFAFA), Color(0xFFFAFAFA)),
    ),
    content: @Composable () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 外层 Box：1dp 黑边
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black)
                .padding(1.dp),
        ) {
            // 内层 Box：实际背景色（默认 #FAFAFA 透出 loading）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp))
                    .background(bgBrush),
            ) {
                content()
            }
        }
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            style = t.micro.copy(fontSize = 10.sp, lineHeight = 13.sp),
            color = c.muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** 复刻 FileThumb 的 IMAGE 渐变（橙色 135°），用于 C 单元模拟 design fallback。 */
private fun designBrushForImage(): Brush = Brush.linearGradient(
    colorStops = arrayOf(0f to Color(0xFFEA580C), 1f to Color(0xFFC2410C)),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(120f, 120f),
)
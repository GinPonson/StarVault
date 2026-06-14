package com.starvault.component

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * 加载态占位（8 行 accent-soft 块）。Home / Files 屏共用。
 * 行高 14dp + 间距 8dp 模拟列表项布局。
 */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = StarVaultTheme.dimens.spaceLg, vertical = StarVaultTheme.dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(StarVaultTheme.dimens.spaceMd),
    ) {
        repeat(8) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.accentSoft),
                )
                Spacer(Modifier.size(StarVaultTheme.dimens.spaceMd))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth(0.6f)
                            .background(c.accentSoft),
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth(0.35f)
                            .background(c.accentSoft),
                    )
                }
            }
        }
    }
}

/**
 * 错误态：居中标题 + 重试按钮。
 * 屏内统一在 catch 分支 `ErrorView(message) { vm.retry() }`。
 */
@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(StarVaultTheme.dimens.spaceXl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("出错了", style = t.title, color = c.fg)
        Spacer(Modifier.height(StarVaultTheme.dimens.spaceSm))
        Text(message, style = t.body, color = c.muted)
        Spacer(Modifier.height(StarVaultTheme.dimens.spaceLg))
        PrimaryButton(text = "重试", onClick = onRetry)
    }
}

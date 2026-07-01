package com.starvault.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.starvault.component.Icons
import com.starvault.theme.StarVaultTheme

/**
 * Files 屏右下角 "+" 入口 — 点开 2 项 DropdownMenu（上传 / 新建文件夹）。
 *
 * ## 布局
 * ```
 *                ┌──────────────┐
 *                │ ↑ 上传文件     │  ← DropdownMenuItem #1
 *                ├──────────────┤
 *                │ 🗂 新建文件夹  │  ← DropdownMenuItem #2
 *                └──────────────┘
 *                    ⬤ +            ← FAB（点这个展开）
 * ```
 *
 * ## 状态
 *  - `expanded` 内部 `remember { mutableStateOf(false) }`，与 host 解耦
 *  - 点 FAB → 展开菜单
 *  - 选某项 → 调对应回调 + 关闭菜单
 *  - 菜单外点击 / 系统 Back / DismissRequest → 关闭菜单
 *
 * ## Paparazzi 兼容性
 *  - 默认 `expanded = false`，snapshot 拍 FAB collapsed 态（与改前视觉差异最小）
 *  - 想拍展开态需 host 在调用前 `var expanded by remember { mutableStateOf(true) }` 然后传 `expanded = expanded, onExpandedChange = { ... }`（不在本计划范围）
 */
@Composable
fun AddMenu(
    onAddUpload: () -> Unit,
    onAddNewFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // FAB（沿用 FilesScreen.kt 旧 Fab 视觉：56dp 圆 + fg 底 + 白 + icon）
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(c.fg)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Plus,
                contentDescription = "新建",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // anchor 自动对齐到 Box（FAB）；offset 让菜单贴 FAB 上方
            modifier = Modifier
                .padding(end = 20.dp, bottom = 4.dp),
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Upload,
                            contentDescription = null,
                            tint = c.fg,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("上传文件", style = t.body, color = c.fg)
                    }
                },
                onClick = {
                    expanded = false
                    onAddUpload()
                },
            )
            HorizontalDivider(color = c.border)
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.NewFolder,
                            contentDescription = null,
                            tint = c.fg,
                            modifier = Modifier.size(24.dp),
                        )
                        Text("新建文件夹", style = t.body, color = c.fg)
                    }
                },
                onClick = {
                    expanded = false
                    onAddNewFolder()
                },
            )
        }
    }
}

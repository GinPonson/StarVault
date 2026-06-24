package com.starvault.ui.files.sort

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.starvault.component.Icons
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.files.SORT_FIELDS
import com.starvault.ui.files.SortOption

/**
 * Files 屏排序 BottomSheet（参考 Material 3 ModalBottomSheet 风格手搓实现）。
 *
 *  - 两级菜单：先选字段（6 项），再选升降序（2 项）
 *  - 当前选中的字段用 ▾ / ▴ 箭头标识（与 SectionHead sortLabel 同步）
 *  - 风格对齐 AlbumScreen FolderSheet（grabber + 28dp 圆角 + 半透明 scrim）
 *
 *  调用方式（FilesRoute）：
 *  ```kotlin
 *  if (sortSheetVisible) {
 *      SortSheet(
 *          currentField = ...,
 *          currentAsc = ...,
 *          onPicked = { field, asc -> vm.applySort(field, asc); sortSheetVisible = false },
 *          onDismiss = { sortSheetVisible = false },
 *      )
 *  }
 *  ```
 *
 *  @param currentField 当前排序字段（115 webapi `o` 参数）
 *  @param currentAsc   当前升降序（0 = 降序，1 = 升序）
 *  @param onPicked     用户选完 (field, asc) 触发；通常关闭 sheet 并 applySort
 *  @param onDismiss    用户点 scrim / 抓把 / 系统返回
 */
@Composable
fun SortSheet(
    currentField: String,
    currentAsc: Int,
    onPicked: (field: String, asc: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography

    // 整体结构：Box (fillMaxSize) + scrim (TopStart) + sheet Column (BottomCenter)
    // 调用方直接 BoxScope.align(BottomCenter) 挂这个 SortSheet，scrim 占整个剩余空间
    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明 scrim：占 sheet 之上整片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
        )

        // Sheet 自身（贴底，圆角顶）
        SheetBody(
            currentField = currentField,
            currentAsc = currentAsc,
            onPicked = onPicked,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 排序 BottomSheet 主体（grabber + 一级 / 二级菜单）。
 * 由 [SortSheet] 包在 Box 底部调用。
 */
@Composable
private fun SheetBody(
    currentField: String,
    currentAsc: Int,
    onPicked: (field: String, asc: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
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

        // 标题（随二级菜单切换文案）
        var pickedField by rememberSaveable(currentField) {
            // 初次进入：currentField 即当前选中；用户点新字段时切到二级菜单
            // 用 saved state 防旋转屏丢失
            mutableStateOf<String?>(null)
        }

        if (pickedField == null) {
            // ───── 一级菜单：选字段 ─────
            Text(
                text = "排序方式",
                style = t.title,
                color = c.fg,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp).padding(bottom = 16.dp),
            )
            SORT_FIELDS.forEach { opt ->
                SortRow(
                    label = "按${opt.label}",
                    arrow = if (opt.field == currentField) (if (currentAsc == 1) "▴" else "▾") else null,
                    isSelected = opt.field == currentField,
                    onClick = { pickedField = opt.field },
                )
            }
        } else {
            // ───── 二级菜单：选升降序 ─────
            // pickedField 是 rememberSaveable 委托属性,Kotlin 不支持 smart-cast
            // (getter 可能被并发修改),所以用本地 val 一次性 snapshot 出非空值
            val field = pickedField ?: return@Column
            val selectedLabel = SORT_FIELDS.firstOrNull { it.field == field }?.label ?: ""
            SortHeader(
                title = "按$selectedLabel",
                onBack = { pickedField = null },
            )
            HorizontalDivider(color = c.border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
            SortRow(
                label = "降序",
                arrow = "▾",
                isSelected = field == currentField && currentAsc == 0,
                onClick = { onPicked(field, 0) },
            )
            SortRow(
                label = "升序",
                arrow = "▴",
                isSelected = field == currentField && currentAsc == 1,
                onClick = { onPicked(field, 1) },
            )
        }
    }
}

/**
 * 二级菜单标题行：← 返回 icon + 标题（"按修改时间"）。
 */
@Composable
private fun SortHeader(title: String, onBack: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Back,
                contentDescription = "返回字段列表",
                tint = c.fg,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            style = t.title,
            color = c.fg,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
    }
}

/**
 * 排序单行：左侧 label + 右侧箭头（选中时显示）。
 */
@Composable
private fun SortRow(
    label: String,
    arrow: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = t.body,
            color = c.fg,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (arrow != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = arrow,
                style = t.body,
                color = c.muted,
            )
        }
    }
}
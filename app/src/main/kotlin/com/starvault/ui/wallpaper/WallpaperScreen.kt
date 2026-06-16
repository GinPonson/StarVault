package com.starvault.ui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.starvault.theme.StarVaultTheme

/**
 * Wallpaper 屏（对应 design/08-wallpaper.html 的"壁纸引擎" 1:1 复刻）。
 *
 * 整体结构（垂直滚动）：
 *   ┌─ TopBar         ← back + "壁纸引擎"（无右侧 toggle）
 *   ├─ SwitchCard     40dp icon + 标题 + sub + iOS-style 44dp toggle（始终可见）
 *   ├─ EngineSections  ON 时才显示
 *   │   ├─ 切换频率  section
 *   │   ├─ 图片来源  section（相册行 + 含 N 张 sub）
 *   │   ├─ 显示模式  section
 *   │   └─ 动态壁纸  section（独立 switch）
 *   └─ FooterBar      48dp 主 CTA "立即切换下一张"（ON 时显示）
 *   + Sheet: AlbumPicker / ModePicker(内嵌 input) / DisplayPicker
 *
 * 状态映射（与 [WallpaperUiState] 对应）：
 *   - Loading : 占位
 *   - Success : 完整渲染
 *   - Error   : message
 */
@Composable
fun WallpaperScreen(
    state: WallpaperUiState,
    onBack: () -> Unit = {},
    onToggleEngine: () -> Unit = {},
    onToggleLiveWallpaper: () -> Unit = {},
    onPickMode: () -> Unit = {},
    onPickAlbum: () -> Unit = {},
    onPickDisplay: () -> Unit = {},
    onSwitchNow: () -> Unit = {},
    onSheetAlbum: (String) -> Unit = {},
    onSheetModeType: (String) -> Unit = {},
    onSheetIntervalValue: (Int) -> Unit = {},
    onSheetIntervalUnit: (IntervalUnit) -> Unit = {},
    onSheetDailyTime: (String) -> Unit = {},
    onSheetDisplay: (String) -> Unit = {},
    onCloseSheet: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = StarVaultTheme.colors
    Box(modifier = modifier.fillMaxSize().background(c.bg)) {
        when (state) {
            is WallpaperUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载中…", style = StarVaultTheme.typography.body, color = c.muted)
                }
            }
            is WallpaperUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, style = StarVaultTheme.typography.body, color = c.danger)
                }
            }
            is WallpaperUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(onBack)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SwitchCard(
                            enabled = state.enabled,
                            onToggle = onToggleEngine,
                        )
                        if (state.enabled) {
                            EngineSections(
                                mode = state.mode,
                                album = state.album,
                                display = state.display,
                                liveWallpaper = state.liveWallpaper,
                                onPickMode = onPickMode,
                                onPickAlbum = onPickAlbum,
                                onPickDisplay = onPickDisplay,
                                onToggleLiveWallpaper = onToggleLiveWallpaper,
                            )
                        }
                        Spacer(Modifier.height(40.dp))
                    }
                    if (state.enabled) {
                        FooterBar(onSwitchNow = onSwitchNow)
                    }
                }

                // Sheet（任意类型）
                if (state.sheet !is WallpaperSheetState.Closed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(onClick = onCloseSheet),
                    )
                    when (val sh = state.sheet) {
                        is WallpaperSheetState.AlbumPicker -> AlbumSheet(
                            albums = state.albumOptions,
                            childrenOf = state.childrenOf,
                            currentId = sh.currentAlbumId,
                            onPick = onSheetAlbum,
                            onClose = onCloseSheet,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                        is WallpaperSheetState.ModePicker -> ModeSheet(
                            current = state.mode,
                            onPickType = onSheetModeType,
                            onIntervalValue = onSheetIntervalValue,
                            onIntervalUnit = onSheetIntervalUnit,
                            onDailyTime = onSheetDailyTime,
                            onClose = onCloseSheet,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                        is WallpaperSheetState.DisplayPicker -> DisplaySheet(
                            currentValue = sh.currentValue,
                            onPick = onSheetDisplay,
                            onClose = onCloseSheet,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}

/* ───────────────────── TopBar ───────────────────── */

@Composable
private fun TopBar(onBack: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .border(1.dp, c.border)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", style = t.subtitle, color = c.fg)
        }
        Text(text = "壁纸引擎", style = t.subtitle, color = c.fg, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.size(40.dp))
    }
}

/* ───────────────────── SwitchCard ───────────────────── */

@Composable
private fun SwitchCard(enabled: Boolean, onToggle: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▣", style = t.subtitle, color = c.accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "壁纸引擎",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = c.fg,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (enabled) "已开启壁纸自动轮换" else "关闭壁纸自动轮换",
                style = t.caption,
                color = c.muted,
            )
        }
        ToggleSwitch(enabled = enabled, onClick = onToggle)
    }
}

@Composable
private fun ToggleSwitch(enabled: Boolean, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (enabled) c.accent else Color(0x52787880))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(if (enabled) Alignment.CenterEnd else Alignment.CenterStart),
        )
    }
}

/* ───────────────────── EngineSections ───────────────────── */

@Composable
private fun EngineSections(
    mode: Mode,
    album: AlbumRef,
    display: DisplayMode,
    liveWallpaper: Boolean,
    onPickMode: () -> Unit,
    onPickAlbum: () -> Unit,
    onPickDisplay: () -> Unit,
    onToggleLiveWallpaper: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Column(modifier = Modifier.fillMaxWidth()) {
        // 切换频率
        SectionHead("切换频率")
        SecCard {
            NavRow(
                label = "切换频率",
                value = mode.label,
                onClick = onPickMode,
            )
        }
        // 图片来源
        SectionHead("图片来源")
        SecCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickAlbum)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("📁", style = t.body)
                Text(
                    text = album.name,
                    style = t.body,
                    color = c.fg,
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("更换", style = t.caption, color = c.muted)
                    Text("›", style = t.caption, color = c.muted)
                }
            }
            HorizontalDivider(color = c.border, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "包含 ",
                    style = TextStyle(fontSize = 13.sp, color = c.muted),
                )
                Text(
                    text = album.photoCount.toString(),
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.fg),
                )
                Text(
                    text = " 张照片",
                    style = TextStyle(fontSize = 13.sp, color = c.muted),
                )
            }
        }
        // 显示模式
        SectionHead("显示模式")
        SecCard {
            NavRow(
                label = "显示模式",
                value = display.label,
                onClick = onPickDisplay,
            )
        }
        // 动态壁纸
        SectionHead("动态壁纸")
        SecCard(singleRow = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleLiveWallpaper)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("启用动态效果", style = t.body, color = c.fg)
                ToggleSwitch(enabled = liveWallpaper, onClick = onToggleLiveWallpaper)
            }
        }
    }
}

@Composable
private fun SectionHead(text: String) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Text(
        text = text,
        style = t.micro,
        color = c.muted,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).padding(top = 10.dp, bottom = 10.dp),
    )
}

@Composable
private fun SecCard(singleRow: Boolean = false, content: @Composable () -> Unit) {
    val c = StarVaultTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

@Composable
private fun NavRow(label: String, value: String, onClick: () -> Unit) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 0.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = c.fg)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = value,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.fg),
            )
            Text("›", style = TextStyle(fontSize = 14.sp), color = c.muted)
        }
    }
}

/* ───────────────────── FooterBar ───────────────────── */

@Composable
private fun FooterBar(onSwitchNow: () -> Unit) {
    val c = StarVaultTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .border(1.dp, c.border)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(c.accent)
                .clickable(onClick = onSwitchNow),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "立即切换下一张",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
            )
        }
    }
}

/* ───────────────────── Sheets ───────────────────── */

@Composable
private fun AlbumSheet(
    albums: List<AlbumRef>,
    childrenOf: Map<String, List<AlbumRef>>,
    currentId: String,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
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
        SheetGrabber()
        Text(
            text = "选择相册",
            style = t.title,
            color = c.fg,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            albums.forEach { a ->
                val isCurrent = a.id == currentId
                val hasChildren = (childrenOf[a.id] ?: emptyList()).isNotEmpty()
                FolderRow(
                    name = a.name,
                    color = a.color,
                    isCurrent = isCurrent,
                    showChev = hasChildren,
                    showCheck = isCurrent,
                    onClick = { onPick(a.id) },
                )
            }
        }
    }
}

@Composable
private fun ModeSheet(
    current: Mode,
    onPickType: (String) -> Unit,
    onIntervalValue: (Int) -> Unit,
    onIntervalUnit: (IntervalUnit) -> Unit,
    onDailyTime: (String) -> Unit,
    onClose: () -> Unit,
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
        SheetGrabber()
        Text(
            text = "切换频率",
            style = t.title,
            color = c.fg,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp),
        )
        // 1. 每次解锁
        ModeOptionRow(
            label = "每次解锁",
            current = current is Mode.Unlock,
            onClick = { onPickType("unlock") },
        ) { }
        // 2. 每 N 单位
        val interval = current as? Mode.Interval
        ModeOptionRow(
            label = "每",
            current = current is Mode.Interval,
            onClick = { onPickType("interval") },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var local by remember { mutableStateOf((interval?.value ?: 6).toString()) }
                OutlinedTextField(
                    value = local,
                    onValueChange = {
                        local = it.filter { c -> c.isDigit() }.take(3)
                        val n = local.toIntOrNull()
                        if (n != null) onIntervalValue(n)
                    },
                    modifier = Modifier.width(64.dp).height(40.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.border,
                    ),
                )
                UnitSelect(
                    current = interval?.unit ?: IntervalUnit.HOUR,
                    onPick = onIntervalUnit,
                )
            }
        }
        // 3. 每天 HH:MM
        val daily = current as? Mode.Daily
        ModeOptionRow(
            label = "每天",
            current = current is Mode.Daily,
            onClick = { onPickType("daily") },
        ) {
            var local by remember { mutableStateOf(daily?.time ?: "09:00") }
            OutlinedTextField(
                value = local,
                onValueChange = {
                    local = it.take(5)
                    onDailyTime(local)
                },
                modifier = Modifier.width(80.dp).height(40.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.border,
                ),
            )
        }
        // 4. 仅手动
        ModeOptionRow(
            label = "仅手动",
            current = current is Mode.Manual,
            onClick = { onPickType("manual") },
        ) { }
    }
}

@Composable
private fun UnitSelect(current: IntervalUnit, onPick: (IntervalUnit) -> Unit) {
    val c = StarVaultTheme.colors
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IntervalUnit.entries.forEach { u ->
            val active = u == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) c.fg else Color.Transparent)
                    .clickable { onPick(u) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = u.label,
                    style = TextStyle(fontSize = 12.sp, color = if (active) c.surface else c.fg),
                )
            }
        }
    }
}

@Composable
private fun ModeOptionRow(
    label: String,
    current: Boolean,
    onClick: () -> Unit,
    extra: @Composable () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (current) c.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 0.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(label, style = t.body, color = c.fg, modifier = Modifier.weight(1f))
        extra()
        if (current) Text("✓", style = t.subtitle, color = c.accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DisplaySheet(
    currentValue: String,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
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
        SheetGrabber()
        Text(
            text = "显示模式",
            style = t.title,
            color = c.fg,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp),
        )
        DisplayMode.entries.forEach { d ->
            val current = d.value == currentValue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (current) c.accentSoft else Color.Transparent)
                    .clickable { onPick(d.value) }
                    .padding(horizontal = 24.dp, vertical = 0.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(d.label, style = t.body, color = c.fg, modifier = Modifier.weight(1f))
                if (current) Text("✓", style = t.subtitle, color = c.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    color: Color,
    isCurrent: Boolean,
    showChev: Boolean,
    showCheck: Boolean,
    onClick: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) c.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 0.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text("▢", style = TextStyle(fontSize = 14.sp, color = Color.White))
        }
        Text(name, style = t.body, color = c.fg, modifier = Modifier.weight(1f))
        if (showChev) Text("›", style = t.subtitle, color = c.muted)
        if (showCheck) Text("✓", style = t.subtitle, color = c.accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SheetGrabber() {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFC4C4C4)),
        )
    }
    Spacer(Modifier.height(20.dp))
}

package com.starvault.ui.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.starvault.theme.StarVaultTheme

/**
 * Files 屏"新建文件夹"对话框 — Material AlertDialog + 单 TextField。
 *
 * ## 用法
 * ```kotlin
 * var showDialog by remember { mutableStateOf(false) }
 * if (showDialog) {
 *     NewFolderDialog(
 *         onConfirm = { name ->
 *             vm.createFolder(name)
 *             showDialog = false
 *         },
 *         onDismiss = { showDialog = false },
 *     )
 * }
 * ```
 *
 * ## 校验
 *  - name.trim().isEmpty() → 确认按钮 disabled（手输空格绕过）
 *  - name 长度限制 1..40（115 文件名实际限制 255；UI 层先卡 40 防误输）
 *
 * ## 输入体验
 *  - `singleLine = true` + `imeAction = Done` → 输入法键盘"完成"按钮触发 onConfirm
 *  - 不接系统 BackHandler：AlertDialog 自带 dismiss
 */
@Composable
fun NewFolderDialog(
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    var name by remember { mutableStateOf("") }
    val canConfirm = name.trim().isNotEmpty() && name.length <= 40

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("新建文件夹", style = t.large, color = c.fg)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    "在当前目录下创建空文件夹",
                    style = t.caption,
                    color = c.muted,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    placeholder = {
                        Text("文件夹名称", style = t.body, color = c.muted)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = canConfirm,
            ) {
                Text("创建", style = t.body, color = if (canConfirm) c.accent else c.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", style = t.body, color = c.muted)
            }
        },
        containerColor = c.surface,
        titleContentColor = c.fg,
        textContentColor = c.fg,
    )
}

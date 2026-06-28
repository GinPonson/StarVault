package com.starvault.ui.dialog

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
 * 重命名对话框 — 仿 [com.starvault.ui.files.NewFolderDialog] 模板,AlertDialog + TextField。
 *
 *  Files 屏 BulkBar RENAME(N==1) + Preview 屏 MoreMenu RENAME 都弹这个 dialog,
 *  复用一个 Composable,避免两边样式漂移。
 *
 *  入参 [currentName] 预填到 TextField(用户可改) — 不强制必须改,直接确认等同无操作(VM 会判
 *  oldName == newName → ToastBus.info("无变化") 或 noop,见 FilesViewModel.renameFile 行为)。
 *
 *  校验:
 *  - newName.trim().isEmpty() → 确认按钮 disabled
 *  - newName == currentName → 确认按钮 disabled(无变化)
 *  - 长度限制 1..255(115 文件名实际硬限制 255,不再像 NewFolderDialog 卡 40)
 *
 *  用法:
 *  ```kotlin
 *  if (renameDialogVisible) {
 *      RenameDialog(
 *          currentName = entry.name,
 *          onConfirm = { newName -> vm.renameFile(entry.id, newName); renameDialogVisible = false },
 *          onDismiss = { renameDialogVisible = false },
 *      )
 *  }
 *  ```
 *
 * @param currentName  当前文件名(预填,允许相同)
 * @param onConfirm    确认回调(参数 = 用户输入的新名,已 trim 完)
 * @param onDismiss    取消回调
 */
@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    var name by remember { mutableStateOf(currentName) }
    val trimmed = name.trim()
    val canConfirm = trimmed.isNotEmpty() && trimmed != currentName.trim() && trimmed.length <= 255

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("重命名", style = t.large, color = c.fg)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    "修改文件或文件夹名",
                    style = t.caption,
                    color = c.muted,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 255) name = it },
                    placeholder = {
                        Text("新名称", style = t.body, color = c.muted)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = canConfirm,
            ) {
                Text("确定", style = t.body, color = if (canConfirm) c.accent else c.muted)
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

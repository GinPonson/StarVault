package com.starvault.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.starvault.theme.StarVaultTheme

/**
 * 通用确认对话框 — AlertDialog + title + 副标题 + 确认/取消 双按钮。
 *
 *  Files 屏 BulkBar 5 动作里:
 *   - DELETE → 标题"确认删除 N 项?",message 描述不可恢复
 *   - 其他动作(MOVE / RENAME)目前不弹确认 — UI 层直接进 FolderPicker / RenameDialog
 *
 *  复用 [NewFolderDialog] 的浅色主题样式(`containerColor = c.surface`),但**不放 TextField**
 *  — 这是只读确认框。
 *
 *  用法:
 *  ```kotlin
 *  if (confirmVisible) {
 *      ConfirmDialog(
 *          title = "确认删除 ${count} 项?",
 *          message = "此操作会移到回收站,7 天内可在 115 端恢复",
 *          confirmLabel = "删除",
 *          onConfirm = { vm.deleteFiles(ids); confirmVisible = false },
 *          onDismiss = { confirmVisible = false },
 *      )
 *  }
 *  ```
 *
 * @param title         标题(主问题)
 * @param message       副标题(描述影响,支持空字符串省略)
 * @param confirmLabel  确认按钮文案,默认"确定"
 * @param dismissLabel  取消按钮文案,默认"取消"
 * @param danger        true → 确认按钮用红色(M3 无内置 destructive,用 accent 区分红绿)
 * @param onConfirm     确认回调(用户点"确定"或回车)
 * @param onDismiss     取消回调(点"取消" / 外部点 / 系统 back)
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String? = null,
    confirmLabel: String = "确定",
    dismissLabel: String = "取消",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = StarVaultTheme.colors
    val t = StarVaultTheme.typography
    // 危险按钮用红色硬编码(浅色系统没有 destructive token;与 BulkBar 里
    // BulkActBtn(danger=true) 的 Color(0xFFFF8A8A) 同色,保持视觉一致)
    val confirmTint = if (danger) androidx.compose.ui.graphics.Color(0xFFE53935) else c.accent

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = t.large, color = c.fg)
        },
        text = if (message.isNullOrBlank()) null else {
            {
                Text(message, style = t.body, color = c.muted)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, style = t.body, color = confirmTint)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, style = t.body, color = c.muted)
            }
        },
        containerColor = c.surface,
        titleContentColor = c.fg,
        textContentColor = c.muted,
    )
}

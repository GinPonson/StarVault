package com.starvault.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.starvault.theme.StarVaultTheme

/**
 * Share 屏 Preview — 3 个 case：
 *  1. Ready（默认）       ：Link tab 完整渲染
 *  2. Tab=转存            ：显示 SaveTabPlaceholder
 *  3. Loading             ：占位状态
 *
 * widthDp/heightDp = 412x900。
 */
@Preview(name = "Share/Ready",  showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun ShareReadyPreview() = StarVaultTheme {
    ShareScreen(
        state = sharePreviewReady(),
        onClose = {},
        onTab = {},
        onAccessType = {},
        onRegenCode = {},
        onExpires = {},
        onForbidTransfer = {},
        onVipOnly = {},
        onLoginRequired = {},
        onCopy = {},
        onCta = {},
    )
}

@Preview(name = "Share/Save",   showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun ShareSavePreview() = StarVaultTheme {
    ShareScreen(
        state = sharePreviewReady().copy(activeTab = ShareTab.Save),
        onClose = {},
        onTab = {},
        onAccessType = {},
        onRegenCode = {},
        onExpires = {},
        onForbidTransfer = {},
        onVipOnly = {},
        onLoginRequired = {},
        onCopy = {},
        onCta = {},
    )
}

@Preview(name = "Share/Loading", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
fun ShareLoadingPreview() = StarVaultTheme {
    ShareScreen(
        state = ShareUiState.Loading(),
        onClose = {},
        onTab = {},
        onAccessType = {},
        onRegenCode = {},
        onExpires = {},
        onForbidTransfer = {},
        onVipOnly = {},
        onLoginRequired = {},
        onCopy = {},
        onCta = {},
    )
}

private fun sharePreviewReady(): ShareUiState.Ready = ShareUiState.Ready(
    file = mockShareFile(),
    activeTab = ShareTab.Link,
    accessType = "有提取码 · 任何人",
    accessCode = "8K3F",
    expiresInDays = 7,
    forbidTransfer = false,
    vipOnly = true,
    loginRequired = true,
    link = com.starvault.data.model.ShareLink(
        fileId = "h-02",
        url = "https://115.com/s/9f2c-7ab4-2026-…",
        accessCode = "8K3F",
    ),
    copiedCount = 3,
)

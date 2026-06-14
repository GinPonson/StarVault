package com.starvault.screenshot

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.starvault.data.model.ShareLink
import com.starvault.theme.StarVaultTheme
import com.starvault.ui.share.ShareScreen
import com.starvault.ui.share.ShareTab
import com.starvault.ui.share.ShareUiState
import com.starvault.ui.share.mockShareFile
import org.junit.Rule
import org.junit.Test

/**
 * Share 屏 Paparazzi 回归基线 — 与 design/03-share.html 对齐。
 *
 *  - link  : 链接分享 tab（默认）
 *  - save  : 转存到我的 tab
 *  - error : 文件元数据拉取失败
 */
class ShareScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = PHONE_412_900,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false,
    )

    private fun mockLink() = ShareLink(
        fileId = "h-02",
        url = "https://115.com/s/9f2c-7ab4-2026-…",
        accessCode = "8K3F",
        expiresAt = 1_780_000_000L,
    )

    @Test fun share_link() = paparazzi.snapshot {
        StarVaultTheme {
            ShareScreen(
                state = ShareUiState.Ready(
                    file = mockShareFile(),
                    activeTab = ShareTab.Link,
                    link = mockLink(),
                ),
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
    }

    @Test fun share_save() = paparazzi.snapshot {
        StarVaultTheme {
            ShareScreen(
                state = ShareUiState.Ready(
                    file = mockShareFile(),
                    activeTab = ShareTab.Save,
                    link = mockLink(),
                ),
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
    }

    @Test fun share_error() = paparazzi.snapshot {
        StarVaultTheme {
            ShareScreen(
                state = ShareUiState.Error(message = "文件不存在或已被删除"),
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
    }
}
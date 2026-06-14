package com.starvault.ui.share

import androidx.lifecycle.ViewModel
import com.starvault.data.model.ShareLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Share 屏 ViewModel — Phase 1 mock 数据（按 design/03-share.html 1:1 复刻）。
 *
 * 真实接入 115 后：
 *  - mockReady 替换为调 /share/create + /share/qrcode 接口
 *  - toggle* 改为调 /share/permission 接口
 *  - regenerateCode / changeExpire 各自调对应接口
 */
class ShareViewModel : ViewModel() {

    private val _state = MutableStateFlow<ShareUiState>(mockReadyState())
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    fun selectTab(tab: ShareTab) {
        val s = _state.value
        _state.value = when (s) {
            is ShareUiState.Ready -> s.copy(activeTab = tab)
            is ShareUiState.Loading -> s.copy(activeTab = tab)
            is ShareUiState.Error -> s.copy(activeTab = tab)
        }
    }

    fun toggleForbidTransfer() = updateReady { it.copy(forbidTransfer = !it.forbidTransfer) }
    fun toggleVipOnly()       = updateReady { it.copy(vipOnly = !it.vipOnly) }
    fun toggleLoginRequired() = updateReady { it.copy(loginRequired = !it.loginRequired) }

    fun regenerateCode() = updateReady {
        val newCode = (1..4).map { ('2'..'9').random() }.joinToString("")
        it.copy(accessCode = newCode)
    }

    fun changeExpiresInDays(days: Int) = updateReady { it.copy(expiresInDays = days) }

    fun changeAccessType(type: String) = updateReady { it.copy(accessType = type) }

    private inline fun updateReady(crossinline block: (ShareUiState.Ready) -> ShareUiState.Ready) {
        val s = _state.value
        if (s is ShareUiState.Ready) _state.value = block(s)
    }

    private fun mockReadyState(): ShareUiState.Ready = ShareUiState.Ready(
        file = mockShareFile(),
        activeTab = ShareTab.Link,
        accessType = "有提取码 · 任何人",
        accessCode = "8K3F",
        expiresInDays = 7,
        forbidTransfer = false,
        vipOnly = true,
        loginRequired = true,
        link = ShareLink(
            fileId = "h-02",
            url = "https://115.com/s/9f2c-7ab4-2026-…",
            accessCode = "8K3F",
            expiresAt = System.currentTimeMillis() / 1000 + 7 * 24 * 3600,
        ),
        copiedCount = 3,
    )
}

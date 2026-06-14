package com.starvault.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Login 屏 ViewModel — Phase 1 mock 实现。
 *
 *  - 启动即处于 [LoginUiState.Waiting]，并跑一个 1s 间隔的倒计时 Job
 *  - 调用 [simulateScan] 模拟用户用 HTML 上的 demoBtn 完成扫码：
 *      Waiting ──(立即)──▶ Scanned ──(1800ms)──▶ LoggedIn
 *  - 调用 [refresh] 重置二维码与倒计时，恢复到 Waiting
 *
 * 真实接入 115 后只需替换：
 *  - mock simulateScan/refresh 改为调用 QR 服务的 long-polling / SSE
 *  - LoggedIn 携带的 access_token 与 user info 由网络回包填充
 */
class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Waiting())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // 当前 QR 过期倒计时 Job，刷新或销毁时取消
    private var expireJob: Job? = null

    init {
        startExpireCountdown()
    }

    /** 模拟扫码：从 Waiting 推进至 Scanned，再延迟 1800ms 推进至 LoggedIn。*/
    fun simulateScan() {
        // 仅在 Waiting 时触发，避免重复点击
        if (_state.value !is LoginUiState.Waiting) return
        viewModelScope.launch {
            _state.value = LoginUiState.Scanned(
                nickname = "Gin",
                deviceName = "iPhone 15 Pro · 来自广东",
                expireSeconds = _state.value.expireSeconds,
            )
            delay(1800)
            _state.value = LoginUiState.LoggedIn(
                nickname = "Gin",
                expireSeconds = _state.value.expireSeconds,
            )
        }
    }

    /** 刷新二维码：重置倒计时 + 回到 Waiting。*/
    fun refresh() {
        expireJob?.cancel()
        _state.value = LoginUiState.Waiting(expireSeconds = 120)
        startExpireCountdown()
    }

    /** 1s tick 的倒计时；过期自动 refresh（与 HTML mock 一致）。*/
    private fun startExpireCountdown() {
        expireJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value
                val nextExpire = (s.expireSeconds - 1).coerceAtLeast(0)
                if (nextExpire == 0) {
                    refresh()
                    return@launch
                }
                _state.value = when (s) {
                    is LoginUiState.Waiting  -> s.copy(expireSeconds = nextExpire)
                    is LoginUiState.Scanned  -> s.copy(expireSeconds = nextExpire)
                    is LoginUiState.LoggedIn -> s.copy(expireSeconds = nextExpire)
                    is LoginUiState.Error    -> return@launch  // Error 时不刷
                }
            }
        }
    }

    override fun onCleared() {
        expireJob?.cancel()
        super.onCleared()
    }
}

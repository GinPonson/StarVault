package com.starvault.ui.login

import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.data.remote.cloud115.ScanLoginManager
import com.starvault.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Login 屏 ViewModel — 真 115 扫码接入版。
 *
 *  - init { }: 立即拉一次 QR 码（fetchQrCode），成功后开 120s expire countdown + 启动 polling
 *  - 状态机：完全复用 [LoginUiState] 4 态，与 ScanStatus 6 态 1:1 映射
 *  - [refresh]: 取消旧 polling，重新拉 QR
 *  - 不再保留 simulateScan（演示按钮一并删除）
 */
class LoginViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Waiting())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null
    private var expireJob: Job? = null

    init {
        loadQrCode()
    }

    /** 拉 QR 码 + 启动 polling + 启动 expire 倒计时。 */
    fun refresh() = loadQrCode()

    private fun loadQrCode() {
        pollingJob?.cancel()
        expireJob?.cancel()

        // 重置到 Loading（Waiting + bitmap=null），UI 渲染加载态
        _state.value = LoginUiState.Waiting(qrBitmap = null, expireSeconds = 120)

        viewModelScope.launch {
            authRepository.fetchQrCode()
                .onSuccess { qr ->
                    _state.value = LoginUiState.Waiting(
                        qrBitmap = qr.bitmap.asImageBitmap(),
                        expireSeconds = 120,
                    )
                    startPolling(qr)
                    startExpireCountdown()
                }
                .onFailure { e ->
                    _state.value = LoginUiState.Error(
                        message = e.message ?: "二维码服务不可达，请稍后重试",
                        expireSeconds = 0,
                    )
                }
        }
    }

    private fun startPolling(qr: ScanLoginManager.QRCodeData) {
        pollingJob = viewModelScope.launch {
            authRepository.signIn(qr).collect { status ->
                _state.value = when (status) {
                    is ScanLoginManager.ScanStatus.Waiting -> _state.value   // 保持当前 Waiting(qrBitmap)
                    is ScanLoginManager.ScanStatus.Scanned -> {
                        val expire = (_state.value as? LoginUiState.Waiting)?.expireSeconds ?: 120
                        LoginUiState.Scanned(
                            nickname = status.nickname.ifBlank { "115 用户" },
                            deviceName = status.deviceName.ifBlank { "已扫码" },
                            expireSeconds = expire,
                        )
                    }
                    is ScanLoginManager.ScanStatus.Success -> {
                        // 落 cookies + user info → authState 自动重发 Authenticated
                        authRepository.persistLogin(
                            cookies = status.cookies,
                            uid = status.uid,
                            userName = status.userName,
                            deviceName = status.deviceName,
                        )
                        LoginUiState.LoggedIn(
                            nickname = status.userName.ifBlank { "已登录" },
                            expireSeconds = 0,
                        )
                    }
                    is ScanLoginManager.ScanStatus.Cancelled ->
                        LoginUiState.Error("用户已取消", expireSeconds = 0)
                    is ScanLoginManager.ScanStatus.Timeout ->
                        LoginUiState.Error("二维码已过期，请刷新", expireSeconds = 0)
                    is ScanLoginManager.ScanStatus.Error ->
                        LoginUiState.Error(status.message, expireSeconds = 0)
                }
            }
        }
    }

    /** 1s tick；120s 到期自动 refresh。 */
    private fun startExpireCountdown() {
        expireJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value
                val next = (s.expireSeconds - 1).coerceAtLeast(0)
                if (next == 0 && s is LoginUiState.Waiting) {
                    refresh()
                    return@launch
                }
                _state.value = when (s) {
                    is LoginUiState.Waiting -> s.copy(expireSeconds = next)
                    is LoginUiState.Scanned -> s.copy(expireSeconds = next)
                    is LoginUiState.Error, is LoginUiState.LoggedIn -> return@launch
                }
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        expireJob?.cancel()
        super.onCleared()
    }
}

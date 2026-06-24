package com.starvault.ui.login

import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.remote.cloud115.OpenAuthManager
import com.starvault.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Login 屏 ViewModel — 115 OAuth 设备码接入版。
 *
 *  - init { }: 立即申请设备码（requestDeviceCode），成功后开 120s expire countdown + 启动 polling
 *  - 状态机：复用 [LoginUiState] 4 态；与 [OpenAuthManager.AuthStatus] 6 态 1:1 映射
 *  - [refresh]: 取消 polling，重新申请设备码
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

    /** 拉设备码 + 启动 polling + 启动 expire 倒计时。 */
    fun refresh() = loadQrCode()

    private fun loadQrCode() {
        pollingJob?.cancel()
        expireJob?.cancel()

        // 重置到 Loading（Waiting + bitmap=null），UI 渲染加载态
        _state.value = LoginUiState.Waiting(qrBitmap = null, expireSeconds = 120)

        viewModelScope.launch {
            authRepository.requestDeviceCode()
                .onSuccess { dc ->
                    _state.value = LoginUiState.Waiting(
                        qrBitmap = dc.bitmap.asImageBitmap(),
                        expireSeconds = 120,
                    )
                    startPolling(dc)
                    startExpireCountdown()
                }
                .onFailure { e ->
                    // 失败：保留 Error 占位屏，toast 提示具体原因
                    ToastBus.error(e.message ?: "二维码服务不可达，请稍后重试")
                    _state.value = LoginUiState.Error(
                        message = e.message ?: "二维码服务不可达，请稍后重试",
                        expireSeconds = 0,
                    )
                }
        }
    }

    private fun startPolling(deviceCode: OpenAuthManager.DeviceCodeData) {
        pollingJob = viewModelScope.launch {
            authRepository.pollForToken(deviceCode).collect { status ->
                _state.value = when (status) {
                    is OpenAuthManager.AuthStatus.Waiting -> _state.value   // 保持当前 Waiting(qrBitmap)
                    is OpenAuthManager.AuthStatus.Scanned -> {
                        val expire = (_state.value as? LoginUiState.Waiting)?.expireSeconds ?: 120
                        // OAuth 协议不区分"已扫码未确认"中间态；UI 端固定提示
                        LoginUiState.Scanned(
                            nickname = "",
                            deviceName = "请在 115 App 中点击「确认登录」",
                            expireSeconds = expire,
                        )
                    }
                    is OpenAuthManager.AuthStatus.Authorized -> {
                        // 落 tokens + user info → authState 自动重发 Authenticated
                        authRepository.persistTokens(
                            accessToken  = status.accessToken,
                            refreshToken = status.refreshToken,
                            expiresIn    = status.expiresIn,
                            uid          = status.uid,
                            userName     = status.userName,
                        )
                        LoginUiState.LoggedIn(
                            nickname = status.userName.ifBlank { "已登录" },
                            expireSeconds = 0,
                        )
                    }
                    is OpenAuthManager.AuthStatus.Denied ->
                        LoginUiState.Error("用户已取消", expireSeconds = 0)
                    is OpenAuthManager.AuthStatus.Expired ->
                        LoginUiState.Error("二维码已过期，请刷新", expireSeconds = 0)
                    is OpenAuthManager.AuthStatus.Error ->
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
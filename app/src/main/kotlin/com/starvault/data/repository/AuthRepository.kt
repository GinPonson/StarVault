package com.starvault.data.repository

import com.starvault.data.local.auth.Cloud115AuthStore
import com.starvault.data.remote.cloud115.ScanLoginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 统一认证状态。
 *
 *  - Unauthenticated : 无 cookies（首次启动 / 已 Logout）
 *  - Authenticated   : cookies 有效，UI 决定是否再用 userName/uid/space
 *
 * VM 只观察这一个 StateFlow；NavHost 据此决定 startDestination。
 */
sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(
        val uid: Long,
        val userName: String,
        val device: String,
        val usedSpace: Long,
        val totalSpace: Long,
    ) : AuthState {
        companion object {
            /** 临时占位：cookies 存在但详情尚未从 DataStore 读全。VM 收到 Success 触发 persistLogin 后 stateIn 会自动重发新值。 */
            val EMPTY = Authenticated(0L, "", "", 0L, 0L)
        }
    }
}

/**
 * 认证仓库：聚合 [Cloud115AuthStore]（持久态）+ [ScanLoginManager]（一次性登录流程）。
 *
 *  - fetchQrCode() : 拉 QR 码给 VM 渲染
 *  - signIn(qr)    : 启动扫码轮询；返回 Flow<ScanStatus>，VM 透传
 *  - persistLogin(): 在 ScanStatus.Success 拿到 cookies + userInfo 时落 DataStore
 *  - signOut()     : 清 cookies → authState 自动变 Unauthenticated
 *  - authState     : 来自 DataStore，进程内 + 跨进程一致
 *
 *  uid/userName 详情：在登录成功后通过 persistLogin 写入 DataStore，authState 也以 Authenticated
 *  形式反映（map 时 uid/userName 暂用 EMPTY 占位，由 VM 收到 Success 触发 persistLogin 后
 *  stateIn 自动重发新值）。
 */
class AuthRepository(
    private val authStore: Cloud115AuthStore,
    private val scanManager: ScanLoginManager,
    appScope: CoroutineScope,
) {
    val authState: StateFlow<AuthState> = authStore.cookiesFlow
        .map { cookies -> if (cookies.isNullOrBlank()) AuthState.Unauthenticated else AuthState.Authenticated.EMPTY }
        .stateIn(appScope, SharingStarted.Eagerly, AuthState.Unauthenticated)

    /** 拿 QR：先调 /qrcode 拿 uid，再下 PNG bitmap。一次性，返回 Result 包装。 */
    suspend fun fetchQrCode(): Result<ScanLoginManager.QRCodeData> = scanManager.getQRCode()

    /** 启动扫码轮询；返回 Flow 给 VM 订阅。 */
    fun signIn(qr: ScanLoginManager.QRCodeData): Flow<ScanLoginManager.ScanStatus> =
        scanManager.signIn(qr)

    /**
     * 登录成功时由 VM 调用：把 cookies + userInfo 落 DataStore。
     * authState StateFlow 会自动重发 Authenticated（带真实 uid/userName）。
     */
    suspend fun persistLogin(
        cookies: String,
        uid: Long,
        userName: String,
        deviceName: String,
    ) {
        authStore.save(
            cookies = cookies,
            uid = uid,
            userName = userName,
            device = deviceName,
        )
    }

    suspend fun signOut() {
        authStore.clear()
    }
}

package com.starvault.data.repository

import com.starvault.data.local.auth.Cloud115AuthStore
import com.starvault.data.remote.cloud115.ScanLoginManager
import com.starvault.data.remote.cloud115.SpaceSummuryData
import com.starvault.data.remote.cloud115.UserApiService
import com.starvault.data.remote.cloud115.UserBaseInfoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
 *  - fetchQrCode()  : 拉 QR 码给 VM 渲染
 *  - signIn(qr)     : 启动扫码轮询；返回 Flow<ScanStatus>，VM 透传
 *  - persistLogin() : 在 ScanStatus.Success 拿到 cookies + userInfo 时落 DataStore
 *  - signOut()      : 清 cookies → authState 自动变 Unauthenticated
 *  - fetchUserInfo(): 并行拉 webapi /users/userinfo + /user/space_summury，组装成 [UserInfo]
 *  - authState      : 来自 DataStore，进程内 + 跨进程一致
 *
 *  uid/userName 详情：在登录成功后通过 persistLogin 写入 DataStore，authState 也以 Authenticated
 *  形式反映（map 时 uid/userName 暂用 EMPTY 占位，由 VM 收到 Success 触发 persistLogin 后
 *  stateIn 自动重发新值）。
 *
 *  标 [open] 是为了 ProfileViewModelTest 可以继承 + 覆盖 [signOut] 注入失败行为。
 *  生产代码不会继承此类，仅 ServiceLocator 直接 new。
 */
open class AuthRepository(
    private val authStore: Cloud115AuthStore,
    private val scanManager: ScanLoginManager,
    private val userApi: UserApiService,
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

    open suspend fun signOut() {
        authStore.clear()
    }

    /**
     * 拉 webapi 用户信息 + 空间概要，并行调用两接口，组装成 [UserInfo]。
     *
     *  行为：
     *  - 两端点都用 cookies（由 [CookieInterceptor] 注入）；无 cookies 时两接口均 errno=990001
     *    → 单端点抛 IOException 不会终止另一端点，组装时取成功的部分
     *  - 完全失败（两接口都抛）→ 整体返回 Result.failure
     *  - 部分失败（仅一端成功）→ 返回 Result.success 带部分字段（空字段为 0L / null）
     *
     *  调用方（ProfileViewModel）负责把 [UserInfo] 映射成 UI 态；网络失败时 fallback mock。
     */
    suspend fun fetchUserInfo(): Result<UserInfo> = coroutineScope {
        val userInfoDeferred = async { runCatching { fetchUserBaseInfo() } }
        val spaceDeferred   = async { runCatching { fetchSpaceSummury() } }
        val userInfoResult  = userInfoDeferred.await()
        val spaceResult     = spaceDeferred.await()

        when {
            userInfoResult.isSuccess && spaceResult.isSuccess ->
                Result.success(UserInfo(userInfoResult.getOrThrow(), spaceResult.getOrThrow()))
            userInfoResult.isSuccess ->
                Result.success(UserInfo(userInfoResult.getOrThrow(), SpaceSummuryData()))
            spaceResult.isSuccess ->
                Result.success(UserInfo(UserBaseInfoData(), spaceResult.getOrThrow()))
            else -> Result.failure(
                userInfoResult.exceptionOrNull() ?: spaceResult.exceptionOrNull() ?: IllegalStateException("fetchUserInfo failed")
            )
        }
    }

    private suspend fun fetchUserBaseInfo(): UserBaseInfoData {
        val resp = userApi.getUserBaseInfo()
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
        val env = resp.body() ?: throw IllegalStateException("empty body")
        if (env.state != 1) throw IllegalStateException(env.message ?: "state=${env.state}")
        return env.data ?: throw IllegalStateException("data is null")
    }

    private suspend fun fetchSpaceSummury(): SpaceSummuryData {
        val resp = userApi.getSpaceSummury()
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
        val env = resp.body() ?: throw IllegalStateException("empty body")
        if (env.state != 1) throw IllegalStateException(env.message ?: "state=${env.state}")
        return env.data ?: throw IllegalStateException("data is null")
    }
}

/** fetchUserInfo 组装结果：用户基础信息 + 空间概要。 */
data class UserInfo(
    val base: UserBaseInfoData,
    val space: SpaceSummuryData,
)

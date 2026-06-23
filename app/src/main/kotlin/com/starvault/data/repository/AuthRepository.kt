package com.starvault.data.repository

import com.starvault.data.local.auth.OpenAuthStore
import com.starvault.data.remote.cloud115.OpenAuthManager
import com.starvault.data.remote.cloud115.OpenAuthApiService
import com.starvault.data.remote.cloud115.SpaceSummuryData
import com.starvault.data.remote.cloud115.UserApiService
import com.starvault.data.remote.cloud115.UserBaseInfoData
import com.starvault.data.remote.cloud115.isOk
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
 *  - Unauthenticated : 无 access_token（首次启动 / 已 Logout）
 *  - Authenticated   : access_token 有效；uid/userName 由 persistTokens 时落库，stateIn 自动重发
 *
 * VM 只观察这一个 StateFlow；NavHost 据此决定 startDestination。
 *
 * 与旧版的差异（决策 #8）：`device` 字段删除（OAuth 响应不带）。
 */
sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(
        val uid: Long,
        val userName: String,
        val usedSpace: Long,
        val totalSpace: Long,
    ) : AuthState {
        companion object {
            /** 临时占位：accessToken 存在但 uid/userName 尚未落库；stateIn 会在下次发射前替换。 */
            val EMPTY = Authenticated(0L, "", 0L, 0L)
        }
    }
}

/**
 * 认证仓库：聚合 [OpenAuthStore]（持久态）+ [OpenAuthManager]（一次性登录流程）。
 *
 *  - requestDeviceCode() : 拉设备码 + QR 给 VM 渲染
 *  - pollForToken(dc)    : 启动轮询；返回 Flow<AuthStatus>，VM 透传
 *  - persistTokens()     : 在 AuthStatus.Authorized 拿到 tokens + userInfo 时落 DataStore
 *  - signOut()           : 先调服务端 revoke（fire-and-forget，吞异常）→ 清本地 store → authState 自动 Unauthenticated
 *  - fetchUserInfo()     : 并行拉 webapi /users/userinfo + /user/space_summury，组装成 [UserInfo]
 *  - authState           : 来自 DataStore accessTokenFlow，进程内 + 跨进程一致
 *
 *  标 [open] 是为了 ProfileViewModelTest 可以继承 + 覆盖 [signOut] 注入失败行为。
 *  生产代码不会继承此类，仅 ServiceLocator 直接 new。
 */
open class AuthRepository(
    private val tokenStore: OpenAuthStore,
    private val authManager: OpenAuthManager,
    private val userApi: UserApiService,
    appScope: CoroutineScope,
) {
    /**
     * authState 派生：
     *  - accessToken 非空 → Authenticated.EMPTY（uid/userName 后续由 persistTokens 触发 stateIn 重发）
     *  - 空 → Unauthenticated
     *
     * 不做 expiresAtMs 时钟校验：lazy refresh 走 /open/authTokenRefresh（Phase 2）
     * 或 401 → 用户掉登录 → 重扫。本期简单直接。
     */
    val authState: StateFlow<AuthState> = tokenStore.accessTokenFlow
        .map { at -> if (at.isNullOrBlank()) AuthState.Unauthenticated else AuthState.Authenticated.EMPTY }
        .stateIn(appScope, SharingStarted.Eagerly, AuthState.Unauthenticated)

    /** 拿设备码 + QR：一次性，返回 Result 包装。 */
    suspend fun requestDeviceCode(): Result<OpenAuthManager.DeviceCodeData> =
        authManager.requestDeviceCode()

    /** 启动 token 轮询；返回 Flow 给 VM 订阅。 */
    fun pollForToken(deviceCode: OpenAuthManager.DeviceCodeData): Flow<OpenAuthManager.AuthStatus> =
        authManager.pollForToken(deviceCode)

    /**
     * 登录成功时由 VM 调用：把 tokens + userInfo 落 DataStore。
     * authState StateFlow 会自动重发 Authenticated（带真实 uid/userName）。
     */
    suspend fun persistTokens(
        accessToken: String,
        refreshToken: String,
        expiresIn: Long,
        uid: Long,
        userName: String,
    ) {
        tokenStore.saveTokens(
            accessToken  = accessToken,
            refreshToken = refreshToken,
            expiresIn    = expiresIn,
            uid          = uid,
            userName     = userName,
        )
    }

    /**
     * signOut 流程（决策 #3 / #11）：
     *  1. 尝试服务端 revoke refresh_token — 任何异常吞掉（fire-and-forget）
     *  2. 清本地 DataStore → accessTokenFlow 触发 Unauthenticated → NavHost 自动跳 Login
     *
     * revoke 失败不致命：用户一定会登出；服务端占位 2h 自然过期。
     */
    open suspend fun signOut() {
        val refresh = tokenStore.refreshToken()
        authManager.revokeToken(refresh.orEmpty())
        tokenStore.clear()
    }

    /**
     * 拉 webapi 用户信息 + 空间概要，并行调用两接口，组装成 [UserInfo]。
     *
     *  行为：
     *  - 两端点都用 Bearer（由 [AuthHeaderInterceptor] 注入）；无 token 时两接口均 errno=990001
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
        if (!env.isOk) throw IllegalStateException(env.error ?: env.message ?: "state=${env.state} errno=${env.errno}")
        return env.data ?: throw IllegalStateException("data is null")
    }

    private suspend fun fetchSpaceSummury(): SpaceSummuryData {
        val resp = userApi.getSpaceSummury()
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
        val r = resp.body() ?: throw IllegalStateException("empty body")
        if (!r.isOk) throw IllegalStateException(r.error ?: "state=${r.state} errno=${r.errno}")
        return SpaceSummuryData(spaceSummury = r.spaceSummury, typeSummury = r.typeSummury)
    }
}

/** fetchUserInfo 组装结果：用户基础信息 + 空间概要。 */
data class UserInfo(
    val base: UserBaseInfoData,
    val space: SpaceSummuryData,
)
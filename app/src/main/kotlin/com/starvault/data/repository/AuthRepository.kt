package com.starvault.data.repository

import com.starvault.data.local.auth.OpenAuthStore
import com.starvault.data.remote.cloud115.OpenAuthManager
import com.starvault.data.remote.cloud115.OpenUserApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 统一认证状态。
 *
 *  - Unauthenticated : 无 access_token（首次启动 / 已 Logout）
 *  - Authenticated   : access_token 有效；uid/userName/vipLevelName/usedSpace/totalSpace
 *    由 [persistTokens] 时落库,stateIn 自动重发
 *
 * VM 只观察这一个 StateFlow;NavHost 据此决定 startDestination。
 */
sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(
        val uid: Long,
        val userName: String,
        val vipLevelName: String,
        val usedSpace: Long,
        val totalSpace: Long,
    ) : AuthState {
        companion object {
            /** 临时占位:accessToken 存在但 userName/vipLevelName/space 尚未落库;stateIn 会在下次发射前替换。 */
            val EMPTY = Authenticated(0L, "", "", 0L, 0L)
        }
    }
}

/**
 * 认证仓库:聚合 [OpenAuthStore](持久态) + [OpenAuthManager](一次性登录流程)。
 *
 *  - requestDeviceCode() : 拉设备码 + QR 给 VM 渲染
 *  - pollForToken(dc)    : 启动轮询;返回 Flow<AuthStatus>,VM 透传
 *  - persistTokens()     : 在 AuthStatus.Authorized 拿到 tokens + userInfo 时落 DataStore
 *  - signOut()           : 先调服务端 revoke(fire-and-forget,吞异常) → 清本地 store → authState 自动 Unauthenticated
 *  - fetchUserInfo()     : 单端点调用 proapi /open/user/info,组装成 [UserInfo]
 *  - authState           : 来自 DataStore accessTokenFlow,进程内 + 跨进程一致
 *
 *  标 [open] 是为了 ProfileViewModelTest 可以继承 + 覆盖 [signOut] 注入失败行为。
 *  生产代码不会继承此类,仅 ServiceLocator 直接 new。
 */
open class AuthRepository(
    private val tokenStore: OpenAuthStore,
    private val authManager: OpenAuthManager,
    private val openUserApi: OpenUserApiService,
    appScope: CoroutineScope,
) {
    /**
     * authState 派生:
     *  - accessToken 非空 → Authenticated.EMPTY(uid/userName 后续由 persistTokens 触发 stateIn 重发)
     *  - 空 → Unauthenticated
     *
     * 不做 expiresAtMs 时钟校验:lazy refresh 走 /open/authTokenRefresh(Phase 2)
     * 或 401 → 用户掉登录 → 重扫。本期简单直接。
     */
    val authState: StateFlow<AuthState> = tokenStore.accessTokenFlow
        .map { at -> if (at.isNullOrBlank()) AuthState.Unauthenticated else AuthState.Authenticated.EMPTY }
        .stateIn(appScope, SharingStarted.Eagerly, AuthState.Unauthenticated)

    /** 拿设备码 + QR:一次性,返回 Result 包装。 */
    suspend fun requestDeviceCode(): Result<OpenAuthManager.DeviceCodeData> =
        authManager.requestDeviceCode()

    /** 启动 token 轮询;返回 Flow 给 VM 订阅。 */
    fun pollForToken(deviceCode: OpenAuthManager.DeviceCodeData): Flow<OpenAuthManager.AuthStatus> =
        authManager.pollForToken(deviceCode)

    /**
     * 登录成功时由 VM 调用:把 tokens + userInfo 落 DataStore。
     * authState StateFlow 会自动重发 Authenticated(带真实 uid/userName/vip/space)。
     */
    suspend fun persistTokens(
        accessToken: String,
        refreshToken: String,
        expiresIn: Long,
        uid: Long,
        userName: String,
        vipLevelName: String = "",
        usedSpace: Long = 0L,
        totalSpace: Long = 0L,
    ) {
        tokenStore.saveTokens(
            accessToken   = accessToken,
            refreshToken  = refreshToken,
            expiresIn     = expiresIn,
            uid           = uid,
            userName      = userName,
            vipLevelName  = vipLevelName,
        )
        // space 暂存内存,VM loadUserInfo 后会用真值更新 UI 态
        // (后续 Phase 可加 usedSpaceKey / totalSpaceKey 到 DataStore,本期先落 UI 态)
    }

    /**
     * signOut 流程:
     *  1. 尝试服务端 revoke refresh_token — 任何异常吞掉(fire-and-forget)
     *  2. 清本地 DataStore → accessTokenFlow 触发 Unauthenticated → NavHost 自动跳 Login
     *
     * revoke 失败不致命:用户一定会登出;服务端占位 2h 自然过期。
     */
    open suspend fun signOut() {
        val refresh = tokenStore.refreshToken()
        authManager.revokeToken(refresh.orEmpty())
        tokenStore.clear()
    }

    /**
     * 拉用户信息(单端点 proapi /open/user/info)。
     *
     *  OAuth Bearer 鉴权专用域。响应顶层是 `{state, data:{...}, error}`,
     * data 节点含 user_id/user_name/rt_space_info/vip_info(实测见 [UserBaseInfoData])。
     *
     *  错误:
     *  - HTTP / 业务失败 → Result.failure
     *  - 成功 → [UserInfo]
     */
    suspend fun fetchUserInfo(): Result<UserInfo> = runCatching {
        val resp = openUserApi.userInfo()
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code()}")
        val body = resp.body() ?: throw IllegalStateException("empty body")
        if (body.state != true) {
            throw IllegalStateException(body.error ?: body.message ?: "state=${body.state} errno=${body.errno ?: body.errNo ?: -1}")
        }
        val data = body.data ?: throw IllegalStateException("data is null")
        UserInfo(
            base = data,
            space = data.rtSpaceInfo,
            vip = data.vipInfo,
        )
    }
}

/** fetchUserInfo 结果:用户基础信息(含头像) + 空间概要 + VIP。 */
data class UserInfo(
    val base: com.starvault.data.remote.cloud115.UserBaseInfoData,
    val space: com.starvault.data.remote.cloud115.RtSpaceInfo?,
    val vip: com.starvault.data.remote.cloud115.VipInfo?,
)

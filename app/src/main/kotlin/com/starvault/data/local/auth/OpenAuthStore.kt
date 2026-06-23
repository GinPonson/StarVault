package com.starvault.data.local.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// DataStore 实例 — name 由旧的 "cloud115_auth" 改为 "cloud115_tokens"，
// 与 Cookie 时代隔离，老用户重新登录一次即可（无迁移代码）。
private val Context.tokensDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "cloud115_tokens")

/**
 * 115 OAuth tokens + 用户信息持久化。
 *
 * 替换 [Cloud115AuthStore]（旧 store 存的是 Cookie 串）。
 *
 * 关键字段：
 *  - accessToken  : Bearer 鉴权串；非空即视为已登录
 *  - refreshToken : 用于 signOut 调 /open/authTokenRevoke，**不入 OkHttp 拦截器**
 *  - expiresAtMs  : 过期时间戳（毫秒）；过期只是兜底，正常走 lazy refresh
 *  - uid / userName / usedSpace / totalSpace : Profile 屏展示用
 *
 * 设计要点：
 *  - accessToken 单独暴露为 Flow<String?>，供 [com.starvault.data.repository.AuthRepository]
 *    派生 authState 用
 *  - 同步读 accessToken 用 [accessTokenBlocking]，供 OkHttp Interceptor 同步接口用
 *  - refreshToken 仅暴露 suspend 接口，避免被拦截器误用
 *  - 不做 "cookiesKey → accessTokenKey" 数据迁移；老用户重新扫码登录一次
 */
class OpenAuthStore(private val context: Context) {

    private val store: DataStore<Preferences> = context.tokensDataStore

    // ─── keys ─────────────────────────────────────────────────────
    private val accessTokenKey  = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val expiresAtMsKey  = longPreferencesKey("expires_at_ms")
    private val uidKey          = longPreferencesKey("uid")
    private val userNameKey     = stringPreferencesKey("user_name")
    private val usedSpaceKey    = longPreferencesKey("used_space")
    private val totalSpaceKey   = longPreferencesKey("total_space")

    // ─── 同步读 accessToken（给 OkHttp Interceptor 用）─────────────────────
    /**
     * 同步取当前 accessToken。
     *
     * Interceptor 是同步接口，必须用 runBlocking（DataStore 内存读够快，blocking 时间 < 1ms）。
     * 未登录时返回 null，调用方据此判断是否加 Authorization 头。
     */
    fun accessTokenBlocking(): String? = runBlocking { accessTokenFlow.first() }

    /** 主状态流：非空 → Authenticated；空 → Unauthenticated。 */
    val accessTokenFlow: Flow<String?> = store.data.map { it[accessTokenKey] }

    /** 当前完整 token 三元组（VM 调试 / 未来 refresh 场景用）。 */
    val tokenStateFlow: Flow<TokenState> = store.data.map { p ->
        TokenState(
            accessToken  = p[accessTokenKey],
            refreshToken = p[refreshTokenKey],
            expiresAtMs  = p[expiresAtMsKey] ?: 0L,
            uid          = p[uidKey] ?: 0L,
            userName     = p[userNameKey].orEmpty(),
        )
    }

    /** 当前完整 token 三元组快照。 */
    data class TokenState(
        val accessToken: String?,
        val refreshToken: String?,
        val expiresAtMs: Long,
        val uid: Long,
        val userName: String,
    )

    /**
     * 取当前 refreshToken（signOut 用）。
     *
     * 单独 suspend 接口，避免被 OkHttp Interceptor 误用。
     */
    suspend fun refreshToken(): String? = store.data.first()[refreshTokenKey]

    /**
     * 一次性落库 OAuth 三件套 + 用户信息。
     *
     * @param accessToken  Bearer 鉴权串（必填）
     * @param refreshToken refresh_token（必填，给 signOut revoke 用）
     * @param expiresIn    115 返回的 expires_in（秒），内部转 expiresAtMs = nowMs + expiresIn*1000
     * @param uid          115 user_id
     * @param userName     115 用户昵称
     * @param usedSpace    已用空间（Profile 屏展示），默认 0
     * @param totalSpace   总空间，默认 0
     */
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresIn: Long,
        uid: Long,
        userName: String,
        usedSpace: Long = 0L,
        totalSpace: Long = 0L,
    ) {
        val expiresAtMs = System.currentTimeMillis() + expiresIn * 1000L
        store.edit { p ->
            p[accessTokenKey]  = accessToken
            p[refreshTokenKey] = refreshToken
            p[expiresAtMsKey]  = expiresAtMs
            p[uidKey]          = uid
            p[userNameKey]     = userName
            p[usedSpaceKey]    = usedSpace
            p[totalSpaceKey]   = totalSpace
        }
    }

    /** 清空本地 token（signOut 用）。调用方应先调 revokeToken 再调此方法。 */
    suspend fun clear() {
        store.edit { it.clear() }
    }
}
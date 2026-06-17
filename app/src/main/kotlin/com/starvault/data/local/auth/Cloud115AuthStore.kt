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

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud115_auth")

/**
 * 115 cookies + 用户信息持久化。
 *
 * 关键字段：
 *  - cookies    : "UID=xxx; CID=xxx; SEID=xxx; KID=xxx" 扫码回包拼接
 *  - uid        : 115 user_id（Int64）
 *  - userName   : 显示用
 *  - device     : 扫码回包里的设备描述
 *  - usedSpace / totalSpace : 用于 Profile 屏（Phase 3 接入）
 *
 * cookies 一旦存在 → authState = Authenticated。
 * cookies 为空  → authState = Unauthenticated。
 */
class Cloud115AuthStore(private val context: Context) {

    private val store: DataStore<Preferences> = context.authDataStore

    // ─── keys ──────────────────────────────────────────────────
    private val cookiesKey    = stringPreferencesKey("cookies")
    private val uidKey        = longPreferencesKey("uid")
    private val userNameKey   = stringPreferencesKey("user_name")
    private val deviceKey     = stringPreferencesKey("device")
    private val usedSpaceKey  = longPreferencesKey("used_space")
    private val totalSpaceKey = longPreferencesKey("total_space")

    // ─── 同步读 cookies（给 OkHttp Interceptor 用，拦截器是同步接口）──
    /** 同步取当前 cookies。Interceptor 是同步接口，必须用 runBlocking（DataStore 读内存够快） */
    fun cookiesBlocking(): String? = runBlocking { cookiesFlow.first() }

    val cookiesFlow: Flow<String?> = store.data.map { it[cookiesKey] }

    // ─── 写 ─────────────────────────────────────────────────────
    suspend fun save(
        cookies: String,
        uid: Long,
        userName: String,
        device: String,
        usedSpace: Long = 0L,
        totalSpace: Long = 0L,
    ) {
        store.edit { p ->
            p[cookiesKey]    = cookies
            p[uidKey]        = uid
            p[userNameKey]   = userName
            p[deviceKey]     = device
            p[usedSpaceKey]  = usedSpace
            p[totalSpaceKey] = totalSpace
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}

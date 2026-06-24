package com.starvault.core

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.starvault.data.local.auth.OpenAuthStore
import com.starvault.data.remote.cloud115.Cloud115ApiClient
import com.starvault.data.remote.cloud115.OpenAuthApiService
import com.starvault.data.remote.cloud115.OpenAuthManager
import com.starvault.data.remote.cloud115.OpenFileApiService
import com.starvault.data.remote.cloud115.OpenUserApiService
import com.starvault.data.remote.cloud115.StatusPollApi
import com.starvault.data.repository.AuthRepository
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.MediaPreviewRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/**
 * 极简 ServiceLocator：单例持有数据层组件。
 *
 * 不引 Hilt / Koin（Phase 1 原则）。MainActivity.onCreate 时 init 一次，全程单例。
 * 所有 ViewModel 通过 [ServiceLocator] 拿依赖。
 *
 * 简单到不需要 Lazy / synchronization：MainActivity.onCreate 早于任何 ViewModel 构造。
 *
 * 替换历史（决策 #9）：Cookie 时代 → OAuth 时代
 *  - `authStore: Cloud115AuthStore`  → `tokenStore: OpenAuthStore`
 *  - `scanApi: ScanApiService`      → `openAuthApi: OpenAuthApiService` + `statusPollApi: StatusPollApi`
 *  - `scanManager: ScanLoginManager`→ `authManager: OpenAuthManager`
 *
 * 两个 OkHttpClient：
 *  - `okHttpClient`     : 30s 常规超时（proapi + qrcodeapi POST 端点 + Coil）
 *  - `statusPollClient` : 65s 长轮询（115 get/status/）
 */
object ServiceLocator {

    lateinit var tokenStore: OpenAuthStore
        private set

    lateinit var openAuthApi: OpenAuthApiService
        private set

    lateinit var statusPollApi: StatusPollApi
        private set

    /**
     * proapi open 域文件端点(供 [FilesRepository] + [MediaPreviewRepository] 用)。
     *
     * 全部走 OAuth Bearer 鉴权,包括 listFiles / searchFiles / getInfo / downurl / videoPlay。
     * webapi 域已经迁完,整个文件域不再走 Cookie。
     *
     * 复用同一个 [okHttpClient](Bearer 注入 + 浏览器伪装头 + Android UA),只换 baseUrl。
     */
    lateinit var openFileApi: OpenFileApiService
        private set

    /**
     * proapi open 域用户端点(供 [AuthRepository.fetchUserInfo] 用)。
     */
    lateinit var openUserApi: OpenUserApiService
        private set

    lateinit var authManager: OpenAuthManager
        private set

    /**
     * 共享 OkHttpClient：Cloud115ApiClient 用（API 请求） + Coil 用（缩略图 GET，带 Bearer）。
     * 这样缩略图请求自动带 115 Bearer（thumb.115.com 签名 URL 必须登录态）。
     */
    lateinit var okHttpClient: OkHttpClient
        private set

    /**
     * 长轮询 OkHttpClient：65s read timeout，独立持有（不与 30s 共享连接池）。
     * 只服务 [StatusPollApi.getStatus]。
     */
    lateinit var statusPollClient: OkHttpClient
        private set

    /**
     * Application scope：长生命周期 scope（不随 ViewModel 取消）。
     *  - authState StateFlow 用此 scope 收集
     *  - 后续 BackgroundWorker / TransferEngine 也可复用
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob())

    lateinit var authRepository: AuthRepository
        private set

    lateinit var filesRepository: FilesRepository
        private set

    /**
     * Preview 仓库（image 原图 URL + video m3u8 URL）。
     * 走 proapi [openFileApi]（getInfo + downurl + videoPlay 三个端点都在 proapi 域）。
     */
    lateinit var mediaPreviewRepository: MediaPreviewRepository
        private set

    fun init(context: Context) {
        val appContext = context.applicationContext
        tokenStore = OpenAuthStore(appContext)
        val tokenProvider = tokenStore::accessTokenBlocking

        // 2 个 OkHttpClient：常规 30s 给 API/Coil，长轮询 65s 给 status 端点
        okHttpClient     = Cloud115ApiClient.buildOkHttpClient(tokenProvider = tokenProvider)
        statusPollClient = Cloud115ApiClient.buildLongPollOkHttpClient(tokenProvider = tokenProvider)

        openAuthApi   = Cloud115ApiClient.openAuthApiService(okHttpClient)
        statusPollApi = Cloud115ApiClient.statusPollApiService(statusPollClient)
        openUserApi   = Cloud115ApiClient.openUserApiService(okHttpClient)
        openFileApi   = Cloud115ApiClient.openFileApiService(okHttpClient)
        authManager   = OpenAuthManager(api = openAuthApi, statusApi = statusPollApi)
        authRepository = AuthRepository(
            tokenStore  = tokenStore,
            authManager = authManager,
            openUserApi = openUserApi,
            appScope    = appScope,
        )
        filesRepository = FilesRepository(api = openFileApi)
        mediaPreviewRepository = MediaPreviewRepository(api = openFileApi)
        // Coil 全局 ImageLoader 注入 OkHttpNetworkFetcher（带 Bearer）
        SingletonImageLoader.setSafe { ctx ->
            ImageLoader.Builder(ctx)
                .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
                .build()
        }
    }
}

/** Coil 3 需要 PlatformContext；这里 alias 一下方便 IDE 提示。 */
private typealias PlatformContextAlias = PlatformContext
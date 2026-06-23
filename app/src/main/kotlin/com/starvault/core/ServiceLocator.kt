package com.starvault.core

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.starvault.data.local.auth.OpenAuthStore
import com.starvault.data.remote.cloud115.Cloud115ApiClient
import com.starvault.data.remote.cloud115.FileApiService
import com.starvault.data.remote.cloud115.OpenAuthApiService
import com.starvault.data.remote.cloud115.OpenAuthManager
import com.starvault.data.remote.cloud115.UserApiService
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
 *  - `scanApi: ScanApiService`      → `openAuthApi: OpenAuthApiService`
 *  - `scanManager: ScanLoginManager`→ `authManager: OpenAuthManager`
 */
object ServiceLocator {

    lateinit var tokenStore: OpenAuthStore
        private set

    lateinit var openAuthApi: OpenAuthApiService
        private set

    lateinit var userApi: UserApiService
        private set

    lateinit var filesApi: FileApiService
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
     * 复用现有 [filesApi] —— 同一个 OkHttpClient + Bearer 注入链路。
     */
    lateinit var mediaPreviewRepository: MediaPreviewRepository
        private set

    fun init(context: Context) {
        val appContext = context.applicationContext
        tokenStore = OpenAuthStore(appContext)
        val tokenProvider = tokenStore::accessTokenBlocking

        // 1 个 OkHttpClient 给所有 cloud115 流量（API + 缩略图）共享，确保 Bearer 一致
        okHttpClient = Cloud115ApiClient.buildOkHttpClient(tokenProvider = tokenProvider)
        openAuthApi = Cloud115ApiClient.openAuthApiService(okHttpClient)
        userApi = Cloud115ApiClient.userApiService(okHttpClient)
        filesApi = Cloud115ApiClient.fileApiService(okHttpClient)
        authManager = OpenAuthManager(api = openAuthApi)
        authRepository = AuthRepository(
            tokenStore  = tokenStore,
            authManager = authManager,
            userApi     = userApi,
            appScope    = appScope,
        )
        filesRepository = FilesRepository(api = filesApi)
        mediaPreviewRepository = MediaPreviewRepository(api = filesApi)
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
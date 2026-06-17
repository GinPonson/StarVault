package com.starvault.core

import android.content.Context
import com.starvault.data.local.auth.Cloud115AuthStore
import com.starvault.data.remote.cloud115.Cloud115ApiClient
import com.starvault.data.remote.cloud115.FileApiService
import com.starvault.data.remote.cloud115.ScanApiService
import com.starvault.data.remote.cloud115.ScanLoginManager
import com.starvault.data.remote.cloud115.UserApiService
import com.starvault.data.repository.AuthRepository
import com.starvault.data.repository.FilesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * 极简 ServiceLocator：单例持有数据层组件。
 *
 * 不引 Hilt / Koin（Phase 1 原则）。MainActivity.onCreate 时 init 一次，全程单例。
 * 所有 ViewModel 通过 [ServiceLocator] 拿依赖。
 *
 * 简单到不需要 Lazy / synchronization：MainActivity.onCreate 早于任何 ViewModel 构造。
 */
object ServiceLocator {

    lateinit var authStore: Cloud115AuthStore
        private set

    lateinit var scanApi: ScanApiService
        private set

    lateinit var userApi: UserApiService
        private set

    lateinit var filesApi: FileApiService
        private set

    lateinit var scanManager: ScanLoginManager
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

    fun init(context: Context) {
        val appContext = context.applicationContext
        authStore = Cloud115AuthStore(appContext)
        val cookieProvider = authStore::cookiesBlocking
        scanApi = Cloud115ApiClient.scanApiService(cookieProvider = cookieProvider)
        userApi = Cloud115ApiClient.userApiService(cookieProvider = cookieProvider)
        filesApi = Cloud115ApiClient.fileApiService(cookieProvider = cookieProvider)
        scanManager = ScanLoginManager(api = scanApi, authStore = authStore)
        authRepository = AuthRepository(
            authStore = authStore,
            scanManager = scanManager,
            userApi = userApi,
            appScope = appScope,
        )
        filesRepository = FilesRepository(api = filesApi)
    }
}

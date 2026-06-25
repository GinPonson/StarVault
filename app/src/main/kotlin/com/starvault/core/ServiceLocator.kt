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
import com.starvault.data.remote.cloud115.OpenUploadApiService
import com.starvault.data.remote.cloud115.OpenUserApiService
import com.starvault.data.remote.cloud115.StatusPollApi
import com.starvault.data.remote.cloud115.Token401Interceptor
import com.starvault.data.repository.AuthRepository
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.MediaPreviewRepository
import com.starvault.data.upload.OssUploader
import com.starvault.data.upload.UploadInitClient
import com.starvault.data.uploadworker.UploadExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import okhttp3.OkHttpClient

/**
 * 极简 ServiceLocator：单例持有数据层组件。
 *
 * 不引 Hilt / Koin（Phase 1 原则）。MainActivity.onCreate 时 init 一次，全程单例。
 * 所有 ViewModel 通过 [ServiceLocator] 拿依赖。
 *
 * ## Init 顺序契约（强约束）
 *
 * 所有 `lateinit var` 都依赖 [init] 在 [MainActivity.onCreate] 同步完成,再有任何 ViewModel
 * 构造。单模块单线程,加 `@Volatile` + `private set` 防止外部写、确保可见性。
 * 如果未来 [init] 改成异步或被多次调用,会立即抛 `UninitializedPropertyAccessException`。
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

    /**
     * proapi open 域上传端点(供 [com.starvault.data.upload.UploadInitClient] 用)。
     *
     * 走 proapi + Bearer 主链路;401 自动 refresh 全复用 [okHttpClient] 上的
     * [Token401Interceptor]。M2 spec §3.1 三端点:
     *  - GET  /open/upload/get_token
     *  - POST /open/upload/init
     *  - POST /open/upload/resume(M3 才用,本 spec 不实现)
     */
    lateinit var openUploadApi: OpenUploadApiService
        private set

    /**
     * 上传执行器(Phase 3 注入)— [com.starvault.data.uploadworker.UploadWorker] 通过此
     * 拿依赖,保持 Worker 本体纯 WorkManager 适配,业务在 [UploadExecutor] 里。
     */
    lateinit var uploadInitClient: UploadInitClient
        private set

    /**
     * 上传 OSS 编排器(Phase 2 注入)— [UploadExecutor] 通过此调 Aliyun OSS。
     * 默认用 [com.starvault.data.upload.OssUploader.NullOssOperations](任何调用都抛),
     * ServiceLocator.init 阶段需要替换成真 [com.starvault.data.upload.AliyunOssOperations] —
     * 那是 Phase 5 接 [AliyunOssClientFactory] 时的事。
     */
    lateinit var ossUploader: OssUploader
        private set

    /**
     * 上传执行器(Phase 3)— Worker 业务编排。
     * 构造在 ServiceLocator.init 末尾,因为依赖 [uploadInitClient] / [ossUploader] / [openUploadApi]。
     */
    lateinit var uploadExecutor: UploadExecutor
        private set

    lateinit var authManager: OpenAuthManager
        private set

    /**
     * 共享 OkHttpClient：Cloud115ApiClient 用（API 请求） + Coil 用（缩略图 GET，带 Bearer）。
     * 这样缩略图请求自动带 115 Bearer（thumb.115.com 签名 URL 必须登录态）。
     *
     * 拦截器链路:browserLikeHeader + AuthHeader + Token401(401 开头自动 refresh,含 99)。
     */
    lateinit var okHttpClient: OkHttpClient
        private set

    /**
     * 独立 refresh 专用 OkHttpClient：只挂浏览器伪装头 + Bearer,**不**挂 Token401。
     * - Bearer:refresh API 走 passportapi `/open/refreshToken`,服务端要 Authorization 头
     * - Token401:避免 refresh API 自身 401 触发递归
     */
    lateinit var refreshClient: OkHttpClient
        private set

    /**
     * refresh API（[OpenAuthApiService.refreshToken]）实例，挂在 [refreshClient] 上。
     * baseUrl = `https://passportapi.115.com/`(对齐 OpenList 115-sdk-go)。
     */
    lateinit var refreshApi: OpenAuthApiService
        private set

    /**
     * 进程级 Mutex:串行化所有 401 开头(或 code==99)触发的 refresh,避免 N 个并发请求各自 refresh。
     * 暴露为 val(不是 lateinit var)因为 Mutex() 无副作用、无 init 依赖。
     */
    val refreshMutex: Mutex = Mutex()

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

        // 3 个 OkHttpClient:
        // - refreshClient   : 独立,挂浏览器头 + Bearer,baseUrl=passportapi,
        //                    调 /open/refreshToken(对齐 OpenList 115-sdk-go const.go)
        // - okHttpClient    : 主链路,挂 Bearer + Token401(401 开头自动 refresh)
        // - statusPollClient: 长轮询 65s,挂 Bearer(不挂 Token401,status 端点不会被误判)
        refreshClient   = Cloud115ApiClient.buildRefreshClient(tokenProvider = tokenProvider)
        refreshApi      = Cloud115ApiClient.passportApiRetrofit(refreshClient)
            .create(OpenAuthApiService::class.java)
        okHttpClient    = Cloud115ApiClient.buildOkHttpClient(
            tokenProvider        = tokenProvider,
            token401Interceptor  = Token401Interceptor(
                tokenStore = tokenStore,
                refreshApi = refreshApi,
                mutex      = refreshMutex,
            ),
        )
        statusPollClient = Cloud115ApiClient.buildLongPollOkHttpClient(tokenProvider = tokenProvider)

        openAuthApi   = Cloud115ApiClient.openAuthApiService(okHttpClient)
        statusPollApi = Cloud115ApiClient.statusPollApiService(statusPollClient)
        openUserApi   = Cloud115ApiClient.openUserApiService(okHttpClient)
        openFileApi   = Cloud115ApiClient.openFileApiService(okHttpClient)
        openUploadApi = Cloud115ApiClient.openUploadApiService(okHttpClient)
        authManager   = OpenAuthManager(api = openAuthApi, statusApi = statusPollApi)
        authRepository = AuthRepository(
            tokenStore  = tokenStore,
            authManager = authManager,
            openUserApi = openUserApi,
            appScope    = appScope,
        )
        filesRepository = FilesRepository(api = openFileApi)
        mediaPreviewRepository = MediaPreviewRepository(api = openFileApi)

        // M2 upload 依赖(Phase 3 引入)
        uploadInitClient = UploadInitClient(api = openUploadApi)
        // ossUploader 必须由调用方在 init 末尾注入(Phase 5 引入 AliyunOssClientFactory) —
        // 这里用 default NullOssOperations 占位,让 ServiceLocator 编译通过。
        // Phase 5 修改:替换成真 AliyunOssOperations。
        ossUploader = OssUploader()
        uploadExecutor = UploadExecutor(
            uploadInitClient = uploadInitClient,
            ossUploader = ossUploader,
            api = openUploadApi,
        )

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
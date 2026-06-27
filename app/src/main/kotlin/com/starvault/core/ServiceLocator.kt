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
import com.starvault.data.download.DownloadSaveUri
import com.starvault.data.download.OssDownloader
import com.starvault.data.downloadworker.DownloadExecutor
import com.starvault.data.downloadworker.DownloadWork
import com.starvault.data.repository.AuthRepository
import com.starvault.data.repository.DownloadRepository
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.MediaPreviewRepository
import com.starvault.data.repository.TransferRepository
import com.starvault.data.upload.OssUploader
import com.starvault.data.upload.UploadInitClient
import com.starvault.data.upload.ossClientFactory
import com.starvault.data.uploadworker.UploadExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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

    /**
     * Application context — [androidx.work.WorkManager.getInstance] 注入用。
     *
     * init() 阶段从 [init] 参数保存。Process 死亡后 Application onCreate 必重 init,
     * 不会出现 lateinit 还没赋值就被读的情况(M1 没遇到)。
     */
    lateinit var appContext: Context
        private set

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

    /**
     * 下载落盘辅助(M3)— [com.starvault.data.downloadworker.DownloadExecutor] 通过此
     * 写 `MediaStore.Downloads` 公开目录。构造依赖 [appContext].contentResolver。
     */
    lateinit var downloadSaveUri: DownloadSaveUri
        private set

    /**
     * 下载 OkHttp 流式 GET 器(M3)— [com.starvault.data.downloadworker.DownloadExecutor]
     * 调 [OssDownloader.download] 拉签名 URL 写流。复用 [okHttpClient](`browserLikeHeader`
     * + `AuthHeader` + `Token401`,UA 已自动注入 — CDN 直链必需)。
     *
     * 已知风险(plan #11):Token401 在签名 URL 401 时浪费 ~60s。暂接受,M4 优化。
     */
    lateinit var ossDownloader: OssDownloader
        private set

    /**
     * 下载执行器(M3)— Worker 业务编排(downurl → MediaStore.insert → 流式写 → publish/delete)。
     * 构造依赖 [openFileApi] / [downloadSaveUri] / [ossDownloader]。
     */
    lateinit var downloadExecutor: DownloadExecutor
        private set

    /**
     * 下载入口仓库(M3)— [com.starvault.ui.files.FilesViewModel] 通过此把单文件下载
     * 任务投递到 WorkManager,内部 `downloadWorkTrigger.trySend(workId)` 把 workId
     * 桥接到 [com.starvault.ui.transfers.TransfersViewModel]。
     */
    lateinit var downloadRepository: DownloadRepository
        private set

    /**
     * 下载 work 触发器 — [DownloadRepository.enqueue] 把 [DownloadWork] envelope
     * 桥接到 [com.starvault.ui.transfers.TransfersViewModel.observeDownloadWork]。
     *
     * ## 选型 Channel(UNLIMITED)
     *  对齐 [filesRefreshTrigger] 的设计(plan 同步策略)— 永不丢信号,新 collector 起来
     *  后 drain 缓冲。
     *
     * ## API
     *  - 生产端:[downloadWorkTrigger].trySend(DownloadWork(...)) — [DownloadRepository.enqueue] 末尾。
     *  - 消费端:[downloadWorkFlow].collect { observeDownloadWork(it) } —
     *    [com.starvault.ui.transfers.TransfersViewModel] init。
     */
    lateinit var downloadWorkTrigger: Channel<DownloadWork>
        private set

    /** 消费端 Flow — TransfersViewModel 订阅这个(Phase B)。 */
    lateinit var downloadWorkFlow: Flow<DownloadWork>
        private set

    /**
     * 内存版 Transfer 仓库(Phase 5)— Transfers 屏 ViewModel 通过此聚合状态。
     * 不依赖 Context,可提前构造。
     */
    lateinit var transferRepository: TransferRepository
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
     * 文件列表刷新触发器 — 上传完成后通知 [com.starvault.ui.files.FilesViewModel] 自动
     * reload 当前目录。
     *
     * ## 选型 Channel(UNLIMITED) 而不是 SharedFlow(replay=0)
     *  - **Queue semantics**:SharedFlow replay=0 时,emit 时如果无 collector 在线,信号直接丢
     *    → "VM 后创建时不会突然触发 reload" 这个目标 SharedFlow 反而达不到(它把信号也丢了,
     *    不是排队)。
     *  - Channel(UNLIMITED) 永不丢:emit 时 buffer 起来,collector subscribe 后从头部依次消费。
     *    FilesViewModel nav-scoped pop 后再回来,新 collector 把历史上挂着的信号一次消费掉。
     *  - 对齐 CLAUDE.md §5 Compose one-shot events 约定(`Channel(UNLIMITED) + receiveAsFlow()`)。
     *
     * ## API
     *  - 生产端:[filesRefreshTrigger].trySend(Unit) — 挂在 [com.starvault.ui.transfers.TransfersViewModel.observeWork]
     *    的 `phase == DONE` 分支(`appScope` 内 collect,跨 nav pop 不死)。
     *  - 消费端:[filesRefreshFlow].collect { refresh() } — 挂在
     *    [com.starvault.ui.files.FilesViewModel] init。
     *
     * ## 多 collector 注意
     *  Channel 是 single-consumer,每条消息给一个 collector。当前只有 FilesVM 一处 collect;
     *  未来需要多 collector 时再 fan-out(BroadcastChannel / 多个 Channel 各自订阅)— 暂不需要。
     */
    lateinit var filesRefreshTrigger: Channel<Unit>
        private set

    /**
     * 消费端 Flow — FilesViewModel 订阅这个。新 collector 起来时 drain channel 缓冲。
     *
     * 挂为 `lateinit var` 是为了测试可重置 — Channel 是 single-consumer,前面 test 创建的
     * FilesViewModel collector 会持续 suspend 在 receive(),后续 test emit 的 Unit 会被旧
     * collector 抢先拿走,造成状态污染。生产代码不要直接重置。
     */
    lateinit var filesRefreshFlow: Flow<Unit>
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

    /**
     * 测试用 — 重置 filesRefreshTrigger 和 filesRefreshFlow,关旧 channel 重建。
     * Channel 是 single-consumer,前面 test 创建的 VM collector 还活着,新 emit 的 Unit
     * 会被旧 collector 抢走。`@Before` 调一次清状态。
     *
     * 只在 `app/src/test/` 调;**生产代码不要碰**(注释里写明)。
     */
    fun resetFilesRefreshTriggerForTest() {
        if (::filesRefreshTrigger.isInitialized) {
            filesRefreshTrigger.close()
        }
        val newChannel = Channel<Unit>(Channel.UNLIMITED)
        filesRefreshTrigger = newChannel
        filesRefreshFlow = newChannel.receiveAsFlow()
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        this.appContext = appContext
        tokenStore = OpenAuthStore(appContext)
        val tokenProvider = tokenStore::accessTokenBlocking

        // M2 upload 完成 → Files 自动刷新的 cross-VM trigger(Phase 6,详见字段 KDoc)
        val refreshChannel = Channel<Unit>(Channel.UNLIMITED)
        filesRefreshTrigger = refreshChannel
        filesRefreshFlow = refreshChannel.receiveAsFlow()

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
        // ossUploader 注入真 STS 工厂,每次 upload() 拿 STS 凭证现场构造 OSSClient。
        // 单测用 NullOssOperations(default)替换 stsClientFactory=null 走 fake ops。
        ossUploader = OssUploader(
            stsClientFactory = { sts -> ossClientFactory(appContext, sts) },
        )
        uploadExecutor = UploadExecutor(
            uploadInitClient = uploadInitClient,
            ossUploader = ossUploader,
            api = openUploadApi,
        )
        // M2 transfer 仓库(Phase 5 引入)
        transferRepository = TransferRepository()

        // M3 下载管道 — 与上传对称,复用 okHttpClient(UA 注入),落盘走 MediaStore.Downloads
        val downloadChannel = Channel<DownloadWork>(Channel.UNLIMITED)
        downloadWorkTrigger = downloadChannel
        downloadWorkFlow = downloadChannel.receiveAsFlow()
        downloadSaveUri = DownloadSaveUri(contentResolver = appContext.contentResolver)
        ossDownloader = OssDownloader(okHttpClient = okHttpClient)
        downloadExecutor = DownloadExecutor(
            api = openFileApi,
            downloadSaveUri = downloadSaveUri,
            ossDownloader = ossDownloader,
        )
        downloadRepository = DownloadRepository(
            context = appContext,
            downloadWorkTrigger = downloadWorkTrigger,
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
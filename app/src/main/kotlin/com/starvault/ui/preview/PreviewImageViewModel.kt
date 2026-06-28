package com.starvault.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.MediaPreviewRepository
import com.starvault.share.shareImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * PreviewImage ViewModel：拿 IMAGE 文件的原图 URL。
 *
 *  流程：
 *  1. [MediaPreviewRepository.fetchMetadata] 拿 pickcode + name + size + isMark
 *  2. [MediaPreviewRepository.fetchImageOriginalUrl] 拿 file_url
 *  3. 暴露 [PreviewUiState.Success]（mediaUrl = file_url）
 *
 *  错误策略:失败时 _state 不动(保持 Loading),错误经 ToastBus.error 投递,由全局 ToastHost 渲染 Snackbar。
 *
 *  复用 [FilesViewModel] 的 markPending 风格不可行——Preview 是只读屏（不切目录），所以状态机简单：
 *  init → Loading → Success / Error。不会中途再切状态。
 *
 *  星标([toggleStar]):
 *  - 初始值取 [MediaPreviewRepository.MediaMetadata.isMark] (`"1"`=已星标)
 *  - 乐观更新 + 失败回滚:点 ❤️ → 立刻翻本地 → 调 `repo.setStar` → 失败 ToastBus + 回滚
 *  - 文件 star 状态不影响图片渲染(图片直链走 downurl)
 *
 *  M5 CRUD:见 [onRename] / [onDelete] / [onMoveConfirmed] / [onShare] / [onDownload]。
 *  - 删除 / 移动成功后 emit [PreviewEvent.Deleted],Route 订阅后调 onBack() 回 Files
 *  - 重命名走乐观更新,失败回滚到 oldName
 *  - 分享走 [com.starvault.share.shareImage] (Android Sharesheet,image-only)
 *  - 下载复用 [MediaPreviewRepository.fetchImageOriginalUrl] + DownloadRepository.enqueue,
 *    对齐 PreviewVideo/Audio 的 onDownload
 */
class PreviewImageViewModel(
    private val fileId: String,
    private val repo: MediaPreviewRepository = ServiceLocator.mediaPreviewRepository,
    private val filesRepo: FilesRepository = ServiceLocator.filesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    /**
     * 当前文件星标状态。初始 false,Success emit 时由 metadata.isMark 同步;toggleStar 时翻。
     */
    private val _isStarred = MutableStateFlow(false)
    val isStarred: StateFlow<Boolean> = _isStarred.asStateFlow()

    /**
     * 一次性事件流(delete / move 成功后 → Route 调 onBack 回 Files)。
     *
     *  Channel(UNLIMITED) 的 queue semantics 保证 VM 不在线时 emit 不丢;
     *  Route 端 `LaunchedEffect(Unit) { vm.oneShotEvents.collect { ... } }` 订阅。
     */
    private val _oneShotEvents = Channel<PreviewEvent>(Channel.UNLIMITED)
    val oneShotEvents: Flow<PreviewEvent> = _oneShotEvents.receiveAsFlow()

    init {
        load()
    }

    /**
     * 加载原图 URL。已 Loading/Success 时再调用是 noop（防止 VM 重启 + 用户手动 retry 之间的竞争）。
     */
    fun load() {
        if (_state.value is PreviewUiState.Success) return
        _state.value = PreviewUiState.Loading
        viewModelScope.launch {
            val metadata = repo.fetchMetadata(fileId)
            metadata.fold(
                onSuccess = { meta ->
                    // proapi downurl 响应是 Map<file_id, item>,跟 OpenList 一致:resp[obj.GetID()]
                    val url = repo.fetchImageOriginalUrl(fileId = meta.fid, pickCode = meta.pickCode)
                    url.fold(
                        onSuccess = { u ->
                            _isStarred.value = meta.isMark == "1"
                            _state.value = PreviewUiState.Success(meta, u, qualityChip = "")
                        },
                        onFailure = { e ->
                            // 失败：_state 保持 Loading,仅 ToastBus 提示
                            ToastBus.error(e.message ?: "无法获取原图")
                        },
                    )
                },
                onFailure = { e ->
                    // 失败：_state 保持 Loading,仅 ToastBus 提示
                    ToastBus.error(e.message ?: "文件不存在或已删除")
                },
            )
        }
    }

    /**
     * 切换星标(❤️/♡):乐观更新 + 失败回滚。
     *
     * Loading 状态时 noop(无 fid 可用);Success 状态时取 meta.fid,115 /open/ufile/update 不需要 pick_code。
     */
    fun toggleStar() {
        val current = _state.value as? PreviewUiState.Success ?: return
        val next = !_isStarred.value
        _isStarred.value = next
        viewModelScope.launch {
            repo.setStar(fileId = current.metadata.fid, star = next)
                .onSuccess {
                    ToastBus.info(if (next) "已收藏" else "已取消收藏")
                }
                .onFailure { e ->
                    _isStarred.value = !next
                    ToastBus.error(e.message ?: "星标失败")
                }
        }
    }

    /**
     * 重命名(乐观更新 + 失败回滚)— 由 PreviewImage 顶栏 MoreMenu "重命名" / "···" 子项触发。
     *
     *  流程:
     *  1. newName 为空 / 与当前同名 → ToastBus 提示,不发请求
     *  2. 立即用 newName 覆盖 _state.metadata.name(乐观更新,UI 立刻变)
     *  3. 调 filesRepo.renameFile → 成功 → ToastBus.info("已重命名: 新名")
     *  4. 失败 → 回滚 _state.metadata.name 到 oldName + ToastBus.error(115 message)
     *
     *  不需要 emit PreviewEvent(Route 无需响应,state 自身已更新)。
     */
    fun onRename(newName: String) {
        val current = _state.value as? PreviewUiState.Success ?: return
        if (newName.isBlank()) {
            ToastBus.error("名称不能为空")
            return
        }
        val oldName = current.metadata.name
        if (newName == oldName) {
            ToastBus.info("名称未变化")
            return
        }
        // 乐观更新
        _state.value = current.copy(metadata = current.metadata.copy(name = newName))
        viewModelScope.launch {
            filesRepo.renameFile(id = current.metadata.fid, newName = newName)
                .onSuccess {
                    ToastBus.info("已重命名: $newName")
                }
                .onFailure { e ->
                    // 回滚到 oldName
                    val s = _state.value as? PreviewUiState.Success ?: return@onFailure
                    _state.value = s.copy(metadata = s.metadata.copy(name = oldName))
                    ToastBus.error(e.message ?: "重命名失败")
                }
        }
    }

    /**
     * 删除当前文件 — 成功 → emit [PreviewEvent.Deleted] → Route 调 onBack 回 Files。
     *
     *  失败:ToastBus.error(115 message,常见 990003 文件已删除);
     *  不 emit 事件,UI 留在 PreviewImage 让用户重试。
     */
    fun onDelete() {
        val current = _state.value as? PreviewUiState.Success ?: return
        viewModelScope.launch {
            filesRepo.deleteFiles(ids = listOf(current.metadata.fid))
                .onSuccess {
                    _oneShotEvents.trySend(PreviewEvent.Deleted)
                }
                .onFailure { e ->
                    ToastBus.error(e.message ?: "删除失败")
                }
        }
    }

    /**
     * 移动到目标目录(由 FolderPicker 选完回传后触发)— 成功 → emit [PreviewEvent.Deleted]。
     *
     *  FolderPicker 已在 UI 层 excludeIds 过滤(自身 + 祖先),VM 不再做防自循环。
     *  失败:ToastBus.error(115 message,常见 990001 目标无效);不 emit 事件。
     */
    fun onMoveConfirmed(targetCid: String) {
        val current = _state.value as? PreviewUiState.Success ?: return
        viewModelScope.launch {
            filesRepo.moveFiles(ids = listOf(current.metadata.fid), toCid = targetCid)
                .onSuccess {
                    _oneShotEvents.trySend(PreviewEvent.Deleted)
                }
                .onFailure { e ->
                    ToastBus.error(e.message ?: "移动失败")
                }
        }
    }

    /**
     * 分享当前图片(Android Sharesheet)— 由 PreviewImage 顶栏 MoreMenu "分享" 触发。
     *
     *  - 仅 image:走 [shareImage] 下载 mediaUrl → cacheDir → FileProvider → ACTION_SEND
     *    createChooser。CDN URL 本身已签 5min,ServiceLocator.okHttpClient 直拉(对齐 M3
     *    download pipeline 复用同一 OkHttp 客户端)
     *  - 失败 → ToastBus.error(shareImage 抛 IOException,message 已包含 ext/大小/HTTP 原因)
     *
     *  需要 application context(非 Activity):FileProvider 拿 cacheDir + startActivity
     *  都不依赖 Activity 生命周期,Application 上下文更稳,屏销毁不丢正在进行的下载。
     *
     * @param context Application/Activity context — 由调用方(LocalContext.current.applicationContext)
     */
    fun onShare(context: Context) {
        val current = _state.value as? PreviewUiState.Success ?: return
        viewModelScope.launch {
            runCatching { shareImage(context, current.mediaUrl) }
                .onFailure { e -> ToastBus.error(e.message ?: "分享失败") }
        }
    }

    /**
     * 下载当前图片 — 由 PreviewImage 顶栏 MoreMenu "下载" 触发。
     *
     *  - mediaUrl 已经在 Success 里(5min 签名直链),直接喂给 [com.starvault.data.repository.DownloadRepository.enqueue]
     *    即可,无需再 fetchImageOriginalUrl
     *  - MediaMetadata.fileCategory 是 String "1"(proapi),ParsedFileItem.fileCategory 是 Int 4
     *    (115 webapi 约定 image),同名不同义 — 合成处显式注释
     *  - parentId 留空:Preview 没拿 parent cid,DownloadWorker 不读
     */
    fun onDownload() {
        val current = _state.value as? PreviewUiState.Success ?: return
        val m = current.metadata
        val item = ParsedFileItem(
            id = m.fid,
            parentId = "",
            name = m.name,
            ico = m.ico,
            sizeBytes = m.sizeBytes,
            mtimeSec = m.mtimeSec,
            pickCode = m.pickCode,
            isFolder = false,
            playLong = 0,
            sha1 = m.sha1,
            fileCategory = 4,                       // 115 webapi fc 约定:4=image
            thumbnailUrl = m.thumbnailUrl,
        )
        ServiceLocator.downloadRepository.enqueue(item)
            .onSuccess { ToastBus.info("已加入下载队列") }
            .onFailure { ToastBus.error(it.message ?: "下载失败") }
    }
}
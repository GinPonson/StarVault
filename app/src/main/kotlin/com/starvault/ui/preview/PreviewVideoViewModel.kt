package com.starvault.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.local.playback.MediaPositionStore
import com.starvault.data.model.FileType
import com.starvault.data.remote.cloud115.ParsedFileItem
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.MediaPreviewRepository
import com.starvault.data.repository.VideoM3u8
import com.starvault.data.repository.toFileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PreviewVideo ViewModel：拿 VIDEO 文件的 m3u8 URL。
 *
 *  流程：
 *  1. [MediaPreviewRepository.fetchMetadata] 拿 pickcode + name + size
 *  2. [MediaPreviewRepository.fetchVideoM3u8Options] 拿全量清晰度列表
 *  3. 默认选第一档（服务端按可用性降序，通常是 1080P 或最高），暴露
 *     [PreviewUiState.Success]（mediaUrl + qualityOptions + qualityChip）
 *  4. [selectQuality] 切换清晰度：把 mediaUrl 改到对应 url，让 Screen 重建 player
 *
 *  ExoPlayer 实例的创建/释放由 Screen 侧 [DisposableEffect] 负责，不放进 VM
 *  （避免 VM 重建时 player 泄漏；player 生命周期应绑 Composable）。
 *
 *  上一集/下一集([loadSiblings]):
 *  - 仅当 [parentCid] 非空时触发(由 Route.PreviewVideo.parentCid 传入)
 *  - 调 [FilesRepository.listFolder] 拿父目录全部项,过滤 video,按 name 升序排列
 *    (115 webapi 排序对齐 listFolder 默认 — 修改时间降序;Preview 屏期望"按文件名
 *    自然顺序",如 S01E01 → S01E02 → S01E03,这里显式按 name asc)
 *  - 找到当前 [fileId] 的 index,前后两条作为 prev/next
 *  - 同一调用顺手填充 [playlist](完整 video 列表),供 ModalBottomSheet 播放列表用
 *  - 失败/找不到 → siblings 保持空,UI 端上一集/下一集按钮降级为 noop
 *
 *  Siblings 单独走一个 StateFlow,跟 metadata 加载解耦:
 *  - 拉兄弟可能很慢(115 /files 一次 50 条;若父目录有 200 条 video 需翻 4 页),
 *    不阻塞首屏播放
 *  - metadata 失败时 siblings 即便成功也不显示(用户已经在 Loading 等不到结果,不会点 prev/next)
 */
class PreviewVideoViewModel(
    private val fileId: String,
    private val parentCid: String? = null,
    private val repo: MediaPreviewRepository = ServiceLocator.mediaPreviewRepository,
    private val filesRepo: FilesRepository = ServiceLocator.filesRepository,
    private val positionStore: MediaPositionStore = ServiceLocator.mediaPositionStore,
) : ViewModel() {

    private val _state = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    /**
     * 当前文件星标状态。初始 false,Success emit 时由 metadata.isMark 同步;toggleStar 时翻。
     */
    private val _isStarred = MutableStateFlow(false)
    val isStarred: StateFlow<Boolean> = _isStarred.asStateFlow()

    /**
     * 兄弟文件状态(用于"上一集/下一集"按钮)。
     *
     *  - [prevId] : 上一集 fileId;null = 没有上一集(已是第一集)或未拉
     *  - [nextId] : 下一集 fileId;null = 没有下一集(已是最后一集)或未拉
     *
     *  VM 不会清空 prev/next(成功/失败都保持最后一次结果),便于 UI 始终能渲染按钮态;
     *  未拉取完成前两者均为 null → Screen 显示"已是单视频"hint。
     */
    data class Siblings(val prevId: String? = null, val nextId: String? = null)

    private val _siblings = MutableStateFlow(Siblings())
    val siblings: StateFlow<Siblings> = _siblings.asStateFlow()

    /**
     * 完整播放列表(同父目录 video 文件,按 name asc 排序)。
     *
     *  - 空 list = 无 parentCid(Search 入口)/ 未拉取完成 / 拉取失败
     *  - 复用 [loadSiblings] 的 [FilesRepository.listFolder] 调用,顺手填充;不发起额外网络
     *  - 1150 上限同 [loadSiblings] — 父目录 video 数 > 1150 时只显示前 1150
     *  - 已过滤 [FileType.VIDEO] + !isFolder,Screen 端直接渲染无需再 filter
     */
    private val _playlist = MutableStateFlow<List<ParsedFileItem>>(emptyList())
    val playlist: StateFlow<List<ParsedFileItem>> = _playlist.asStateFlow()

    init {
        load()
        if (!parentCid.isNullOrBlank()) loadSiblings()
    }

    fun load() {
        if (_state.value is PreviewUiState.Success) return
        _state.value = PreviewUiState.Loading
        viewModelScope.launch {
            // 并发拉 metadata + 已保存的播放位置,缩短首屏时间(对齐 PreviewAudioViewModel)
            val savedPositionMs = positionStore.load(fileId)?.toInt() ?: 0
            val metadata = repo.fetchMetadata(fileId)
            metadata.fold(
                onSuccess = { meta ->
                    val options = repo.fetchVideoM3u8Options(meta.pickCode)
                    options.fold(
                        onSuccess = { list ->
                            val first = list.first()
                            _isStarred.value = meta.isMark == "1"
                            _state.value = PreviewUiState.Success(
                                metadata = meta,
                                mediaUrl = first.url,
                                qualityChip = first.qualityDesc,
                                qualityOptions = list,
                                resumePositionMs = savedPositionMs,
                            )
                        },
                        onFailure = { e ->
                            // 失败：_state 保持 Loading,仅 ToastBus 提示
                            ToastBus.error(e.message ?: "无法获取播放地址")
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
     * Loading 状态时 noop;Success 状态时取 meta.fid 调 /open/ufile/update。
     * 切清晰度([selectQuality])不会影响星标态 — 走 _state.copy 不动 _isStarred。
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
     * 保存当前播放位置(由 Screen 5s 节流 + onDispose 兜底调用)。
     *
     * VM 不持有 ExoPlayer 实例(player 在 Screen 侧 `remember(mediaUrl)` 持有),由 Screen
     * 把 player.currentPosition 拿到后调此函数写盘。
     */
    fun savePosition(positionMs: Long) {
        viewModelScope.launch {
            positionStore.save(fileId, positionMs)
        }
    }

    /**
     * 拉父目录全部 video 兄弟,定位当前 fid 的 index,算 prev/next。
     *
     *  - 调用 [FilesRepository.listFolder] 时显式 `order=file_name asc`(对应
     *    115 `o=file_name asc=1`)让兄弟按文件名自然顺序排列
     *  - 一次拉到 1150 上限;父目录 video 数 > 1150 时只显示前 1150 内的 prev/next
     *  - 翻页:暂不分页 — 1150 条以上同一目录 95% 概率是云盘根目录或大型合集,通常 prev/next
     *    不会跨过 1150 边界
     */
    private fun loadSiblings() {
        val cid = parentCid ?: return
        viewModelScope.launch {
            filesRepo.listFolder(cid = cid, limit = 1150, order = "file_name", asc = 1)
                .onSuccess { page ->
                    val videos = page.items
                        .filter { !it.isFolder && it.toFileType() == FileType.VIDEO }
                    val idx = videos.indexOfFirst { it.id == fileId }
                    if (idx < 0) return@onSuccess
                    _siblings.value = Siblings(
                        prevId = videos.getOrNull(idx - 1)?.id,
                        nextId = videos.getOrNull(idx + 1)?.id,
                    )
                    // 完整列表(同 listFolder 调用,无额外网络):ModalBottomSheet 播放列表用
                    _playlist.value = videos
                }
                .onFailure {
                    // 拉兄弟失败不影响主播放;_siblings + _playlist 保持初值(empty),
                    // UI 端按钮降级 noop / sheet 显示"暂无播放列表"占位
                }
        }
    }

    /**
     * 切换清晰度。Screen 监听 state.mediaUrl 变化重建 ExoPlayer;
     * 签名 5 分钟有效期内切档无需重新 fetch，超过则需要重新调用 load。
     */
    fun selectQuality(option: VideoM3u8) {
        val current = _state.value as? PreviewUiState.Success ?: return
        if (current.mediaUrl == option.url) return
        _state.value = current.copy(
            mediaUrl = option.url,
            qualityChip = option.qualityDesc,
        )
    }
}
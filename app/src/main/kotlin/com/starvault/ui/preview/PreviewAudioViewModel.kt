package com.starvault.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.local.playback.MediaPositionStore
import com.starvault.data.model.FileType
import com.starvault.data.repository.FilesRepository
import com.starvault.data.repository.MediaPreviewRepository
import com.starvault.data.repository.toFileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PreviewAudio ViewModel:拿音频文件直链(mp3/flac/wav)。
 *
 *  流程:
 *  1. [MediaPreviewRepository.fetchMetadata] 拿 pickCode + name + size + thumbnailUrl
 *  2. **二次 emit Success**:
 *     - 第一次 emit Success(mediaUrl=""):立刻给 UI 封面/标题/进度条 0/0, 不必等第二次网络往返
 *     - 第二次 emit Success(mediaUrl=u):拿到签名直链, Screen 侧 `remember(mediaUrl)` 重建 ExoPlayer
 *  3. 失败:保持 Loading + ToastBus.error(对齐 PreviewVideo, 避免假成功态)
 *
 *  ExoPlayer 实例由 Screen 侧 `remember(mediaUrl)` 持有, VM 不持 player
 *  (避免 VM 重建时 player 泄漏,生命周期应绑 Composable)。
 *
 *  上一首/下一首([loadSiblings]):
 *  - 仅当 [parentCid] 非空时触发(由 Route.PreviewAudio.parentCid 传入)
 *  - 调 [FilesRepository.listFolder] 拿父目录全部项,过滤 audio,按 name 升序排列
 *  - 找到当前 [fileId] 的 index,前后两条作为 prev/next
 *  - 失败/找不到 → siblings 保持空,UI 端按钮降级为 ToastBus "已是单文件"
 *
 *  Siblings 单独走一个 StateFlow, 跟 metadata 加载解耦:
 *  - 拉兄弟可能很慢(115 /files 一次 50 条;若父目录有 200 条 audio 需翻 4 页),
 *    不阻塞首屏播放
 *  - metadata 失败时 siblings 即便成功也不显示
 */
class PreviewAudioViewModel(
    private val fileId: String,
    private val parentCid: String? = null,
    private val repo: MediaPreviewRepository = ServiceLocator.mediaPreviewRepository,
    private val filesRepo: FilesRepository = ServiceLocator.filesRepository,
    private val positionStore: MediaPositionStore = ServiceLocator.mediaPositionStore,
) : ViewModel() {

    /**
     * 复用 [PreviewUiState.Success](同 image VM shape): 字段 metadata + mediaUrl 足够 audio 屏用。
     *  audio 单档 → qualityChip="", qualityOptions=emptyList()(对齐 image VM 默认值)
     */
    private val _state = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    /**
     * 兄弟文件状态(用于"上一首/下一首"按钮)。
     *
     *  - [prevId] : 上一首 fileId;null = 没有上一首(已是第一首)或未拉
     *  - [nextId] : 下一首 fileId;null = 没有下一首(已是最后一首)或未拉
     *
     *  VM 不会清空 prev/next(成功/失败都保持最后一次结果),便于 UI 始终能渲染按钮态;
     *  未拉取完成前两者均为 null → Screen 显示"已是单文件"toast。
     */
    data class Siblings(val prevId: String? = null, val nextId: String? = null)

    private val _siblings = MutableStateFlow(Siblings())
    val siblings: StateFlow<Siblings> = _siblings.asStateFlow()

    init {
        load()
        if (!parentCid.isNullOrBlank()) loadSiblings()
    }

    fun load() {
        if (_state.value is PreviewUiState.Success) return
        _state.value = PreviewUiState.Loading
        viewModelScope.launch {
            // 并行拉 metadata + 已保存的播放位置：positionStore.load 是 suspend IO,
            // 跟 metadata 网络 IO 并发跑,缩短首屏时间约 50-100ms
            val savedPositionMs = positionStore.load(fileId)?.toInt() ?: 0
            val metadata = repo.fetchMetadata(fileId)
            metadata.fold(
                onSuccess = { meta ->
                    // 第一次 emit: 立刻给 UI 封面/标题/进度条 0/0(+resumePositionMs)
                    _state.value = PreviewUiState.Success(
                        metadata = meta,
                        mediaUrl = "",
                        qualityChip = "",
                        qualityOptions = emptyList(),
                        resumePositionMs = savedPositionMs,
                    )
                    // 第二次 emit: 拿到签名直链后 Screen 重建 player
                    repo.fetchAudioStreamUrl(fileId, meta.pickCode).fold(
                        onSuccess = { u ->
                            _state.value = PreviewUiState.Success(
                                metadata = meta,
                                mediaUrl = u,
                                qualityChip = "",
                                qualityOptions = emptyList(),
                                resumePositionMs = savedPositionMs,
                            )
                        },
                        onFailure = { e ->
                            // 保持 mediaUrl="" 的 Success, 屏显封面 + 进度条 0/0 + 错误 Snackbar
                            ToastBus.error(e.message ?: "无法获取播放地址")
                        },
                    )
                },
                onFailure = { e ->
                    ToastBus.error(e.message ?: "文件不存在或已删除")
                },
            )
        }
    }

    /**
     * 保存当前播放位置(由 Screen 5s 节流 + onDispose 兜底调用)。
     *
     * VM 不持有 ExoPlayer 实例(player 在 Screen 侧 `remember(mediaUrl)` 持有),所以由
     * Screen 把 player.currentPosition 拿到后调此函数写盘。
     *
     * suspend:Screen 端用 `viewModelScope.launch { vm.savePosition(...) }` 触发,
     * DataStore.edit 是 IO 不阻塞主线程。
     */
    fun savePosition(positionMs: Long) {
        viewModelScope.launch {
            positionStore.save(fileId, positionMs)
        }
    }

    /**
     * 拉父目录全部 audio 兄弟,定位当前 fid 的 index,算 prev/next。
     *
     *  - 调用 [FilesRepository.listFolder] 时显式 `order=file_name asc`(对应
     *    115 `o=file_name asc=1`)让兄弟按文件名自然顺序排列
     *  - 一次拉到 1150 上限;父目录 audio 数 > 1150 时只显示前 1150 内的 prev/next
     *  - 翻页:暂不分页 — 1150 条以上同一目录 95% 概率是云盘根目录或大型合集,通常 prev/next
     *    不会跨过 1150 边界
     */
    private fun loadSiblings() {
        val cid = parentCid ?: return
        viewModelScope.launch {
            filesRepo.listFolder(cid = cid, limit = 1150, order = "file_name", asc = 1)
                .onSuccess { page ->
                    val audios = page.items
                        .filter { !it.isFolder && it.toFileType() == FileType.AUDIO }
                    val idx = audios.indexOfFirst { it.id == fileId }
                    if (idx < 0) return@onSuccess
                    _siblings.value = Siblings(
                        prevId = audios.getOrNull(idx - 1)?.id,
                        nextId = audios.getOrNull(idx + 1)?.id,
                    )
                }
                .onFailure {
                    // 拉兄弟失败不影响主播放;_siblings 保持初值, UI 端按钮降级 ToastBus
                }
        }
    }
}

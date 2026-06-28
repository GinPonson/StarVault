package com.starvault.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.repository.MediaPreviewRepository
import com.starvault.data.repository.VideoM3u8
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
 */
class PreviewVideoViewModel(
    private val fileId: String,
    private val repo: MediaPreviewRepository = ServiceLocator.mediaPreviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_state.value is PreviewUiState.Success) return
        _state.value = PreviewUiState.Loading
        viewModelScope.launch {
            val metadata = repo.fetchMetadata(fileId)
            metadata.fold(
                onSuccess = { meta ->
                    val options = repo.fetchVideoM3u8Options(meta.pickCode)
                    options.fold(
                        onSuccess = { list ->
                            val first = list.first()
                            _state.value = PreviewUiState.Success(
                                metadata = meta,
                                mediaUrl = first.url,
                                qualityChip = first.qualityDesc,
                                qualityOptions = list,
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
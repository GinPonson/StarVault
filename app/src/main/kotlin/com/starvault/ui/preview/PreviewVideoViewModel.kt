package com.starvault.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.repository.MediaPreviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PreviewVideo ViewModel：拿 VIDEO 文件的 m3u8 URL。
 *
 *  流程：
 *  1. [MediaPreviewRepository.fetchMetadata] 拿 pickcode + name + size
 *  2. [MediaPreviewRepository.fetchVideoM3u8Url] 拿 video_url
 *  3. 暴露 [PreviewUiState.Success]（mediaUrl = m3u8 URL）
 *
 *  与 PreviewImage 几乎对称，区别只是第二步的 endpoint。
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
                    val url = repo.fetchVideoM3u8Url(meta.pickCode)
                    url.fold(
                        onSuccess = { u ->
                            _state.value = PreviewUiState.Success(meta, u)
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
}
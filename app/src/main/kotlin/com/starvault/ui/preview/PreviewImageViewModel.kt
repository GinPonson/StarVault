package com.starvault.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.data.repository.MediaPreviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PreviewImage ViewModel：拿 IMAGE 文件的原图 URL。
 *
 *  流程：
 *  1. [MediaPreviewRepository.fetchMetadata] 拿 pickcode + name + size
 *  2. [MediaPreviewRepository.fetchImageOriginalUrl] 拿 file_url
 *  3. 暴露 [PreviewUiState.Success]（mediaUrl = file_url）
 *
 *  错误策略：失败时 [PreviewUiState.Error]，message 给到 UI 展示。
 *
 *  复用 [FilesViewModel] 的 markPending 风格不可行——Preview 是只读屏（不切目录），所以状态机简单：
 *  init → Loading → Success / Error。不会中途再切状态。
 */
class PreviewImageViewModel(
    private val fileId: String,
    private val repo: MediaPreviewRepository = ServiceLocator.mediaPreviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

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
                    val url = repo.fetchImageOriginalUrl(meta.pickCode)
                    url.fold(
                        onSuccess = { u ->
                            _state.value = PreviewUiState.Success(meta, u)
                        },
                        onFailure = { e ->
                            _state.value = PreviewUiState.Error(e.message ?: "无法获取原图")
                        },
                    )
                },
                onFailure = { e ->
                    _state.value = PreviewUiState.Error(e.message ?: "文件不存在或已删除")
                },
            )
        }
    }
}
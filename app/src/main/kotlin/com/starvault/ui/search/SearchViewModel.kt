package com.starvault.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starvault.core.ServiceLocator
import com.starvault.core.ToastBus
import com.starvault.data.repository.FilesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 搜索屏 ViewModel：实时调 115 `GET /files/search`，debounce 500ms。
 *
 *  状态机：
 *    - query 变化（用户输入 / 清空） → onQueryChange(q)
 *        - q.isBlank() → Idle("") + 取消 inflight searchJob
 *        - q 非空 → Idle(q)（先 Idle 让输入框响应快）+ delay(500ms) + Searching → Success/Empty/Error
 *
 *  分页（[loadMore]）：
 *    - 仅 Success 态可触发
 *    - 与 FilesViewModel.loadMore 同款并发防护（isLoadingMore / hasMore / cid 一致性）
 *    - searchFiles 响应同 listFiles shape，merge 用 distinctBy(id)
 *
 *  请求取消：
 *    - _searchJob: 唯一的搜索 Job；新 query 进来 cancel 它（debounce 内 + 首次搜索都可打断）
 *    - _loadMoreJob: 独立的"加载更多"任务（参考 FilesViewModel：loadJob 与 loadMoreJob 不互斥）
 */
class SearchViewModel(
    private val repo: FilesRepository = ServiceLocator.filesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle(""))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    /** 当前 query（VM 缓存，与 state.query 一致但更易用——避免每次都 read Flow）。 */
    private var currentQuery: String = ""

    /** 分页 offset：单次搜索的下一页位置（每次新 query 归零）。 */
    private var currentOffset: Int = 0

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    /**
     * 用户输入变化时调（TextField onValueChange）。
     *
     *  - 立即把 query 写到 Idle（无网络往返，让输入框 0 延迟）
     *  - 取消 inflight 搜索（debounce 内打断 + 首次搜索未完成时打断）
     *  - 空 query 直接 return（停留在 Idle("")）
     *  - 非空 query → debounce 500ms → 触发搜索
     */
    fun onQueryChange(q: String) {
        currentQuery = q
        searchJob?.cancel()
        loadMoreJob?.cancel()
        if (q.isBlank()) {
            _state.value = SearchUiState.Idle("")
            currentOffset = 0
            return
        }
        // 立即 Idle 让 UI 输入框响应；Searching 在 500ms 后才显示
        _state.value = SearchUiState.Idle(q)
        searchJob = viewModelScope.launch {
            delay(500)  // debounce
            searchFirstPage(q)
        }
    }

    /** 清空 query（点 X 按钮触发）。 */
    fun clearQuery() {
        onQueryChange("")
    }

    /**
     * 拉搜索结果第一页。
     *
     *  - query 与 [currentQuery] 不一致时 return（已被新 query 取代）
     *  - Searching → Success(q, results, ...) 或 Empty(q) 或 Error(q, msg)
     */
    private suspend fun searchFirstPage(q: String) {
        _state.value = SearchUiState.Searching(q)
        val result = repo.searchFiles(searchValue = q, offset = 0)
        // 二次防护：搜索中用户又改了 query → 丢弃旧结果
        if (currentQuery != q) return

        result.onSuccess { page ->
            val items = page.items.map { it.toSearchEntry() }
            currentOffset = items.size
            _state.value = if (items.isEmpty()) {
                SearchUiState.Empty(q)
            } else {
                SearchUiState.Success(
                    query = q,
                    results = items,
                    hasMore = page.hasMore,
                )
            }
        }.onFailure { e ->
            // 失败：_state 不动(可能保持 Searching),错误仅走 ToastBus
            // 屏不被错误占位 —— Snackbar 提示原因
            ToastBus.error(e.message ?: "搜索失败")
        }
    }

    /**
     * 加载搜索结果下一页。
     *
     *  - 仅 Success 状态可触发
     *  - isLoadingMore=true 或 hasMore=false → 直接 return
     *  - 拉回后 merge 到 results 末尾，distinctBy(id) 兜底防 115 偶发重复
     *  - 二次防护：拉回时 query 已变化 → 丢弃结果
     */
    fun loadMore() {
        val s = _state.value as? SearchUiState.Success ?: return
        if (s.isLoadingMore || !s.hasMore) return
        if (s.query != currentQuery) return
        val offset = currentOffset
        _state.value = s.copy(isLoadingMore = true)
        loadMoreJob = viewModelScope.launch {
            repo.searchFiles(searchValue = s.query, offset = offset)
                .onSuccess { page ->
                    // 二次防护
                    val current = _state.value as? SearchUiState.Success ?: return@onSuccess
                    if (current.query != currentQuery) return@onSuccess
                    val merged = (current.results + page.items.map { it.toSearchEntry() })
                        .distinctBy { it.id }
                    currentOffset = offset + page.items.size
                    _state.value = current.copy(
                        results = merged,
                        hasMore = page.hasMore,
                        isLoadingMore = false,
                    )
                }
                .onFailure {
                    val current = _state.value as? SearchUiState.Success ?: return@onFailure
                    if (current.query != currentQuery) return@onFailure
                    // 失败不弹 Error 屏（已有结果可用），仅清 isLoadingMore；toast 由 ToastBus 提示
                    ToastBus.error("加载更多失败")
                    _state.value = current.copy(isLoadingMore = false)
                }
        }
    }

    /**
     * 打开搜索结果（点 file 行触发）。
     *
     *  - 文件夹：MVP 不支持（无 cid 传回 Files 屏跳对应目录）
     *  - IMAGE/VIDEO：跳 Preview（由 SearchRoute 用 nav.navigate 实现）
     *  - 其它类型：MVP noop
     *
     *  此方法只暴露意图，SearchRoute 监听后调 nav.navigate。
     */
    fun onOpen(entry: com.starvault.ui.files.FileEntry) {
        // 具体跳转逻辑放在 Route 层（避免 VM 依赖 NavController）
    }
}
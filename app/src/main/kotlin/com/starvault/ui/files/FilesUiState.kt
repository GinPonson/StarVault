package com.starvault.ui.files

import com.starvault.data.model.FileType

/**
 * Files 屏 UiState（对应 design/06-files.html）。
 *
 *  - Loading : 占位
 *  - Success : 完整文件列表 + tab 计数 + 视图模式 + 选中集合
 *  - Error   : 拉取失败
 */
sealed interface FilesUiState {

    data class Loading(
        val placeholder: Unit = Unit,
    ) : FilesUiState

    data class Success(
        val folderId: String?,                    // null → "我的文件" 根
        val folderPath: List<FolderCrumb>,        // 根→当前 的完整路径，每段可点跳回
        val all: List<FileEntry>,                 // 当前目录的扁平列表（可能跨多页）
        val activeType: FileType?,                // null = 全部
        val viewMode: ViewMode,                   // 列表 / 网格
        val selectedIds: Set<String>,             // 多选集合
        val sortLabel: String,                    // "按修改时间 ▾"（SectionHead 显示用）
        /**
         * 当前排序字段（115 webapi `o` 参数）。驱动 SortSheet 显示当前选中项。
         * 与 sortLabel 同步——sortLabel 是给人看的，sortField/sortAsc 是给 115 用的。
         */
        val sortField: String = "user_ptime",
        /** 当前排序方向：0 = 降序，1 = 升序 */
        val sortAsc: Int = 0,
        /** 顶部 "共 N 项" 用的总数；优先用 115 报的 totalServerCount，否则用已加载 size */
        val totalCount: Int,
        /**
         * 115 当前筛选下的总条数（来自 115 /files 响应的 `count` 字段）。
         * null = 还没拉到（首屏未到前）或老数据。供 ViewModel 翻页决策与 UI "已加载 X / 共 Y" 文案。
         */
        val totalServerCount: Int? = null,
        /** 是否还有下一页。true 时滚动到末尾触发 loadMore() */
        val hasMore: Boolean = false,
        /** 正在拉下一页（防重复触发）。UI 用于列表底部小转圈 */
        val isLoadingMore: Boolean = false,
        /** 切目录/refresh 时旧列表保留渲染，新数据到位前显示顶部进度条 */
        val pendingLoad: Boolean = false,
    ) : FilesUiState {
        /** 5 个 tab + 全部的计数（顺序：全部/视频/图片/文档/音频/其他） */
        val tabCounts: TabCounts get() = TabCounts(
            all   = all.size,
            video = all.count { it.type == FileType.VIDEO },
            image = all.count { it.type == FileType.IMAGE },
            doc   = all.count { it.type == FileType.DOC },
            audio = all.count { it.type == FileType.AUDIO },
            other = all.count { it.type == FileType.OTHER || it.type == FileType.ZIP },
        )
    }

    data class Error(
        val message: String,
    ) : FilesUiState
}

enum class ViewMode { LIST, GRID }

data class TabCounts(
    val all: Int,
    val video: Int,
    val image: Int,
    val doc: Int,
    val audio: Int,
    val other: Int,
)

/**
 * 列表单行（与 HTML .file-row 一一对应）。把所有展示需要的扁平化字符串都带好，
 * Screen 只负责布局，不再做格式化。
 *
 *  - thumbnailUrl 仅 IMAGE / VIDEO 类型有值（115 webapi /files 响应 `u` 字段）；
 *    folder / audio / doc / zip 留 null，FileRow 仍用渐变色块 fallback
 */
data class FileEntry(
    val id: String,
    val name: String,
    val type: FileType,
    val metaSegments: List<String>,  // 3-4 段，用 " · " 拼成 meta 行
    val isFolder: Boolean,
    val thumbnailUrl: String? = null,
)

/**
 * 面包屑一段（根→当前 路径上的一节）。
 *
 *  - cid  : 115 文件夹 id；根 = "0"，固定 name = "我的文件"
 *  - name : 面包屑显示文本；最后一节（当前目录）加粗 + 无下划线
 *
 * Crumb 点击通过 index 反查 → vm.popToFolder(index)，截断 stack 后重新加载。
 */
data class FolderCrumb(
    val cid: String,
    val name: String,
)

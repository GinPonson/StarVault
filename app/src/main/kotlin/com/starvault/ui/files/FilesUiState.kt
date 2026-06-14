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
        val all: List<FileEntry>,                 // 当前目录的扁平列表
        val activeType: FileType?,                // null = 全部
        val viewMode: ViewMode,                   // 列表 / 网格
        val selectedIds: Set<String>,             // 多选集合
        val sortLabel: String,                    // "按修改时间"（sort 菜单待 T22+）
        val totalCount: Int,                      // 段头 "共 N 项"
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
 */
data class FileEntry(
    val id: String,
    val name: String,
    val type: FileType,
    val metaSegments: List<String>,  // 3-4 段，用 " · " 拼成 meta 行
    val isFolder: Boolean,
)

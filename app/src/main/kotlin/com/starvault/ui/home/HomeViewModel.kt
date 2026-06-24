package com.starvault.ui.home

import androidx.lifecycle.ViewModel
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.FileType
import com.starvault.data.model.TagColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Home 屏 ViewModel — Phase 1 mock 数据（严格按 design/01-home.html 的 6 条文件复刻）。
 *
 *  - 不读 JSON：design/01-home.html 的 6 条文件是为了视觉验证手动编排的，
 *    真实接入 115 后改为 [FixtureLoader] 拉 + 排序。
 *  - 提供 [setTag] / [clearTag]：tag-strip 上的 active 切换
 *  - 列表过滤：选了某个 tag 后只显示带该 tag 的文件；选 "全部" 显示所有
 *  - mock 阶段直接 [Success] 一次性 publish，避免 Loading 闪烁
 *
 * 时间策略：用与设计 HTML 完全一致的硬编码相对时间（"2 小时前" / "今天 09:12" / "昨天 23:14"），
 * 不在 ViewModel 里算 relative time（避免 ms 漂移导致 Paparazzi golden 不稳定），
 * 相对时间字符串写在 [HomeRelativeTimes] 工具里由 UI 层读取。
 */
class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(
        HomeUiState.Success(
            files = homeDesignFiles(),
            relTimes = homeRelativeTimes(),
        ),
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** 切换 tag 过滤。传入 null 表示"全部"。*/
    fun setTag(tag: HomeQuickTag?) {
        val current = _state.value
        val next = when (current) {
            is HomeUiState.Success -> current.copy(activeTag = tag?.toFileTag())
            is HomeUiState.Loading -> current.copy(activeTag = tag?.toFileTag())
        }
        _state.value = next
    }

    /**
     * Design/01-home.html 6 条文件（逐行 1:1 复刻）：
     *  1. 设计交付 / 2026Q2            FOLDER  28 项         2 小时前   tag 工作
     *  2. Final.Destination.2026.1080p VIDEO   2.18 GB      1:42:08    tag 影视
     *  3. 陈奕迅 - 孤勇者.flac         AUDIO   32.4 MB      04:23      tag 音乐
     *  4. 2026 年度产品规划 v3.2.pdf   DOC     8.7 MB       今天 09:12 tag 工作
     *  5. 京都樱花 / 4 月 / DSC_4821  IMAGE   14.2 MB      4032×3024  tag 生活
     *  6. 毕业设计源码 v2.zip          ZIP     62.1 MB      昨天 23:14 tag 学习
     */
    private fun homeDesignFiles(): List<FileItem> = listOf(
        FileItem(
            id = "h-01",
            name = "设计交付 / 2026Q2",
            type = FileType.FOLDER,
            durationOrCount = "28 项",
            mtime = 1_718_000_000_000L,  // 任意未来稳定值：相对时间由 UI 层覆写
            tag = FileTag("工作", TagColor.TAG1),
        ),
        FileItem(
            id = "h-02",
            name = "Final.Destination.2026.1080p.mkv",
            type = FileType.VIDEO,
            sizeBytes = 2_340_234_240L,  // 2.18 GB
            durationOrCount = "1:42:08",
            mtime = 1_717_990_000_000L,
            tag = FileTag("影视", TagColor.TAG3),
        ),
        FileItem(
            id = "h-03",
            name = "陈奕迅 - 孤勇者.flac",
            type = FileType.AUDIO,
            sizeBytes = 33_976_320L,  // 32.4 MB
            durationOrCount = "04:23",
            mtime = 1_717_980_000_000L,
            tag = FileTag("音乐", TagColor.TAG5),
        ),
        FileItem(
            id = "h-04",
            name = "2026 年度产品规划 v3.2.pdf",
            type = FileType.DOC,
            sizeBytes = 9_126_400L,  // 8.7 MB
            mtime = 1_717_970_000_000L,
            tag = FileTag("工作", TagColor.TAG1),
        ),
        FileItem(
            id = "h-05",
            name = "京都樱花 / 4 月 / DSC_4821.jpg",
            type = FileType.IMAGE,
            sizeBytes = 14_901_248L,  // 14.2 MB
            durationOrCount = "4032 × 3024",
            mtime = 1_717_960_000_000L,
            tag = FileTag("生活", TagColor.TAG2),
        ),
        FileItem(
            id = "h-06",
            name = "毕业设计源码 v2.zip",
            type = FileType.ZIP,
            sizeBytes = 65_142_784L,  // 62.1 MB
            mtime = 1_717_950_000_000L,
            tag = FileTag("学习", TagColor.TAG2),
        ),
    )

    /**
     * 6 个文件 id → 相对时间字符串（与 design/01-home.html 的 6 行 1:1）。
     * 用 fileId 而非 index 索引，避免列表顺序变化导致映射错位。
     */
    private fun homeRelativeTimes(): Map<String, String> = mapOf(
        "h-01" to "2 小时前",
        "h-02" to "1:42:08",   // 视频用时长当时间位（与 HTML 一致）
        "h-03" to "04:23",     // 音频同理
        "h-04" to "今天 09:12",
        "h-05" to "4032 × 3024",  // 图片用分辨率占位
        "h-06" to "昨天 23:14",
    )
}

package com.starvault.fixtures

import com.starvault.data.model.AlbumPhoto
import com.starvault.data.model.Direction
import com.starvault.data.model.DisplayMode
import com.starvault.data.model.FileItem
import com.starvault.data.model.FileTag
import com.starvault.data.model.FileType
import com.starvault.data.model.ShareLink
import com.starvault.data.model.TagColor
import com.starvault.data.model.Transfer
import com.starvault.data.model.TransferStatus
import com.starvault.data.model.User
import com.starvault.data.model.Wallpaper
import com.starvault.data.model.WallpaperConfig

// 单测 / Paparazzi golden 用的硬编码数据。
//
// 与 assets/fixtures/*.json 的 runtime 数据保持结构一致但数量更小 ——
// 测试不需要 30 条 file，只看 happy path。
object FixturePresets {

    fun homeFiles(): List<FileItem> = listOf(
        FileItem("f01", "旅行 2025",     FileType.FOLDER, durationOrCount = "38 项", mtime = 1_718_000_000_000, tag = FileTag("生活", TagColor.TAG2)),
        FileItem("f02", "东京 vlog.mp4", FileType.VIDEO,  sizeBytes = 104_857_600, durationOrCount = "08:42",     mtime = 1_717_900_000_000, tag = FileTag("生活", TagColor.TAG2)),
        FileItem("f03", "海边日落.jpg",   FileType.IMAGE,  sizeBytes = 2_097_152,  durationOrCount = "4032 × 3024", mtime = 1_717_800_000_000, tag = FileTag("影视", TagColor.TAG3)),
        FileItem("f04", "会议纪要.docx",  FileType.DOC,    sizeBytes = 40_960,     mtime = 1_717_700_000_000),
        FileItem("f05", "钢琴曲.wav",     FileType.AUDIO,  sizeBytes = 31_457_280, durationOrCount = "02:18",     mtime = 1_717_600_000_000, tag = FileTag("音乐", TagColor.TAG5)),
    )

    fun transfers(): List<Transfer> = listOf(
        Transfer("t01", "movie.mp4",  Direction.DOWN, 2_147_483_648, 1_073_741_824, 5_242_880,  TransferStatus.RUNNING, 1_718_100_000),
        Transfer("t02", "song.flac",  Direction.UP,   52_428_800,     52_428_800,     0,          TransferStatus.SUCCESS, 1_718_000_000),
        Transfer("t03", "doc.pdf",    Direction.DOWN, 5_242_880,      2_621_440,      0,          TransferStatus.PAUSED,  1_717_900_000),
        Transfer("t04", "corrupt.avi",Direction.DOWN, 209_715_200,    52_428_800,     0,          TransferStatus.FAILED,  1_717_800_000),
    )

    fun profile(): User = User(
        nickname = "Vint",
        vipLevel = 3,
        vipExpiresAt = 1_735_689_600,
        totalBytes = 1_099_511_627_776,
        usedBytes  = 329_853_488_332,
    )

    fun shareLinksFor(fileId: String): List<ShareLink> = listOf(
        ShareLink(fileId, "https://starvault.example/s/abc123", "8848", expiresAt = 1_722_470_400),
        ShareLink(fileId, "https://starvault.example/s/def456", "1234", expiresAt = null),
    )

    fun albumPhotos(): List<AlbumPhoto> = listOf(
        AlbumPhoto("p01", "sample/p01.jpg", 1080, 1920, 1_718_000_000_000, isFavorite = true),
        AlbumPhoto("p02", "sample/p02.jpg", 1920, 1080, 1_717_900_000_000),
        AlbumPhoto("p03", "sample/p03.jpg", 1200, 1600, 1_717_800_000_000, isFavorite = true),
        AlbumPhoto("p04", "sample/p04.jpg", 1600, 1200, 1_717_700_000_000),
        AlbumPhoto("p05", "sample/p05.jpg", 1080, 1080, 1_717_600_000_000),
    )

    fun wallpapers(): List<Wallpaper> = listOf(
        Wallpaper("w01", "sample/w01.jpg", "自然"),
        Wallpaper("w02", "sample/w02.jpg", "城市"),
        Wallpaper("w03", "sample/w03.jpg", "抽象"),
        Wallpaper("w04", "sample/w04.jpg", "极简"),
    )

    fun wallpaperConfig(): WallpaperConfig = WallpaperConfig(
        enabled = true,
        intervalSeconds = 300,
        displayMode = DisplayMode.FILL_CROP,
        categories = listOf("自然", "极简"),
    )
}

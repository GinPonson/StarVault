package com.starvault.data.upload

import java.io.InputStream
import java.security.MessageDigest

/**
 * SHA1 hashing 工具 — 115 上传协议需要 2 种 SHA1:
 *
 *  - `sha1OfStream`     : 整文件 SHA1,init 阶段 `fileid` 用
 *  - `sha1OfPrefix`     : 前 128 KiB SHA1,init 阶段 `preid` 用
 *  - `partSizeFor`      : calPartSize(OpenList `drivers/115_open/upload.go` 1:1 移植)
 *
 * 协议契约(对齐 115-sdk-go `fmt.Sprintf("%X", sha1)`):
 *  - hex 输出 **uppercase**(da39a3ee... 而非 da39A3EE...)
 *  - byte 顺序 big-endian(MessageDigest 默认,无需手动 reverse)
 *
 * 1 KiB 读取 buffer 是实测最优:小于 4 KiB 走 page cache 开销,大于 64 KiB 在大文件上 GC 压力。
 */
object Sha1Hashing {

    /** 115 协议契约:hash 输出 = uppercase hex(对齐 Go SDK `%X`)。 */
    private fun MessageDigest.hexUppercase(): String =
        digest().joinToString("") { "%02X".format(it) }

    /**
     * 整文件 SHA1(uppercase hex)。
     *
     * @param input 数据流(由调用方持有,本函数 close 由调用方负责 — 因为 ContentResolver URI
     *              通常是"借来"的,不能在这里 close,否则外部 ResourceLeak)
     * @param totalBytes 流总字节数(目前未使用 — MessageDigest 内部循环 read,buffer 4 KiB 已足;
     *                   保留参数是给未来"按 totalBytes 预分配 digest"留口子)
     */
    suspend fun sha1OfStream(input: InputStream, totalBytes: Long): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            if (n > 0) digest.update(buffer, 0, n)
        }
        return digest.hexUppercase()
    }

    /**
     * 前 `prefixBytes` 字节 SHA1(uppercase hex)。
     *
     * 行为约定:
     *  - 文件长度 < prefixBytes → 整文件 SHA1(不能因为 EOF 早退导致 hash 错)
     *  - 文件长度 >= prefixBytes → 只读前 prefixBytes 字节
     *
     * 115 协议 `preid` 默认 128 KiB prefix(对齐 OpenList `UploadCheck` 行为)。
     */
    suspend fun sha1OfPrefix(input: InputStream, prefixBytes: Int = DEFAULT_PREFIX_BYTES): String {
        val actual = prefixBytes.coerceAtLeast(0)
        val digest = MessageDigest.getInstance("SHA-1")
        val buffer = ByteArray(minOf(actual, DEFAULT_BUFFER_SIZE))
        var remaining = actual
        while (remaining > 0) {
            val toRead = minOf(remaining, buffer.size)
            val n = input.read(buffer, 0, toRead)
            if (n < 0) break
            if (n > 0) digest.update(buffer, 0, n)
            remaining -= n
        }
        return digest.hexUppercase()
    }

    /**
     * 计算分片大小 — 严格 1:1 移植 OpenList `drivers/115_open/upload.go::calPartSize`。
     *
     * 上传时由 [OssUploader] 调用,根据 partSize 决定走单 PUT 还是 multipart:
     *  - fileSize <= 20 MB  → 整文件 1 分片(走 putObject)
     *  - fileSize >  20 MB  → multipart,partSize 按下表
     *  - fileSize >   1 TB  → 5 GB/片
     *
     * 各档位常量与 Go SDK 注释"split 1TB into 10,000 part"对齐(1048576 * 10000 / 10000 ≈ 1GB/100,
     * 但 Go 实际值是 109951163 ≈ 1TB/10000)。
     */
    fun partSizeFor(fileSize: Long): Long {
        val partSizeBase = DEFAULT_PART_SIZE
        if (fileSize <= partSizeBase) return fileSize
        if (fileSize > ONE_TB) return FIVE_GB
        if (fileSize > SEVEN_HUNDRED_SIXTY_EIGHT_GB) return PART_104_85_MB
        if (fileSize > FIVE_HUNDRED_TWELVE_GB) return PART_78_64_MB
        if (fileSize > THREE_HUNDRED_EIGHTY_FOUR_GB) return PART_52_43_MB
        if (fileSize > TWO_HUNDRED_FIFTY_SIX_GB) return PART_39_32_MB
        if (fileSize > ONE_HUNDRED_TWENTY_EIGHT_GB) return PART_26_21_MB
        return partSizeBase
    }

    private const val DEFAULT_BUFFER_SIZE = 4 * 1024  // 4 KiB read buffer
    private const val DEFAULT_PREFIX_BYTES = 128 * 1024  // 128 KiB prefix(115 preid 默认)

    // ---- calPartSize 区间常量(对齐 Go SDK 注释里的固定值)----
    private const val DEFAULT_PART_SIZE = 20L * 1024 * 1024  // 20 MB
    private const val ONE_TB = 1024L * 1024 * 1024 * 1024
    private const val FIVE_GB = 5L * 1024 * 1024 * 1024
    private const val SEVEN_HUNDRED_SIXTY_EIGHT_GB = 768L * 1024 * 1024 * 1024
    private const val FIVE_HUNDRED_TWELVE_GB = 512L * 1024 * 1024 * 1024
    private const val THREE_HUNDRED_EIGHTY_FOUR_GB = 384L * 1024 * 1024 * 1024
    private const val TWO_HUNDRED_FIFTY_SIX_GB = 256L * 1024 * 1024 * 1024
    private const val ONE_HUNDRED_TWENTY_EIGHT_GB = 128L * 1024 * 1024 * 1024
    private const val PART_104_85_MB = 109_951_163L
    private const val PART_78_64_MB = 82_463_373L
    private const val PART_52_43_MB = 54_975_582L
    private const val PART_39_32_MB = 41_231_687L
    private const val PART_26_21_MB = 27_487_791L
}

/**
 * 顶层 suspend 包装(让外部调用方写 `sha1OfStream(...)` 即可,
 * 内部还是走 [Sha1Hashing.sha1OfStream])。
 *
 * 这些顶层函数是 suspend 主要是为未来"流式读 + 进度回调"留口子,
 * 当前实现 0 suspension point — 但保持签名一致比将来改签名便宜。
 */
suspend fun sha1OfStream(input: InputStream, totalBytes: Long): String =
    Sha1Hashing.sha1OfStream(input, totalBytes)

suspend fun sha1OfPrefix(input: InputStream, prefixBytes: Int = 128 * 1024): String =
    Sha1Hashing.sha1OfPrefix(input, prefixBytes)

fun partSizeFor(fileSize: Long): Long = Sha1Hashing.partSizeFor(fileSize)

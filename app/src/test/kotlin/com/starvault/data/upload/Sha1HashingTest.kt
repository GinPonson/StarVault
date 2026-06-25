package com.starvault.data.upload

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * SHA1 hashing 单元测试 — 纯函数 TDD(fixed input → fixed hex)。
 *
 * 参考:
 *  - RFC 3174 SHA1 测试向量
 *  - 115-sdk-go: `UploadFile` 走 full file SHA1,`UploadCheck` 走前 128 KiB SHA1
 *
 * 这些测试是 Phase 2 RED 起点 —— 任何 hex 大小写(uppercase 是协议契约)、
 * 边界(prefixBytes > fileSize)、partSize 阈值表 偏差都会先在这里炸出来。
 */
class Sha1HashingTest {

    // ---------- sha1OfStream ----------

    @Test fun `sha1OfStream of empty input returns da39a3ee5e6b4b0d3255bfef95601890afd80709`() = runBlocking {
        // RFC 3174 + NIST FIPS 180-4: empty string SHA1 = da39a3ee5e6b4b0d3255bfef95601890afd80709
        val input: InputStream = ByteArrayInputStream(ByteArray(0))
        assertEquals(
            "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709",
            sha1OfStream(input, totalBytes = 0L),
        )
    }

    @Test fun `sha1OfStream of abc returns a9993e364706816aba3e25717850c26c9cd0d89d uppercase`() = runBlocking {
        // RFC 3174 example: "abc" → a9993e364706816aba3e25717850c26c9cd0d89d
        // 115 协议契约:uppercase hex(对齐 Go SDK fmt.Sprintf("%X", sha1))
        val input: InputStream = ByteArrayInputStream("abc".toByteArray())
        assertEquals(
            "A9993E364706816ABA3E25717850C26C9CD0D89D",
            sha1OfStream(input, totalBytes = 3L),
        )
    }

    @Test fun `sha1OfStream of 1MB of zero bytes matches canonical vector`() = runBlocking {
        // 已知向量:1 MiB(1048576 字节)零字节 SHA1 = 3b71f43ff30f4b15b5cd85dd9e95ebc7e84eb5a3
        // (用 python3 hashlib.sha1(b'\x00'*1048576).hexdigest() 验证)
        val bytes = ByteArray(1024 * 1024) // 1 MiB = 1_048_576 bytes
        val input: InputStream = ByteArrayInputStream(bytes)
        assertEquals(
            "3B71F43FF30F4B15B5CD85DD9E95EBC7E84EB5A3",
            sha1OfStream(input, totalBytes = bytes.size.toLong()),
        )
    }

    // ---------- sha1OfPrefix ----------

    @Test fun `sha1OfPrefix of file smaller than prefix returns sha1 of full file`() = runBlocking {
        // 5 字节文件,prefixBytes=128*1024 → 应该返回整个文件的 SHA1(不能只读 5 字节)
        val input: InputStream = ByteArrayInputStream("hello".toByteArray())
        val actual = sha1OfPrefix(input, prefixBytes = 128 * 1024)
        // sha1("hello") = aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d
        assertEquals("AAF4C61DDCC5E8A2DABEDE0F3B482CD9AEA9434D", actual)
    }

    @Test fun `sha1OfPrefix of file larger than prefix reads exactly 128 KiB`() = runBlocking {
        // 文件 1MB,prefixBytes=131072 → 取前 128 KiB 计算 SHA1
        // 128 KiB 零字节 SHA1 = 67dfd19f3eb3649d6f3f6631e44d0bd36b8d8d19
        // (用 python3 hashlib.sha1(b'\x00'*(128*1024)).hexdigest() 验证)
        val bytes = ByteArray(1024 * 1024)
        val input: InputStream = ByteArrayInputStream(bytes)
        val actual = sha1OfPrefix(input, prefixBytes = 128 * 1024)
        assertEquals("67DFD19F3EB3649D6F3F6631E44D0BD36B8D8D19", actual)
    }

    @Test fun `sha1OfPrefix defaults to 128 KiB prefix`() = runBlocking {
        // 默认参数就是 131072 (128 KiB)
        val bytes = ByteArray(1024 * 1024)
        val input: InputStream = ByteArrayInputStream(bytes)
        val default = sha1OfPrefix(input)
        val explicit = sha1OfPrefix(input, prefixBytes = 128 * 1024)
        assertEquals(explicit, default)
    }

    // ---------- partSizeFor (spec §3.2 calPartSize table) ----------
    //
    // 重要:115 / Aliyun OSS 用 **binary** MB(1 MB = 1024 * 1024 = 1_048_576),
    // 跟十进制 SI MB(1 MB = 1_000_000)差 4.86%。
    // OpenList calPartSize 全部用 binary(`utils.MB = 1024 * 1024`),
    // 测试也必须 binary,否则阈值边界会差出 5% — 在 20MB / 128GB 临界点会算错分片数。

    @Test fun `partSizeFor 10MB returns 10MB single part`() {
        // fileSize ≤ 20MB → 整文件 1 分片
        val tenMb = 10L * 1024 * 1024
        assertEquals(tenMb, partSizeFor(tenMb))
    }

    @Test fun `partSizeFor exactly 20MB returns 20MB single part`() {
        // 边界:20MB 整 → 1 分片
        val twentyMb = 20L * 1024 * 1024
        assertEquals(twentyMb, partSizeFor(twentyMb))
    }

    @Test fun `partSizeFor 25MB returns 20MB part size`() {
        // 20MB < fileSize ≤ 128GB → 20MB/片
        val actual = partSizeFor(25L * 1024 * 1024)
        assertEquals(20L * 1024 * 1024, actual)
    }

    @Test fun `partSizeFor 100MB returns 20MB part size`() {
        val actual = partSizeFor(100L * 1024 * 1024)
        assertEquals(20L * 1024 * 1024, actual)
    }

    @Test fun `partSizeFor 100GB returns 20MB part size`() {
        // 100GB 远低于 128GB 阈值,还是 20MB/片
        val actual = partSizeFor(100L * 1024 * 1024 * 1024)
        assertEquals(20L * 1024 * 1024, actual)
    }

    @Test fun `partSizeFor 200GB returns about 26_2MB part size`() {
        // 128GB < fileSize ≤ 256GB → 27487791L (~26.2144 MB)
        // OpenList calPartSize 精确值,非估算
        assertEquals(27_487_791L, partSizeFor(200L * 1024 * 1024 * 1024))
    }

    @Test fun `partSizeFor 600GB returns about 78_6MB part size`() {
        // 512GB < fileSize ≤ 768GB → 82463373L (~78.6432 MB)
        assertEquals(82_463_373L, partSizeFor(600L * 1024 * 1024 * 1024))
    }

    @Test fun `partSizeFor 2TB returns 5GB part size`() {
        // fileSize > 1TB → 5GB/片
        val actual = partSizeFor(2L * 1024 * 1024 * 1024 * 1024)
        assertEquals(5L * 1024 * 1024 * 1024, actual)
    }
}

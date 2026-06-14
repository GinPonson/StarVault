package com.starvault.data

import com.starvault.data.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FixtureLoader 单元测试：直接传 ByteArrayInputStream，
 * 绕开 final AssetManager 不可被 mock 的限制。
 */
class FixtureLoaderTest {

    @Test
    fun `loads List of FileItem from stream`() {
        val json = """[{"id":"f1","name":"a.mp4","type":"VIDEO","mtime":1}]"""
        val items = FixtureLoader.loadFromStream<List<FileItem>>(json.byteInputStream())
        assertTrue(items.isNotEmpty())
        assertEquals("f1", items.first().id)
    }
}

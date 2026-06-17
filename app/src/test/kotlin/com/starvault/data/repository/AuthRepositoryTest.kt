package com.starvault.data.repository

import android.graphics.Bitmap
import com.starvault.data.local.auth.Cloud115AuthStore
import com.starvault.data.remote.cloud115.ScanLoginManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthRepositoryTest {
    private val fakeBitmap: Bitmap = mockk(relaxed = true)

    @Test
    fun `signOut clears auth store`() = runTest {
        val store = mockk<Cloud115AuthStore>(relaxed = true)
        val mgr = mockk<ScanLoginManager>(relaxed = true)
        val repo = AuthRepository(store, mgr, TestScope(UnconfinedTestDispatcher()))

        repo.signOut()

        coVerify { store.clear() }
    }

    @Test
    fun `signIn delegates to scanManager with same qr`() = runTest {
        val store = mockk<Cloud115AuthStore>(relaxed = true)
        val mgr = mockk<ScanLoginManager>(relaxed = true)
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        coEvery { mgr.signIn(qr, any()) } returns flowOf(ScanLoginManager.ScanStatus.Waiting(fakeBitmap))

        val repo = AuthRepository(store, mgr, TestScope(UnconfinedTestDispatcher()))
        repo.signIn(qr).toList()  // 触发 collect

        coVerify { mgr.signIn(qr, any()) }
    }
}

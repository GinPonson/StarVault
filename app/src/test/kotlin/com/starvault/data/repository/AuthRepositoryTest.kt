package com.starvault.data.repository

import android.graphics.Bitmap
import com.starvault.data.local.auth.Cloud115AuthStore
import com.starvault.data.remote.cloud115.ApiEnvelope
import com.starvault.data.remote.cloud115.ScanLoginManager
import com.starvault.data.remote.cloud115.SpaceSummuryData
import com.starvault.data.remote.cloud115.SpaceSummuryInner
import com.starvault.data.remote.cloud115.SpaceSummuryResponse
import com.starvault.data.remote.cloud115.SizeInfo
import com.starvault.data.remote.cloud115.UserApiService
import com.starvault.data.remote.cloud115.UserBaseInfoData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.Response

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {
    private val fakeBitmap: Bitmap = mockk(relaxed = true)

    @Test
    fun `signOut clears auth store`() = runTest {
        val store = mockk<Cloud115AuthStore>(relaxed = true)
        val mgr = mockk<ScanLoginManager>(relaxed = true)
        val userApi = mockk<UserApiService>(relaxed = true)
        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))

        repo.signOut()

        coVerify { store.clear() }
    }

    @Test
    fun `signIn delegates to scanManager with same qr`() = runTest {
        val store = mockk<Cloud115AuthStore>(relaxed = true)
        val mgr = mockk<ScanLoginManager>(relaxed = true)
        val userApi = mockk<UserApiService>(relaxed = true)
        val qr = ScanLoginManager.QRCodeData("u1", 100L, "s1", fakeBitmap)
        coEvery { mgr.signIn(qr, any()) } returns flowOf(ScanLoginManager.ScanStatus.Waiting(fakeBitmap))

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        repo.signIn(qr).toList()  // 触发 collect

        coVerify { mgr.signIn(qr, any()) }
    }

    @Test
    fun `fetchUserInfo assembles UserInfo from both endpoints when both succeed`() = runTest {
        val store = mockk<Cloud115AuthStore>(relaxed = true)
        val mgr = mockk<ScanLoginManager>(relaxed = true)
        val userApi = mockk<UserApiService>(relaxed = true)
        coEvery { userApi.getUserBaseInfo() } returns Response.success(
            ApiEnvelope(state = JsonPrimitive(1), data = UserBaseInfoData(userId = 12345L, userName = "alice", userFace = null))
        )
        coEvery { userApi.getSpaceSummury() } returns Response.success(
            SpaceSummuryResponse(
                state = JsonPrimitive(true),
                spaceSummury = SpaceSummuryInner(
                    allTotal = SizeInfo(size = 1_099_511_627_776.0, sizeFormat = "1.0 TB", percent = 1.0),    // 1 TB
                    allRemain = SizeInfo(size = 319_074_206_720.0, sizeFormat = "297.0 GB", percent = 0.29),  // 29%
                    files = SizeInfo(size = 780_437_421_056.0, sizeFormat = "726.3 GB", percent = 0.71),       // 71%
                    rb = SizeInfo(size = 2_254_857_216.0, sizeFormat = "2.1 GB", percent = 0.0),               // 2.1 GB
                ),
            )
        )

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        val r = repo.fetchUserInfo()

        assertTrue(r.isSuccess)
        val info = r.getOrThrow()
        assertEquals(12345L, info.base.userId)
        assertEquals("alice", info.base.userName)
        assertEquals(1_099_511_627_776L, info.space.spaceSummury.allTotal.size.toLong())
        assertEquals(780_437_421_056L, info.space.spaceSummury.files.size.toLong())
    }

    @Test
    fun `fetchUserInfo returns success with empty base when only space summury succeeds`() = runTest {
        val store = mockk<Cloud115AuthStore>(relaxed = true)
        val mgr = mockk<ScanLoginManager>(relaxed = true)
        val userApi = mockk<UserApiService>(relaxed = true)
        coEvery { userApi.getUserBaseInfo() } throws RuntimeException("network down")
        coEvery { userApi.getSpaceSummury() } returns Response.success(
            SpaceSummuryResponse(
                state = JsonPrimitive(true),
                spaceSummury = SpaceSummuryInner(
                    allTotal = SizeInfo(size = 1024.0, sizeFormat = "1.0 KB", percent = 1.0),
                    files = SizeInfo(size = 512.0, sizeFormat = "512 B", percent = 0.5),
                ),
            )
        )

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        val r = repo.fetchUserInfo()

        assertTrue(r.isSuccess)
        val info = r.getOrThrow()
        assertEquals(0L, info.base.userId)                              // base 全 0 / null
        assertEquals(1024L, info.space.spaceSummury.allTotal.size.toLong())  // space 正常
    }

    @Test
    fun `fetchUserInfo returns failure when both endpoints fail`() = runTest {
        val store = mockk<Cloud115AuthStore>(relaxed = true)
        val mgr = mockk<ScanLoginManager>(relaxed = true)
        val userApi = mockk<UserApiService>(relaxed = true)
        coEvery { userApi.getUserBaseInfo() } throws RuntimeException("user info down")
        coEvery { userApi.getSpaceSummury() } throws RuntimeException("space down")

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        val r = repo.fetchUserInfo()

        assertTrue(r.isFailure)
    }
}

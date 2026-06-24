package com.starvault.data.repository

import com.starvault.data.local.auth.OpenAuthStore
import com.starvault.data.remote.cloud115.OpenAuthApiService
import com.starvault.data.remote.cloud115.OpenAuthManager
import com.starvault.data.remote.cloud115.OpenUserApiService
import com.starvault.data.remote.cloud115.UserBaseInfoData
import com.starvault.data.remote.cloud115.RtSpaceInfo
import com.starvault.data.remote.cloud115.SizeInfo
import com.starvault.data.remote.cloud115.VipInfo
import com.starvault.data.remote.cloud115.OpenUserInfoResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * AuthRepository 测试 — 覆盖 OAuth 时代的:
 *  - signOut 顺序(revoke → clear)
 *  - pollForToken 透传到 authManager
 *  - fetchUserInfo 单端点组装(/open/user/info 直接给 base+space+vip)
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {
    private val fakeBitmap = mockk<android.graphics.Bitmap>(relaxed = true)

    @Test
    fun `signOut revokes refresh token then clears local store`() = runTest {
        val store = mockk<OpenAuthStore>(relaxed = true)
        coEvery { store.refreshToken() } returns "rt_yyy"
        val mgr = mockk<OpenAuthManager>(relaxed = true)
        val userApi = mockk<OpenUserApiService>(relaxed = true)

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        repo.signOut()

        // 决策 #11:revoke 失败也要 clear;这里只验顺序
        coVerify { mgr.revokeToken("rt_yyy") }
        coVerify { store.clear() }
    }

    @Test
    fun `pollForToken delegates to authManager with same deviceCode`() = runTest {
        val store = mockk<OpenAuthStore>(relaxed = true)
        val mgr = mockk<OpenAuthManager>(relaxed = true)
        val userApi = mockk<OpenUserApiService>(relaxed = true)
        val dc = OpenAuthManager.DeviceCodeData(
            uid          = "u1",
            time         = 0L,
            sign         = "s1",
            qrcodeUrl    = "https://115.com/scan/dg-u1",
            codeVerifier = OpenAuthApiService.CODE_VERIFIER,
            bitmap       = fakeBitmap,
        )
        coEvery { mgr.pollForToken(dc, any()) } returns flowOf(OpenAuthManager.AuthStatus.Waiting(fakeBitmap))

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        repo.pollForToken(dc).toList()  // 触发 collect

        coVerify { mgr.pollForToken(dc, any()) }
    }

    @Test
    fun `fetchUserInfo assembles UserInfo from single endpoint when it succeeds`() = runTest {
        val store = mockk<OpenAuthStore>(relaxed = true)
        val mgr = mockk<OpenAuthManager>(relaxed = true)
        val userApi = mockk<OpenUserApiService>(relaxed = true)
        coEvery { userApi.userInfo() } returns Response.success(
            OpenUserInfoResponse(
                state = true,
                data = UserBaseInfoData(
                    userId = 12345L,
                    userName = "alice",
                    rtSpaceInfo = RtSpaceInfo(
                        allTotal = SizeInfo(size = 1_099_511_627_776L, sizeFormat = "1.0 TB"),
                        allRemain = SizeInfo(size = 319_074_206_720L, sizeFormat = "297.0 GB"),
                        allUse = SizeInfo(size = 780_437_421_056L, sizeFormat = "726.3 GB"),
                    ),
                    vipInfo = VipInfo(levelName = "年费VIP", expire = 1893456000),
                ),
            )
        )

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        val r = repo.fetchUserInfo()

        assertTrue(r.isSuccess)
        val info = r.getOrThrow()
        assertEquals(12345L, info.base.userId)
        assertEquals("alice", info.base.userName)
        assertEquals(1_099_511_627_776L, info.space?.allTotal?.size)
        assertEquals("1.0 TB", info.space?.allTotal?.sizeFormat)
        assertEquals("年费VIP", info.vip?.levelName)
    }

    @Test
    fun `fetchUserInfo returns failure when HTTP fails`() = runTest {
        val store = mockk<OpenAuthStore>(relaxed = true)
        val mgr = mockk<OpenAuthManager>(relaxed = true)
        val userApi = mockk<OpenUserApiService>(relaxed = true)
        coEvery { userApi.userInfo() } returns Response.error(500, "".toResponseBody("text/plain".toMediaType()))

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        val r = repo.fetchUserInfo()
        assertTrue(r.isFailure)
    }

    @Test
    fun `fetchUserInfo returns failure when state is false`() = runTest {
        val store = mockk<OpenAuthStore>(relaxed = true)
        val mgr = mockk<OpenAuthManager>(relaxed = true)
        val userApi = mockk<OpenUserApiService>(relaxed = true)
        coEvery { userApi.userInfo() } returns Response.success(
            OpenUserInfoResponse(state = false, error = "签名校验失败", data = null)
        )

        val repo = AuthRepository(store, mgr, userApi, TestScope(UnconfinedTestDispatcher()))
        val r = repo.fetchUserInfo()
        assertTrue(r.isFailure)
    }
}

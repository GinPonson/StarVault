package com.starvault.ui.profile

import com.starvault.core.ToastBus
import com.starvault.data.remote.cloud115.OpenAuthApiService
import com.starvault.data.remote.cloud115.OpenAuthManager
import com.starvault.data.remote.cloud115.OpenUserApiService
import com.starvault.data.remote.cloud115.RtSpaceInfo
import com.starvault.data.remote.cloud115.SizeInfo
import com.starvault.data.remote.cloud115.UserBaseInfoData
import com.starvault.data.remote.cloud115.VipInfo
import com.starvault.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * ProfileViewModel 测试 — 聚焦 [ProfileViewModel.onSignOut] 行为。
 *
 *  - 成功路径:调 [AuthRepository.signOut],不发 effect(NavHost 自动处理跳转)
 *  - 失败路径:signOut 抛异常 → 发 [Effect.Error] 给 Route 端 Snackbar
 *  - fetchUserInfo 成功 → state.storage 字段被 rt_space_info / vip_info 覆盖
 *  - fetchUserInfo 失败 → 发 [Effect.Error]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Before
    fun setUp() {
        // ProfileViewModel 用 android.util.Log.w;JVM unit test 下 stub 掉,免抛 UnsatisfiedLinkError
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0

        // ProfileViewModel 用 ToastBus.error 投递错误;stub 避免走真 Channel
        mockkObject(ToastBus)
        every { ToastBus.error(any()) } returns Unit

        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(android.util.Log::class)
        unmockkObject(ToastBus)
    }

    @Test
    fun `onSignOut delegates to AuthRepository signOut`() = runTest {
        val repo = FakeAuthRepository()  // 默认 userApi 走成功 stub
        val vm = ProfileViewModel(repo)

        vm.onSignOut()

        assertEquals(1, repo.signOutCalls.get())
    }

    @Test
    fun `onSignOut success does not emit any effect`() = runTest {
        val repo = FakeAuthRepository()  // 默认 userApi 走成功 stub,init 阶段不 emit Error
        val vm = ProfileViewModel(repo)

        vm.onSignOut()
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { ToastBus.error(any()) }
    }

    @Test
    fun `onSignOut failure emits Effect Error with exception message`() = runTest {
        val errMsg = "disk full"
        val repo = FakeAuthRepository(throwOnSignOut = RuntimeException(errMsg))
        val vm = ProfileViewModel(repo)

        vm.onSignOut()
        testScheduler.advanceUntilIdle()

        // 只有 onSignOut 抛错 → emit 1 个 Error(init 阶段 userApi 走成功 stub,不会 emit)
        verify(exactly = 1) { ToastBus.error(errMsg) }
    }

    @Test
    fun `fetchUserInfo success merges rt_space_info and vip_info into state`() = runTest {
        val userApi = mockk<OpenUserApiService>(relaxed = true)
        coEvery { userApi.userInfo() } returns Response.success(
            com.starvault.data.remote.cloud115.OpenUserInfoResponse(
                state = true,
                data = UserBaseInfoData(
                    userId = 99L,
                    userName = "alice",
                    rtSpaceInfo = RtSpaceInfo(
                        allTotal = SizeInfo(size = 1000L, sizeFormat = "1.0 KB"),
                        allRemain = SizeInfo(size = 700L, sizeFormat = "0.7 KB"),
                        allUse = SizeInfo(size = 300L, sizeFormat = "0.3 KB"),
                    ),
                    vipInfo = VipInfo(levelName = "年费VIP", expire = 1893456000),
                ),
            )
        )
        val repo = FakeAuthRepository(userApi = userApi)
        val vm = ProfileViewModel(repo)
        // 让 init 阶段的 loadUserInfo 跑完
        testScheduler.advanceUntilIdle()

        val state = vm.state.value as ProfileUiState.Success
        assertEquals("alice", state.storage.userName)
        assertEquals("年费VIP", state.storage.vipLevelName)
        // usedPct = 300/1000*100 = 30
        assertEquals(30, state.storage.usedPct)
        assertEquals("1.0 KB", state.storage.totalLabel)
        assertEquals("0.7 KB", state.storage.remainingGb)
    }

    @Test
    fun `fetchUserInfo failure emits Effect Error`() = runTest {
        val userApi = mockk<OpenUserApiService>(relaxed = true)
        // 走 throw 路径,避免构造 ResponseBody 触发 android/graphics/ColorSpace class 加载
        coEvery { userApi.userInfo() } throws RuntimeException("boom")
        val repo = FakeAuthRepository(userApi = userApi)
        val vm = ProfileViewModel(repo)

        testScheduler.advanceUntilIdle()

        // 至少有 1 个 Error effect(message 含 "boom")
        verify(atLeast = 1) { ToastBus.error(match { it.contains("boom") }) }
    }
}

/**
 * Fake [AuthRepository]:用 counter 验证调用,用 throw 控制失败。
 *
 *  appScope 走真实 [TestScope](stateIn 是 lazy,但 appScope 作为构造参数是 eager 求值)。
 *  tokenStore / authManager 走 mockk(VM 测试不调它们,只让类型通过)。
 *  userApi 用于 fetchUserInfo 委托。
 */
private class FakeAuthRepository(
    private val throwOnSignOut: Exception? = null,
    userApi: OpenUserApiService = defaultUserApi(),
) : AuthRepository(
    tokenStore  = mockk(relaxed = true),
    authManager = mockk<OpenAuthManager>(relaxed = true),
    openUserApi = userApi,
    appScope    = TestScope(UnconfinedTestDispatcher()),
) {
    val signOutCalls = java.util.concurrent.atomic.AtomicInteger(0)

    override suspend fun signOut() {
        signOutCalls.incrementAndGet()
        throwOnSignOut?.let { throw it }
    }
}

/**
 * 默认 userApi:userInfo() 返回最小成功响应 + 空 rt_space_info,这样
 * init 阶段的 loadUserInfo() 不会 emit Error effect(测试只关注 onSignOut)。
 * 测试要覆盖 userInfo 失败路径时,显式传入 throw 的 userApi。
 */
private fun defaultUserApi(): OpenUserApiService {
    val api = mockk<OpenUserApiService>(relaxed = true)
    coEvery { api.userInfo() } returns Response.success(
        com.starvault.data.remote.cloud115.OpenUserInfoResponse(
            state = true,
            data = UserBaseInfoData(
                userId = 1L,
                userName = "tester",
                rtSpaceInfo = RtSpaceInfo(),
                vipInfo = VipInfo(),
            ),
        )
    )
    return api
}

private fun String.toResponseBody(mediaType: okhttp3.MediaType?, contentType: Int?) =
    okhttp3.ResponseBody.create(mediaType, this)

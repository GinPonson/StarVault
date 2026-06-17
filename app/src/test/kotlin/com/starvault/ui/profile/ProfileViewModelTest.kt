package com.starvault.ui.profile

import com.starvault.data.local.auth.Cloud115AuthStore
import com.starvault.data.remote.cloud115.ScanLoginManager
import com.starvault.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * ProfileViewModel 测试 — 聚焦 [ProfileViewModel.onSignOut] 行为。
 *
 *  - 成功路径：调 [AuthRepository.signOut]，不发 effect（NavHost 自动处理跳转）
 *  - 失败路径：signOut 抛异常 → 发 [Effect.Error] 给 Route 端 Snackbar
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSignOut delegates to AuthRepository signOut`() = runTest {
        val repo = FakeAuthRepository()
        val vm = ProfileViewModel(repo)

        vm.onSignOut()

        assertEquals(1, repo.signOutCalls.get())
    }

    @Test
    fun `onSignOut success does not emit any effect`() = runTest {
        val repo = FakeAuthRepository()  // 不 throw = success path
        val vm = ProfileViewModel(repo)
        val collected = mutableListOf<ProfileViewModel.Effect>()
        val collectJob = launch { vm.effect.collect { collected.add(it) } }

        vm.onSignOut()
        // Channel(UNLIMITED) + trySend：trySend 在 success path 不调，所以 collected 应为空
        testScheduler.advanceUntilIdle()
        collectJob.cancel()

        assertTrue("success should not emit any effect; got=$collected", collected.isEmpty())
    }

    @Test
    fun `onSignOut failure emits Effect Error with exception message`() = runTest {
        val errMsg = "disk full"
        val repo = FakeAuthRepository(throwOnSignOut = RuntimeException(errMsg))
        val vm = ProfileViewModel(repo)
        val collected = mutableListOf<ProfileViewModel.Effect>()
        val collectJob = launch { vm.effect.collect { collected.add(it) } }

        vm.onSignOut()
        // Channel 收到 trySend → collect 拿到
        testScheduler.advanceUntilIdle()
        collectJob.cancel()

        assertEquals(1, collected.size)
        assertTrue(collected.first() is ProfileViewModel.Effect.Error)
        assertEquals(errMsg, (collected.first() as ProfileViewModel.Effect.Error).message)
    }
}

/**
 * Fake [AuthRepository]：用 counter 验证调用，用 throw 控制失败。
 *
 *  appScope 走真实 [TestScope]（stateIn 是 lazy，但 appScope 作为构造参数是 eager 求值）。
 *  authStore / scanManager 走 mockk（VM 测试不调它们，只让类型通过）。
 */
private class FakeAuthRepository(
    private val throwOnSignOut: Exception? = null,
) : AuthRepository(
    authStore = fakeAuthStore(),
    scanManager = fakeScanManager(),
    appScope = TestScope(),
) {
    val signOutCalls = AtomicInteger(0)
    override suspend fun signOut() {
        signOutCalls.incrementAndGet()
        throwOnSignOut?.let { throw it }
    }
}

private fun fakeAuthStore(): Cloud115AuthStore = mockk(relaxed = true) {
    coEvery { cookiesFlow } returns flowOf(null)
}

private fun fakeScanManager(): ScanLoginManager = mockk(relaxed = true)

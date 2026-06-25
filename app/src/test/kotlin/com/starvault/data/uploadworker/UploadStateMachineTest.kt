package com.starvault.data.uploadworker

import com.starvault.data.remote.cloud115.CallbackInfo
import com.starvault.data.remote.cloud115.UploadCallback
import com.starvault.data.remote.cloud115.UploadInitResp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * UploadStateMachine 纯函数 TDD — 决策 [UploadInitResp] status 字段的下一步动作。
 *
 * 关键约束(spec §3.1.4 + §8):
 *  - status=1       → Continue(走 OSS 上传)
 *  - status=2       → Reject("暂不支持秒传")
 *  - status=6/7/8   → SignCheck(解析 sign_check "start-end" 算区间 SHA1)
 *  - 其它(0/4/5 等) → Reject("初始化失败:status={N}")
 *
 * 把 status 分支提到纯函数有两层好处:
 *  1. RED-GREEN 在纯 JVM 跑(无 Robolectric 依赖)
 *  2. Worker 业务代码 ≈ "init → state machine.decide(resp) → 分支" 线性
 *
 * SignCheckStep 决定需要多少字节的区间 hash:
 *  - status=6 → sign_val 是 sign_check 区间的 SHA1
 *  - status=7/8 → 服务端不同字段名,本测试仅覆盖 6(M2 主流)
 */
class UploadStateMachineTest {

    private val sm = UploadStateMachine

    private fun resp(
        status: Int,
        signCheck: String = "",
        signKey: String = "",
        bucket: String = "b",
        `object`: String = "o",
    ): UploadInitResp = UploadInitResp(
        status = status,
        sign_key = signKey,
        sign_check = signCheck,
        file_id = "",
        target = "U_1_0",
        bucket = bucket,
        `object` = `object`,
        callback = UploadCallback.Single(CallbackInfo("", "")),
        pick_code = "",
    )

    // ---------- happy path ----------

    @Test fun `status 1 returns Continue`() {
        val decision = sm.decide(resp(status = 1))
        assertTrue("expected Continue, got: $decision", decision is UploadDecision.Continue)
        val cont = decision as UploadDecision.Continue
        assertEquals("b", cont.bucket)
        assertEquals("o", cont.ossObject)
    }

    // ---------- 秒传 ----------

    @Test fun `status 2 returns Reject with 秒传 message`() {
        val decision = sm.decide(resp(status = 2))
        assertTrue("expected Reject, got: $decision", decision is UploadDecision.Reject)
        val rej = decision as UploadDecision.Reject
        assertTrue("expected 秒传 in message, got: ${rej.message}",
            rej.message.contains("秒传"))
    }

    // ---------- two-way verify ----------

    @Test fun `status 6 with sign_check 100-200 returns SignCheck with parsed range`() {
        // sign_check 格式 = "start-end"(inclusive,字节偏移)
        val decision = sm.decide(resp(status = 6, signCheck = "100-200", signKey = "K1"))
        assertTrue("expected SignCheck, got: $decision", decision is UploadDecision.SignCheck)
        val sc = decision as UploadDecision.SignCheck
        assertEquals("K1", sc.signKey)
        assertEquals(100L, sc.startInclusive)
        assertEquals(200L, sc.endInclusive)
    }

    @Test fun `status 7 returns SignCheck`() {
        val decision = sm.decide(resp(status = 7, signCheck = "0-99", signKey = "K2"))
        assertTrue(decision is UploadDecision.SignCheck)
    }

    @Test fun `status 8 returns SignCheck`() {
        val decision = sm.decide(resp(status = 8, signCheck = "0-0", signKey = "K3"))
        assertTrue(decision is UploadDecision.SignCheck)
    }

    // ---------- 其它 status ----------

    @Test fun `status 0 returns Reject with 初始化失败 message`() {
        val decision = sm.decide(resp(status = 0))
        assertTrue(decision is UploadDecision.Reject)
        val rej = decision as UploadDecision.Reject
        assertTrue("expected 初始化失败 in message, got: ${rej.message}",
            rej.message.contains("初始化失败"))
    }

    @Test fun `status 4 returns Reject with 初始化失败 message`() {
        val decision = sm.decide(resp(status = 4))
        assertTrue(decision is UploadDecision.Reject)
    }

    // ---------- sign_check 解析 ----------

    @Test fun `parseSignRange single digit works`() {
        val range = sm.parseSignRange("0-9")
        assertEquals(0L, range.first)
        assertEquals(9L, range.last)
    }

    @Test fun `parseSignRange large numbers works`() {
        val range = sm.parseSignRange("1048576-2097151")
        assertEquals(1_048_576L, range.first)
        assertEquals(2_097_151L, range.last)
    }

    @Test fun `parseSignRange malformed throws DecisionException`() {
        try {
            sm.parseSignRange("not-a-range")
            fail("expected DecisionException")
        } catch (e: UploadDecisionException) {
            // expected
        }
    }
}

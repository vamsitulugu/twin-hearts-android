package com.twinhearts.app.core.pro

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProTest {

    private class FakeRepo(var status: AppResult<ProStatus>) : ProRepository {
        var calls = 0
        override suspend fun status(): AppResult<ProStatus> { calls++; return status }
        override suspend fun createOrder(plan: ProPlan) = AppResult.Failure(AppError.Unknown)
        override suspend fun verify(orderId: String, paymentId: String, signature: String) = AppResult.Failure(AppError.Unknown)
        override suspend fun history() = AppResult.Success(emptyList<ProPayment>())
    }

    private val now = 1_800_000_000_000L
    private val future = java.time.Instant.ofEpochMilli(now + 86_400_000).toString()
    private val past = java.time.Instant.ofEpochMilli(now - 86_400_000).toString()

    private fun TestScope.gate(repo: ProRepository) =
        ProGate(repo, backgroundScope, { now }, MutableStateFlow("couple1"))

    @Test fun `status is active only while not expired`() {
        assertTrue(ProStatus(true, ProPlan.Monthly, future).isActive(now))
        assertFalse(ProStatus(true, ProPlan.Monthly, past).isActive(now))
        assertTrue(ProStatus(true, ProPlan.Lifetime, null).isActive(now))
        assertFalse(ProStatus.Free.isActive(now))
    }

    @Test fun `pro couple runs the action without a paywall`() = runTest(StandardTestDispatcher()) {
        val g = gate(FakeRepo(AppResult.Success(ProStatus(true, ProPlan.Yearly, future))))
        var ran = 0
        val shown = mutableListOf<String>()
        backgroundCollect(g, shown)
        g.require("Pin a Memory") { ran++ }
        advanceUntilIdle()
        assertEquals(1, ran)
        assertTrue(shown.isEmpty())
    }

    @Test fun `free couple gets the paywall and the action does not run`() = runTest(StandardTestDispatcher()) {
        val g = gate(FakeRepo(AppResult.Success(ProStatus.Free)))
        var ran = 0
        val shown = mutableListOf<String>()
        backgroundCollect(g, shown)
        g.require("Music") { ran++ }
        advanceUntilIdle()
        assertEquals(0, ran)
        assertEquals(listOf("Music"), shown)
    }

    @Test fun `server unreachable shows the paywall instead of unlocking`() = runTest(StandardTestDispatcher()) {
        val g = gate(FakeRepo(AppResult.Failure(AppError.Network)))
        var ran = 0
        val shown = mutableListOf<String>()
        backgroundCollect(g, shown)
        g.require("Both") { ran++ }
        advanceUntilIdle()
        assertEquals(0, ran)
        assertEquals(listOf("Both"), shown)
    }

    @Test fun `known pro is trusted without asking the server again`() = runTest(StandardTestDispatcher()) {
        val repo = FakeRepo(AppResult.Success(ProStatus(true, ProPlan.Lifetime, null)))
        val g = gate(repo)
        g.refresh()
        var ran = 0
        g.require("x") { ran++ }
        assertEquals(1, ran)
        assertEquals(1, repo.calls)
    }

    @Test fun `razorpay cancel and failure messages`() {
        val b = PaymentBridge()
        assertEquals(PaymentBridge.DEFAULT_FAILURE, PaymentBridge.readableMessage(null))
        assertEquals("Card declined", PaymentBridge.readableMessage("{\"error\":{\"description\":\"Card declined\"}}"))
        assertEquals("Plain text", PaymentBridge.readableMessage("Plain text"))
        assertEquals(0, PaymentBridge.CODE_CANCELLED)
        b.error(0, null) // must not throw
    }

    private fun TestScope.backgroundCollect(g: ProGate, into: MutableList<String>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { g.paywall.collect { into.add(it) } }
    }
}

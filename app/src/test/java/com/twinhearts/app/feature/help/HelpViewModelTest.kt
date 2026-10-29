package com.twinhearts.app.feature.help

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.help.data.HelpRepository
import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.HelpCategory
import com.twinhearts.app.feature.help.domain.model.SendResult
import com.twinhearts.app.feature.help.domain.model.SenderKind
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import com.twinhearts.app.feature.help.domain.model.SupportMode
import com.twinhearts.app.feature.help.domain.model.SupportTicket
import com.twinhearts.app.feature.help.ui.ChatLink
import com.twinhearts.app.feature.help.ui.HelpEvent
import com.twinhearts.app.feature.help.ui.HelpTab
import com.twinhearts.app.feature.help.ui.HelpViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HelpViewModelTest {
    @get:Rule val mainDispatcher = HelpMainDispatcherRule()

    private val repository = mockk<HelpRepository>()
    private val faq = FaqItem("f1", "Pairing & Account", "How do I pair?", "Settings.", emptyList())

    private fun msg(id: String, sender: SenderKind = SenderKind.User, text: String = id) =
        SupportMessage(id, sender, "Vamsi", text, null)

    private val aiTicket = SupportTicket("t1", SupportMode.Ai, listOf(msg("w", SenderKind.Ai)))

    private fun viewModel(
        faqs: AppResult<List<FaqItem>> = AppResult.Success(listOf(faq)),
        open: AppResult<SupportTicket> = AppResult.Success(aiTicket),
        saved: SavedStateHandle = SavedStateHandle(),
    ): HelpViewModel {
        every { repository.myName } returns flowOf("Vamsi")
        coEvery { repository.faqs() } returns faqs
        coEvery { repository.openTicket(any()) } returns open
        return HelpViewModel(repository, saved)
    }

    @Test
    fun loadsQuestions() {
        assertEquals(UiState.Content(listOf(faq)), viewModel().state.value.faqs)
        assertEquals("Vamsi", viewModel().state.value.myName)
    }

    @Test
    fun emptyAndFailedQuestionLists() {
        assertEquals(UiState.Empty, viewModel(faqs = AppResult.Success(emptyList())).state.value.faqs)
        val failed = viewModel(faqs = AppResult.Failure(AppError.Network)).state.value.faqs
        assertEquals(UiState.Error(AppError.Network), failed)
    }

    @Test
    fun searchTopicAndTabAreKeptInSavedState() {
        val saved = SavedStateHandle()
        val vm = viewModel(saved = saved)
        vm.onQueryChange("vault")
        vm.onCategory(HelpCategory.Privacy)
        vm.selectTab(HelpTab.Chat)
        val restored = viewModel(saved = saved).state.value
        assertEquals("vault", restored.query)
        assertEquals(HelpCategory.Privacy, restored.category)
        assertEquals(HelpTab.Chat, restored.tab)
    }

    @Test
    fun severalQuestionsCanBeOpen() {
        val vm = viewModel()
        vm.toggleFaq("a")
        vm.toggleFaq("b")
        vm.toggleFaq("a")
        assertEquals(setOf("b"), vm.state.value.expandedIds)
    }

    @Test
    fun openingTheChatTabConnectsAndShowsTheThread() {
        val vm = viewModel()
        vm.selectTab(HelpTab.Chat)
        val chat = vm.state.value.chat
        assertEquals(ChatLink.Connected, chat.link)
        assertEquals("t1", chat.ticketId)
        assertEquals(aiTicket.messages, chat.messages)
    }

    @Test
    fun connectFailureShowsErrorAndRetryWorks() {
        val vm = viewModel(open = AppResult.Failure(AppError.Network))
        vm.selectTab(HelpTab.Chat)
        assertTrue(vm.state.value.chat.link is ChatLink.Failed)
        coEvery { repository.openTicket(any()) } returns AppResult.Success(aiTicket)
        vm.retryConnect()
        assertEquals(ChatLink.Connected, vm.state.value.chat.link)
    }

    @Test
    fun myMessageShowsAtOnceThenTheServerThreadReplacesIt() = runTest {
        val vm = viewModel()
        vm.selectTab(HelpTab.Chat)
        val gate = CompletableDeferred<Unit>()
        val reply = SendResult(
            SupportTicket("t1", SupportMode.Ai, listOf(msg("w", SenderKind.Ai), msg("m1"), msg("a1", SenderKind.Ai))),
            null,
            msg("a1", SenderKind.Ai),
        )
        coEvery { repository.send("t1", "hello", SupportMode.Ai) } coAnswers { gate.await(); AppResult.Success(reply) }

        vm.onDraftChange("hello")
        vm.send()
        assertTrue(vm.state.value.chat.sending)
        assertEquals("", vm.state.value.chat.draft)
        assertEquals("hello", vm.state.value.chat.messages.last().text)

        gate.complete(Unit)
        assertFalse(vm.state.value.chat.sending)
        assertEquals(reply.ticket?.messages, vm.state.value.chat.messages)
    }

    @Test
    fun failedSendRemovesMessageRestoresDraftAndToasts() = runTest {
        val vm = viewModel()
        vm.selectTab(HelpTab.Chat)
        coEvery { repository.send(any(), any(), any()) } returns AppResult.Failure(AppError.Network)
        vm.onDraftChange("hello")
        vm.send()
        assertEquals("hello", vm.state.value.chat.draft)
        assertEquals(aiTicket.messages, vm.state.value.chat.messages)
        vm.events.test { assertEquals(HelpEvent.SendFailed, awaitItem()) }
    }

    @Test
    fun connectFailureWhileSendingToastsAndKeepsDraft() = runTest {
        val vm = viewModel(open = AppResult.Failure(AppError.Network))
        vm.onDraftChange("hello")
        vm.send()
        assertEquals("hello", vm.state.value.chat.draft)
        vm.events.test { assertEquals(HelpEvent.ConnectFailed, awaitItem()) }
    }

    @Test
    fun liveAgentSendToastsSentAndAiWithoutAnswerToastsUnavailable() = runTest {
        val vm = viewModel()
        vm.selectTab(HelpTab.Chat)
        coEvery { repository.send("t1", "one", SupportMode.Ai) } returns
            AppResult.Success(SendResult(null, msg("m1"), null))
        vm.sendQuick("one")
        vm.events.test { assertEquals(HelpEvent.AiUnavailable, awaitItem()) }

        coEvery { repository.switchMode("t1", SupportMode.Human) } returns
            AppResult.Success(SupportTicket("t1", SupportMode.Human, aiTicket.messages))
        vm.selectMode(SupportMode.Human)
        coEvery { repository.send("t1", "two", SupportMode.Human) } returns
            AppResult.Success(SendResult(null, msg("m2"), null))
        vm.sendQuick("two")
        vm.events.test { assertEquals(HelpEvent.MessageSent, awaitItem()) }
    }

    @Test
    fun startChatWithHumanSwitchesThenSends() {
        val vm = viewModel()
        coEvery { repository.switchMode("t1", SupportMode.Human) } returns
            AppResult.Success(SupportTicket("t1", SupportMode.Human, aiTicket.messages))
        coEvery { repository.send(any(), any(), any()) } returns AppResult.Success(SendResult(null, msg("m"), null))

        vm.startChat(SupportMode.Human, "I have a question")

        assertEquals(HelpTab.Chat, vm.state.value.tab)
        assertEquals(SupportMode.Human, vm.state.value.chat.mode)
        coVerifyOrder {
            repository.switchMode("t1", SupportMode.Human)
            repository.send("t1", "I have a question", SupportMode.Human)
        }
    }

    @Test
    fun failedSwitchGoesBackToTheServerMode() {
        val vm = viewModel()
        vm.selectTab(HelpTab.Chat)
        coEvery { repository.switchMode(any(), any()) } returns AppResult.Failure(AppError.Network)
        vm.selectMode(SupportMode.Human)
        assertEquals(SupportMode.Ai, vm.state.value.chat.mode)
    }

    @Test
    fun sameModeTapDoesNothing() {
        val vm = viewModel()
        vm.selectTab(HelpTab.Chat)
        vm.selectMode(SupportMode.Ai)
        coVerify(exactly = 0) { repository.switchMode(any(), any()) }
    }

    @Test
    fun pollingOnlyAddsNewMessages() = runTest {
        val vm = viewModel()
        vm.selectTab(HelpTab.Chat)
        coEvery { repository.refresh("t1") } returns AppResult.Success(aiTicket)
        vm.pollOnce()
        assertEquals(aiTicket.messages, vm.state.value.chat.messages)

        val longer = SupportTicket("t1", SupportMode.Ai, aiTicket.messages + msg("admin1", SenderKind.Admin))
        coEvery { repository.refresh("t1") } returns AppResult.Success(longer)
        vm.pollOnce()
        assertEquals(2, vm.state.value.chat.messages.size)
    }
}

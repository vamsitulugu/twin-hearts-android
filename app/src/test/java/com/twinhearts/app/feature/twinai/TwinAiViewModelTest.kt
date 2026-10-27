package com.twinhearts.app.feature.twinai

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.twinai.data.TwinAiRepository
import com.twinhearts.app.feature.twinai.domain.model.AiAuthor
import com.twinhearts.app.feature.twinai.domain.model.AiIdentity
import com.twinhearts.app.feature.twinai.domain.model.AiMessageState
import com.twinhearts.app.feature.twinai.domain.model.AiStreamEvent
import com.twinhearts.app.feature.twinai.ui.TwinAiViewModel
import com.twinhearts.app.feature.twinai.ui.TwinTab
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TwinAiViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<TwinAiRepository>(relaxed = true)
    private val who = AiIdentity("c1", "user1", "Vamsi Krishna", "Likitha", "")

    private fun viewModel(bothOn: Boolean = false): TwinAiViewModel {
        coEvery { repository.identity() } returns who
        coEvery { repository.bothEnabled(who) } returns AppResult.Success(bothOn)
        return TwinAiViewModel(repository)
    }

    @Test
    fun greetsWithFirstNameOnly() {
        assertEquals("Vamsi", viewModel().state.value.greetingName)
    }

    @Test
    fun bothTabCanBeOpenedWithoutAnyServerFlag() {
        val vm = viewModel(bothOn = false)
        vm.selectTab(TwinTab.Both)
        assertEquals(TwinTab.Both, vm.state.value.tab)
    }

    @Test
    fun streamedReplyFillsTheBubble() {
        val vm = viewModel()
        every { repository.reply(who, "hi") } returns flowOf(
            AiStreamEvent.Delta("Hello "), AiStreamEvent.Delta("love"), AiStreamEvent.Done,
        )
        vm.ask("hi")
        val messages = vm.state.value.messages
        assertEquals(2, messages.size)
        assertEquals(AiAuthor.Me, messages[0].author)
        assertEquals("Hello love", messages[1].text)
        assertEquals(AiMessageState.Done, messages[1].state)
        assertFalse(vm.state.value.streaming)
        assertFalse(vm.state.value.isWelcome)
    }

    @Test
    fun failureMarksBubbleFailedAndRetryAsksAgain() {
        val vm = viewModel()
        every { repository.reply(who, "hi") } returns flowOf(AiStreamEvent.Failure(AppError.Network))
        vm.ask("hi")
        val failed = vm.state.value.messages.last()
        assertEquals(AiMessageState.Failed, failed.state)
        assertEquals(AppError.Network, failed.error)

        every { repository.reply(who, "hi") } returns flowOf(AiStreamEvent.Delta("ok"), AiStreamEvent.Done)
        vm.askAgain(failed)
        assertEquals(3, vm.state.value.messages.size) // no second "me" bubble
        assertEquals("ok", vm.state.value.messages.last().text)
    }

    @Test
    fun blankInputIsIgnored() {
        val vm = viewModel()
        vm.onInputChange("   ")
        vm.sendInput()
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun sendingClearsTheInput() {
        val vm = viewModel()
        every { repository.reply(who, "hello") } returns flowOf(AiStreamEvent.Done)
        vm.onInputChange("hello")
        vm.sendInput()
        assertEquals("", vm.state.value.input)
    }
}

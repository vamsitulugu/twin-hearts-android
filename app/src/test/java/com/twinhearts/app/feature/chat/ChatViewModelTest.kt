package com.twinhearts.app.feature.chat

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.chat.data.ChatRepository
import com.twinhearts.app.feature.chat.domain.model.ChatIdentity
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.ui.ChatBanner
import com.twinhearts.app.feature.chat.ui.ChatViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import com.twinhearts.app.feature.chat.data.ChatLocalStore
import com.twinhearts.app.feature.chat.data.ChatFiles
import com.twinhearts.app.feature.chat.domain.model.ChatSettings
import com.twinhearts.app.feature.chat.domain.model.Wallpaper
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/*
 * Why this file exists: unit tests for the Chat feature.
 */

class ChatViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<ChatRepository>(relaxed = true)
    private val local = mockk<ChatLocalStore>(relaxed = true)
    private val files = mockk<ChatFiles>(relaxed = true)
    private val who = ChatIdentity("couple", "user1", "Vamsi", "Likitha")

    private fun message(id: Long, clientId: String, mine: Boolean = false, delivery: DeliveryState = DeliveryState.Sent, starred: Boolean = false) =
        ChatMessage(
            id = id, clientId = clientId, mine = mine, kind = MessageKind.Text, text = "t$id", mediaUrl = null,
            createdAt = Instant.parse("2026-10-26T09:00:00Z").plusSeconds(id), delivery = delivery, starred = starred,
        )

    private fun stubSettings() {
        every { local.settings(any()) } returns flowOf(ChatSettings())
        every { local.recentEmoji() } returns flowOf(emptyList())
        every { local.wallpaperShared(any()) } returns flowOf(false)
        every { local.localWallpaper(any()) } returns flowOf(Wallpaper())
        coEvery { local.draft(any()) } returns ""
        coEvery { repository.disappearingSeconds(who) } returns AppResult.Success(0)
        coEvery { repository.sharedWallpaper(who) } returns AppResult.Success(Wallpaper())
    }

    private fun viewModel(initial: List<ChatMessage> = emptyList()): ChatViewModel {
        coEvery { repository.identity() } returns who
        coEvery { repository.recent(who) } returns AppResult.Success(initial)
        coEvery { repository.partnerPresence(who) } returns AppResult.Success(PartnerPresence(PresenceState.Online, null))
        coEvery { repository.scheduled(who) } returns AppResult.Success(emptyList())
        coEvery { repository.markRead(who) } returns AppResult.Success(Unit)
        stubSettings()
        return ChatViewModel(repository, local, files)
    }

    @Test
    fun load_showsMessages_andMarksPartnerMessagesRead() {
        val vm = viewModel(listOf(message(1, "a")))
        assertEquals(1, vm.state.value.messages.size)
        assertEquals(PresenceState.Online, vm.state.value.presence.state)
        coVerify { repository.markRead(who) }
        assertEquals(DeliveryState.Read, vm.state.value.messages.first().delivery)
    }

    @Test
    fun load_failure_showsError_whenNothingToShow() {
        coEvery { repository.identity() } returns who
        coEvery { repository.recent(who) } returns AppResult.Failure(AppError.Network)
        coEvery { repository.partnerPresence(who) } returns AppResult.Failure(AppError.Network)
        coEvery { repository.scheduled(who) } returns AppResult.Failure(AppError.Network)
        stubSettings()
        val vm = ChatViewModel(repository, local, files)
        assertEquals(AppError.Network, vm.state.value.loadError)
    }

    @Test
    fun sendText_replacesLocalCopyWithServerCopy() {
        val vm = viewModel()
        coEvery { repository.sendText(who, any(), "Hello", null, null) } answers {
            AppResult.Success(message(9, secondArg(), mine = true))
        }
        vm.sendText("  Hello  ")
        val sent = vm.state.value.messages
        assertEquals(1, sent.size)
        assertEquals(9L, sent.first().id)
    }

    @Test
    fun sendText_failure_marksFailed_andRetrySendsAgain() {
        val vm = viewModel()
        coEvery { repository.sendText(who, any(), "Hi", null, null) } returns AppResult.Failure(AppError.Network)
        vm.sendText("Hi")
        val failed = vm.state.value.messages.single()
        assertEquals(DeliveryState.Failed, failed.delivery)
        assertEquals(ChatBanner.Failure(AppError.Network), vm.state.value.banner)
        coEvery { repository.sendText(who, failed.clientId, "Hi", null, null) } returns AppResult.Success(message(10, failed.clientId, mine = true))
        vm.retry(failed.clientId)
        assertEquals(10L, vm.state.value.messages.single().id)
    }

    @Test
    fun emptyText_isNotSent() {
        val vm = viewModel()
        vm.sendText("   ")
        coVerify(exactly = 0) { repository.sendText(any(), any(), any(), any(), any()) }
    }

    @Test
    fun reply_isSentWithTheMessageId_andThenCleared() {
        val vm = viewModel(listOf(message(5, "a")))
        vm.startReply(vm.state.value.messages.first())
        coEvery { repository.sendText(who, any(), "ok", 5L, null) } answers { AppResult.Success(message(11, secondArg(), mine = true)) }
        vm.sendText("ok")
        coVerify { repository.sendText(who, any(), "ok", 5L, null) }
        assertNull(vm.state.value.replyingTo)
    }

    @Test
    fun star_flipsAtOnce_andServerAnswerWins() {
        val vm = viewModel(listOf(message(3, "a")))
        coEvery { repository.toggleStar(who, 3L) } returns AppResult.Success(message(3, "a", starred = true))
        vm.toggleStar(vm.state.value.messages.first())
        assertTrue(vm.state.value.messages.first().starred)
        assertEquals(1, vm.state.value.starred.size)
    }

    @Test
    fun star_failure_putsItBack() {
        val vm = viewModel(listOf(message(3, "a")))
        coEvery { repository.toggleStar(who, 3L) } returns AppResult.Failure(AppError.Network)
        vm.toggleStar(vm.state.value.messages.first())
        assertEquals(false, vm.state.value.messages.first().starred)
        assertNotNull(vm.state.value.banner)
    }

    @Test
    fun edit_changesTextAtOnce_andMarksEdited() {
        val own = message(4, "e", mine = true)
        val vm = viewModel(listOf(own))
        coEvery { repository.edit(who, 4L, "new") } returns AppResult.Success(own.copy(text = "new", edited = true))
        vm.startEdit(vm.state.value.messages.first())
        vm.sendText("new")
        val m = vm.state.value.messages.first()
        assertEquals("new", m.text)
        assertTrue(m.edited)
        assertNull(vm.state.value.editing)
    }

    @Test
    fun deleteForMe_removesIt_andStaysGoneAfterRefresh() {
        val vm = viewModel(listOf(message(1, "a"), message(2, "b")))
        coEvery { repository.delete(who, 1L, false) } returns AppResult.Success(Unit)
        vm.delete(listOf(vm.state.value.messages.first()), forEveryone = false)
        assertEquals(listOf("b"), vm.state.value.messages.map { it.clientId })
        coEvery { repository.recent(who) } returns AppResult.Success(listOf(message(1, "a"), message(2, "b")))
        vm.poll(full = true)
        assertEquals(listOf("b"), vm.state.value.messages.map { it.clientId })
    }

    @Test
    fun deleteForEveryone_keepsTheRow_asDeleted() {
        val own = message(6, "m", mine = true)
        val vm = viewModel(listOf(own))
        coEvery { repository.delete(who, 6L, true) } returns AppResult.Success(Unit)
        vm.delete(listOf(own), forEveryone = true)
        val m = vm.state.value.messages.single()
        assertTrue(m.deleted)
        assertNull(m.text)
    }

    @Test
    fun pin_flipsAtOnce() {
        val vm = viewModel(listOf(message(3, "a")))
        coEvery { repository.pin(who, 3L, true) } returns AppResult.Success(message(3, "a").copy(pinned = true))
        vm.togglePin(vm.state.value.messages.first())
        assertEquals(1, vm.state.value.pinned.size)
    }

    @Test
    fun selectMode_togglesAndClears() {
        val vm = viewModel(listOf(message(1, "a"), message(2, "b")))
        vm.toggleSelect(vm.state.value.messages[0])
        vm.toggleSelect(vm.state.value.messages[1])
        assertEquals(2, vm.state.value.selectedMessages.size)
        vm.toggleSelect(vm.state.value.messages[0])
        assertEquals(setOf("b"), vm.state.value.selected)
        vm.clearSelection()
        assertTrue(vm.state.value.selected.isEmpty())
    }

    @Test
    fun poll_needsAQuestionAndTwoOptions() {
        val vm = viewModel()
        vm.sendPoll("Dinner?", listOf("Pizza", " "))
        coVerify(exactly = 0) { repository.sendPoll(any(), any(), any(), any(), any()) }
    }
}

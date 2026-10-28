package com.twinhearts.app.feature.music

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.music.data.MusicRepository
import com.twinhearts.app.feature.music.domain.model.LibraryScope
import com.twinhearts.app.feature.music.domain.model.MusicIdentity
import com.twinhearts.app.feature.music.domain.model.MusicLibrary
import com.twinhearts.app.feature.music.domain.model.PlaylistsResult
import com.twinhearts.app.feature.music.player.MusicPlayerController
import com.twinhearts.app.feature.music.player.PlayerState
import com.twinhearts.app.feature.music.ui.MusicPage
import com.twinhearts.app.feature.music.ui.MusicTab
import com.twinhearts.app.feature.music.ui.MusicViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MusicViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val who = MusicIdentity("c1", "user1", "Me", "Partner")
    private val repository = mockk<MusicRepository>(relaxed = true)
    private val controller = mockk<MusicPlayerController>(relaxed = true)

    private fun library(vararg songs: com.twinhearts.app.feature.music.domain.model.Song) =
        MusicLibrary(songs.toList(), PlaylistsResult(true, emptyList()))

    private fun viewModel(): MusicViewModel {
        coEvery { repository.identity() } returns who
        every { controller.state } returns MutableStateFlow(PlayerState())
        every { controller.played } returns MutableSharedFlow()
        every { controller.failed } returns MutableSharedFlow()
        return MusicViewModel(repository, controller)
    }

    @Test
    fun emptyLibrary_showsEmptyState() = runTest {
        coEvery { repository.library(who) } returns AppResult.Success(library())
        val vm = viewModel()
        assertEquals(UiState.Empty, vm.state.value.library)
    }

    @Test
    fun songs_showAsContent() = runTest {
        coEvery { repository.library(who) } returns AppResult.Success(library(song("a")))
        val vm = viewModel()
        assertTrue(vm.state.value.library is UiState.Content)
        assertFalse(vm.state.value.syncing)
    }

    @Test
    fun firstLoadFailure_showsError_butLaterFailureKeepsSongs() = runTest {
        coEvery { repository.library(who) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        assertEquals(UiState.Error(AppError.Network), vm.state.value.library)

        coEvery { repository.library(who) } returns AppResult.Success(library(song("a")))
        vm.refresh()
        coEvery { repository.library(who) } returns AppResult.Failure(AppError.Network)
        vm.refresh()
        assertTrue(vm.state.value.library is UiState.Content)
    }

    @Test
    fun like_isOptimistic_andRevertsOnFailure() = runTest {
        val s = song("a")
        coEvery { repository.library(who) } returns AppResult.Success(library(s))
        coEvery { repository.setLiked(who, "a", true) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.toggleLike(s)
        val songs = (vm.state.value.library as UiState.Content).data.songs
        assertFalse(songs.first().liked)
        coVerify { repository.setLiked(who, "a", true) }
    }

    @Test
    fun deleteSong_removesItFromListAndQueue() = runTest {
        val s = song("a")
        coEvery { repository.library(who) } returns AppResult.Success(library(s, song("b")))
        coEvery { repository.deleteSong(who, "a") } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.deleteSong(s)
        assertEquals(listOf("b"), (vm.state.value.library as UiState.Content).data.songs.map { it.id })
        verify { controller.remove("a") }
    }

    @Test
    fun back_closesPlayerThenPagesThenTab_thenLeaves() = runTest {
        coEvery { repository.library(who) } returns AppResult.Success(library(song("a")))
        val vm = viewModel()
        vm.setTab(MusicTab.Recordings)
        vm.open(MusicPage.Songs(LibraryScope.Mine))
        vm.openPlayer()
        assertTrue(vm.back())
        assertFalse(vm.state.value.playerOpen)
        assertTrue(vm.back())
        assertEquals(null, vm.state.value.page)
        assertTrue(vm.back())
        assertEquals(MusicTab.Music, vm.state.value.tab)
        assertFalse(vm.back())
    }
}

package com.twinhearts.app.feature.profile

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.profile.data.AvatarEncoder
import com.twinhearts.app.feature.profile.data.ProfileRepository
import com.twinhearts.app.feature.profile.domain.model.ConnectionState
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.ProfileData
import com.twinhearts.app.feature.profile.ui.ProfilePhase
import com.twinhearts.app.feature.profile.ui.ProfileScreenKind
import com.twinhearts.app.feature.profile.ui.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val mainDispatcher = ProfileMainDispatcherRule()

    private val repository = mockk<ProfileRepository>(relaxed = true)
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private val encoder = mockk<AvatarEncoder>()

    private fun data(paired: Boolean = true, role: String = "user1") = ProfileData(
        myName = "Vamsi", partnerName = "Likitha", role = role,
        connection = if (paired) ConnectionState.Connected else ConnectionState.NotConnected,
        anniversary = null, myBirthday = null, partnerBirthday = null, myBio = "", partnerBio = "",
        myLoveLanguage = null, partnerLoveLanguage = null, myGender = null, myAvatarUrl = null, partnerAvatarUrl = null,
        memoriesCount = 0, dreamsCount = 0, milestones = emptyList(), dates = emptyList(), partnerSeenAtMs = null,
    )

    private fun viewModel(d: ProfileData = data()): ProfileViewModel {
        coEvery { repository.load() } returns AppResult.Success(d)
        return ProfileViewModel(repository, sessionStore, encoder)
    }

    @Test
    fun load_success_showsContent() = runTest {
        val vm = viewModel()
        assertTrue(vm.state.value.phase is ProfilePhase.Content)
    }

    @Test
    fun load_failure_showsError_thenRetryRecovers() = runTest {
        coEvery { repository.load() } returns AppResult.Failure(AppError.Network)
        val vm = ProfileViewModel(repository, sessionStore, encoder)
        assertTrue(vm.state.value.phase is ProfilePhase.Failed)
        coEvery { repository.load() } returns AppResult.Success(data())
        vm.load()
        assertTrue(vm.state.value.phase is ProfilePhase.Content)
    }

    @Test
    fun backStack_returnsStepByStep_thenLeavesPage() = runTest {
        val vm = viewModel()
        vm.open(ProfileScreenKind.Settings)
        vm.open(ProfileScreenKind.Love)
        assertEquals(ProfileScreenKind.Love, vm.state.value.screen)
        assertTrue(vm.back())
        assertEquals(ProfileScreenKind.Settings, vm.state.value.screen)
        assertTrue(vm.back())
        assertFalse(vm.back()) // on Main: the page itself should close
    }

    @Test
    fun toggleLoveLanguage_sameTwice_clearsIt() = runTest {
        val vm = viewModel()
        coEvery { repository.saveLoveLanguages(any(), any()) } returns AppResult.Success(Unit)
        vm.toggleLoveLanguage(mine = true, language = LoveLanguage.QualityTime)
        assertEquals(LoveLanguage.QualityTime, (vm.state.value.phase as ProfilePhase.Content).data.myLoveLanguage)
        vm.toggleLoveLanguage(mine = true, language = LoveLanguage.QualityTime)
        assertEquals(null, (vm.state.value.phase as ProfilePhase.Content).data.myLoveLanguage)
    }

    @Test
    fun sendPartnerRequest_badPhone_isNotSent() = runTest {
        val vm = viewModel(data(paired = false))
        vm.sendPartnerRequest("12345")
        coVerify(exactly = 0) { repository.sendPartnerRequest(any()) }
        vm.sendPartnerRequest("9876543210")
        coVerify(exactly = 1) { repository.sendPartnerRequest("9876543210") }
    }

    @Test
    fun removePartner_asSecondUser_signsOut() = runTest {
        val vm = viewModel(data(role = "user2"))
        coEvery { repository.removePartner() } returns AppResult.Success(Unit)
        vm.removePartner()
        coVerify { sessionStore.clear() }
    }

    @Test
    fun removePartner_asFirstUser_staysSignedIn() = runTest {
        val vm = viewModel(data(role = "user1"))
        coEvery { repository.removePartner() } returns AppResult.Success(Unit)
        vm.removePartner()
        coVerify(exactly = 0) { sessionStore.clear() }
    }

    @Test
    fun avatarPicked_unreadablePhoto_doesNotUpload() = runTest {
        val vm = viewModel()
        coEvery { encoder.toJpeg(any()) } returns null
        vm.onAvatarPicked("content://x")
        coVerify(exactly = 0) { repository.uploadAvatar(any()) }
        assertFalse(vm.state.value.uploadingAvatar)
    }
}

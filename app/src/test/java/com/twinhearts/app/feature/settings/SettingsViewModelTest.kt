package com.twinhearts.app.feature.settings

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.settings.data.SettingsPrefs
import com.twinhearts.app.feature.settings.data.SettingsRepository
import com.twinhearts.app.feature.settings.domain.model.AppPrefs
import com.twinhearts.app.feature.settings.domain.model.ImportOutcome
import com.twinhearts.app.feature.settings.domain.model.PartnerOverview
import com.twinhearts.app.feature.settings.domain.model.PartnerStatus
import com.twinhearts.app.feature.settings.domain.model.ProfileEdit
import com.twinhearts.app.feature.settings.domain.model.ProfileGender
import com.twinhearts.app.feature.settings.domain.model.ProfileInfo
import com.twinhearts.app.feature.settings.domain.model.RemoveOutcome
import com.twinhearts.app.feature.settings.domain.model.SettingsFlag
import com.twinhearts.app.feature.settings.domain.model.SettingsPage
import com.twinhearts.app.feature.settings.domain.model.StorageSummary
import com.twinhearts.app.feature.settings.ui.PinResult
import com.twinhearts.app.feature.settings.ui.SettingsEvent
import com.twinhearts.app.feature.settings.ui.SettingsMessage
import com.twinhearts.app.feature.settings.ui.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SettingsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<SettingsRepository>(relaxed = true)
    private val prefs = mockk<SettingsPrefs>(relaxed = true)
    private val info = ProfileInfo("Vamsi", "Likitha", ProfileGender.Male, LocalDate.of(2025, 10, 12), null, null, null, null)

    private fun viewModel(
        page: String? = null,
        partner: PartnerStatus = PartnerStatus.NotConnected,
    ): SettingsViewModel {
        every { prefs.prefs } returns flowOf(AppPrefs())
        coEvery { repository.profile() } returns AppResult.Success(info)
        coEvery { repository.partner() } returns AppResult.Success(PartnerOverview(partner, emptyList()))
        coEvery { repository.storage() } returns AppResult.Success(StorageSummary(emptyList(), 0, 100))
        val saved = SavedStateHandle(if (page == null) emptyMap() else mapOf("settings_page" to page))
        return SettingsViewModel(repository, prefs, saved)
    }

    @Test
    fun startsByLoadingProfileAndPartner() {
        val vm = viewModel()
        assertEquals(UiState.Content(info), vm.state.value.profile)
        assertEquals("Vamsi", vm.state.value.form.myName)
        assertTrue(vm.state.value.partner.overview is UiState.Content)
    }

    @Test
    fun firstLoadFailure_isAnErrorNotAnEmptyScreen() {
        every { prefs.prefs } returns flowOf(AppPrefs())
        coEvery { repository.profile() } returns AppResult.Failure(AppError.Network)
        coEvery { repository.partner() } returns AppResult.Failure(AppError.Network)
        val vm = SettingsViewModel(repository, prefs, SavedStateHandle())
        assertEquals(UiState.Error(AppError.Network), vm.state.value.profile)
        assertEquals(UiState.Error(AppError.Network), vm.state.value.partner.overview)
    }

    @Test
    fun openPage_isKeptInSavedState_andBackClearsIt() {
        val vm = viewModel()
        vm.onOpenPage(SettingsPage.Privacy)
        assertEquals(SettingsPage.Privacy, vm.state.value.page)
        vm.onBack()
        assertEquals(null, vm.state.value.page)
    }

    @Test
    fun pageIsRestoredAfterTheAppWasKilled() {
        val vm = viewModel(page = "Notifications")
        assertEquals(SettingsPage.Notifications, vm.state.value.page)
    }

    @Test
    fun openingStorage_loadsIt() {
        val vm = viewModel()
        vm.onOpenPage(SettingsPage.Storage)
        coVerify { repository.storage() }
        assertTrue(vm.state.value.storage is UiState.Content)
    }

    @Test
    fun saveProfile_sendsNormalizedNames_andShowsSavedMessage() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        val sent = slot<ProfileEdit>()
        coEvery { repository.saveProfile(capture(sent)) } returns AppResult.Success(Unit)
        vm.onMyName("   ")
        vm.onPartnerName("")
        vm.events.test {
            vm.onSaveProfile()
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_saved)), awaitItem())
        }
        assertEquals("You", sent.captured.myName)
        assertEquals("Partner", sent.captured.partnerName)
        assertEquals("You", vm.state.value.form.myName)
    }

    @Test
    fun weakPin_isRejectedBeforeAnyRequest() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        vm.onCurrentPin("2468")
        vm.onNewPin("1111")
        vm.events.test {
            vm.onUpdatePin()
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_pin_repeated)), awaitItem())
        }
        coVerify(exactly = 0) { repository.changePin(any(), any()) }
    }

    @Test
    fun pinFields_keepOnlyFourDigits() {
        val vm = viewModel()
        vm.onNewPin("12a3456")
        assertEquals("1234", vm.state.value.pin.new)
    }

    @Test
    fun missingCurrentPin_isAsked() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        vm.onNewPin("1357")
        vm.events.test {
            vm.onUpdatePin()
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_pin_enter_current)), awaitItem())
        }
    }

    @Test
    fun pinUpdate_success_clearsFields() {
        val vm = viewModel()
        coEvery { repository.changePin("2468", "1357") } returns AppResult.Success(Unit)
        vm.onCurrentPin("2468")
        vm.onNewPin("1357")
        vm.onUpdatePin()
        assertEquals(PinResult.Success, vm.state.value.pin.result)
        assertEquals("", vm.state.value.pin.current)
        assertEquals("", vm.state.value.pin.new)
    }

    @Test
    fun pinUpdate_failure_isShownUnderTheButton() {
        val vm = viewModel()
        val error = AppError.Server(401, "Current PIN is wrong")
        coEvery { repository.changePin(any(), any()) } returns AppResult.Failure(error)
        vm.onCurrentPin("0000")
        vm.onNewPin("1357")
        vm.onUpdatePin()
        assertEquals(PinResult.Failure(error), vm.state.value.pin.result)
        assertEquals("1357", vm.state.value.pin.new) // fields stay so the person can fix them
    }

    @Test
    fun phoneField_keepsOnlyTenDigits() {
        val vm = viewModel()
        vm.onPhone("98-76 54321098765")
        assertEquals("9876543210", vm.state.value.partner.phone)
    }

    @Test
    fun emptyPhone_isRejectedBeforeServer() {
        val vm = viewModel()
        vm.onSendRequest()
        assertEquals(SettingsMessage.Res(R.string.settings_err_enter_phone), vm.state.value.partner.problem)
        coVerify(exactly = 0) { repository.sendRequest(any()) }
    }

    @Test
    fun sendRequest_success_clearsPhone() {
        val vm = viewModel()
        coEvery { repository.sendRequest("9876543210") } returns AppResult.Success(Unit)
        vm.onPhone("9876543210")
        vm.onSendRequest()
        assertEquals("", vm.state.value.partner.phone)
        coVerify { repository.sendRequest("9876543210") }
    }

    @Test
    fun removePartner_asUser2_signsOut() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        coEvery { repository.removePartner() } returns AppResult.Success(RemoveOutcome.MustSignOut)
        vm.events.test {
            vm.onRemovePartner()
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_partner_removed)), awaitItem())
            assertEquals(SettingsEvent.SignedOut, awaitItem())
        }
        coVerify { repository.signOut() }
    }

    @Test
    fun removePartner_asUser1_staysSignedIn() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        coEvery { repository.removePartner() } returns AppResult.Success(RemoveOutcome.StaySignedIn)
        vm.events.test {
            vm.onRemovePartner()
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_partner_removed)), awaitItem())
            expectNoEvents()
        }
        coVerify(exactly = 0) { repository.signOut() }
    }

    @Test
    fun logOut_clearsSession_andNavigatesAway() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onLogOut()
            assertEquals(SettingsEvent.SignedOut, awaitItem())
        }
        coVerify { repository.signOut() }
    }

    @Test
    fun switch_isSavedOnThePhone_andConfirmed() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onFlag(SettingsFlag.ReduceMotion, true)
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_enabled)), awaitItem())
            vm.onFlag(SettingsFlag.ReduceMotion, false)
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_disabled)), awaitItem())
        }
        coVerify { prefs.setFlag(SettingsFlag.ReduceMotion, true) }
    }

    @Test
    fun invalidBackupFile_saysSo() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        val uri = mockk<Uri>()
        coEvery { repository.importBackup(uri) } returns AppResult.Success(ImportOutcome.InvalidFile)
        vm.events.test {
            vm.onImportBackup(uri)
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_backup_invalid)), awaitItem())
        }
    }

    @Test
    fun exportPhotos_withNoPhotos_saysSo() = kotlinx.coroutines.test.runTest {
        val vm = viewModel()
        val folder = mockk<Uri>()
        coEvery { repository.exportPhotos(folder) } returns AppResult.Success(0)
        vm.events.test {
            vm.onExportPhotos(folder)
            assertEquals(SettingsEvent.Show(SettingsMessage.Res(R.string.settings_no_photos)), awaitItem())
        }
        assertEquals(false, vm.state.value.storageBusy)
    }
}

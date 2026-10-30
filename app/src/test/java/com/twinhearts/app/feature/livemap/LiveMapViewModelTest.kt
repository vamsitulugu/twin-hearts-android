package com.twinhearts.app.feature.livemap

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.livemap.data.LiveMapPrefs
import com.twinhearts.app.feature.livemap.data.LiveMapRepository
import com.twinhearts.app.feature.livemap.data.nav.NavEngine
import com.twinhearts.app.feature.livemap.domain.nav.NavSession
import kotlinx.coroutines.flow.MutableStateFlow
import com.twinhearts.app.feature.livemap.data.LiveMapSettings
import com.twinhearts.app.feature.livemap.data.LocationSource
import com.twinhearts.app.feature.livemap.data.share.ShareLauncher
import com.twinhearts.app.feature.livemap.domain.model.AlertKey
import com.twinhearts.app.feature.livemap.domain.model.AlertPrefs
import com.twinhearts.app.feature.livemap.domain.model.CouplePositions
import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import com.twinhearts.app.feature.livemap.domain.model.SharedMapState
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import com.twinhearts.app.feature.livemap.ui.LiveMapPhase
import com.twinhearts.app.feature.livemap.ui.LiveMapViewModel
import com.twinhearts.app.feature.livemap.ui.LmScreen
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LiveMapViewModelTest {
    @get:Rule val mainDispatcher = LiveMapMainDispatcherRule()

    private val repository = mockk<LiveMapRepository>(relaxed = true)
    private val sessionStore = mockk<SessionStore>()
    private val prefs = mockk<LiveMapPrefs>(relaxed = true)
    private val location = mockk<LocationSource>(relaxed = true)
    private val launcher = mockk<ShareLauncher>(relaxed = true)
    private val navEngine = mockk<NavEngine>(relaxed = true)

    private val settings = LiveMapSettings(
        sharing = null, invisible = false, approximate = false, pausedUntilMs = 0L, vehicle = Vehicle.Walking,
        mapStyle = MapStyleKind.Street, threeD = false, followPartner = false, showRouteLine = false,
        showPlaceLabels = true, showWeather = false, tellBattery = true, tellNetwork = true, tellGps = true,
    )
    private val shared = SharedMapState(emptyList(), emptyList(), emptyList(), AlertPrefs())

    private fun viewModel(): LiveMapViewModel {
        every { sessionStore.session } returns emptyFlow<Session?>()
        every { prefs.settings } returns flowOf(settings)
        every { location.updates(any()) } returns emptyFlow()
        every { navEngine.session } returns MutableStateFlow<NavSession?>(null)
        return LiveMapViewModel(repository, sessionStore, prefs, location, launcher, navEngine)
    }

    @Test
    fun load_success_becomesReady() = runTest {
        coEvery { repository.sharedState() } returns AppResult.Success(shared)
        coEvery { repository.positions() } returns AppResult.Success(CouplePositions(null, null))
        val vm = viewModel()
        assertEquals(LiveMapPhase.Ready, vm.state.value.phase)
        assertFalse(vm.state.value.offline)
    }

    @Test
    fun load_bothFail_showsError_thenRetryRecovers() = runTest {
        coEvery { repository.sharedState() } returns AppResult.Failure(AppError.Network)
        coEvery { repository.positions() } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        assertTrue(vm.state.value.phase is LiveMapPhase.Failed)
        coEvery { repository.sharedState() } returns AppResult.Success(shared)
        coEvery { repository.positions() } returns AppResult.Success(CouplePositions(null, null))
        vm.load()
        assertEquals(LiveMapPhase.Ready, vm.state.value.phase)
    }

    @Test
    fun backStack_returnsToPreviousScreen_thenLeavesPage() = runTest {
        coEvery { repository.sharedState() } returns AppResult.Success(shared)
        coEvery { repository.positions() } returns AppResult.Success(CouplePositions(null, null))
        val vm = viewModel()
        vm.open(LmScreen.More)
        vm.open(LmScreen.Privacy)
        assertEquals(LmScreen.Privacy, vm.state.value.screen)
        assertTrue(vm.back())
        assertEquals(LmScreen.More, vm.state.value.screen)
        assertTrue(vm.back())
        assertEquals(LmScreen.Map, vm.state.value.screen)
        assertFalse(vm.back()) // already on the Map: the page itself should close
    }

    @Test
    fun switchingTabs_alwaysBacksOutToMap() = runTest {
        coEvery { repository.sharedState() } returns AppResult.Success(shared)
        coEvery { repository.positions() } returns AppResult.Success(CouplePositions(null, null))
        val vm = viewModel()
        vm.switchTab(LmScreen.Track)
        vm.switchTab(LmScreen.Places)
        assertTrue(vm.back())
        assertEquals(LmScreen.Map, vm.state.value.screen)
    }

    @Test
    fun setAlert_failure_rollsBack() = runTest {
        coEvery { repository.sharedState() } returns AppResult.Success(shared)
        coEvery { repository.positions() } returns AppResult.Success(CouplePositions(null, null))
        coEvery { repository.saveAlertPrefs(any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.setAlert(AlertKey.Arrive, false)
        assertTrue(vm.state.value.shared.alerts.arrive) // rolled back
    }

    @Test
    fun setSharingOff_stopsServiceAndTellsServer() = runTest {
        coEvery { repository.sharedState() } returns AppResult.Success(shared)
        coEvery { repository.positions() } returns AppResult.Success(CouplePositions(null, null))
        val vm = viewModel()
        vm.setSharing(false)
        verify { launcher.stop() }
        coVerify { repository.stopSharing() }
    }
}

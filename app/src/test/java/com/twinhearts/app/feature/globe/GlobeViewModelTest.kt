package com.twinhearts.app.feature.globe

import app.cash.turbine.test
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.globe.data.GlobeIdentity
import com.twinhearts.app.feature.globe.data.GlobeLocationSource
import com.twinhearts.app.feature.globe.data.GlobeRepository
import com.twinhearts.app.feature.globe.data.WorldAtlasSource
import com.twinhearts.app.feature.globe.domain.model.GlobeFilter
import com.twinhearts.app.feature.globe.domain.model.GlobeViewMode
import com.twinhearts.app.feature.globe.ui.GlobeEvent
import com.twinhearts.app.feature.globe.ui.GlobeLoad
import com.twinhearts.app.feature.globe.ui.GlobePanel
import com.twinhearts.app.feature.globe.ui.GlobeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlobeViewModelTest {
    @get:Rule val mainDispatcher = GlobeMainDispatcherRule()

    private val repository = mockk<GlobeRepository>(relaxed = true)
    private val atlasSource = mockk<WorldAtlasSource>()
    private val location = mockk<GlobeLocationSource>(relaxed = true)

    private val paris = memory("1", "Paris", "France", 48.85, 2.35, "2025-03-01", cost = 5000.0)
    private val rome = memory("2", "Rome", "Italy", 41.9, 12.5, "2024-06-01", "🤩 Excited")

    private fun viewModel(memories: List<com.twinhearts.app.feature.globe.domain.model.Memory> = listOf(paris, rome)): GlobeViewModel {
        coEvery { repository.identity() } returns GlobeIdentity("c1", "user1", "Me", "Asha")
        coEvery { repository.memories() } returns AppResult.Success(memories)
        coEvery { atlasSource.atlas() } returns null
        return GlobeViewModel(repository, atlasSource, location)
    }

    @Test fun load_buildsGroupsAndSummary() {
        val vm = viewModel()
        val s = vm.state.value
        assertEquals(GlobeLoad.Ready, s.load)
        assertEquals(2, s.groups.size)
        assertEquals(2, s.summary.cities)
        assertEquals("Asha", s.partnerName)
        assertEquals(listOf("2025", "2024"), s.years)
    }

    @Test fun load_failure_isShownWithTheError() {
        coEvery { repository.identity() } returns null
        coEvery { repository.memories() } returns AppResult.Failure(AppError.Network)
        coEvery { atlasSource.atlas() } returns null
        val vm = GlobeViewModel(repository, atlasSource, location)
        assertEquals(GlobeLoad.Failed(AppError.Network), vm.state.value.load)
    }

    @Test fun filter_changesWhatIsShown_notWhatIsStored() {
        val vm = viewModel()
        vm.setFilter(GlobeFilter.Year("2024"))
        assertEquals(1, vm.state.value.visible.size)
        assertEquals(2, vm.state.value.memories.size)
        assertEquals(1, vm.state.value.groups.size)
    }

    @Test fun pinTap_flies_thenOpensCityPanelOnArrival_andClearsRequest() {
        val vm = viewModel()
        vm.onPinTap("Paris,France")
        val request = vm.state.value.camera!!
        assertEquals(48.85, request.lat, 0.0)
        assertTrue(vm.state.value.panels.isEmpty())
        vm.onCameraArrived(request.id)
        assertEquals(listOf<GlobePanel>(GlobePanel.City("Paris,France")), vm.state.value.panels)
        assertNull(vm.state.value.camera)
    }

    @Test fun staleArrival_doesNotOpenAPanel() {
        val vm = viewModel()
        vm.onPinTap("Paris,France")
        vm.onCameraArrived(999)
        assertTrue(vm.state.value.panels.isEmpty())
    }

    @Test fun back_closesTopmostFirst() {
        val vm = viewModel()
        vm.openDetail("1")
        vm.openStats()
        assertTrue(vm.back())
        assertEquals(false, vm.state.value.statsOpen)
        assertEquals(1, vm.state.value.panels.size)
        assertTrue(vm.back())
        assertEquals(false, vm.back())
    }

    @Test fun pin_whenNotPro_showsMessage_andOpensNoForm() = runTest {
        coEvery { repository.isPro() } returns AppResult.Success(false)
        val vm = viewModel()
        vm.events.test {
            vm.requestPin()
            assertEquals(GlobeEvent.Message(R.string.globe_toast_pro), awaitItem())
        }
        assertTrue(vm.state.value.panels.none { it is GlobePanel.Form })
    }

    @Test fun pin_whenPro_opensForm() {
        coEvery { repository.isPro() } returns AppResult.Success(true)
        val vm = viewModel()
        vm.requestPin()
        assertTrue(vm.state.value.panels.last() is GlobePanel.Form)
    }

    @Test fun editMemory_replacesDetailWithForm() {
        val vm = viewModel()
        vm.openDetail("1")
        vm.editMemory("1")
        val panels = vm.state.value.panels
        assertEquals(1, panels.size)
        assertEquals("1", (panels.single() as GlobePanel.Form).editingId)
    }

    @Test fun deleteMemory_success_closesSheetsAndReloads() {
        coEvery { repository.deleteMemory("1") } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.openDetail("1")
        vm.deleteMemory("1")
        assertTrue(vm.state.value.panels.isEmpty())
        coVerify(atLeast = 2) { repository.memories() }
    }

    @Test fun switchingToMap_requestsRoadsForShortLegsOnly() {
        val near = memory("3", "Paris", "France", 48.85, 2.35, "2025-01-01")
        val next = memory("4", "Lyon", "France", 45.76, 4.84, "2025-02-01")
        coEvery { repository.roadRoute(any(), any()) } returns AppResult.Success(null)
        val vm = viewModel(listOf(near, next))
        vm.setView(GlobeViewMode.Map)
        coVerify(exactly = 1) { repository.roadRoute(any(), any()) }
    }

    @Test fun searchCoordinates_areRecognisedWithoutANetworkCall() {
        val vm = viewModel()
        vm.onSearchQuery("12.97, 77.59")
        assertNotNull(vm.state.value.search.coordinates)
        coVerify(exactly = 0) { repository.searchPlaces(any()) }
    }
}

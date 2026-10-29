package com.twinhearts.app.feature.globe

import app.cash.turbine.test
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.globe.data.GlobeRepository
import com.twinhearts.app.feature.globe.data.SaveOutcome
import com.twinhearts.app.feature.globe.domain.model.PlaceHit
import com.twinhearts.app.feature.globe.domain.model.WeatherNow
import com.twinhearts.app.feature.globe.ui.MemoryFormEvent
import com.twinhearts.app.feature.globe.ui.MemoryFormViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryFormViewModelTest {
    @get:Rule val mainDispatcher = GlobeMainDispatcherRule()
    private val repository = mockk<GlobeRepository>(relaxed = true)
    private val hit = PlaceHit("Paris", "France", "Île-de-France", 48.85, 2.35, "Paris, France")

    @Test fun start_withSameToken_keepsWhatWasTyped() {
        val vm = MemoryFormViewModel(repository)
        vm.start(1, null, null)
        vm.edit { it.copy(tripName = "Honeymoon") }
        vm.start(1, null, null)
        assertEquals("Honeymoon", vm.state.value.draft.tripName)
        vm.start(2, null, null)
        assertEquals("", vm.state.value.draft.tripName)
    }

    @Test fun save_withoutPickedPlace_asksForOne() = runTest {
        val vm = MemoryFormViewModel(repository)
        vm.start(1, null, null)
        vm.events.test {
            vm.save()
            assertEquals(MemoryFormEvent.Message(R.string.globe_toast_need_location), awaitItem())
        }
        coVerify(exactly = 0) { repository.saveMemory(any(), any(), any()) }
    }

    @Test fun pickPlace_fillsPlaceAndWeather() {
        coEvery { repository.weatherAt(48.85, 2.35) } returns AppResult.Success(WeatherNow("Rainy", 14))
        val vm = MemoryFormViewModel(repository)
        vm.start(1, null, null)
        vm.pickPlace(hit)
        val d = vm.state.value.draft
        assertTrue(d.hasLocation)
        assertEquals("Paris, France", d.locationText)
        assertEquals("Rainy", d.weather)
        assertEquals("14", d.temperature)
    }

    @Test fun save_success_emitsSaved() = runTest {
        coEvery { repository.saveMemory(null, any(), any()) } returns AppResult.Success(SaveOutcome("m1", 0))
        coEvery { repository.weatherAt(any(), any()) } returns AppResult.Success(null)
        val vm = MemoryFormViewModel(repository)
        vm.start(1, null, null)
        vm.pickPlace(hit)
        vm.events.test {
            vm.save()
            assertEquals(MemoryFormEvent.Message(R.string.globe_toast_pinned), awaitItem())
            assertEquals(MemoryFormEvent.Saved, awaitItem())
        }
    }
}

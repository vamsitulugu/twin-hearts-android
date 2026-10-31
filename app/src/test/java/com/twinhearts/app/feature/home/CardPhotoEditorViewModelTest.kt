package com.twinhearts.app.feature.home

import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.home.data.CardPhotoProcessor
import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.data.LoadedPhoto
import com.twinhearts.app.feature.home.data.PhotoLoadResult
import com.twinhearts.app.feature.home.domain.CropMath
import com.twinhearts.app.feature.home.domain.model.CardPhotoSlot
import com.twinhearts.app.feature.home.ui.CardPhotoEditorViewModel
import com.twinhearts.app.feature.home.ui.HomeMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CardPhotoEditorViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<HomeRepository>()
    private val processor = mockk<CardPhotoProcessor>()
    private val photo = LoadedPhoto(1600, 900, Any())

    private fun viewModel(): CardPhotoEditorViewModel {
        coEvery { processor.load("content://photo") } returns PhotoLoadResult.Loaded(photo)
        return CardPhotoEditorViewModel(repository, processor)
    }

    @Test
    fun pickedPhoto_isKeptForTheCurrentSlot_centred() {
        val vm = viewModel()
        vm.open(CardPhotoSlot.Hug)
        vm.onPhotoPicked("content://photo")
        val pending = vm.state.value.pending[CardPhotoSlot.Hug]
        assertNotNull(pending)
        assertEquals(CropMath.initial(1600, 900), pending?.crop)
        assertFalse(vm.state.value.isBusy)
    }

    @Test
    fun eachSlotKeepsItsOwnPendingPhoto() {
        val vm = viewModel()
        vm.open(CardPhotoSlot.Hug)
        vm.onPhotoPicked("content://photo")
        vm.selectSlot(CardPhotoSlot.Touch)
        assertNull(vm.state.value.pending[CardPhotoSlot.Touch])
        vm.selectSlot(CardPhotoSlot.Hug)
        assertNotNull(vm.state.value.pending[CardPhotoSlot.Hug])
    }

    @Test
    fun notAnImage_showsAProblem_andKeepsNothing() {
        coEvery { processor.load("content://doc") } returns PhotoLoadResult.NotAnImage
        val vm = CardPhotoEditorViewModel(repository, processor)
        vm.onPhotoPicked("content://doc")
        assertEquals(HomeMessage.Problem(R.string.home_photo_not_image), vm.state.value.message)
        assertEquals(emptyMap<CardPhotoSlot, Any>(), vm.state.value.pending)
    }

    @Test
    fun tooLargePhoto_showsAProblem() {
        coEvery { processor.load("content://big") } returns PhotoLoadResult.TooLarge
        val vm = CardPhotoEditorViewModel(repository, processor)
        vm.onPhotoPicked("content://big")
        assertEquals(HomeMessage.Problem(R.string.home_photo_too_large), vm.state.value.message)
    }

    @Test
    fun cancelledPicker_doesNothing() {
        val vm = viewModel()
        vm.onPhotoPicked(null)
        assertNull(vm.state.value.message)
        coVerify(exactly = 0) { processor.load(any()) }
    }

    @Test
    fun save_success_clearsPending_andCountsTheChange() {
        coEvery { processor.render(any(), any()) } returns byteArrayOf(1, 2, 3)
        coEvery { repository.saveCardPhoto(CardPhotoSlot.Hug, any()) } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.onPhotoPicked("content://photo")
        vm.save()
        assertEquals(HomeMessage.Info(R.string.home_photo_saved), vm.state.value.message)
        assertEquals(1, vm.state.value.changeCount)
        assertNull(vm.state.value.pending[CardPhotoSlot.Hug])
        assertFalse(vm.state.value.isBusy)
    }

    @Test
    fun save_failure_keepsThePhoto_andShowsTheError() {
        coEvery { processor.render(any(), any()) } returns byteArrayOf(1)
        coEvery { repository.saveCardPhoto(any(), any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.onPhotoPicked("content://photo")
        vm.save()
        assertEquals(HomeMessage.Failure(AppError.Network), vm.state.value.message)
        assertNotNull(vm.state.value.pending[CardPhotoSlot.Hug])
        assertEquals(0, vm.state.value.changeCount)
    }

    @Test
    fun save_whenPhotoCannotBeCut_showsAProblem_andUploadsNothing() {
        coEvery { processor.render(any(), any()) } returns null
        val vm = viewModel()
        vm.onPhotoPicked("content://photo")
        vm.save()
        assertEquals(HomeMessage.Problem(R.string.home_photo_save_failed), vm.state.value.message)
        coVerify(exactly = 0) { repository.saveCardPhoto(any(), any()) }
    }

    @Test
    fun save_withoutAPickedPhoto_doesNothing() {
        val vm = viewModel()
        vm.save()
        coVerify(exactly = 0) { repository.saveCardPhoto(any(), any()) }
    }

    @Test
    fun useDefault_success_showsMessage_andCountsTheChange() {
        coEvery { repository.resetCardPhoto(CardPhotoSlot.MissYou) } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.open(CardPhotoSlot.MissYou)
        vm.useDefault()
        assertEquals(HomeMessage.Info(R.string.home_photo_back_to_default), vm.state.value.message)
        assertEquals(1, vm.state.value.changeCount)
    }

    @Test
    fun dragAndZoom_changeTheCrop() {
        val vm = viewModel()
        vm.onPhotoPicked("content://photo")
        vm.zoomTo(2f, 0.5f, 0.5f)
        vm.pan(dx = 50f, dy = 0f, stageWidthPx = 400f)
        val crop = vm.state.value.pending[CardPhotoSlot.Hug]?.crop
        assertEquals(2f, crop?.zoom ?: 0f, 0.01f)
        assertEquals(700f, crop?.centerX ?: 0f, 0.01f)
    }
}

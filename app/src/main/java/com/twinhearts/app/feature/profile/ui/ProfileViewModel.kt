package com.twinhearts.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.profile.data.AvatarEncoder
import com.twinhearts.app.feature.profile.data.ProfileRepository
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.MyProfileEdit
import com.twinhearts.app.feature.profile.domain.model.NewMilestone
import com.twinhearts.app.feature.profile.domain.model.NewSpecialDate
import com.twinhearts.app.feature.profile.domain.model.PartnerEdit
import com.twinhearts.app.feature.profile.domain.model.ProfileData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Brain of the Our Profile page: loads the couple's profile, keeps the small back stack of sub-screens
 * and runs every save. After each save it reloads, so the screen always shows what the server has.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val sessionStore: SessionStore,
    private val avatarEncoder: AvatarEncoder,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    /** First load, and the "Try again" button. */
    fun load() {
        _state.update { it.copy(phase = ProfilePhase.Loading) }
        viewModelScope.launch {
            when (val result = repository.load()) {
                is AppResult.Success -> _state.update { it.copy(phase = ProfilePhase.Content(result.data), todayEpochDay = today(), nowMs = System.currentTimeMillis()) }
                is AppResult.Failure -> _state.update { it.copy(phase = ProfilePhase.Failed(result.error)) }
            }
        }
    }

    /** Refreshes in the background: the page stays on screen, only the numbers change. */
    private suspend fun reloadQuietly() {
        val result = repository.load()
        if (result is AppResult.Success) {
            _state.update { it.copy(phase = ProfilePhase.Content(result.data), todayEpochDay = today(), nowMs = System.currentTimeMillis()) }
        }
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    // ── sub-screens ──────────────────────────────────────────────────────────────────────────────

    fun open(screen: ProfileScreenKind) {
        if (screen == ProfileScreenKind.Main) return
        _state.update { it.copy(stack = it.stack + screen) }
    }

    /** Goes back one sub-screen. Returns false when already on the main screen, so the page itself should close. */
    fun back(): Boolean {
        if (_state.value.stack.size <= 1) return false
        _state.update { it.copy(stack = it.stack.dropLast(1)) }
        return true
    }

    // ── saves ────────────────────────────────────────────────────────────────────────────────────

    fun saveMyProfile(edit: MyProfileEdit) =
        perform(R.string.profile_msg_profile_saved) { repository.saveMyProfile(edit) }

    fun savePartnerDetails(edit: PartnerEdit) =
        perform(R.string.profile_msg_saved) { repository.savePartnerDetails(edit) }

    /** Saves both bios, then goes back (same as the web "Save" on Our Story). */
    fun saveStory(myBio: String, partnerBio: String) =
        perform(R.string.profile_msg_saved, onDone = { back() }) { repository.saveStory(myBio, partnerBio) }

    /** Tapping the chosen language again clears it (same as the web). [mine] false = my partner's language. */
    fun toggleLoveLanguage(mine: Boolean, language: LoveLanguage) {
        val data = (state.value.phase as? ProfilePhase.Content)?.data ?: return
        val newMine = if (mine) language.takeIf { it != data.myLoveLanguage } else data.myLoveLanguage
        val newPartner = if (!mine) language.takeIf { it != data.partnerLoveLanguage } else data.partnerLoveLanguage
        updateData { it.copy(myLoveLanguage = newMine, partnerLoveLanguage = newPartner) }
        viewModelScope.launch {
            if (repository.saveLoveLanguages(newMine, newPartner) is AppResult.Failure) {
                reloadQuietly()
                emit(ProfileEvent.Message(R.string.profile_msg_could_not_save))
            }
        }
    }

    fun addDate(date: NewSpecialDate) = perform(R.string.profile_msg_date_added) { repository.addDate(date) }

    fun addMilestone(milestone: NewMilestone) =
        perform(R.string.profile_msg_milestone_saved) { repository.addMilestone(milestone) }

    fun deleteMilestone(id: Long) = perform(R.string.profile_msg_milestone_removed) { repository.deleteMilestone(id) }

    /** [uri] is the address of the photo the person picked. */
    fun onAvatarPicked(uri: String) {
        if (_state.value.uploadingAvatar) return
        _state.update { it.copy(uploadingAvatar = true) }
        viewModelScope.launch {
            val jpeg = avatarEncoder.toJpeg(uri)
            if (jpeg == null) {
                emit(ProfileEvent.Message(R.string.profile_msg_photo_unreadable))
            } else {
                when (val result = repository.uploadAvatar(jpeg)) {
                    is AppResult.Success -> {
                        reloadQuietly()
                        emit(ProfileEvent.Message(R.string.profile_msg_photo_saved))
                    }
                    is AppResult.Failure -> emit(errorEvent(result.error))
                }
            }
            _state.update { it.copy(uploadingAvatar = false) }
        }
    }

    /** The web form takes exactly 10 digits and the server finds the account by that number. */
    fun sendPartnerRequest(phone: String) {
        val digits = phone.trim()
        if (digits.length != PHONE_DIGITS || !digits.all { it.isDigit() }) {
            emit(ProfileEvent.Message(R.string.profile_msg_phone_invalid))
            return
        }
        perform(R.string.profile_msg_request_sent) { repository.sendPartnerRequest(digits) }
    }

    /** The screen has already asked twice ("Are you sure?"), like the web app. */
    fun removePartner() {
        val role = (state.value.phase as? ProfilePhase.Content)?.data?.role
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = repository.removePartner()) {
                is AppResult.Success -> {
                    emit(ProfileEvent.Message(R.string.profile_msg_partner_removed))
                    if (role == "user2") {
                        // The old app signs the second person out here, because their couple space ends.
                        sessionStore.clear()
                        emit(ProfileEvent.SignedOut)
                    } else {
                        reloadQuietly()
                    }
                }
                is AppResult.Failure -> emit(errorEvent(result.error))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun exportData() {
        perform(null) { repository.exportJson().also { if (it is AppResult.Success) emit(ProfileEvent.ExportReady(it.data)) } }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────

    /** Runs one save: busy flag on, call, reload and message on success, readable error on failure (R-46). */
    private fun perform(successMessage: Int?, onDone: () -> Unit = {}, call: suspend () -> AppResult<*>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = call()) {
                is AppResult.Success -> {
                    reloadQuietly()
                    if (successMessage != null) emit(ProfileEvent.Message(successMessage))
                    onDone()
                }
                is AppResult.Failure -> emit(errorEvent(result.error))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    private fun updateData(change: (ProfileData) -> ProfileData) {
        _state.update { s ->
            val content = s.phase as? ProfilePhase.Content ?: return@update s
            s.copy(phase = content.copy(data = change(content.data)))
        }
    }

    private fun errorEvent(error: AppError): ProfileEvent.Message = when (error) {
        AppError.Network -> ProfileEvent.Message(R.string.core_error_network)
        is AppError.Unauthorized -> ProfileEvent.Message(R.string.core_error_unauthorized)
        is AppError.Server ->
            if (error.message.isNullOrBlank()) ProfileEvent.Message(R.string.core_error_server)
            else ProfileEvent.Message(R.string.profile_msg_server_text, error.message)
        AppError.Unknown -> ProfileEvent.Message(R.string.core_error_unknown)
    }

    private fun emit(event: ProfileEvent) {
        _events.trySend(event)
    }

    private companion object {
        const val PHONE_DIGITS = 10
    }
}

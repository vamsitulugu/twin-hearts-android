package com.twinhearts.app.feature.settings.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.settings.data.SettingsPrefs
import com.twinhearts.app.feature.settings.data.SettingsRepository
import com.twinhearts.app.feature.settings.domain.PinProblem
import com.twinhearts.app.feature.settings.domain.model.ImportOutcome
import com.twinhearts.app.feature.settings.domain.model.ProfileEdit
import com.twinhearts.app.feature.settings.domain.model.ProfileGender
import com.twinhearts.app.feature.settings.domain.model.RemoveOutcome
import com.twinhearts.app.feature.settings.domain.model.SettingsFlag
import com.twinhearts.app.feature.settings.domain.model.SettingsPage
import com.twinhearts.app.feature.settings.domain.pinProblem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * Holds the Settings state. Flow: Screen -> this ViewModel -> [SettingsRepository] / [SettingsPrefs] (R-40).
 * The open sub-page is kept in [SavedStateHandle] so it survives the phone killing the app.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val prefsStore: SettingsPrefs,
    private val savedState: SavedStateHandle,
) : ViewModel(), SettingsActions {

    private val _state = MutableStateFlow(SettingsUiState(page = restorePage()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    /** The Profile form is refilled from the server only while the person has not typed anything. */
    private var formTouched = false

    /** Picture just uploaded, sent along with the next "Save changes" like the web does. */
    private var newAvatarUrl: String? = null

    init {
        viewModelScope.launch { prefsStore.prefs.collect { p -> _state.update { it.copy(prefs = p) } } }
        loadProfile()
        loadPartner(showLoading = true)
    }

    private fun restorePage(): SettingsPage? =
        savedState.get<String>(KEY_PAGE)?.let { name -> SettingsPage.entries.firstOrNull { it.name == name } }

    private fun say(message: SettingsMessage) {
        _events.trySend(SettingsEvent.Show(message))
    }

    private fun say(id: Int, vararg args: Any) {
        say(SettingsMessage.Res(id, args.toList()))
    }

    // ───────────── navigation inside Settings ─────────────

    override fun onQuery(query: String) = _state.update { it.copy(query = query) }

    override fun onOpenPage(page: SettingsPage) {
        savedState[KEY_PAGE] = page.name
        _state.update { it.copy(page = page, query = "") }
        when (page) {
            SettingsPage.Storage -> loadStorage()
            SettingsPage.Partner -> loadPartner(showLoading = false)
            else -> Unit
        }
    }

    override fun onBack() {
        savedState[KEY_PAGE] = null
        _state.update { it.copy(page = null) }
    }

    /** The "Sync" button: read everything again. */
    override fun onSync() {
        loadProfile()
        loadPartner(showLoading = false)
        if (_state.value.page == SettingsPage.Storage) loadStorage()
        say(R.string.settings_synced)
    }

    // ───────────── profile ─────────────

    override fun onRetryProfile() {
        _state.update { it.copy(profile = UiState.Loading) }
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = repository.profile()) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        profile = UiState.Content(result.data),
                        form = if (formTouched) it.form else ProfileForm.from(result.data),
                    )
                }
                is AppResult.Failure -> _state.update {
                    // A refresh that fails must not wipe a header that is already showing.
                    if (it.profile is UiState.Content) it else it.copy(profile = UiState.Error(result.error))
                }
            }
        }
    }

    private fun editForm(change: (ProfileForm) -> ProfileForm) {
        formTouched = true
        _state.update { it.copy(form = change(it.form)) }
    }

    override fun onMyName(text: String) = editForm { it.copy(myName = text) }
    override fun onPartnerName(text: String) = editForm { it.copy(partnerName = text) }
    override fun onGender(gender: ProfileGender) = editForm { it.copy(gender = gender) }
    override fun onAnniversary(date: LocalDate?) = editForm { it.copy(anniversary = date) }
    override fun onMyBirthday(date: LocalDate?) = editForm { it.copy(myBirthday = date) }
    override fun onPartnerBirthday(date: LocalDate?) = editForm { it.copy(partnerBirthday = date) }

    override fun onSaveProfile() {
        val current = _state.value
        if (current.savingProfile) return
        val form = current.form
        val edit = ProfileEdit(
            myName = form.myName,
            partnerName = form.partnerName,
            gender = form.gender,
            anniversary = form.anniversary,
            myBirthday = form.myBirthday,
            partnerBirthday = form.partnerBirthday,
            avatarUrl = newAvatarUrl ?: (current.profile as? UiState.Content)?.data?.myAvatarUrl,
        ).normalized()
        _state.update { it.copy(savingProfile = true) }
        viewModelScope.launch {
            when (val result = repository.saveProfile(edit)) {
                is AppResult.Success -> {
                    formTouched = false
                    _state.update { s ->
                        val old = (s.profile as? UiState.Content)?.data
                        val updated = old?.copy(
                            myName = edit.myName,
                            partnerName = edit.partnerName,
                            gender = edit.gender,
                            anniversary = edit.anniversary,
                            myBirthday = edit.myBirthday,
                            partnerBirthday = edit.partnerBirthday,
                        )
                        s.copy(
                            savingProfile = false,
                            profile = if (updated != null) UiState.Content(updated) else s.profile,
                            form = ProfileForm(
                                edit.myName, edit.partnerName, edit.gender,
                                edit.anniversary, edit.myBirthday, edit.partnerBirthday,
                            ),
                        )
                    }
                    say(R.string.settings_saved)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(savingProfile = false) }
                    say(SettingsMessage.Failure(result.error))
                }
            }
        }
    }

    override fun onAvatarPicked(uri: Uri) {
        if (_state.value.uploadingAvatar) return
        _state.update { it.copy(uploadingAvatar = true) }
        viewModelScope.launch {
            when (val result = repository.changeAvatar(uri)) {
                is AppResult.Success -> {
                    newAvatarUrl = result.data
                    _state.update { s ->
                        val old = (s.profile as? UiState.Content)?.data
                        s.copy(
                            uploadingAvatar = false,
                            profile = if (old != null) UiState.Content(old.copy(myAvatarUrl = result.data)) else s.profile,
                        )
                    }
                    say(R.string.settings_photo_updated)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(uploadingAvatar = false) }
                    say(SettingsMessage.Failure(result.error))
                }
            }
        }
    }

    // ───────────── partner connection ─────────────

    private fun loadPartner(showLoading: Boolean) {
        if (showLoading) _state.update { it.copy(partner = it.partner.copy(overview = UiState.Loading)) }
        viewModelScope.launch {
            when (val result = repository.partner()) {
                is AppResult.Success -> _state.update {
                    it.copy(partner = it.partner.copy(overview = UiState.Content(result.data)))
                }
                is AppResult.Failure -> _state.update {
                    // The 5-second refresh must not replace a page that is already showing with an error.
                    if (it.partner.overview is UiState.Content) it
                    else it.copy(partner = it.partner.copy(overview = UiState.Error(result.error)))
                }
            }
        }
    }

    override fun onRetryPartner() = loadPartner(showLoading = true)

    /** Called every few seconds while the Partner page is on screen (R-66: the web polls too). */
    fun refreshPartner() = loadPartner(showLoading = false)

    override fun onPhone(text: String) {
        val digits = text.filter { it.isDigit() }.take(PHONE_DIGITS)
        _state.update { it.copy(partner = it.partner.copy(phone = digits, problem = null)) }
    }

    override fun onSendRequest() {
        val partner = _state.value.partner
        if (partner.working) return
        val phone = partner.phone.trim()
        if (phone.isEmpty()) {
            _state.update { it.copy(partner = it.partner.copy(problem = SettingsMessage.Res(R.string.settings_err_enter_phone))) }
            return
        }
        _state.update { it.copy(partner = it.partner.copy(working = true, problem = null)) }
        viewModelScope.launch {
            when (val result = repository.sendRequest(phone)) {
                is AppResult.Success -> {
                    _state.update { it.copy(partner = it.partner.copy(working = false, phone = "")) }
                    say(R.string.settings_invite_sent)
                    loadPartner(showLoading = false)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(partner = it.partner.copy(working = false, problem = SettingsMessage.Failure(result.error)))
                }
            }
        }
    }

    private fun partnerAction(call: suspend () -> AppResult<Unit>, doneMessage: Int?) {
        if (_state.value.partner.working) return
        _state.update { it.copy(partner = it.partner.copy(working = true)) }
        viewModelScope.launch {
            val result = call()
            _state.update { it.copy(partner = it.partner.copy(working = false)) }
            when (result) {
                is AppResult.Success -> {
                    doneMessage?.let { say(it) }
                    loadPartner(showLoading = false)
                }
                is AppResult.Failure -> say(SettingsMessage.Failure(result.error))
            }
        }
    }

    override fun onCancelRequest(requestId: String) =
        partnerAction({ repository.cancelRequest(requestId) }, R.string.settings_request_cancelled)

    override fun onDecline(requestId: String) =
        partnerAction({ repository.decline(requestId) }, R.string.settings_invite_declined)

    override fun onAccept(requestId: String) {
        if (_state.value.partner.working) return
        _state.update { it.copy(partner = it.partner.copy(working = true)) }
        viewModelScope.launch {
            val result = repository.accept(requestId)
            _state.update { it.copy(partner = it.partner.copy(working = false)) }
            when (result) {
                is AppResult.Success -> {
                    say(R.string.settings_connected_with, result.data)
                    loadPartner(showLoading = false)
                    loadProfile()
                }
                is AppResult.Failure -> say(SettingsMessage.Failure(result.error))
            }
        }
    }

    /** The two "are you sure" questions are asked by the screen before this runs. */
    override fun onRemovePartner() {
        if (_state.value.partner.working) return
        _state.update { it.copy(partner = it.partner.copy(working = true)) }
        viewModelScope.launch {
            val result = repository.removePartner()
            _state.update { it.copy(partner = it.partner.copy(working = false)) }
            when (result) {
                is AppResult.Success -> {
                    say(R.string.settings_partner_removed)
                    if (result.data == RemoveOutcome.MustSignOut) {
                        repository.signOut()
                        _events.trySend(SettingsEvent.SignedOut)
                    } else {
                        loadPartner(showLoading = false)
                        loadProfile()
                    }
                }
                is AppResult.Failure -> say(SettingsMessage.Failure(result.error))
            }
        }
    }

    // ───────────── switches saved on this phone ─────────────

    override fun onFlag(flag: SettingsFlag, on: Boolean) {
        viewModelScope.launch { prefsStore.setFlag(flag, on) }
        say(if (on) R.string.settings_enabled else R.string.settings_disabled)
    }

    override fun onQuietStart(time: LocalTime) {
        val end = _state.value.prefs.quietEnd
        viewModelScope.launch { prefsStore.setQuietHours(time, end) }
    }

    override fun onQuietEnd(time: LocalTime) {
        val start = _state.value.prefs.quietStart
        viewModelScope.launch { prefsStore.setQuietHours(start, time) }
    }

    // ───────────── vault PIN ─────────────

    override fun onCurrentPin(text: String) =
        _state.update { it.copy(pin = it.pin.copy(current = text.filter(Char::isDigit).take(PIN_DIGITS), result = PinResult.None)) }

    override fun onNewPin(text: String) =
        _state.update { it.copy(pin = it.pin.copy(new = text.filter(Char::isDigit).take(PIN_DIGITS), result = PinResult.None)) }

    override fun onUpdatePin() {
        val pin = _state.value.pin
        if (pin.result == PinResult.Working) return
        pinProblem(pin.new)?.let { problem ->
            say(
                when (problem) {
                    PinProblem.NotFourDigits -> R.string.settings_pin_not_four
                    PinProblem.RepeatedDigits -> R.string.settings_pin_repeated
                    PinProblem.Sequence -> R.string.settings_pin_sequence
                },
            )
            return
        }
        if (pin.current.isEmpty()) {
            say(R.string.settings_pin_enter_current)
            return
        }
        _state.update { it.copy(pin = it.pin.copy(result = PinResult.Working)) }
        viewModelScope.launch {
            when (val result = repository.changePin(pin.current, pin.new)) {
                is AppResult.Success -> {
                    _state.update { it.copy(pin = PinUiState(result = PinResult.Success)) }
                    say(R.string.settings_pin_updated_toast)
                }
                is AppResult.Failure -> _state.update { it.copy(pin = it.pin.copy(result = PinResult.Failure(result.error))) }
            }
        }
    }

    // ───────────── storage & data ─────────────

    override fun onRetryStorage() {
        _state.update { it.copy(storage = UiState.Loading) }
        loadStorage()
    }

    private fun loadStorage() {
        viewModelScope.launch {
            when (val result = repository.storage()) {
                is AppResult.Success -> _state.update { it.copy(storage = UiState.Content(result.data)) }
                is AppResult.Failure -> _state.update {
                    if (it.storage is UiState.Content) it else it.copy(storage = UiState.Error(result.error))
                }
            }
        }
    }

    private fun busy(block: suspend () -> Unit) {
        if (_state.value.storageBusy) return
        _state.update { it.copy(storageBusy = true) }
        viewModelScope.launch {
            block()
            _state.update { it.copy(storageBusy = false) }
        }
    }

    override fun onClearCache() = busy {
        repository.clearCache()
        say(R.string.settings_cache_cleared)
        loadStorage()
    }

    override fun onExportBackup(target: Uri) = busy {
        when (val result = repository.exportBackup(target)) {
            is AppResult.Success -> {
                prefsStore.setLastBackup(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                say(R.string.settings_backup_exported)
            }
            is AppResult.Failure -> say(SettingsMessage.Failure(result.error))
        }
    }

    override fun onImportBackup(source: Uri) = busy {
        when (val result = repository.importBackup(source)) {
            is AppResult.Success -> when (result.data) {
                ImportOutcome.Restored -> {
                    say(R.string.settings_backup_restored)
                    loadStorage()
                    loadProfile()
                }
                ImportOutcome.InvalidFile -> say(R.string.settings_backup_invalid)
            }
            is AppResult.Failure -> say(SettingsMessage.Failure(result.error))
        }
    }

    override fun onExportPhotos(folder: Uri) = busy {
        when (val result = repository.exportPhotos(folder)) {
            is AppResult.Success ->
                if (result.data == 0) say(R.string.settings_no_photos) else say(R.string.settings_photos_saved, result.data)
            is AppResult.Failure -> say(SettingsMessage.Failure(result.error))
        }
    }

    // ───────────── log out ─────────────

    /** The question "Log out?" is asked by the screen first. The cloud data is not touched. */
    override fun onLogOut() {
        viewModelScope.launch {
            repository.signOut()
            _events.trySend(SettingsEvent.SignedOut)
        }
    }

    private companion object {
        const val KEY_PAGE = "settings_page"
        const val PHONE_DIGITS = 10
        const val PIN_DIGITS = 4
    }
}

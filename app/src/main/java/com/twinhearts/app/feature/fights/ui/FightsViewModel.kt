package com.twinhearts.app.feature.fights.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.TwinPromptHandoff
import com.twinhearts.app.feature.fights.data.FightsRepository
import com.twinhearts.app.feature.fights.domain.FightMath
import com.twinhearts.app.feature.fights.domain.model.ActivityFilter
import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightDraft
import com.twinhearts.app.feature.fights.domain.model.FightForm
import com.twinhearts.app.feature.fights.domain.model.FightIdentity
import com.twinhearts.app.feature.fights.domain.model.FightRange
import com.twinhearts.app.feature.fights.domain.model.FightStarter
import com.twinhearts.app.feature.fights.domain.model.FightStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** The three in-page screens that share the Fight Log page (same as the web's dashboard / log / activities). */
enum class FightView { Dashboard, Log, Activities }

/** The fights list in its own states (R-41). */
sealed interface FightsLoad {
    data object Loading : FightsLoad
    data class Ready(val fights: List<Fight>) : FightsLoad
    data class Error(val error: AppError) : FightsLoad
}

data class FightsUiState(
    val load: FightsLoad = FightsLoad.Loading,
    val syncing: Boolean = false,
    val view: FightView = FightView.Dashboard,
    val range: FightRange = FightRange.Days90,
    val showAll: Boolean = false,
    val form: FightForm = FightForm(),
    val step: Int = 1,
    val saving: Boolean = false,
    val titleMissing: Boolean = false,
    val activityFilter: ActivityFilter = ActivityFilter.All,
    val detail: Fight? = null,
    val confirmDelete: Boolean = false,
    val partnerName: String = "",
    val myRole: String = "",
)

/** One-off things the screen reacts to. */
sealed interface FightsEvent {
    data class Message(@StringRes val id: Int) : FightsEvent
    data object OpenTwin : FightsEvent
}

/**
 * Why this file exists: runs the whole Fight Log page: dashboard numbers, the three-step Log Fight
 * wizard (with a draft kept on this phone), the details sheet, and reconnect activities.
 */
@HiltViewModel
class FightsViewModel @Inject constructor(
    private val repository: FightsRepository,
    private val handoff: TwinPromptHandoff,
) : ViewModel() {

    private val _state = MutableStateFlow(FightsUiState())
    val state: StateFlow<FightsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FightsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<FightsEvent> = _events.asSharedFlow()

    private var draftChecked = false

    init {
        load()
    }

    /** First load and the Sync button. Keeps showing the old list while refreshing. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, load = if (it.load is FightsLoad.Ready) it.load else FightsLoad.Loading) }
            val who: FightIdentity? = repository.identity()
            _state.update { it.copy(partnerName = who?.partnerName.orEmpty(), myRole = who?.role.orEmpty()) }
            when (val r = repository.fights()) {
                is AppResult.Success -> _state.update { it.copy(load = FightsLoad.Ready(r.data), syncing = false) }
                is AppResult.Failure -> _state.update {
                    it.copy(
                        load = if (it.load is FightsLoad.Ready) it.load else FightsLoad.Error(r.error),
                        syncing = false,
                    )
                }
            }
        }
    }

    // ── Dashboard ──

    fun setRange(range: FightRange) = _state.update { it.copy(range = range) }

    fun toggleShowAll() = _state.update { it.copy(showAll = !it.showAll) }

    fun openActivities() = _state.update { it.copy(view = FightView.Activities) }

    fun setActivityFilter(filter: ActivityFilter) = _state.update { it.copy(activityFilter = filter) }

    /** Back inside the page. Returns true when it was handled here (so the screen must not leave the page). */
    fun handleBack(): Boolean {
        val s = _state.value
        return when (s.view) {
            FightView.Log -> {
                if (s.step > 1) goStep(s.step - 1) else _state.update { it.copy(view = FightView.Dashboard) }
                true
            }
            FightView.Activities -> {
                _state.update { it.copy(view = FightView.Dashboard) }
                true
            }
            FightView.Dashboard -> false
        }
    }

    // ── Log wizard ──

    fun openLog() {
        if (!draftChecked) {
            draftChecked = true
            repository.loadDraft()?.let { d ->
                _state.update { it.copy(form = d.form, step = d.step) }
                say(R.string.fights_msg_draft_restored)
            }
        }
        _state.update { it.copy(view = FightView.Log, titleMissing = false) }
    }

    fun onForm(change: (FightForm) -> FightForm) = _state.update { it.copy(form = change(it.form), titleMissing = false) }

    fun pickCategory(label: String) = onForm { it.copy(category = label) }
    fun pickIntensity(value: Int) = onForm { it.copy(intensity = value) }
    fun pickStarter(starter: FightStarter) = onForm { it.copy(started = starter) }
    fun pickStatus(status: FightStatus) = onForm { it.copy(status = status) }
    fun toggleFeeling(value: String) = onForm { it.copy(feelings = it.feelings.toggled(value)) }
    fun toggleApproach(key: String) = onForm { it.copy(approach = it.approach.toggled(key)) }

    fun goStep(n: Int) {
        if (n > 1 && _state.value.form.title.isBlank()) {
            _state.update { it.copy(step = 1, titleMissing = true) }
            say(R.string.fights_msg_title_needed)
            return
        }
        _state.update { it.copy(step = n.coerceIn(1, 3)) }
    }

    fun next() {
        if (_state.value.step < 3) goStep(_state.value.step + 1) else save()
    }

    fun saveDraft() {
        val s = _state.value
        say(if (repository.saveDraft(FightDraft(s.form, s.step))) R.string.fights_msg_draft_saved else R.string.fights_msg_draft_failed)
    }

    private fun save() {
        val form = _state.value.form
        if (form.title.isBlank()) {
            goStep(1)
            return
        }
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.add(form)) {
                is AppResult.Success -> {
                    repository.clearDraft()
                    _state.update {
                        it.copy(
                            load = FightsLoad.Ready(r.data), saving = false, form = FightForm(), step = 1,
                            view = FightView.Dashboard,
                        )
                    }
                    say(R.string.fights_msg_logged)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(saving = false) }
                    say(R.string.fights_msg_failed)
                }
            }
        }
    }

    // ── Details sheet ──

    fun openDetail(fight: Fight) = _state.update { it.copy(detail = fight) }

    fun closeDetail() = _state.update { it.copy(detail = null, confirmDelete = false) }

    fun resolveDetail() {
        val fight = _state.value.detail ?: return
        if (!fight.isOpen) return
        viewModelScope.launch {
            when (val r = repository.resolve(fight.id)) {
                is AppResult.Success -> {
                    _state.update { it.copy(load = FightsLoad.Ready(r.data), detail = null) }
                    say(R.string.fights_msg_resolved)
                }
                is AppResult.Failure -> say(R.string.fights_msg_failed)
            }
        }
    }

    /** "AI Help": asks Twin AI about this fight (same sentence as the web). */
    fun askAi() {
        val fight = _state.value.detail ?: return
        handoff.put(FightMath.aiPrompt(fight))
        _state.update { it.copy(detail = null) }
        _events.tryEmit(FightsEvent.OpenTwin)
    }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }

    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun confirmDelete() {
        val fight = _state.value.detail ?: return
        viewModelScope.launch {
            when (val r = repository.delete(fight.id)) {
                is AppResult.Success -> _state.update { it.copy(load = FightsLoad.Ready(r.data), detail = null, confirmDelete = false) }
                is AppResult.Failure -> {
                    _state.update { it.copy(confirmDelete = false) }
                    say(R.string.fights_msg_failed)
                }
            }
        }
    }

    fun today(): LocalDate = LocalDate.now()

    private fun say(@StringRes id: Int) {
        _events.tryEmit(FightsEvent.Message(id))
    }

    private fun List<String>.toggled(value: String): List<String> =
        if (value in this) this - value else this + value
}

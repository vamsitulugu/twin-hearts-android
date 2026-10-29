package com.twinhearts.app.feature.period.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.period.data.PeriodRepository
import com.twinhearts.app.feature.period.domain.model.NewPeriod
import com.twinhearts.app.feature.period.domain.model.PeriodAccess
import com.twinhearts.app.feature.period.domain.model.PeriodData
import com.twinhearts.app.feature.period.domain.model.Symptom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** One-off result shown once as a toast (R-42: not part of the long-lived state). */
sealed interface PeriodMessage {
    data object PeriodLogged : PeriodMessage
    data object SymptomsSaved : PeriodMessage
    data object GenderSaved : PeriodMessage

    /** The person tried to change something without access (web `Gender.guardPeriod`). */
    data class NoAccess(val access: PeriodAccess) : PeriodMessage
    data class Failed(val error: AppError) : PeriodMessage
}

data class PeriodUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed; later save errors come as a [PeriodMessage]. */
    val error: AppError? = null,
    val data: PeriodData? = null,
    /** Symptom keys ticked for today, not saved until "Save Today's Symptoms". */
    val todaySymptoms: Set<String> = emptySet(),
    val saving: Boolean = false,
    val message: PeriodMessage? = null,
)

/** Loads the shared period data, enforces who may edit, and saves logs and symptoms. */
@HiltViewModel
class PeriodViewModel @Inject constructor(
    private val repository: PeriodRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PeriodUiState())
    val state: StateFlow<PeriodUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.load()) {
                is AppResult.Success -> _state.update {
                    it.copy(loading = false, data = result.data, todaySymptoms = savedToday(result.data))
                }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun toggleSymptom(symptom: Symptom) {
        if (!canEdit()) return
        _state.update {
            val next = if (symptom.key in it.todaySymptoms) it.todaySymptoms - symptom.key else it.todaySymptoms + symptom.key
            it.copy(todaySymptoms = next)
        }
    }

    fun saveSymptoms() {
        if (!canEdit()) return
        val ticked = _state.value.todaySymptoms
        // Keep the web's chip order so the saved list is the same whichever device saves it.
        val ordered = Symptom.entries.map { it.key }.filter { it in ticked }
        save(PeriodMessage.SymptomsSaved) { repository.saveSymptoms(LocalDate.now(), ordered) }
    }

    fun logPeriod(period: NewPeriod) {
        if (!canEdit()) return
        save(PeriodMessage.PeriodLogged) { repository.logPeriod(period) }
    }

    fun deletePeriod(id: String) {
        if (!canEdit()) return
        save(null) { repository.deletePeriod(id) }
    }

    fun chooseGender(female: Boolean) = save(PeriodMessage.GenderSaved) { repository.chooseGender(female) }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** True when this person may change data; otherwise tells them why, like the web. */
    private fun canEdit(): Boolean {
        val access = _state.value.data?.access ?: return false
        if (access == PeriodAccess.Edit) return true
        _state.update { it.copy(message = PeriodMessage.NoAccess(access)) }
        return false
    }

    private fun savedToday(data: PeriodData): Set<String> =
        data.symptoms[LocalDate.now().toString()].orEmpty().toSet()

    private fun save(success: PeriodMessage?, call: suspend () -> AppResult<PeriodData>) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val result = call()) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, data = result.data, todaySymptoms = savedToday(result.data), message = success)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, message = PeriodMessage.Failed(result.error))
                }
            }
        }
    }
}

package com.twinhearts.app.feature.livemap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.livemap.data.LiveMapRepository
import com.twinhearts.app.feature.livemap.domain.GeoMath
import com.twinhearts.app.feature.livemap.domain.model.DaySummary
import com.twinhearts.app.feature.livemap.domain.model.RouteDay
import com.twinhearts.app.feature.livemap.domain.model.RouteSettings
import com.twinhearts.app.feature.livemap.domain.model.TimelineEvent
import com.twinhearts.app.feature.livemap.ui.map.CameraRequest
import com.twinhearts.app.feature.livemap.ui.map.MapPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TrackRange(val days: Int) { Today(1), Week(7), Month(30) }

sealed interface DayState {
    data object Loading : DayState
    data class Content(val day: RouteDay, val cumulativeKm: List<Double>) : DayState
    data object Empty : DayState
    data class Error(val error: AppError) : DayState
}

sealed interface RangeState {
    data object Idle : RangeState
    data object Loading : RangeState
    data class Content(val days: List<DaySummary>) : RangeState
    data object Empty : RangeState
    data class Error(val error: AppError) : RangeState
}

data class PlaybackState(
    val index: Int = 0,
    val playing: Boolean = false,
    val speed: Float = 1f,
)

data class TrackUiState(
    val ofPartner: Boolean = false,
    val range: TrackRange = TrackRange.Today,
    val dates: List<String> = emptyList(),
    val selectedDate: String = LocalDate.now().toString(),
    val day: DayState = DayState.Loading,
    val timeline: List<TimelineEvent> = emptyList(),
    val rangeState: RangeState = RangeState.Idle,
    val settings: RouteSettings? = null,
    val settingsOpen: Boolean = false,
    val playback: PlaybackState = PlaybackState(),
    val camera: CameraRequest? = null,
)

/** Route history: one day's route, timeline, stops, playback, Week/Month totals and the privacy switch. */
@HiltViewModel
class TrackViewModel @Inject constructor(
    private val repository: LiveMapRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TrackUiState())
    val state: StateFlow<TrackUiState> = _state.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var playJob: Job? = null
    private var cameraCounter = 0L

    init {
        loadSettings()
        loadDay()
    }

    /** Opens the screen for one person: false = me, true = partner. */
    fun showFor(partner: Boolean) {
        if (_state.value.ofPartner == partner && _state.value.range == TrackRange.Today) return
        stopPlayback()
        _state.update { it.copy(ofPartner = partner, range = TrackRange.Today, selectedDate = LocalDate.now().toString()) }
        loadDay()
    }

    fun setRange(range: TrackRange) {
        stopPlayback()
        _state.update { it.copy(range = range) }
        if (range == TrackRange.Today) loadDay() else loadRange(range)
    }

    fun selectDate(date: String) {
        stopPlayback()
        _state.update { it.copy(range = TrackRange.Today, selectedDate = date) }
        loadDay()
    }

    fun retry() = if (_state.value.range == TrackRange.Today) loadDay() else loadRange(_state.value.range)

    private fun loadDay() {
        val s = _state.value
        _state.update { it.copy(day = DayState.Loading, timeline = emptyList()) }
        viewModelScope.launch {
            val datesCall = async { repository.routeDates(s.ofPartner) }
            val dayCall = async { repository.routeDay(s.ofPartner, s.selectedDate) }
            val timelineCall = async { repository.timeline(s.ofPartner, s.selectedDate) }
            val dates = datesCall.await()
            val day = dayCall.await()
            val timeline = timelineCall.await()
            if (_state.value.ofPartner != s.ofPartner || _state.value.selectedDate != s.selectedDate) return@launch
            _state.update { st ->
                st.copy(
                    dates = (dates as? AppResult.Success)?.data ?: st.dates,
                    timeline = (timeline as? AppResult.Success)?.data ?: emptyList(),
                    day = when (day) {
                        is AppResult.Failure -> DayState.Error(day.error)
                        is AppResult.Success ->
                            if (day.data.points.isEmpty()) DayState.Empty else DayState.Content(day.data, cumulative(day.data))
                    },
                    playback = PlaybackState(),
                )
            }
            val content = _state.value.day as? DayState.Content
            if (content != null) {
                cameraFit(content.day.points.map { MapPoint(it.lat, it.lng) })
            }
        }
    }

    private fun cumulative(day: RouteDay): List<Double> {
        val out = ArrayList<Double>(day.points.size)
        var total = 0.0
        day.points.forEachIndexed { i, p ->
            if (i > 0) total += GeoMath.distanceM(day.points[i - 1].lat, day.points[i - 1].lng, p.lat, p.lng) / 1000.0
            out.add(total)
        }
        return out
    }

    /** Week / Month: totals plus one line per day that has data. */
    private fun loadRange(range: TrackRange) {
        _state.update { it.copy(rangeState = RangeState.Loading) }
        viewModelScope.launch {
            val ofPartner = _state.value.ofPartner
            val datesResult = repository.routeDates(ofPartner)
            if (datesResult is AppResult.Failure) {
                _state.update { it.copy(rangeState = RangeState.Error(datesResult.error)) }
                return@launch
            }
            val cutoff = LocalDate.now().minusDays((range.days - 1).toLong()).toString()
            val dates = (datesResult as AppResult.Success).data.filter { it >= cutoff }.sortedDescending()
            val summaries = dates.map { d -> async { repository.daySummary(ofPartner, d) } }.awaitAll()
                .mapNotNull { (it as? AppResult.Success)?.data }
            if (_state.value.range != range) return@launch
            _state.update { it.copy(rangeState = if (summaries.isEmpty()) RangeState.Empty else RangeState.Content(summaries)) }
        }
    }

    // ── playback ─────────────────────────────────────────────────────────────────────────────────

    fun play() {
        val content = _state.value.day as? DayState.Content ?: return
        val last = content.day.points.lastIndex
        if (last < 1 || playJob?.isActive == true) return
        if (_state.value.playback.index >= last) _state.update { it.copy(playback = it.playback.copy(index = 0)) }
        _state.update { it.copy(playback = it.playback.copy(playing = true)) }
        playJob = viewModelScope.launch {
            while (_state.value.playback.index < last) {
                delay((STEP_MS / _state.value.playback.speed).toLong())
                _state.update { it.copy(playback = it.playback.copy(index = minOf(it.playback.index + 1, last))) }
            }
            _state.update { it.copy(playback = it.playback.copy(playing = false)) }
        }
    }

    fun pause() {
        playJob?.cancel()
        _state.update { it.copy(playback = it.playback.copy(playing = false)) }
    }

    fun setSpeed(speed: Float) = _state.update { it.copy(playback = it.playback.copy(speed = speed)) }

    private fun stopPlayback() {
        playJob?.cancel()
        _state.update { it.copy(playback = PlaybackState(speed = it.playback.speed)) }
    }

    // ── route history privacy ────────────────────────────────────────────────────────────────────

    fun toggleSettingsPanel() = _state.update { it.copy(settingsOpen = !it.settingsOpen) }

    private fun loadSettings() {
        viewModelScope.launch {
            // Fail closed like the web app: when unknown, treat route history as off.
            val result = repository.routeSettings()
            _state.update { it.copy(settings = (result as? AppResult.Success)?.data ?: it.settings) }
        }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        val current = _state.value.settings
        val retention = if (enabled) (current?.retentionDays ?: DEFAULT_RETENTION) else current?.retentionDays
        saveSettings(enabled, retention, if (enabled) R.string.livemap_msg_history_on else R.string.livemap_msg_history_off)
    }

    /** [days] 7, 30 or null (forever). */
    fun setRetention(days: Int?) = saveSettings(true, days, R.string.livemap_msg_retention_saved)

    private fun saveSettings(enabled: Boolean, retention: Int?, message: Int) {
        viewModelScope.launch {
            when (val result = repository.saveRouteSettings(enabled, retention)) {
                is AppResult.Success -> {
                    _state.update { it.copy(settings = result.data) }
                    _messages.trySend(message)
                }
                is AppResult.Failure -> _messages.trySend(R.string.livemap_msg_could_not_save)
            }
        }
    }

    /** Deletes only the signed-in person's own recorded history. */
    fun clearMyHistory() {
        viewModelScope.launch {
            when (repository.clearMyRouteHistory()) {
                is AppResult.Success -> {
                    _messages.trySend(R.string.livemap_msg_history_cleared)
                    if (!_state.value.ofPartner) loadDay()
                }
                is AppResult.Failure -> _messages.trySend(R.string.livemap_msg_history_clear_failed)
            }
        }
    }

    /** GPX text of the shown day, or null when nothing is recorded. */
    fun gpx(who: String): String? {
        val day = (_state.value.day as? DayState.Content)?.day ?: return null
        return buildGpx(who, day)
    }

    private fun cameraFit(points: List<MapPoint>) =
        _state.update { it.copy(camera = CameraRequest(++cameraCounter, fit = points)) }

    private companion object {
        const val STEP_MS = 350f
        const val DEFAULT_RETENTION = 30
    }
}

/** Same GPX layout as the web "Export Route". */
internal fun buildGpx(who: String, day: RouteDay): String {
    fun esc(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    val points = day.points.joinToString("\n") { p ->
        "<trkpt lat=\"${p.lat}\" lon=\"${p.lng}\"><time>${java.time.Instant.ofEpochMilli(p.atMs)}</time></trkpt>"
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<gpx version=\"1.1\" creator=\"Twin Hearts\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n" +
        "<trk><name>${esc("$who ${day.date}")}</name><trkseg>\n$points\n</trkseg></trk></gpx>"
}

package com.twinhearts.app.feature.cinema3d.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.feature.cinema3d.data.CinemaAssets
import com.twinhearts.app.feature.cinema3d.data.CinemaFiles
import com.twinhearts.app.feature.cinema3d.domain.ViewPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Loading the three theatre files. There is no "empty" state: the files ship inside the app. */
sealed interface SpikeLoad {
    data object Loading : SpikeLoad
    data class Ready(val files: CinemaFiles) : SpikeLoad
    data object Failed : SpikeLoad
}

/** The test buttons on the spike screen. */
data class SpikeControls(
    val preset: ViewPreset = ViewPreset.Seat,
    val showLobby: Boolean = true,
    val highQuality: Boolean = true,
    /** Exposure in stops, -2 to +4. */
    val brightnessEv: Float = 0f,
)

@HiltViewModel
class Cinema3dSpikeViewModel @Inject constructor(
    private val assets: CinemaAssets,
) : ViewModel() {

    private val _load = MutableStateFlow<SpikeLoad>(SpikeLoad.Loading)
    val load: StateFlow<SpikeLoad> = _load.asStateFlow()

    private val _controls = MutableStateFlow(SpikeControls())
    val controls: StateFlow<SpikeControls> = _controls.asStateFlow()

    init {
        loadFiles()
    }

    fun retry() {
        loadFiles()
    }

    fun selectPreset(preset: ViewPreset) = _controls.update { it.copy(preset = preset) }

    fun toggleLobby() = _controls.update { it.copy(showLobby = !it.showLobby) }

    fun toggleQuality() = _controls.update { it.copy(highQuality = !it.highQuality) }

    fun setBrightness(ev: Float) = _controls.update { it.copy(brightnessEv = ev.coerceIn(MIN_EV, MAX_EV)) }

    private fun loadFiles() {
        _load.value = SpikeLoad.Loading
        viewModelScope.launch {
            _load.value = try {
                SpikeLoad.Ready(assets.load())
            } catch (e: Exception) {
                // Never shown raw to the user (R-46); the log has the detail for the developer.
                Log.w(TAG, "Could not read the cinema3d files", e)
                SpikeLoad.Failed
            }
        }
    }

    companion object {
        private const val TAG = "Cinema3dSpike"
        const val MIN_EV = -2f
        const val MAX_EV = 4f
    }
}

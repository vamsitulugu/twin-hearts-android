package com.twinhearts.app.feature.calls.ui

import androidx.lifecycle.ViewModel
import com.twinhearts.app.feature.calls.data.CallEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Why this file exists: gives the call screen the app-wide [CallEngine] (R-42: state lives outside composables). */
@HiltViewModel
class CallViewModel @Inject constructor(val engine: CallEngine) : ViewModel()

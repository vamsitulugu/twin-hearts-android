package com.twinhearts.app.core.common

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: lets one feature hand a question to Twin AI without the two features knowing
 * each other (R-83). Fight Log's "AI Help" puts the question here, opens the Twin page, and Twin AI
 * takes it once and asks it.
 */
@Singleton
class TwinPromptHandoff @Inject constructor() {
    @Volatile
    private var pending: String? = null

    fun put(prompt: String) {
        pending = prompt
    }

    /** Returns the waiting question and clears it, or null. */
    fun take(): String? {
        val prompt = pending
        pending = null
        return prompt
    }
}

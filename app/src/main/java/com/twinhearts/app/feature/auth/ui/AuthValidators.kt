package com.twinhearts.app.feature.auth.ui

import androidx.annotation.StringRes
import com.twinhearts.app.R

/** The same rules the server enforces (it checks again), so mistakes show instantly. */
internal object AuthValidators {
    const val MIN_PASSWORD_LENGTH = 6
    const val PIN_LENGTH = 4

    fun isPasswordTooShort(password: String): Boolean = password.length < MIN_PASSWORD_LENGTH

    /** Returns a message for a bad vault PIN, or null when it is fine. */
    @StringRes
    fun vaultPinProblem(pin: String): Int? = when {
        pin.length != PIN_LENGTH || !pin.all { it in '0'..'9' } -> R.string.auth_err_pin_digits
        pin.all { it == pin[0] } -> R.string.auth_err_pin_repeated
        "0123456789".contains(pin) || "9876543210".contains(pin) -> R.string.auth_err_pin_sequence
        else -> null
    }
}

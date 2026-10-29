package com.twinhearts.app.feature.vault.domain

import androidx.annotation.StringRes
import com.twinhearts.app.R

/** PIN rules. They mirror `routes/auth.js` (the server checks every PIN again) and the web `weakPinMessage`. */
object VaultLogic {

    const val PIN_LENGTH = 4

    /** The text resource that explains why [pin] is not allowed, or null when it is fine. */
    @StringRes
    fun weakPinMessage(pin: String): Int? = when {
        pin.length != PIN_LENGTH || !pin.all { it in '0'..'9' } -> R.string.vault_pin_four_digits
        pin.all { it == pin[0] } -> R.string.vault_pin_repeated
        "0123456789".contains(pin) || "9876543210".contains(pin) -> R.string.vault_pin_sequence
        else -> null
    }
}

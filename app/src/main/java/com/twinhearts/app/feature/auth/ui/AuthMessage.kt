package com.twinhearts.app.feature.auth.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText

/** The red (or green) message box at the top of an auth form. */
sealed interface AuthMessage {
    /** A check made on the phone, such as "Fill in all fields". */
    data class Resource(@StringRes val id: Int) : AuthMessage

    /** The server or network said no. Shows the server's own friendly text when it sent one. */
    data class Failure(val error: AppError) : AuthMessage

    /** Green message, e.g. "Password updated successfully." */
    data class Success(@StringRes val id: Int) : AuthMessage
}

@Composable
fun AuthMessage.asText(): String = when (this) {
    is AuthMessage.Resource -> stringResource(id)
    is AuthMessage.Success -> stringResource(id)
    is AuthMessage.Failure -> error.asText()
}

/** One-off things a screen must react to (navigate). Sent through a channel, never kept as state (R-42). */
sealed interface AuthEvent {
    data object SignedIn : AuthEvent
    data object PasswordUpdated : AuthEvent
}

package com.twinhearts.app.core.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.twinhearts.app.R

/** Small set of errors the UI understands (R-46). Repositories turn exceptions into one of these. */
sealed interface AppError {
    /** No internet, DNS failure or timeout (also what a sleeping Render server looks like). */
    data object Network : AppError

    /** HTTP 401. [message] is the server's own friendly text when it sent one. */
    data class Unauthorized(val message: String? = null) : AppError

    /** Any other HTTP error. [message] is the server's own friendly text when it sent one. */
    data class Server(val code: Int, val message: String? = null) : AppError

    data object Unknown : AppError
}

/** What the user reads. The server's `error` text is written for users, so it is shown as is. */
@Composable
fun AppError.asText(): String = when (this) {
    AppError.Network -> stringResource(R.string.core_error_network)
    is AppError.Unauthorized -> message ?: stringResource(R.string.core_error_unauthorized)
    is AppError.Server -> message ?: stringResource(R.string.core_error_server)
    AppError.Unknown -> stringResource(R.string.core_error_unknown)
}

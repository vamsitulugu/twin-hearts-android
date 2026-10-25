package com.twinhearts.app.core.common

/** Result of a repository call: either data or an [AppError]. Never throws to the UI. */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/** The four states every screen shows (R-41). */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val error: AppError) : UiState<Nothing>
}

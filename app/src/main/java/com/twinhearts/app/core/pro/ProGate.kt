package com.twinhearts.app.core.pro

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one door every Pro feature goes through:
 *
 *     proGate.require("Pin a Memory") { openForm() }
 *
 * - Pro (known and still running): [action] runs at once, so file pickers open instantly.
 * - Otherwise the server is asked once, fresh. Pro now → [action] runs. Not Pro → [paywall] emits the
 *   feature name and the app opens the Pro screen (TwinApp listens).
 * The server also refuses these features for free couples, so this is the friendly front, not the lock.
 */
@Singleton
class ProGate internal constructor(
    private val repository: ProRepository,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    coupleIds: Flow<String?>,
) {
    @Inject
    constructor(repository: ProRepository, sessionStore: SessionStore) : this(
        repository,
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        System::currentTimeMillis,
        sessionStore.session.map { it?.coupleId },
    )

    private val _status = MutableStateFlow(ProStatus.Free)
    val status: StateFlow<ProStatus> = _status.asStateFlow()

    private val _paywall = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        // Another couple signed in on this phone: never carry over the previous couple's Pro.
        scope.launch { coupleIds.distinctUntilChanged().collect { _status.value = ProStatus.Free } }
    }

    /** Emits the feature name whenever a free couple taps a Pro feature. */
    val paywall: SharedFlow<String> = _paywall.asSharedFlow()

    fun require(feature: String, action: () -> Unit) {
        if (_status.value.isActive(clock())) {
            action()
            return
        }
        scope.launch {
            when (val r = refresh()) {
                is AppResult.Success -> if (r.data.isActive(clock())) action() else _paywall.emit(feature)
                // Could not reach the server: the Pro screen explains it and offers a retry.
                is AppResult.Failure -> _paywall.emit(feature)
            }
        }
    }

    /** Asks the server and remembers the answer. */
    suspend fun refresh(): AppResult<ProStatus> {
        val r = repository.status()
        if (r is AppResult.Success) _status.value = r.data
        return r
    }

    /** Called after a verified payment so every screen sees Pro immediately. */
    fun onStatus(status: ProStatus) {
        _status.value = status
    }

}

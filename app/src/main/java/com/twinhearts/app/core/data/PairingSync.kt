package com.twinhearts.app.core.data

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.pairing.data.remote.PairingApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps this phone's saved session in step with what the server says about the couple.
 *
 * WHY THIS EXISTS: when two people connect, the person who ACCEPTS gets the new couple saved
 * straight from the accept answer, but the person who SENT the request is told nothing. Before,
 * the sender's phone only noticed if they happened to open the Pairing or Settings screen, so
 * one phone showed "Connected" and the other kept showing "Connect your partner". Now Home (and
 * the "partner accepted" push) call [sync], so the sender's phone catches up by itself.
 *
 * The server is the truth. A failed network call never changes the session.
 */
@Singleton
class PairingSync @Inject constructor(
    private val api: PairingApi,
    private val sessionStore: SessionStore,
) {
    private val lock = Mutex()

    /** Returns true when the saved session changed. */
    suspend fun sync(): Boolean = lock.withLock {
        val before = sessionStore.session.first() ?: return false
        val result = safeApiCall { api.status(before.userId) }
        val dto = (result as? AppResult.Success)?.data ?: return false

        val serverCouple = dto.coupleId?.takeIf { it.isNotBlank() }
        val serverRole = dto.role?.takeIf { it == "user1" || it == "user2" }

        if (dto.status == "connected") {
            val partner = dto.partnerName?.takeIf { it.isNotBlank() } ?: "Partner"
            sessionStore.update {
                it.copy(
                    paired = true,
                    partnerName = partner,
                    coupleId = serverCouple ?: it.coupleId,
                    role = serverRole ?: it.role,
                )
            }
        } else {
            // Not connected (never was, or my partner removed me). In the second case the server
            // has moved me to a fresh couple, so ALWAYS take its couple + role. Keeping the old
            // ones would make this phone keep reading and writing my ex-partner's shared data.
            sessionStore.update {
                it.copy(
                    paired = false,
                    partnerName = "Partner",
                    coupleId = serverCouple ?: it.coupleId,
                    role = serverRole ?: it.role,
                )
            }
        }
        val after = sessionStore.session.first()
        after != before
    }
}

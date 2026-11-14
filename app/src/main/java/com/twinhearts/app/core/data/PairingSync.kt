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

        when (dto.status) {
            "connected" -> {
                val partner = dto.partnerName?.takeIf { it.isNotBlank() } ?: "Partner"
                val coupleId = dto.coupleId?.takeIf { it.isNotBlank() }
                val role = dto.role?.takeIf { it == "user1" || it == "user2" }
                sessionStore.update {
                    it.copy(
                        paired = true,
                        partnerName = partner,
                        coupleId = coupleId ?: it.coupleId,
                        role = role ?: it.role,
                    )
                }
            }
            // Only an explicit "not connected / waiting / invitation" answer from the server
            // clears the flag (the partner removed the link). Errors returned earlier above.
            else -> if (before.paired) {
                sessionStore.update { it.copy(paired = false, partnerName = "Partner") }
            }
        }
        val after = sessionStore.session.first()
        after != before
    }
}

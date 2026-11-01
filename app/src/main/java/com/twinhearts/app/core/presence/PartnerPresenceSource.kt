package com.twinhearts.app.core.presence

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.chat.data.mapper.forRole
import com.twinhearts.app.feature.chat.data.mapper.parseServerTime
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.domain.presenceFrom
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this exists: the side menu shows my partner's Online / Away / Last seen (like the web
 * sidebar), but the side menu is not the Chat screen. This reads the same server rows the Chat
 * header uses ("GET api/chat/{coupleId}/presence") and turns them into a [PartnerPresence].
 */
@Singleton
class PartnerPresenceSource @Inject constructor(
    private val api: ChatApi,
    private val sessionStore: SessionStore,
) {
    /** The partner's presence now, or null when nobody is paired / the server could not be reached. */
    suspend fun fetch(now: Instant = Instant.now()): PartnerPresence? {
        val session = sessionStore.session.first() ?: return null
        if (!session.paired || session.coupleId.isBlank()) return null
        val partnerRole = if (session.role == "user1") "user2" else "user1"
        return when (val r = safeApiCall { api.presence(session.coupleId) }) {
            is AppResult.Success -> {
                val row = r.data.forRole(partnerRole)
                if (row == null) PartnerPresence(PresenceState.Offline, null)
                else presenceFrom(row.status, parseServerTime(row.lastSeen), now)
            }
            is AppResult.Failure -> null
        }
    }
}

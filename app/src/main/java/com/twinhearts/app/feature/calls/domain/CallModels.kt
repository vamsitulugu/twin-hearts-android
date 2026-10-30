package com.twinhearts.app.feature.calls.domain

import kotlinx.serialization.json.JsonObject

/** Why this file exists: the plain Kotlin shapes Calls uses (no Android, no network types). */

/** Who is calling: [myRole] and [partnerRole] are "user1" / "user2". */
data class CallIdentity(val coupleId: String, val myRole: String, val partnerName: String) {
    val partnerRole: String get() = if (myRole == "user1") "user2" else "user1"
}

data class IceServerInfo(val urls: List<String>, val username: String? = null, val credential: String? = null)

/** One row of the shared signal table. */
data class Signal(val id: Long, val payload: JsonObject)

enum class CallPhase { Idle, Calling, Incoming, Connecting, Connected, Ended }

/** Why a call stopped; the screen shows a short line for each. */
enum class EndReason { Ended, Declined, NoAnswer, Missed, Failed, PermissionDenied }

/** Where a "turn on video" request stands during a voice call (the web app's video-upgrade-* signals). */
enum class VideoUpgrade { None, Requested, Asked }

package com.twinhearts.app.feature.music.singtogether.data

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.core.network.unwrapState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Why this file exists: the message pipe between the two phones. Each phone keeps a short list of
 * messages under its own key (`ck_user1` or `ck_user2`) in the couple's shared state and reads the
 * partner's list by polling. Same format as the web app, so both apps understand each other.
 *
 * One channel serves one signed-in person; the engine makes a new one when the couple changes.
 */
class SingChannel(
    private val api: SingApi,
    private val coupleId: String,
    private val myRole: String,
    private val scope: CoroutineScope,
    private val myName: String = "",
) {
    private val myKey = "ck_$myRole"
    private val peerKey = "ck_" + if (myRole == "user1") "user2" else "user1"

    private val _messages = MutableSharedFlow<JsonObject>(extraBufferCapacity = 64)

    /** Every new message from the partner, oldest first. */
    val messages: SharedFlow<JsonObject> = _messages.asSharedFlow()

    private var outbox: List<JsonObject> = emptyList()
    private var lastPeerSeq = 0.0
    private var minTs = 0L
    private var pollJob: Job? = null
    private var seqCounter = 0
    private val saveLock = Mutex()

    /** Messages older than this time (partner's clock) are skipped: leftovers from an earlier room. */
    fun setMinTimestamp(ts: Long) {
        minTs = ts
    }

    /** Puts one message in my list. [type] is the web app's message name ("invite", "offer", …). */
    suspend fun send(type: String, build: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {}) {
        val now = System.currentTimeMillis()
        // The web uses Date.now() + Math.random() as the order number. Same idea: always rising.
        seqCounter = (seqCounter + 1) % SEQ_STEPS
        val seq = now.toDouble() + seqCounter.toDouble() / SEQ_STEPS
        val message = buildJsonObject {
            put("type", type)
            build()
            put("from", myRole)
            put("ts", now)
            put("seq", seq)
        }
        // A burst of ICE candidates must not push the offer out before the partner's next poll.
        outbox = (outbox + message).takeLast(OUTBOX_SIZE)
        // One save at a time, and each save sends the NEWEST list, so a slow earlier request can
        // never overwrite a later one and drop a message.
        saveLock.withLock {
            val current = outbox
            safeApiCall { api.saveState(SingSaveBody(coupleId, buildJsonObject { put(myKey, JsonArray(current)) }, myRole, myName)) }
        }
    }

    /** Reads the partner's list once and emits what is new. */
    suspend fun pollOnce() {
        val root = (safeApiCall { api.state(coupleId) } as? AppResult.Success)?.data ?: return
        val list = (unwrapState(root) as? JsonObject)?.get(peerKey) as? JsonArray ?: return
        val fresh = list.mapNotNull { it as? JsonObject }
            .filter { (it["seq"] as? JsonPrimitive)?.doubleOrNull?.let { s -> s > lastPeerSeq } == true }
            .sortedBy { (it["seq"] as JsonPrimitive).doubleOrNull ?: 0.0 }
        if (fresh.isEmpty()) return
        lastPeerSeq = (fresh.last()["seq"] as JsonPrimitive).doubleOrNull ?: lastPeerSeq
        fresh.filter { minTs == 0L || ((it["ts"] as? JsonPrimitive)?.longOrNull ?: 0L) >= minTs }
            .forEach { _messages.emit(it) }
    }

    /** Polls every [everyMs] until [stopPolling]. */
    fun startPolling(everyMs: Long) {
        stopPolling()
        pollJob = scope.launch {
            while (isActive) {
                pollOnce()
                delay(everyMs)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private companion object {
        const val OUTBOX_SIZE = 60
        const val SEQ_STEPS = 1000
    }
}

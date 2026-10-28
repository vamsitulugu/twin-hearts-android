package com.twinhearts.app.feature.twinai.data.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: listens to the same Supabase "broadcast" channel the web page uses
 * (`both_round:<coupleId>`), so a partner's submit or a finished result shows up at once instead
 * of after the next 3-second check. It is only a speed-up: the 3-second check stays as the fallback,
 * and this does nothing until [RealtimeConfig] is filled in.
 */
@Singleton
class BothRealtime @Inject constructor(client: OkHttpClient, private val json: Json) {

    // Idle sockets must not time out: Supabase is kept alive by the heartbeat below.
    private val http = client.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: WebSocket? = null
    private var job: Job? = null
    private var ref = 0

    /** Starts listening. [onEvent] gets the event name ("submitted", "locked", "result_ready", "failed", "session_deleted"). */
    fun connect(coupleId: String, onEvent: (String) -> Unit) {
        if (!RealtimeConfig.isConfigured) return
        close()
        job = scope.launch {
            while (isActive) {
                runSocket(coupleId, onEvent)
                delay(RETRY_MS) // dropped: try again while the tab is still on screen
            }
        }
    }

    fun close() {
        job?.cancel()
        job = null
        socket?.close(1000, null)
        socket = null
    }

    private suspend fun runSocket(coupleId: String, onEvent: (String) -> Unit) {
        val base = RealtimeConfig.SUPABASE_URL.trimEnd('/').replaceFirst("http", "ws")
        val url = "$base/realtime/v1/websocket?apikey=${RealtimeConfig.ANON_KEY}&vsn=1.0.0"
        val topic = "realtime:both_round:$coupleId"
        val closed = kotlinx.coroutines.CompletableDeferred<Unit>()
        val ws = http.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(
                        buildJsonObject {
                            put("topic", topic)
                            put("event", "phx_join")
                            put("ref", next())
                            put("payload", buildJsonObject {
                                put("config", buildJsonObject {
                                    put("broadcast", buildJsonObject { put("self", true) })
                                })
                            })
                        }.toString(),
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val message = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
                    if (message["event"]?.jsonPrimitive?.content != "broadcast") return
                    val inner: JsonObject = message["payload"] as? JsonObject ?: return
                    inner["event"]?.jsonPrimitive?.content?.let(onEvent)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { closed.complete(Unit) }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { closed.complete(Unit) }
            },
        )
        socket = ws
        val beat = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_MS)
                ws.send(buildJsonObject { put("topic", "phoenix"); put("event", "heartbeat"); put("ref", next()); put("payload", buildJsonObject {}) }.toString())
            }
        }
        try {
            closed.await()
        } finally {
            beat.cancel()
            ws.cancel()
        }
    }

    private fun next(): String = (++ref).toString()

    private companion object {
        const val HEARTBEAT_MS = 25_000L
        const val RETRY_MS = 5_000L
    }
}

package com.twinhearts.app.feature.chat.data.realtime

import com.twinhearts.app.feature.twinai.data.realtime.RealtimeConfig
import kotlinx.coroutines.CompletableDeferred
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
 * Why this file exists: instant chat. Listens to the same Supabase "broadcast" channel the web
 * chat uses (`chat-<coupleId>`), so a new message and every tick change (sent, delivered, seen)
 * show up at once instead of after the next 3-second check. The 3-second poll stays as the
 * safety net, and this does nothing until [RealtimeConfig] is filled in.
 */
interface ChatLive {
    /** [onEvent] gets the event name ("new_message", "message_status") and its payload. */
    fun connect(coupleId: String, onEvent: (String, JsonObject) -> Unit)
    fun close()
}

/** Used by unit tests and while realtime is not configured. */
object NoChatLive : ChatLive {
    override fun connect(coupleId: String, onEvent: (String, JsonObject) -> Unit) = Unit
    override fun close() = Unit
}

@Singleton
class ChatRealtime @Inject constructor(client: OkHttpClient, private val json: Json) : ChatLive {

    private val http = client.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: WebSocket? = null
    private var job: Job? = null
    private var ref = 0

    override fun connect(coupleId: String, onEvent: (String, JsonObject) -> Unit) {
        if (!RealtimeConfig.isConfigured) return
        close()
        job = scope.launch {
            while (isActive) {
                runSocket(coupleId, onEvent)
                delay(RETRY_MS)
            }
        }
    }

    override fun close() {
        job?.cancel()
        job = null
        socket?.close(1000, null)
        socket = null
    }

    private suspend fun runSocket(coupleId: String, onEvent: (String, JsonObject) -> Unit) {
        val base = RealtimeConfig.SUPABASE_URL.trimEnd('/').replaceFirst("http", "ws")
        val url = "$base/realtime/v1/websocket?apikey=${RealtimeConfig.ANON_KEY}&vsn=1.0.0"
        val topic = "realtime:chat-$coupleId"
        val closed = CompletableDeferred<Unit>()
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
                    val inner = message["payload"] as? JsonObject ?: return
                    val event = inner["event"]?.jsonPrimitive?.content ?: return
                    val payload = inner["payload"] as? JsonObject ?: JsonObject(emptyMap())
                    onEvent(event, payload)
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
        const val RETRY_MS = 3_000L
    }
}

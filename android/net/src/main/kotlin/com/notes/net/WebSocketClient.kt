package com.notes.net

import com.notes.model.BoundingBox
import com.notes.model.Line
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit

class TutorWebSocketClient(
    private val serverUrl: String = "ws://10.0.2.2:8000/ws",
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var webSocket: WebSocket? = null
    var sessionId: String = UUID.randomUUID().toString()
        private set

    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ServerMessage> = _messages.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    fun connect() {
        if (webSocket != null) return
        val request = Request.Builder().url(serverUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connectionState.tryEmit(true)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                scope.launch {
                    handleIncomingText(text)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                _connectionState.tryEmit(false)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connectionState.tryEmit(false)
                webSocket = null
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.tryEmit(false)
                webSocket = null
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal Closure")
        webSocket = null
        _connectionState.tryEmit(false)
    }

    fun sendRegionSubmit(
        lines: List<Line>,
        boundingBox: BoundingBox,
        pageRev: Long,
        trigger: String = "manual"
    ): String {
        val messageId = UUID.randomUUID().toString()
        val payload = RegionSubmitPayload(
            lines = lines,
            bounding_box = boundingBox,
            trigger = trigger
        )
        val envelope = MessageEnvelope(
            v = 1,
            type = "region.submit",
            id = messageId,
            session_id = sessionId,
            page_rev = pageRev,
            payload = TolerantProtocolJson.encodeToJsonElement(payload)
        )
        val jsonString = TolerantProtocolJson.encodeToString(envelope)
        webSocket?.send(jsonString)
        return messageId
    }

    fun sendPing(timestamp: Long = System.currentTimeMillis(), pageRev: Long = 0L): String {
        val messageId = UUID.randomUUID().toString()
        val payload = PingPayload(timestamp = timestamp)
        val envelope = MessageEnvelope(
            v = 1,
            type = "ping",
            id = messageId,
            session_id = sessionId,
            page_rev = pageRev,
            payload = TolerantProtocolJson.encodeToJsonElement(payload)
        )
        val jsonString = TolerantProtocolJson.encodeToString(envelope)
        webSocket?.send(jsonString)
        return messageId
    }

    private suspend fun handleIncomingText(text: String) {
        try {
            val env = TolerantProtocolJson.decodeFromString<MessageEnvelope>(text)
            sessionId = env.session_id
            val parsedMessage: ServerMessage = when (env.type) {
                "status" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<StatusPayload>(env.payload)
                    ServerMessage.Status(env, p)
                }
                "action.underline" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ActionUnderlinePayload>(env.payload)
                    ServerMessage.Underline(env, p)
                }
                "action.sticky_note" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ActionStickyNotePayload>(env.payload)
                    ServerMessage.StickyNote(env, p)
                }
                "action.dismiss" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ActionDismissPayload>(env.payload)
                    ServerMessage.Dismiss(env, p)
                }
                "error" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ErrorPayload>(env.payload)
                    ServerMessage.Error(env, p)
                }
                "pong" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<PongPayload>(env.payload)
                    ServerMessage.Pong(env, p)
                }
                else -> ServerMessage.Unknown(env)
            }
            _messages.emit(parsedMessage)
        } catch (e: Exception) {
            // Log or ignore unparseable message
        }
    }
}

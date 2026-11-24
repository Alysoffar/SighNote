package com.notes.net

import com.notes.model.BoundingBox
import com.notes.model.Line
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class WebSocketClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        try {
            server.shutdown()
        } catch (_: Exception) {
        }
    }

    @Test
    fun testReceiveUnderlineAction() = runBlocking {
        val serverUrl = server.url("/ws").toString().replace("http", "ws")
        val targetLineId = UUID.randomUUID().toString()

        val stubResponseJson = """
        {
          "v": 1,
          "type": "action.underline",
          "id": "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
          "session_id": "test-session",
          "page_rev": 1,
          "payload": {
            "action_id": "44444444-4444-4444-4444-444444444444",
            "page_rev": 1,
            "line_id": "$targetLineId",
            "fallback_bbox": {
              "min_x": 10.0,
              "min_y": 20.0,
              "max_x": 150.0,
              "max_y": 60.0
            },
            "color": "#E53935",
            "style": "wavy",
            "explanation": "Sign error"
          }
        }
        """.trimIndent()

        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                webSocket.send(stubResponseJson)
            }
        }))

        val client = TutorWebSocketClient(serverUrl = serverUrl)
        client.connect()

        val testLine = Line(
            id = targetLineId,
            stroke_ids = emptyList(),
            bounding_box = BoundingBox(10f, 20f, 150f, 60f)
        )

        client.sendRegionSubmit(
            lines = listOf(testLine),
            boundingBox = BoundingBox(10f, 20f, 150f, 60f),
            pageRev = 1L
        )

        val message = withTimeout(5000) {
            client.messages.first()
        }

        assertTrue("Expected Underline message", message is ServerMessage.Underline)
        val underline = message as ServerMessage.Underline
        assertEquals(targetLineId, underline.payload.line_id)
        assertEquals("#E53935", underline.payload.color)
        assertEquals("wavy", underline.payload.style)

        client.disconnect()
    }
}

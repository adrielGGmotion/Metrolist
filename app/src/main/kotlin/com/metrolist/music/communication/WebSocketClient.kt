package com.metrolist.music.communication

import com.metrolist.music.discovery.Device
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val client: HttpClient
) {
    private var session: DefaultClientWebSocketSession? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun connect(device: Device) {
        coroutineScope.launch {
            session?.close()
            client.webSocket(
                method = HttpMethod.Get,
                host = device.host,
                port = device.port,
                path = "/control"
            ) {
                session = this
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        // TODO: Handle incoming messages
                    }
                }
            }
        }
    }

    fun sendCommand(command: PlaybackCommand) {
        coroutineScope.launch {
            val commandJson = Json.encodeToString(command)
            session?.send(Frame.Text(commandJson))
        }
    }

    fun disconnect() {
        coroutineScope.launch {
            session?.close()
            session = null
        }
    }

    fun isConnected(): Boolean {
        return session != null
    }
}

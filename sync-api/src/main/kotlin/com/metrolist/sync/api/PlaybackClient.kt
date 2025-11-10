package com.metrolist.sync.api

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.HttpMethod
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import timber.log.Timber

class PlaybackClient(
    private val host: String,
    private val port: Int
) {
    private val client = HttpClient {
        install(WebSockets)
    }
    private var session: ClientWebSocketSession? = null

    suspend fun connect(onStateUpdate: (PlaybackState) -> Unit) {
        client.webSocket(
            method = HttpMethod.Get,
            host = host,
            port = port,
            path = "/playback"
        ) {
            session = this
            Timber.tag(TAG).d("Connected to server")
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val state = Json.decodeFromString<PlaybackState>(text)
                    onStateUpdate(state)
                }
            }
        }
    }

    suspend fun sendCommand(command: PlaybackCommand) {
        Timber.tag(TAG).d("Sending command: %s", command)
        val text = Json.encodeToString(PlaybackCommand.serializer(), command)
        session?.send(Frame.Text(text))
    }

    fun close() {
        client.close()
    }

    companion object {
        private const val TAG = "PlaybackClient"
    }
}

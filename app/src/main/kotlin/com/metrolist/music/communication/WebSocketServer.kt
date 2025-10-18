package com.metrolist.music.communication

import android.content.Context
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import java.util.Collections
import javax.inject.Inject

class WebSocketServer @Inject constructor(
    private val context: Context
) {
    private val connections = Collections.synchronizedSet<DefaultWebSocketServerSession>(LinkedHashSet())
    private val _incomingCommands = MutableSharedFlow<PlaybackCommand>()
    val incomingCommands = _incomingCommands.asSharedFlow()

    private val server = embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        install(ContentNegotiation) {
            json()
        }
        routing {
            webSocket("/control") {
                connections += this
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val command = Json.decodeFromString<PlaybackCommand>(text)
                            _incomingCommands.emit(command)
                        }
                    }
                } finally {
                    connections -= this
                }
            }
        }
    }

    fun start() {
        server.start(wait = false)
    }

    fun stop() {
        server.stop(1000, 1000)
    }

    fun broadcast(command: PlaybackCommand) {
        connections.forEach { connection ->
            val commandJson = Json.encodeToString(command)
            connection.outgoing.trySend(Frame.Text(commandJson))
        }
    }
}

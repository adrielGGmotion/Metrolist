package com.metrolist.music.sync

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

class SyncServer(
    private val onCommand: (String) -> Unit
) {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    private val sessions = Collections.synchronizedList<DefaultWebSocketSession>(mutableListOf())

    fun start(port: Int) {
        if (server != null) return

        server = embeddedServer(CIO, port = port) {
            install(WebSockets) {
                pingPeriod = 15.seconds
                timeout = 15.seconds
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket("/sync") {
                    sessions.add(this)
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val commandJson = frame.readText()
                                onCommand(commandJson)
                            }
                        }
                    } catch (e: Exception) {
                        // Handle exceptions
                    } finally {
                        sessions.remove(this)
                    }
                }
            }
        }.start(wait = false)
    }

    suspend fun broadcastState(stateJson: String) {
        sessions.forEach { session ->
            try {
                session.send(stateJson)
            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
        sessions.clear()
    }
}
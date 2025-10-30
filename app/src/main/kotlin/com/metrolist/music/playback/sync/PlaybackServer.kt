package com.metrolist.music.playback.sync

import android.app.Application
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class PlaybackServer(private val application: Application) {

    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val clients = ConcurrentHashMap<WebSocketServerSession, Unit>()

    fun start() {
        if (server != null) {
            Timber.d("Server is already running")
            return
        }
        server = embeddedServer(Netty, port = 8080) {
            install(WebSockets)
            routing {
                webSocket("/playback") {
                    Timber.d("Client connected")
                    clients[this] = Unit
                    try {
                        for (frame in incoming) {
                            // Handle incoming frames
                        }
                    } finally {
                        Timber.d("Client disconnected")
                        clients.remove(this)
                    }
                }
            }
        }.start(wait = false)
        Timber.d("PlaybackServer started")
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
        clients.clear()
        Timber.d("PlaybackServer stopped")
    }
}

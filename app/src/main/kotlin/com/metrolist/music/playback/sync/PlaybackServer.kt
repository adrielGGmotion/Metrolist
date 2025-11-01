package com.metrolist.music.playback.sync

import dagger.hilt.android.scopes.ServiceScoped
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
class PlaybackServer @Inject constructor() {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun start() {
        if (server != null) {
            Timber.d("Server already running")
            return
        }

        server = embeddedServer(Netty, port = 8080) {
            install(WebSockets)
            routing {
                webSocket("/playback") {
                    Timber.d("Client connected")
                    for (frame in incoming) {
                        // Handle incoming frames
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
    }
}

package com.metrolist.sync.playback

import android.net.nsd.NsdServiceInfo
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackClient @Inject constructor() {
    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun connect(service: NsdServiceInfo) {
        try {
            Timber.d("Attempting to connect to WebSocket at ws://${service.host}:${service.port}/playback")
            session = client.webSocketSession(
                method = io.ktor.http.HttpMethod.Get,
                host = service.host.hostAddress,
                port = service.port,
                path = "/playback"
            )
            Timber.d("WebSocket connection successful.")
        } catch (e: Exception) {
            Timber.e(e, "WebSocket connection failed.")
        }
    }

    suspend fun disconnect() {
        session?.close()
        session = null
    }
}

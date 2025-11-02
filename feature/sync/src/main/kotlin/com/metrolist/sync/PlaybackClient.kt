package com.metrolist.sync

import com.metrolist.sync.api.DiscoveredDevice
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import timber.log.Timber
import javax.inject.Inject

class PlaybackClient @Inject constructor(
    private val httpClient: HttpClient
) {
    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(device: DiscoveredDevice) {
        if (session?.isActive == true) {
            Timber.d("Already connected to a device.")
            return
        }

        try {
            Timber.d("Attempting to connect to ws://${device.hostAddress}:${device.port}/playback")
            session = httpClient.webSocketSession {
                url("ws://${device.hostAddress}:${device.port}/playback")
            }
            Timber.d("WebSocket connection successful.")
        } catch (e: Exception) {
            Timber.e(e, "WebSocket connection failed.")
        }
    }

    suspend fun disconnect() {
        session?.close()
        session = null
        Timber.d("WebSocket session disconnected.")
    }
}

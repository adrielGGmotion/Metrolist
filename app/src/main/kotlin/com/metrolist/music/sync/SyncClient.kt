package com.metrolist.music.sync

import okhttp3.*

class SyncClient(
    private val onStateUpdate: (String) -> Unit,
    private val onConnectionOpen: () -> Unit,
    private val onConnectionClosed: () -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private inner class SyncWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            onConnectionOpen()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            onStateUpdate(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            onConnectionClosed()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            onConnectionClosed()
        }
    }

    fun connect(ip: String, port: Int) {
        val request = Request.Builder().url("ws://$ip:$port/sync").build()
        webSocket = client.newWebSocket(request, SyncWebSocketListener())
    }

    fun sendCommand(commandJson: String) {
        webSocket?.send(commandJson)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnected")
    }
}
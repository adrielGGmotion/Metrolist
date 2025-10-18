package com.metrolist.music.communication

import android.content.Context
import com.metrolist.music.discovery.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webSocketClient: WebSocketClient
) {
    private var webSocketServer: WebSocketServer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _incomingCommands = MutableSharedFlow<PlaybackCommand>()
    val incomingCommands = _incomingCommands.asSharedFlow()


    fun startServer() {
        if (webSocketServer == null) {
            webSocketServer = WebSocketServer(context).apply {
                start()
                coroutineScope.launch {
                    this@apply.incomingCommands.collect {
                        _incomingCommands.emit(it)
                    }
                }
            }
        }
    }

    fun stopServer() {
        webSocketServer?.stop()
        webSocketServer = null
    }

    fun connectToDevice(device: Device) {
        webSocketClient.connect(device)
    }

    fun broadcastPlaybackState(playbackCommand: PlaybackCommand) {
        webSocketServer?.broadcast(playbackCommand)
    }

    fun sendPlay() {
        webSocketClient.sendCommand(PlaybackCommand.Play)
    }

    fun sendPause() {
        webSocketClient.sendCommand(PlaybackCommand.Pause)
    }

    fun sendSeek(position: Long) {
        webSocketClient.sendCommand(PlaybackCommand.Seek(position))
    }

    fun sendNext() {
        webSocketClient.sendCommand(PlaybackCommand.Next)
    }

    fun sendPrevious() {
        webSocketClient.sendCommand(PlaybackCommand.Previous)
    }

    fun sendCommand(command: PlaybackCommand) {
        webSocketClient.sendCommand(command)
    }

    fun isConnected(): Boolean {
        return webSocketClient.isConnected()
    }
}

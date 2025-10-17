package com.metrolist.music.sync

import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.metrolist.music.playback.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ServerSocket

class SyncManager(
    private val context: Context,
    private val musicService: MusicService,
    private val scope: CoroutineScope
) {
    private val _discoveredDevices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private var syncServer: SyncServer? = null
    private var syncClient: SyncClient? = null
    private var nsdRegistrar: NsdRegistrar? = null
    private var nsdDiscoverer: NsdDiscoverer? = null

    private var serverPort: Int? = null

    fun start() {
        startServer()
        startDiscovery()
    }

    fun stop() {
        stopServer()
        stopDiscovery()
        disconnect()
    }

    private fun startServer() {
        if (syncServer != null) return

        serverPort = getAvailablePort()
        serverPort?.let { port ->
            syncServer = SyncServer(
                onCommand = { commandJson ->
                    handleCommand(commandJson)
                }
            ).apply {
                start(port)
            }
            nsdRegistrar = NsdRegistrar(context).apply {
                registerService(port, Build.MODEL)
            }
        }
    }

    private fun stopServer() {
        syncServer?.stop()
        syncServer = null
        nsdRegistrar?.unregisterService()
        nsdRegistrar = null
    }

    private fun startDiscovery() {
        if (nsdDiscoverer != null) return

        nsdDiscoverer = NsdDiscoverer(
            context = context,
            onServiceResolved = { serviceInfo ->
                val currentDevices = _discoveredDevices.value.toMutableList()
                if (currentDevices.none { it.serviceName == serviceInfo.serviceName }) {
                    currentDevices.add(serviceInfo)
                    _discoveredDevices.value = currentDevices
                }
            },
            onServiceLost = { serviceInfo ->
                val currentDevices = _discoveredDevices.value.toMutableList()
                currentDevices.removeAll { it.serviceName == serviceInfo.serviceName }
                _discoveredDevices.value = currentDevices
            }
        ).apply {
            startDiscovery()
        }
    }

    private fun stopDiscovery() {
        nsdDiscoverer?.stopDiscovery()
        nsdDiscoverer = null
    }

    fun connectToDevice(serviceInfo: NsdServiceInfo) {
        disconnect()
        syncClient = SyncClient(
            onStateUpdate = { stateJson ->
                handleStateUpdate(stateJson)
            },
            onConnectionOpen = {
                scope.launch {
                    val command = Command.RequestState
                    val commandJson = syncProtocolJson.encodeToString(Command.serializer(), command)
                    syncClient?.sendCommand(commandJson)
                }
            },
            onConnectionClosed = {
                // Handle disconnection
            }
        ).apply {
            connect(serviceInfo.host.hostAddress, serviceInfo.port)
        }
    }

    fun disconnect() {
        syncClient?.disconnect()
        syncClient = null
    }

    private fun handleCommand(commandJson: String) {
        scope.launch {
            val command = syncProtocolJson.decodeFromString(Command.serializer(), commandJson)
            musicService.handleSyncCommand(command)
        }
    }

    private fun handleStateUpdate(stateJson: String) {
        scope.launch {
            val state = syncProtocolJson.decodeFromString(PlaybackState.serializer(), stateJson)
            musicService.handleSyncStateUpdate(state)
        }
    }

    fun broadcastPlaybackState(state: PlaybackState) {
        scope.launch {
            val stateJson = syncProtocolJson.encodeToString(PlaybackState.serializer(), state)
            syncServer?.broadcastState(stateJson)
        }
    }

    private fun getAvailablePort(): Int {
        return try {
            ServerSocket(0).use { it.localPort }
        } catch (e: IOException) {
            -1
        }
    }
}
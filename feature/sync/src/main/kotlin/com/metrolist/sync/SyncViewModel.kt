package com.metrolist.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.common.data.DataStoreUtil
import com.metrolist.sync.api.DiscoveredDevice
import com.metrolist.sync.api.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val dataStoreUtil: DataStoreUtil,
    private val serviceDiscoverer: ServiceDiscoverer,
    private val playbackClient: PlaybackClient,
    private val syncState: SyncState
) : ViewModel() {
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>
        get() = syncState.discoveredDevices

    init {
        viewModelScope.launch {
            dataStoreUtil.isSyncEnabled().collect { isSyncEnabled ->
                if (isSyncEnabled) {
                    startDiscovery()
                } else {
                    stopDiscovery()
                }
            }
        }
    }

    private fun startDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            serviceDiscoverer.startDiscovery(
                onServiceResolved = { serviceInfo ->
                    viewModelScope.launch {
                        val deviceName = serviceInfo.attributes["device"]?.toString(Charsets.UTF_8) ?: serviceInfo.serviceName
                        val discoveredDevice = DiscoveredDevice(
                            serviceName = serviceInfo.serviceName,
                            deviceName = deviceName,
                            hostAddress = serviceInfo.host.hostAddress,
                            port = serviceInfo.port,
                            isSelf = syncState.isSelfDevice(serviceInfo)
                        )
                        syncState.addDiscoveredDevice(discoveredDevice)
                    }
                },
                onServiceLost = { serviceInfo ->
                    syncState.removeDiscoveredDevice(serviceInfo)
                }
            )
        }
    }

    private fun stopDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            serviceDiscoverer.stopDiscovery()
        }
    }

    fun refreshDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            syncState.clearDiscoveredDevices()
            serviceDiscoverer.stopDiscovery()
            startDiscovery()
        }
    }

    fun connectToDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            playbackClient.connect(device)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
        viewModelScope.launch {
            playbackClient.disconnect()
        }
    }
}

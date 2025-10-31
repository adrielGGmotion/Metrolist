package com.metrolist.sync

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.common.data.DataStoreUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveredDevice(
    val serviceName: String,
    val deviceName: String,
    val hostAddress: String,
    val port: Int,
    val isSelf: Boolean = false
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val dataStoreUtil: DataStoreUtil,
    private val serviceDiscoverer: ServiceDiscoverer,
    private val playbackClient: PlaybackClient
) : ViewModel() {
    private val userEmail: StateFlow<String?> = dataStoreUtil.getEmail()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

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
                            isSelf = isSelfDevice(serviceInfo)
                        )
                        if (_discoveredDevices.value.none { it.serviceName == discoveredDevice.serviceName }) {
                            _discoveredDevices.value = _discoveredDevices.value + discoveredDevice
                        }
                    }
                },
                onServiceLost = { serviceInfo ->
                    _discoveredDevices.value = _discoveredDevices.value.filter {
                        it.serviceName != serviceInfo.serviceName
                    }
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
            _discoveredDevices.value = emptyList()
            serviceDiscoverer.stopDiscovery()
            startDiscovery()
        }
    }

    fun connectToDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            playbackClient.connect(device)
        }
    }

    private suspend fun isSelfDevice(serviceInfo: NsdServiceInfo): Boolean {
        val deviceEmail = serviceInfo.attributes["email"]?.toString(Charsets.UTF_8)
        return userEmail.first() == deviceEmail
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
        viewModelScope.launch {
            playbackClient.disconnect()
        }
    }
}

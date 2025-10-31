package com.metrolist.music.viewmodels

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.common.data.DataStoreUtil
import com.metrolist.sync.api.DiscoveredDevice
import com.metrolist.sync.ServiceDiscoverer
import com.metrolist.sync.api.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStoreUtil: DataStoreUtil,
    private val serviceDiscoverer: ServiceDiscoverer
) : ViewModel(), SyncState {
    private val _showPlayer = MutableStateFlow(true)
    val showPlayer = _showPlayer.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val userEmail: StateFlow<String?> = dataStoreUtil.getEmail()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

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

    fun setShowPlayer(show: Boolean) {
        _showPlayer.value = show
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

    override fun refreshDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            _discoveredDevices.value = emptyList()
            serviceDiscoverer.stopDiscovery()
            startDiscovery()
        }
    }

    private suspend fun isSelfDevice(serviceInfo: NsdServiceInfo): Boolean {
        val deviceEmail = serviceInfo.attributes["email"]?.toString(Charsets.UTF_8)
        return userEmail.first() == deviceEmail
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}

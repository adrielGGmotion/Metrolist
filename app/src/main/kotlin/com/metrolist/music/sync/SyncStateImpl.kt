package com.metrolist.music.sync

import com.metrolist.sync.api.DiscoveredDevice
import com.metrolist.sync.api.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStateImpl @Inject constructor() : SyncState {
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>>
        get() = _discoveredDevices.asStateFlow()

    override fun refreshDiscovery() {
        // TODO: Implement
    }

    override fun isSelfDevice(serviceInfo: Any): Boolean {
        // TODO: Implement
        return false
    }

    override fun addDiscoveredDevice(device: DiscoveredDevice) {
        if (_discoveredDevices.value.none { it.serviceName == device.serviceName }) {
            _discoveredDevices.value = _discoveredDevices.value + device
        }
    }

    override fun removeDiscoveredDevice(serviceInfo: Any) {
        // TODO: Implement
    }

    override fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }
}

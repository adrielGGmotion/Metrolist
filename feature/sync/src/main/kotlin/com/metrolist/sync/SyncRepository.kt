package com.metrolist.sync

import android.net.nsd.NsdServiceInfo
import com.metrolist.common.data.DataStoreUtil
import com.metrolist.sync.api.DiscoveredDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val serviceDiscoverer: ServiceDiscoverer,
    private val dataStoreUtil: DataStoreUtil
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _serviceInfoMap = mutableMapOf<String, NsdServiceInfo>()

    private val userEmail: StateFlow<String?> = dataStoreUtil.getEmail()
        .stateIn(coroutineScope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

    fun startDiscovery() {
        coroutineScope.launch {
            serviceDiscoverer.startDiscovery(
                onServiceResolved = { serviceInfo ->
                    coroutineScope.launch {
                        _serviceInfoMap[serviceInfo.serviceName] = serviceInfo
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
                    _serviceInfoMap.remove(serviceInfo.serviceName)
                    _discoveredDevices.value = _discoveredDevices.value.filter {
                        it.serviceName != serviceInfo.serviceName
                    }
                }
            )
        }
    }

    fun stopDiscovery() {
        coroutineScope.launch {
            serviceDiscoverer.stopDiscovery()
        }
    }

    fun refreshDiscovery() {
        coroutineScope.launch {
            _discoveredDevices.value = emptyList()
            _serviceInfoMap.clear()
            serviceDiscoverer.stopDiscovery()
            startDiscovery()
        }
    }

    fun getServiceInfo(serviceName: String): NsdServiceInfo? {
        return _serviceInfoMap[serviceName]
    }

    private suspend fun isSelfDevice(serviceInfo: NsdServiceInfo): Boolean {
        val deviceEmail = serviceInfo.attributes["email"]?.toString(Charsets.UTF_8)
        return userEmail.first() == deviceEmail
    }
}

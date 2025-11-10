package com.metrolist.music.sync

import android.content.Context
import com.metrolist.sync.api.DiscoveredDevice
import com.metrolist.sync.api.SyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncState {

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing

    private val serviceDiscoverer = ServiceDiscoverer(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            serviceDiscoverer.discoveredDevices.collect { devices ->
                _discoveredDevices.value = devices
            }
        }
    }

    fun startDiscovery() {
        serviceDiscoverer.registerService(8080)
        serviceDiscoverer.discoverServices()
        _isSyncing.value = true
    }

    fun stopDiscovery() {
        serviceDiscoverer.stopDiscovery()
        _isSyncing.value = false
    }
}

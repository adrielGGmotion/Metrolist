package com.metrolist.sync.api

import kotlinx.coroutines.flow.StateFlow

interface SyncState {
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>
    fun refreshDiscovery()
    fun isSelfDevice(serviceInfo: Any): Boolean
    fun addDiscoveredDevice(device: DiscoveredDevice)
    fun removeDiscoveredDevice(serviceInfo: Any)
    fun clearDiscoveredDevices()
}

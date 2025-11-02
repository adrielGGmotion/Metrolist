package com.metrolist.sync.api

import kotlinx.coroutines.flow.StateFlow

interface SyncState {
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>
    fun refreshDiscovery()
}

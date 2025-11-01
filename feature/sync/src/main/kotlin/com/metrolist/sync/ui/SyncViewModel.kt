package com.metrolist.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.sync.playback.PlaybackClient
import com.metrolist.sync.SyncRepository
import com.metrolist.sync.api.DiscoveredDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val playbackClient: PlaybackClient
) : ViewModel() {
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = syncRepository.discoveredDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun connectToDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            val serviceInfo = syncRepository.getServiceInfo(device.serviceName)
            if (serviceInfo != null) {
                playbackClient.connect(serviceInfo)
            }
        }
    }

    fun refreshDiscovery() {
        syncRepository.refreshDiscovery()
    }
}

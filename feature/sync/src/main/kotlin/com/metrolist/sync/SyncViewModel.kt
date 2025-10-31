package com.metrolist.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.sync.api.DiscoveredDevice
import com.metrolist.sync.api.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val playbackClient: PlaybackClient
) : ViewModel() {
    private lateinit var syncState: SyncState

    val discoveredDevices: StateFlow<List<DiscoveredDevice>>
        get() = syncState.discoveredDevices

    fun init(syncState: SyncState) {
        this.syncState = syncState
    }

    fun refreshDiscovery() {
        syncState.refreshDiscovery()
    }

    fun connectToDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            playbackClient.connect(device)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            playbackClient.disconnect()
        }
    }
}

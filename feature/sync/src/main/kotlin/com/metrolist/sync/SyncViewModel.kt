package com.metrolist.sync

import androidx.lifecycle.ViewModel
import com.metrolist.sync.api.DiscoveredDevice
import com.metrolist.sync.api.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.sync.api.PlaybackClient
import com.metrolist.sync.api.PlaybackCommand
import com.metrolist.sync.api.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncState: SyncState
) : ViewModel(), SyncState by syncState {

    private val _showDeviceSelection = MutableStateFlow(false)
    val showDeviceSelection: StateFlow<Boolean> = _showDeviceSelection

    private val _playbackState = MutableStateFlow<PlaybackState?>(null)
    val playbackState: StateFlow<PlaybackState?> = _playbackState

    private var client: PlaybackClient? = null

    fun showDeviceSelection() {
        _showDeviceSelection.value = true
    }

    fun hideDeviceSelection() {
        _showDeviceSelection.value = false
    }

    fun connectToDevice(device: DiscoveredDevice) {
        client = PlaybackClient(device.ip, device.port)
        viewModelScope.launch {
            client?.connect { state ->
                _playbackState.value = state
            }
        }
    }

    fun sendCommand(command: PlaybackCommand) {
        viewModelScope.launch {
            client?.sendCommand(command)
        }
    }

    override fun onCleared() {
        super.onCleared()
        client?.close()
    }
}

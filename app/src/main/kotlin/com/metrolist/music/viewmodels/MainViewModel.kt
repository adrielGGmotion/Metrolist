package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.common.data.DataStoreUtil
import com.metrolist.sync.SyncRepository
import com.metrolist.sync.api.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStoreUtil: DataStoreUtil,
    private val syncRepository: SyncRepository
) : ViewModel(), SyncState {
    private val _showPlayer = MutableStateFlow(true)
    val showPlayer = _showPlayer.asStateFlow()

    override val discoveredDevices = syncRepository.discoveredDevices

    init {
        viewModelScope.launch {
            dataStoreUtil.isSyncEnabled().collect { isSyncEnabled ->
                if (isSyncEnabled) {
                    syncRepository.startDiscovery()
                } else {
                    syncRepository.stopDiscovery()
                }
            }
        }
    }

    fun setShowPlayer(show: Boolean) {
        _showPlayer.value = show
    }

    override fun refreshDiscovery() {
        syncRepository.refreshDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        syncRepository.stopDiscovery()
    }
}

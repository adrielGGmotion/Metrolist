package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.utils.SyncStatus
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val syncUtils: SyncUtils
) : ViewModel() {
    val isScanning = syncUtils.syncState
        .map { it.localFiles is SyncStatus.Syncing }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun scanLocalFiles(uris: Set<String>) {
        syncUtils.syncLocalFiles(uris)
    }
}

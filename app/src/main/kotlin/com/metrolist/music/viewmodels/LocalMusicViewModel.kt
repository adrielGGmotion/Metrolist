package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val syncUtils: SyncUtils
) : ViewModel() {
    fun scanLocalFiles(uris: Set<String>) {
        syncUtils.syncLocalFiles(uris)
    }
}

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.di.MainCoroutineDispatcher
import com.metrolist.music.di.SettingsDatastore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDatastore: SettingsDatastore,
    @MainCoroutineDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    val multiDeviceControlEnabled = settingsDatastore.multiDeviceControlEnabled.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        false
    )

    fun setMultiDeviceControl(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            settingsDatastore.setMultiDeviceControl(enabled)
        }
    }
}

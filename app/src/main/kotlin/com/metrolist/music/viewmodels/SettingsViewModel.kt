package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.metrolist.music.di.MainCoroutineDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.metrolist.music.constants.MultiDeviceControlEnabledKey
import kotlinx.coroutines.flow.map

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @MainCoroutineDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    val multiDeviceControlEnabled = dataStore.data.map { it[MultiDeviceControlEnabledKey] ?: false }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        false
    )

    fun setMultiDeviceControl(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            dataStore.edit {
                it[MultiDeviceControlEnabledKey] = enabled
            }
        }
    }
}

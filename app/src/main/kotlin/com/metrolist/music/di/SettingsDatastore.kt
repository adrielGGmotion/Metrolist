package com.metrolist.music.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDatastore @Inject constructor(private val dataStore: DataStore<Preferences>) {
    companion object {
        val MULTI_DEVICE_CONTROL_ENABLED = booleanPreferencesKey("multi_device_control_enabled")
    }

    val multiDeviceControlEnabled =
        dataStore.data.map { it[MULTI_DEVICE_CONTROL_ENABLED] ?: false }

    suspend fun setMultiDeviceControl(enabled: Boolean) {
        dataStore.edit {
            it[MULTI_DEVICE_CONTROL_ENABLED] = enabled
        }
    }
}

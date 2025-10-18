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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDatastore @Inject constructor(@ApplicationContext context: Context) {
    private val datastore = context.dataStore

    companion object {
        val MULTI_DEVICE_CONTROL_ENABLED = booleanPreferencesKey("multi_device_control_enabled")
    }

    val multiDeviceControlEnabled =
        datastore.data.map { it[MULTI_DEVICE_CONTROL_ENABLED] ?: false }

    suspend fun setMultiDeviceControl(enabled: Boolean) {
        datastore.edit {
            it[MULTI_DEVICE_CONTROL_ENABLED] = enabled
        }
    }
}

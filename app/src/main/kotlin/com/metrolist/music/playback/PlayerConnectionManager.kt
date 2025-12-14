package com.metrolist.music.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerConnectionManager @Inject constructor() {
    private val _playerConnection = MutableStateFlow<PlayerConnection?>(null)
    val playerConnection: StateFlow<PlayerConnection?> = _playerConnection

    fun setPlayerConnection(playerConnection: PlayerConnection?) {
        _playerConnection.value = playerConnection
    }
}

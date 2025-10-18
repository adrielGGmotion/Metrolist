package com.metrolist.music.ui.player

import androidx.lifecycle.ViewModel
import com.metrolist.music.communication.CommunicationManager
import com.metrolist.music.communication.PlaybackCommand
import com.metrolist.music.discovery.Device
import com.metrolist.music.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val communicationManager: CommunicationManager
) : ViewModel() {
    fun connectToDevice(device: Device, playerConnection: PlayerConnection) {
        communicationManager.connectToDevice(device)
        val player = playerConnection.player
        val command = PlaybackCommand.StateUpdate(
            trackId = player.currentMediaItem?.mediaId,
            isPlaying = player.isPlaying,
            position = player.currentPosition,
            queue = List(player.mediaItemCount) { i -> player.getMediaItemAt(i).mediaId }
        )
        communicationManager.sendCommand(command)
    }
}

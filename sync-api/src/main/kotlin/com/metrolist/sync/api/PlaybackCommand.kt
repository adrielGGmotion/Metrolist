package com.metrolist.sync.api

import kotlinx.serialization.Serializable

@Serializable
sealed class PlaybackCommand {
    @Serializable
    object Play : PlaybackCommand()
    @Serializable
    object Pause : PlaybackCommand()
    @Serializable
    object Stop : PlaybackCommand()
    @Serializable
    object Next : PlaybackCommand()
    @Serializable
    object Previous : PlaybackCommand()
    @Serializable
    data class Seek(val position: Long) : PlaybackCommand()
    @Serializable
    data class PlaySong(val songId: String) : PlaybackCommand()
}

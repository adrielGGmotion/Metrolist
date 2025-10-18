package com.metrolist.music.communication

import kotlinx.serialization.Serializable

@Serializable
sealed class PlaybackCommand {
    @Serializable
    data object Play : PlaybackCommand()

    @Serializable
    data object Pause : PlaybackCommand()

    @Serializable
    data object Next : PlaybackCommand()

    @Serializable
    data object Previous : PlaybackCommand()

    @Serializable
    data class Seek(val position: Long) : PlaybackCommand()

    @Serializable
    data class StateUpdate(
        val trackId: String?,
        val isPlaying: Boolean,
        val position: Long,
        val queue: List<String>
    ) : PlaybackCommand()
}

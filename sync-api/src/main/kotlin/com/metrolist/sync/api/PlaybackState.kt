package com.metrolist.sync.api

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val isPlaying: Boolean,
    val songId: String?,
    val position: Long,
    val duration: Long
)

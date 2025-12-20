package com.metrolist.music.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class AppleMusicSearchResponse(
    val results: List<AppleMusicTrack>
)

@Serializable
data class AppleMusicTrack(
    val id: String,
    val songName: String,
    val artistName: String,
    val albumName: String
)

@Serializable
data class AppleMusicLyricsResponse(
    val elrcMultiPerson: String? = null,
    val elrc: String? = null,
    val lrc: String? = null
)

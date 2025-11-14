package com.metrolist.music.apple

import kotlinx.serialization.Serializable

@Serializable
data class AppleMusicLyricsResponse(
    val data: List<AppleMusicLyricsData>? = null
)

@Serializable
data class AppleMusicLyricsData(
    val attributes: AppleMusicLyricsAttributes? = null
)

@Serializable
data class AppleMusicLyricsAttributes(
    val lrc: String? = null // This is the synced LRC lyrics string
)

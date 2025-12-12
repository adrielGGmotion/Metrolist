package com.metrolist.music.wrapped

data class WrappedSong(
    val id: String,
    val title: String,
    val artists: List<WrappedArtist>,
    val totalPlayTime: Long
)

data class WrappedArtist(
    val id: String?,
    val name: String
)

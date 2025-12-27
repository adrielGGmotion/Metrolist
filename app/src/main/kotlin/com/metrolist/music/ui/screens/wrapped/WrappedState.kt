package com.metrolist.music.ui.screens.wrapped

import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.SongWithStats

data class WrappedState(
    val accountInfo: AccountInfo? = null,
    val totalMinutes: Long = 0,
    val topSongs: List<SongWithStats> = emptyList(),
    val topArtists: List<Artist> = emptyList(),
    val uniqueSongCount: Int = 0,
    val uniqueArtistCount: Int = 0,
    val isDataReady: Boolean = false,
    val trackMap: Map<WrappedScreenType, String?> = emptyMap(),
    val playlistCreationState: PlaylistCreationState = PlaylistCreationState.Idle
)

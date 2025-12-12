package com.metrolist.music.wrapped

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem

data class WrappedStats(
    val totalMinutes: Long,
    val topSongs: List<SongItem>,
    val topArtists: List<ArtistItem>,
    val topAlbum: AlbumItem?
)

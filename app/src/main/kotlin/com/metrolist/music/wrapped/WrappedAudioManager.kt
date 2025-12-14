package com.metrolist.music.wrapped

import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WrappedAudioManager(
    private val playerConnection: PlayerConnection?,
    private val coroutineScope: CoroutineScope
) {
    fun play(songId: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val playerResponse = com.metrolist.innertube.YouTube.player(songId, client = com.metrolist.innertube.models.YouTubeClient.WEB_REMIX).getOrNull()
            if (playerResponse != null) {
                val song = MediaMetadata(
                    id = playerResponse.videoDetails?.videoId ?: "",
                    title = playerResponse.videoDetails?.title ?: "",
                    artists = playerResponse.videoDetails?.author?.let { author ->
                        listOf(MediaMetadata.Artist(id = null, name = author))
                    } ?: emptyList(),
                    duration = playerResponse.videoDetails?.lengthSeconds?.toInt() ?: -1,
                    thumbnailUrl = playerResponse.videoDetails?.thumbnail?.thumbnails?.firstOrNull()?.url
                )
                playerConnection?.playQueue(
                    YouTubeQueue(
                        WatchEndpoint(videoId = song.id),
                        song
                    )
                )
            }
        }
    }

    fun release() {
        playerConnection?.player?.pause()
    }
}

package com.metrolist.music.sync

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.sync.api.PlaybackCommand
import com.metrolist.sync.api.PlaybackState
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Collections

object PlaybackServer {

    private const val TAG = "PlaybackServer"
    private val scope = CoroutineScope(Dispatchers.IO) + Job()
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val sessions = Collections.synchronizedSet<DefaultWebSocketServerSession>(LinkedHashSet())

    fun start(musicServiceBinder: MusicService.MusicBinder, database: MusicDatabase) {
        if (server != null) {
            Timber.tag(TAG).d("Server is already running")
            return
        }

        server = embeddedServer(Netty, port = 8080) {
            install(WebSockets)
            install(ContentNegotiation) {
                json()
            }
            routing {
                webSocket("/playback") {
                    sessions.add(this)
                    Timber.tag(TAG).d("New client connected")
                    val initialState = getPlaybackState(musicServiceBinder)
                    outgoing.send(Frame.Text(Json.encodeToString(PlaybackState.serializer(), initialState)))
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val command = Json.decodeFromString<PlaybackCommand>(text)
                            handleCommand(command, musicServiceBinder, database)
                        }
                    }
                    sessions.remove(this)
                }
            }
        }.start(wait = false)
        scope.launch {
            val player = musicServiceBinder.service.player
            while (true) {
                val state = PlaybackState(
                    isPlaying = player.isPlaying,
                    songId = player.currentMediaItem?.mediaId,
                    position = player.currentPosition,
                    duration = player.duration
                )
                sessions.forEach { session ->
                    session.send(Frame.Text(Json.encodeToString(PlaybackState.serializer(), state)))
                }
                kotlinx.coroutines.delay(500)
            }
        }
        Timber.tag(TAG).d("Server started on port 8080")
    }

    fun stop() {
        server?.stop(1_000, 2_000)
        server = null
        scope.coroutineContext.cancelChildren()
        Timber.tag(TAG).d("Server stopped")
    }

    private fun getPlaybackState(musicServiceBinder: MusicService.MusicBinder): PlaybackState {
        val player = musicServiceBinder.service.player
        return PlaybackState(
            isPlaying = player.isPlaying,
            songId = player.currentMediaItem?.mediaId,
            position = player.currentPosition,
            duration = player.duration
        )
    }

    private suspend fun handleCommand(
        command: PlaybackCommand,
        musicServiceBinder: MusicService.MusicBinder,
        database: MusicDatabase
    ) {
        val player = musicServiceBinder.service.player
        when (command) {
            is PlaybackCommand.Play -> player.play()
            is PlaybackCommand.Pause -> player.pause()
            is PlaybackCommand.Stop -> player.stop()
            is PlaybackCommand.Next -> player.seekToNext()
            is PlaybackCommand.Previous -> player.seekToPrevious()
            is PlaybackCommand.Seek -> player.seekTo(command.position)
            is PlaybackCommand.PlaySong -> {
                val song = database.song(command.songId).first()
                if (song != null) {
                    val mediaMetadata = MediaMetadata(
                        id = song.song.id,
                        title = song.song.title,
                        artists = song.artists.map { MediaMetadata.Artist(it.id, it.name) },
                        duration = song.song.duration,
                        thumbnailUrl = song.song.thumbnailUrl,
                        album = song.album?.let { MediaMetadata.Album(it.id, it.title) }
                    )
                    val queue = YouTubeQueue(
                        endpoint = com.metrolist.innertube.models.WatchEndpoint(videoId = song.song.id),
                        preloadItem = mediaMetadata
                    )
                    musicServiceBinder.service.playQueue(queue)
                }
            }
        }
    }
}

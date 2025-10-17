package com.metrolist.music.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface Command {
    @Serializable
    @SerialName("PLAY")
    data object Play : Command

    @Serializable
    @SerialName("PAUSE")
    data object Pause : Command

    @Serializable
    @SerialName("SEEK")
    data class Seek(val positionMs: Long) : Command

    @Serializable
    @SerialName("PLAY_TRACK")
    data class PlayTrack(val trackId: String, val positionInQueue: Int) : Command

    @Serializable
    @SerialName("ADD_TO_QUEUE")
    data class AddToQueue(val trackId: String) : Command

    @Serializable
    @SerialName("REQUEST_STATE")
    data object RequestState : Command
}

@Serializable
data class PlaybackState(
    val isPlaying: Boolean,
    val currentTrack: TrackInfo?,
    val positionMs: Long,
    val durationMs: Long,
    val volume: Int, // e.g., 0-100
    val shuffleMode: Boolean,
    val repeatMode: RepeatMode,
    val queue: List<TrackInfo>
)

@Serializable
data class TrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val durationMs: Long
)

@Serializable
enum class RepeatMode {
    NONE,
    ONE,
    ALL
}

val syncProtocolJson = Json {
    serializersModule = SerializersModule {
        polymorphic(Command::class) {
            subclass(Command.Play::class, Command.Play.serializer())
            subclass(Command.Pause::class, Command.Pause.serializer())
            subclass(Command.Seek::class, Command.Seek.serializer())
            subclass(Command.PlayTrack::class, Command.PlayTrack.serializer())
            subclass(Command.AddToQueue::class, Command.AddToQueue.serializer())
            subclass(Command.RequestState::class, Command.RequestState.serializer())
        }
    }
    ignoreUnknownKeys = true
    classDiscriminator = "commandType"
}
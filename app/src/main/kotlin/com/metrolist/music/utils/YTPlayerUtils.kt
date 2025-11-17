package com.metrolist.music.utils

import com.github.sealed.io.youtubedl.YoutubeDL
import com.github.sealed.io.youtubedl.YoutubeDL
import com.github.sealed.io.youtubedl.YoutubeDLRequest
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.Format
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.music.constants.AudioQuality
import kotlin.math.abs

object YTPlayerUtils {
    suspend fun playerResponseForMetadata(videoId: String, playlistId: String?): Result<PlayerResponse> = runCatching {
        YouTube.player(videoId = videoId, playlistId = playlistId, client = YouTubeClient.WEB).getOrThrow()
    }

    suspend fun playerResponseForPlayback(
        videoId: String,
    ): Result<PlayerResponse> = runCatching {
        val playerResponse = YouTube.player(videoId = videoId, client = YouTubeClient.WEB).getOrThrow()
        if (playerResponse.streamingData?.adaptiveFormats?.any { it.signatureCipher != null } == true) {
            getStreamingData(playerResponse).getOrThrow()
        } else {
            playerResponse
        }
    }

    private fun getStreamingData(
        playerResponse: PlayerResponse
    ): Result<PlayerResponse> = runCatching {
        val url = "https://www.youtube.com/watch?v=${playerResponse.videoDetails?.videoId}"
        val request = YoutubeDLRequest(url, null)
        val response = YoutubeDL.getVideoInfo(request)
        val streamingData = playerResponse.streamingData?.copy(
            adaptiveFormats = playerResponse.streamingData.adaptiveFormats.map {
                val format = response.formats.firstOrNull { f -> f.itag == it.itag }
                if (format != null) {
                    it.copy(
                        url = format.url,
                        contentLength = format.fileSize,
                        averageBitrate = format.bitrate,
                        audioQuality = format.audioBitrate?.toString(),
                    )
                } else {
                    it
                }
            }
        )
        playerResponse.copy(
            streamingData = streamingData
        )
    }

    fun selectFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        isMetered: Boolean
    ): Format? {
        val formats = playerResponse.streamingData?.adaptiveFormats?.filter { it.mimeType.contains("audio") }
        if (formats.isNullOrEmpty()) return null

        return when (audioQuality) {
            AudioQuality.HIGH -> formats.maxByOrNull { it.bitrate }
            AudioQuality.LOW -> formats.minByOrNull { it.bitrate }
            AudioQuality.AUTO -> {
                if (isMetered) {
                    formats.minByOrNull { it.bitrate }
                } else {
                    // Select the format with bitrate closest to 128kbps
                    formats.minByOrNull { abs(it.bitrate - 128000) }
                }
            }
        }
    }
}
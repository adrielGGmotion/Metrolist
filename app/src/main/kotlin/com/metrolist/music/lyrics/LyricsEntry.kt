package com.metrolist.music.lyrics

import kotlinx.coroutines.flow.MutableStateFlow

data class LyricsEntry(
    val time: Long,
    val text: String,
    val romanizedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null)
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}

data class AppleMusicLyricsLine(
    val time: Long,
    val words: List<AppleMusicWord>,
    val speaker: String?
)

data class AppleMusicWord(
    val startTime: Long,
    val endTime: Long,
    val word: String,
    val trailingSpace: Boolean
)

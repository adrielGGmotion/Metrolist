package com.metrolist.music.lyrics

data class AppleMusicWord(
    val text: String,
    val startTime: Long,
    val endTime: Long
)

data class AppleMusicLyricsLine(
    val speaker: String?,
    val words: List<AppleMusicWord>,
    val startTime: Long,
    val endTime: Long
)

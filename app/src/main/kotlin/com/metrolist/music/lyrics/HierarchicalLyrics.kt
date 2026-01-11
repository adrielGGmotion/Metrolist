/**
 * Metrolist Project (C) 2024
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.lyrics

sealed class SpeakerRole {
    object V1 : SpeakerRole()
    object V2 : SpeakerRole()
    object BG : SpeakerRole()
    object NONE: SpeakerRole()
}

data class Word(
    val text: String,
    val startTime: Float,
    val endTime: Float,
    val glowStrength: Float = 0f
)

data class LyricLine(
    val text: String,
    val startTime: Float,
    val endTime: Float,
    val speaker: SpeakerRole,
    val words: List<Word>,
    val parentSpeaker: SpeakerRole? = null  // For BG lines, the speaker of the line it should align with
)

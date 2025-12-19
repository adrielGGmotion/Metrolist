package com.metrolist.music.lyrics

import android.text.format.DateUtils

object AppleMusicLyricsParser {

    private const val FALLBACK_WORD_DURATION_MS = 1000L
    private val LINE_REGEX = "^\\[(\\d{2}:\\d{2}\\.\\d{3})](v\\d:)?\\s*(.*)$".toRegex()
    private val WORD_REGEX = "<(\\d{2}:\\d{2}\\.\\d{3})>([^<]+)".toRegex()
    private val END_TIME_REGEX = "<(\\d{2}:\\d{2}\\.\\d{3})>$".toRegex()

    fun parse(lyrics: String): List<AppleMusicLyricsLine>? {
        if (!lyrics.contains("<") || !lyrics.contains(">")) {
             return null
        }

        val lines = mutableListOf<AppleMusicLyricsLine>()
        lyrics.lines().forEach { line ->
            LINE_REGEX.matchEntire(line.trim())?.let { lineMatch ->
                val speaker = lineMatch.groupValues[2].trim().removeSuffix(":")
                val content = lineMatch.groupValues[3]

                val wordMatches = WORD_REGEX.findAll(content).toList()
                val endTimeMatch = END_TIME_REGEX.find(content)

                if (wordMatches.isNotEmpty()) {
                    val words = mutableListOf<AppleMusicWord>()
                    for (i in wordMatches.indices) {
                        val currentMatch = wordMatches[i]
                        val text = currentMatch.groupValues[2].trim()
                        val startTime = timestampToMillis(currentMatch.groupValues[1])

                        val endTime = if (i + 1 < wordMatches.size) {
                            timestampToMillis(wordMatches[i + 1].groupValues[1])
                        } else {
                            endTimeMatch?.groupValues?.get(1)?.let { timestampToMillis(it) } ?: (startTime + FALLBACK_WORD_DURATION_MS) // Fallback
                        }
                        words.add(AppleMusicWord(text, startTime, endTime))
                    }

                    if (words.isNotEmpty()) {
                        lines.add(
                            AppleMusicLyricsLine(
                                speaker = if (speaker.isNotEmpty()) speaker else null,
                                words = words,
                                startTime = words.first().startTime,
                                endTime = words.last().endTime
                            )
                        )
                    }
                }
            }
        }
        return if (lines.isEmpty()) null else lines
    }

    private fun timestampToMillis(timestamp: String): Long {
        val parts = timestamp.split(":", ".")
        val minutes = parts[0].toLong()
        val seconds = parts[1].toLong()
        val millis = parts[2].toLong()
        return (minutes * DateUtils.MINUTE_IN_MILLIS) + (seconds * DateUtils.SECOND_IN_MILLIS) + millis
    }
}

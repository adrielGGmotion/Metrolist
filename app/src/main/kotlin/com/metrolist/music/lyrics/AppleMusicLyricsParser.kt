/**
 * Metrolist Project (C) 2024
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.lyrics

import java.util.regex.Pattern

object AppleMusicLyricsParser {
    private val lineRegex = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](v1:|v2:|\\[bg\\]:)?(.*)")
    // BG format: [bg: <timestamp>text<timestamp>]
    private val bgLineRegex = Pattern.compile("\\[bg:\\s*(<.*>)\\]")
    private val partRegex = Pattern.compile("<(\\d{2}):(\\d{2})\\.(\\d{2,3})>([^<]*)")

    private object GlowProcessor {
        private const val GLOW_DURATION_THRESHOLD = 1.8f

        fun processAndAssignGlow(originalWords: List<Word>): List<Word> {
            val newWords = mutableListOf<Word>()
            var i = 0
            while (i < originalWords.size) {
                val currentWord = originalWords[i]
                val conceptualParts = mutableListOf(currentWord)

                // Group words that are not separated by a space into a "conceptual word"
                if (!currentWord.text.endsWith(" ") && i < originalWords.size - 1) {
                    var j = i + 1
                    while (j < originalWords.size) {
                        val nextWord = originalWords[j]
                        conceptualParts.add(nextWord)
                        if (nextWord.text.endsWith(" ")) {
                            break // End of conceptual word
                        }
                        j++
                    }
                }

                val conceptualDuration = conceptualParts.last().endTime - conceptualParts.first().startTime

                var glowStrength = 0f
                var glowingPart: Word? = null

                if (conceptualDuration > GLOW_DURATION_THRESHOLD) {
                    glowStrength = 1.0f
                    // The glow applies to the longest part of the conceptual word
                    glowingPart = conceptualParts.maxByOrNull { it.endTime - it.startTime }
                }

                // Add the processed parts to the new word list and advance the index
                conceptualParts.forEach { part ->
                    newWords.add(part.copy(glowStrength = if (part == glowingPart) glowStrength else 0f))
                }
                i += conceptualParts.size
            }
            return newWords
        }
    }

    private fun parseBgLine(content: String, nextLineStartTime: Float, parentSpeaker: SpeakerRole): LyricLine? {
        val parts = mutableListOf<Pair<Float, String>>()
        val partMatcher = partRegex.matcher(content)
        while (partMatcher.find()) {
            val partMin = partMatcher.group(1)?.toFloat() ?: 0f
            val partSec = partMatcher.group(2)?.toFloat() ?: 0f
            val partMs = partMatcher.group(3)?.toFloat() ?: 0f
            val partStartTime = partMin * 60 + partSec + partMs / 1000
            val partText = partMatcher.group(4) ?: ""
            parts.add(Pair(partStartTime, partText))
        }

        if (parts.isEmpty()) return null

        val words = mutableListOf<Word>()
        for (j in parts.indices) {
            val currentPart = parts[j]
            val text = currentPart.second
            if (text.isNotEmpty()) {
                val endTime = if (j + 1 < parts.size) {
                    parts[j + 1].first
                } else {
                    nextLineStartTime.coerceAtLeast(currentPart.first + 0.5f)
                }
                words.add(Word(text, currentPart.first, endTime))
            }
        }

        if (words.isEmpty()) return null

        val processedWords = GlowProcessor.processAndAssignGlow(words)
        val fullText = processedWords.joinToString(separator = "") { it.text }
        val lineStartTime = processedWords.firstOrNull()?.startTime ?: 0f
        val lineEndTime = processedWords.lastOrNull()?.endTime ?: lineStartTime

        return LyricLine(
            text = fullText,
            startTime = lineStartTime,
            endTime = lineEndTime,
            speaker = SpeakerRole.BG,
            words = processedWords,
            parentSpeaker = parentSpeaker
        )
    }

    fun parse(lyricsText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val textLines = lyricsText.lines()
        var lastMainLineSpeaker: SpeakerRole = SpeakerRole.V1

        for (i in textLines.indices) {
            val line = textLines[i]
            
            // Check for BG line format: [bg: <timestamp>text<timestamp>]
            val bgMatcher = bgLineRegex.matcher(line)
            if (bgMatcher.matches()) {
                val bgContent = bgMatcher.group(1) ?: ""
                // Find next line start time for end time calculation
                val nextLineStartTime = findNextLineStartTime(textLines, i + 1)
                val bgLine = parseBgLine(bgContent, nextLineStartTime, lastMainLineSpeaker)
                if (bgLine != null) {
                    lines.add(bgLine)
                }
                continue
            }
            
            val lineMatcher = lineRegex.matcher(line)
            if (lineMatcher.matches()) {
                val lineMin = lineMatcher.group(1)?.toFloat() ?: 0f
                val lineSec = lineMatcher.group(2)?.toFloat() ?: 0f
                val lineMs = lineMatcher.group(3)?.toFloat() ?: 0f
                val lineStartTime = lineMin * 60 + lineSec + lineMs / 1000

                val speaker = when (lineMatcher.group(4)) {
                    "v1:" -> SpeakerRole.V1
                    "v2:" -> SpeakerRole.V2
                    "[bg]:" -> SpeakerRole.BG
                    else -> SpeakerRole.NONE
                }

                val content = lineMatcher.group(5) ?: ""

                val parts = mutableListOf<Pair<Float, String>>()
                val partMatcher = partRegex.matcher(content)
                while(partMatcher.find()) {
                    val partMin = partMatcher.group(1)?.toFloat() ?: 0f
                    val partSec = partMatcher.group(2)?.toFloat() ?: 0f
                    val partMs = partMatcher.group(3)?.toFloat() ?: 0f
                    val partStartTime = partMin * 60 + partSec + partMs / 1000
                    val partText = partMatcher.group(4) ?: ""
                    parts.add(Pair(partStartTime, partText))
                }

                if (parts.isEmpty()) continue

                val words = mutableListOf<Word>()
                for (j in parts.indices) {
                    val currentPart = parts[j]
                    val text = currentPart.second

                    if (text.isNotEmpty()) {
                        val endTime = if (j + 1 < parts.size) {
                            parts[j + 1].first
                        } else {
                             if (i + 1 < textLines.size) {
                                val nextLineMatcher = lineRegex.matcher(textLines[i + 1])
                                if (nextLineMatcher.matches()) {
                                    val nextMin = nextLineMatcher.group(1)?.toFloat() ?: 0f
                                    val nextSec = nextLineMatcher.group(2)?.toFloat() ?: 0f
                                    val nextMs = nextLineMatcher.group(3)?.toFloat() ?: 0f
                                    nextMin * 60 + nextSec + nextMs / 1000
                                } else {
                                    currentPart.first + 1f
                                }
                            } else {
                                currentPart.first + 1f
                            }
                        }
                        words.add(Word(text, currentPart.first, endTime))
                    }
                }

                val processedWords = GlowProcessor.processAndAssignGlow(words)
                val fullText = processedWords.joinToString(separator = "") { it.text }
                val lineEndTime = processedWords.lastOrNull()?.endTime ?: lineStartTime

                // Track the speaker for BG alignment
                if (speaker != SpeakerRole.BG) {
                    lastMainLineSpeaker = speaker
                }

                lines.add(
                    LyricLine(
                        text = fullText,
                        startTime = lineStartTime,
                        endTime = lineEndTime,
                        speaker = speaker,
                        words = processedWords
                    )
                )
            }
        }
        
        return lines
    }
    
    private fun findNextLineStartTime(textLines: List<String>, startIndex: Int): Float {
        for (i in startIndex until textLines.size) {
            val lineMatcher = lineRegex.matcher(textLines[i])
            if (lineMatcher.matches()) {
                val lineMin = lineMatcher.group(1)?.toFloat() ?: 0f
                val lineSec = lineMatcher.group(2)?.toFloat() ?: 0f
                val lineMs = lineMatcher.group(3)?.toFloat() ?: 0f
                return lineMin * 60 + lineSec + lineMs / 1000
            }
        }
        return Float.MAX_VALUE
    }
}

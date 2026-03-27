/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.ui.component.LyricsListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LyricsViewModel : ViewModel() {
    private val _lines = MutableStateFlow<List<LyricsEntry>>(emptyList())
    val lines: StateFlow<List<LyricsEntry>> = _lines.asStateFlow()

    private val _mergedLyricsList = MutableStateFlow<List<LyricsListItem>>(emptyList())
    val mergedLyricsList: StateFlow<List<LyricsListItem>> = _mergedLyricsList.asStateFlow()

    fun processLyrics(
        lyrics: String?,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        showIntervalIndicator: Boolean
    ) {
        viewModelScope.launch {
            val processedLines = withContext(Dispatchers.Default) {
                if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                    emptyList()
                } else if (lyrics.trim().startsWith("[")) {
                    val parsedLines = LyricsUtils.parseLyrics(lyrics)
                    if (parsedLines.isNotEmpty()) {
                        parsedLines.map { entry ->
                            LyricsEntry(
                                entry.time,
                                entry.text,
                                entry.words,
                                agent = entry.agent,
                                isBackground = entry.isBackground
                            )
                        }.let {
                            listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
                        }
                    } else {
                        // Fallback for metadata-only LRC
                        lyrics.lines().filter { it.isNotBlank() }.mapIndexed { index, line ->
                            LyricsEntry(Long.MAX_VALUE / 2 + index, line)
                        }
                    }
                } else {
                    lyrics.lines().filter { it.isNotBlank() }.mapIndexed { index, line ->
                        LyricsEntry(Long.MAX_VALUE / 2 + index, line)
                    }
                }
            }
            
            _lines.value = processedLines
            updateMergedList(processedLines, showIntervalIndicator)

            // Romanize in the background after the UI has been updated
            if (lyrics != null && lyrics != LYRICS_NOT_FOUND && enabledLanguages.isNotEmpty()) {
                launch(Dispatchers.Default) {
                    processedLines.forEach { entry ->
                        if (entry == LyricsEntry.HEAD_LYRICS_ENTRY) return@forEach
                        entry.romanizedTextFlow.value = LyricsUtils.romanize(
                            text = lyrics,
                            line = entry.text,
                            enabledLanguages = enabledLanguages,
                            romanizeCyrillicByLine = romanizeCyrillicByLine
                        )
                    }
                }
            }
        }
    }

    private fun updateMergedList(lines: List<LyricsEntry>, showIntervalIndicator: Boolean) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) {
            _mergedLyricsList.value = result
            return
        }
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) {
                result.add(LyricsListItem.Line(i, entry))
            }
            if (showIntervalIndicator && i < lines.size - 1) {
                val nextStart = lines[i + 1].time
                val currentEnd = if (!entry.words.isNullOrEmpty()) {
                    (entry.words.last().endTime * 1000).toLong()
                } else if (entry.text.isBlank()) {
                    entry.time
                } else {
                    null
                }

                if (currentEnd != null && currentEnd < nextStart) {
                    val gap = nextStart - currentEnd
                    if (gap > 4000L) {
                        result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                    }
                }
            }
        }
        _mergedLyricsList.value = result
    }
}

/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DevToolsLogBuffer(private val maxSize: Int = 1000) {
    private val _logs = MutableStateFlow<List<DevToolsLog>>(emptyList())
    val logs: StateFlow<List<DevToolsLog>> = _logs.asStateFlow()
    
    private val buffer = ArrayDeque<DevToolsLog>(maxSize)

    @Synchronized
    fun add(log: DevToolsLog) {
        if (buffer.size >= maxSize) {
            buffer.removeFirst()
        }
        buffer.addLast(log)
        _logs.value = buffer.toList()
    }

    @Synchronized
    fun clear() {
        buffer.clear()
        _logs.value = emptyList()
    }
}

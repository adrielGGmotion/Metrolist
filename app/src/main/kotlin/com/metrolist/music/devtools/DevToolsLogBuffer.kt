/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DevToolsLogBuffer(private val maxSize: Int = 1000) {
    init {
        require(maxSize > 0) { "maxSize must be positive, got $maxSize" }
    }
    private val buffer = arrayOfNulls<DevToolsLog>(maxSize)
    private var index = 0
    private var isFull = false
    private val lock = ReentrantLock()

    private val _logs = MutableStateFlow<List<DevToolsLog>>(emptyList())
    val logs: StateFlow<List<DevToolsLog>> = _logs.asStateFlow()

    private fun getSnapshotLocked(): List<DevToolsLog> {
        val size = if (isFull) maxSize else index
        val result = ArrayList<DevToolsLog>(size)
        if (isFull) {
            for (i in index until maxSize) {
                buffer[i]?.let { result.add(it) }
            }
        }
        for (i in 0 until index) {
            buffer[i]?.let { result.add(it) }
        }
        return result
    }

    fun add(log: DevToolsLog) {
        lock.withLock {
            buffer[index] = log
            index++
            if (index >= maxSize) {
                index = 0
                isFull = true
            }
            _logs.value = getSnapshotLocked()
        }
    }

    fun clear() {
        lock.withLock {
            buffer.fill(null)
            index = 0
            isFull = false
            _logs.value = emptyList()
        }
    }
}

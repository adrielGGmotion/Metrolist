/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools

import timber.log.Timber

class DevToolsTimberTree(
    private val buffer: DevToolsLogBuffer
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val log = DevToolsLog(
            priority = priority,
            tag = tag ?: "Unknown",
            message = message,
            throwable = t?.stackTraceToString()
        )
        buffer.add(log)
    }
}

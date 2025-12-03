package com.metrolist.music.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    val size = width ?: height ?: return this

    val regex = Regex("=s[0-9]+")
    val matchResult = regex.find(this) ?: return this

    val baseUrl = this.substring(0, matchResult.range.first)
    val remaining = this.substring(matchResult.range.last + 1)

    if (remaining.startsWith("-c")) {
        return "$baseUrl=s$size$remaining"
    } else {
        return "$baseUrl=s$size-c$remaining"
    }
}

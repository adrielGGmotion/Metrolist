package com.metrolist.music.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    val size = width ?: height ?: return this
    return this.split("=").first() + "=s$size-c"
}

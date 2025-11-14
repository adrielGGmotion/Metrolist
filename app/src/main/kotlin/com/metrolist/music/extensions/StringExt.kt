package com.metrolist.music.extensions

import androidx.sqlite.db.SimpleSQLiteQuery
import java.net.InetSocketAddress
import java.net.InetSocketAddress.createUnresolved

inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
    if (this == null) {
        defaultValue
    } else {
        try {
            enumValueOf(this)
        } catch (e: IllegalArgumentException) {
            defaultValue
        }
    }

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

fun String.toInetSocketAddress(): InetSocketAddress {
    val (host, port) = split(":")
    return createUnresolved(host, port.toInt())
}

fun String.substringBetween(delimiter1: String, delimiter2: String): String? {
    val firstIndex = this.indexOf(delimiter1)
    if (firstIndex == -1) return null

    val secondIndex = this.indexOf(delimiter2, firstIndex + delimiter1.length)
    if (secondIndex == -1) return null

    return this.substring(firstIndex + delimiter1.length, secondIndex)
}

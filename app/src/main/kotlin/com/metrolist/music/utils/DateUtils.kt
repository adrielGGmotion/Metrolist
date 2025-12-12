package com.metrolist.music.utils

import java.util.Calendar

fun isWrappedVisible(): Boolean {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.MONTH) == Calendar.DECEMBER ||
            (calendar.get(Calendar.MONTH) == Calendar.JANUARY && calendar.get(Calendar.DAY_OF_MONTH) <= 10)
}

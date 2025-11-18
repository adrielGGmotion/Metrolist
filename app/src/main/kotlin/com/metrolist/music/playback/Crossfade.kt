package com.metrolist.music.playback

import kotlin.math.log10

data class CrossfadeConfig(
    val duration: Int = 0,
    val curve: VolumeInterpolator = VolumeInterpolator.Linear,
    val isEnabled: Boolean = false,
    val isAutomatic: Boolean = false
)

fun interface VolumeInterpolator {
    fun transform(progress: Float): Float

    companion object {
        val Linear = VolumeInterpolator { progress -> progress }
        val Logarithmic = VolumeInterpolator { progress -> (log10(progress * 9 + 1)) }
        val Exponential = VolumeInterpolator { progress -> progress * progress }
    }
}

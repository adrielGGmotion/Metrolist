package com.metrolist.music.playback

import kotlin.math.log10

data class CrossfadeConfig(
    val isEnabled: Boolean = false,
    val triggerPosition: Int = 0,
    val fadeDuration: Int = 0,
    val curve: CrossfadeCurve = CrossfadeCurve.Linear,
    val isAutomatic: Boolean = false
)

enum class CrossfadeCurve(val interpolator: VolumeInterpolator) {
    Linear(VolumeInterpolator.Linear),
    Logarithmic(VolumeInterpolator.Logarithmic),
    Exponential(VolumeInterpolator.Exponential)
}

fun interface VolumeInterpolator {
    fun transform(progress: Float): Float

    companion object {
        val Linear = VolumeInterpolator { progress -> progress }
        val Logarithmic = VolumeInterpolator { progress -> (log10(progress * 9 + 1)) }
        val Exponential = VolumeInterpolator { progress -> progress * progress }
    }
}

package com.metrolist.music.playback

import kotlin.math.log10

data class CrossfadeConfig(
    val duration: Int = 0,
    val curve: VolumeInterpolator = Linear,
    val isEnabled: Boolean = false,
    val isAutomatic: Boolean = false
)

interface VolumeInterpolator {
    fun transform(value: Float): Float
}

object Linear : VolumeInterpolator {
    override fun transform(value: Float): Float = value
}

object Logarithmic : VolumeInterpolator {
    override fun transform(value: Float): Float = log10(value * 9 + 1)
}

object Exponential : VolumeInterpolator {
    override fun transform(value: Float): Float = value * value
}

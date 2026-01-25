package com.metrolist.music.eq


import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.eq.data.SavedEQProfile
import com.metrolist.music.eq.audio.CustomEqualizerAudioProcessor
import com.metrolist.music.eq.data.ParametricEQ
import timber.log.Timber
import java.util.Collections
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing custom EQ using ExoPlayer's AudioProcessor
 * Supports 10+ band Parametric EQ format (APO)
 *
 * Supports multiple concurrent processors (e.g. for crossfading)
 */
@Singleton
class EqualizerService @Inject constructor() {

    // Track multiple processors using WeakReference to avoid leaks
    private val audioProcessors = Collections.newSetFromMap(WeakHashMap<CustomEqualizerAudioProcessor, Boolean>())

    private var activeProfile: SavedEQProfile? = null
    private var isEnabled: Boolean = true // Default state (or track disabled explicit)

    companion object {
        private const val TAG = "EqualizerService"
    }

    /**
     * Add an audio processor instance to be managed
     * This should be called when a new ExoPlayer instance is created
     */
    @OptIn(UnstableApi::class)
    fun addAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        audioProcessors.add(processor)
        Timber.tag(TAG).d("Added audio processor. Count: ${audioProcessors.size}")

        // Sync state to the new processor
        if (!isEnabled) {
            processor.disable()
        } else if (activeProfile != null) {
            applyProfileToProcessor(processor, activeProfile!!)
        }
    }

    /**
     * Legacy method for single-player support, redirects to addAudioProcessor
     */
    @OptIn(UnstableApi::class)
    fun setAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        addAudioProcessor(processor)
    }

    /**
     * Apply an EQ profile to all managed processors
     */
    @OptIn(UnstableApi::class)
    fun applyProfile(profile: SavedEQProfile): Result<Unit> {
        activeProfile = profile
        isEnabled = true

        var anyFailed = false
        var failureException: Exception? = null

        // Apply to all tracked processors
        val iterator = audioProcessors.iterator()
        while (iterator.hasNext()) {
            val processor = iterator.next()
            try {
                applyProfileToProcessor(processor, profile)
            } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to apply profile to a processor: ${e.message}")
                anyFailed = true
                failureException = e
            }
        }

        if (anyFailed && failureException != null) {
             return Result.failure(failureException)
        }

        Timber.tag(TAG).d("Applied EQ profile globally: ${profile.name}")
        return Result.success(Unit)
    }

    @OptIn(UnstableApi::class)
    private fun applyProfileToProcessor(processor: CustomEqualizerAudioProcessor, profile: SavedEQProfile) {
        val parametricEQ = ParametricEQ(
            preamp = profile.preamp,
            bands = profile.bands
        )
        processor.applyProfile(parametricEQ)
    }

    /**
     * Disable the equalizer (flat response) on all processors
     */
    @OptIn(UnstableApi::class)
    fun disable() {
        isEnabled = false
        activeProfile = null

        val iterator = audioProcessors.iterator()
        while (iterator.hasNext()) {
            val processor = iterator.next()
            try {
                processor.disable()
            } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to disable equalizer on processor: ${e.message}")
            }
        }
        Timber.tag(TAG).d("Equalizer disabled globally")
    }

    /**
     * Check if at least one processor is tracked
     */
    fun isInitialized(): Boolean {
        return !audioProcessors.isEmpty()
    }

    /**
     * Check if equalizer is enabled (based on service state)
     */
    @OptIn(UnstableApi::class)
    fun isEnabled(): Boolean {
        return isEnabled
    }

    /**
     * Get information about the current EQ capabilities
     */
    fun getEqualizerInfo(): EqualizerInfo {
        return EqualizerInfo(
            supportsUnlimitedBands = true,
            maxBands = Int.MAX_VALUE,
            description = "Custom ExoPlayer AudioProcessor with biquad filters"
        )
    }

    /**
     * Release resources
     */
    fun release() {
        audioProcessors.clear()
        Timber.tag(TAG).d("All audio processor references cleared")
    }
}

/**
 * Information about equalizer capabilities
 */
data class EqualizerInfo(
    val supportsUnlimitedBands: Boolean,
    val maxBands: Int,
    val description: String
)

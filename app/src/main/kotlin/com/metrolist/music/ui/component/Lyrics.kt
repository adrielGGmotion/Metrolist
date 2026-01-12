/**
 * Metrolist Project (C) 2024
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Shadow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsRomanizeBelarusianKey
import com.metrolist.music.constants.LyricsRomanizeBulgarianKey
import com.metrolist.music.constants.LyricsRomanizeCyrillicByLineKey
import com.metrolist.music.constants.LyricsGlowEffectKey
import com.metrolist.music.constants.LyricsHigherAnchorKey
import com.metrolist.music.constants.LyricsStandbyEffectKey
import com.metrolist.music.constants.LyricsAnimationStyle
import com.metrolist.music.constants.LyricsAnimationStyleKey
import com.metrolist.music.constants.LyricsTextSizeKey
import com.metrolist.music.constants.LyricsLineSpacingKey
import com.metrolist.music.constants.LyricsRomanizeChineseKey
import com.metrolist.music.constants.LyricsRomanizeJapaneseKey
import com.metrolist.music.constants.LyricsRomanizeKoreanKey
import com.metrolist.music.constants.LyricsRomanizeKyrgyzKey
import com.metrolist.music.constants.LyricsRomanizeRussianKey
import com.metrolist.music.constants.LyricsRomanizeSerbianKey
import com.metrolist.music.constants.LyricsRomanizeUkrainianKey
import com.metrolist.music.constants.LyricsRomanizeMacedonianKey
import com.metrolist.music.constants.LyricsScrollKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.lyrics.AppleMusicLyricsParser
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.WordTimestamp
import com.metrolist.music.lyrics.LyricLine
import com.metrolist.music.lyrics.SpeakerRole
import com.metrolist.music.lyrics.LyricsUtils.findCurrentLineIndex
import com.metrolist.music.lyrics.LyricsUtils.isBelarusian
import com.metrolist.music.lyrics.LyricsUtils.isChinese
import com.metrolist.music.lyrics.LyricsUtils.isJapanese
import com.metrolist.music.lyrics.LyricsUtils.isKorean
import com.metrolist.music.lyrics.LyricsUtils.isKyrgyz
import com.metrolist.music.lyrics.LyricsUtils.isRussian
import com.metrolist.music.lyrics.LyricsUtils.isSerbian
import com.metrolist.music.lyrics.LyricsUtils.isBulgarian
import com.metrolist.music.lyrics.LyricsUtils.isUkrainian
import com.metrolist.music.lyrics.LyricsUtils.isMacedonian
import com.metrolist.music.lyrics.LyricsUtils.parseLyrics
import com.metrolist.music.lyrics.LyricsUtils.romanizeCyrillic
import com.metrolist.music.lyrics.LyricsUtils.romanizeJapanese
import com.metrolist.music.lyrics.LyricsUtils.romanizeKorean
import com.metrolist.music.lyrics.LyricsUtils.romanizeChinese
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.utils.ComposeToImage
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

private const val MAX_GLOW_RADIUS = 30f
private const val GLOW_ALPHA_DIVISOR = 40f

private fun calculateTargetBlur(
    isScrolling: Boolean,
    currentIndex: Int,
    activeLineIndices: Set<Int>,
    focalPoint: Int,
    isSynced: Boolean,
    stepBlur: Dp,
    maxBlur: Dp
): Dp {
    // Blur should disappear when scrolling or when lyrics aren't synced
    if (isScrolling || !isSynced) {
        return 0.dp
    }

    if (activeLineIndices.contains(currentIndex)) {
        return 0.dp
    }

    val delta = if (activeLineIndices.isNotEmpty()) {
        activeLineIndices.minOf { activeIndex -> abs(currentIndex - activeIndex) }
    } else {
        abs(currentIndex - focalPoint)
    }

    if (delta == 0) return 0.dp
    
    // Use exponential falloff for smoother blur transition
    // This prevents the "square" artifact by creating a more natural blur gradient
    val linearBlur = (stepBlur * delta).coerceAtMost(maxBlur)
    val normalizedDistance = (linearBlur.value / maxBlur.value).coerceIn(0f, 1f)
    
    // Apply ease-out curve for smoother transition
    val easedDistance = 1f - (1f - normalizedDistance) * (1f - normalizedDistance)
    
    return (maxBlur.value * easedDistance).dp
}

@Composable
private fun rememberAnimatedBlur(
    lazyListState: LazyListState,
    currentIndex: Int,
    activeLineIndices: Set<Int>,
    blurFocalPoint: Int,
    isSynced: Boolean,
    stepBlur: Dp,
    maxBlur: Dp,
    animationSpec: AnimationSpec<Dp>,
    isAnimating: Boolean
): Dp {
    val isUserScrolling = lazyListState.isScrollInProgress && !isAnimating

    val targetBlur = calculateTargetBlur(
        isScrolling = isUserScrolling,
        currentIndex = currentIndex,
        activeLineIndices = activeLineIndices,
        focalPoint = blurFocalPoint,
        isSynced = isSynced,
        stepBlur = stepBlur,
        maxBlur = maxBlur
    )

    val animatedBlur by animateDpAsState(
        targetValue = targetBlur,
        animationSpec = animationSpec,
        label = "blur"
    )

    return animatedBlur
}

private sealed class LyricsContent {
    data class Standard(val lines: List<LyricsEntry>) : LyricsContent()
    data class Hierarchical(val lines: List<LyricLine>) : LyricsContent()
    object Empty : LyricsContent()
    object NotFound : LyricsContent()
}

private const val STANDBY_THRESHOLD_MS = 3000L
private const val STANDBY_HIDE_BEFORE_MS = 400L

/**
 * Data class to hold standby indicator state
 */
private data class StandbyState(
    val isVisible: Boolean = false,
    val progress: Float = 0f,
    val insertAfterIndex: Int = -1
)

/**
 * Calculates the standby state based on current playback position and lyrics lines
 */
@Composable
private fun rememberStandbyState(
    lines: List<LyricLine>,
    currentPosition: Long,
    activeLineIndices: Set<Int>
): StandbyState {
    if (lines.isEmpty()) return StandbyState()
    
    // Find the next line that will become active
    val nextLineIndex = lines.indexOfFirst { line ->
        val startTimeMs = (line.startTime * 1000).toLong()
        startTimeMs > currentPosition
    }
    
    if (nextLineIndex == -1) return StandbyState()
    
    val nextLine = lines[nextLineIndex]
    val nextLineStartMs = (nextLine.startTime * 1000).toLong()
    val timeUntilNextLine = nextLineStartMs - currentPosition
    
    // Find the previous line (the one that just ended or is about to end)
    val prevLineIndex = if (nextLineIndex > 0) nextLineIndex - 1 else -1
    val prevLineEndMs = if (prevLineIndex >= 0) {
        (lines[prevLineIndex].endTime * 1000).toLong()
    } else {
        0L
    }
    
    // Calculate the gap between previous line end and next line start
    val gapDuration = nextLineStartMs - prevLineEndMs
    
    // Only show standby if:
    // 1. No active lines currently
    // 2. Gap is more than threshold
    // 3. We're past the previous line's end time
    // 4. We're more than STANDBY_HIDE_BEFORE_MS away from next line
    val shouldShow = activeLineIndices.isEmpty() &&
            gapDuration > STANDBY_THRESHOLD_MS &&
            currentPosition >= prevLineEndMs &&
            timeUntilNextLine > STANDBY_HIDE_BEFORE_MS
    
    if (!shouldShow) return StandbyState()
    
    // Calculate progress (0 to 1) within the standby period
    val standbyDuration = gapDuration - STANDBY_HIDE_BEFORE_MS
    val timeInStandby = currentPosition - prevLineEndMs
    val progress = (timeInStandby.toFloat() / standbyDuration.toFloat()).coerceIn(0f, 1f)
    
    return StandbyState(
        isVisible = true,
        progress = progress,
        insertAfterIndex = prevLineIndex
    )
}

/**
 * Standby indicator composable that shows a wavy progress indicator during long intervals
 * with smooth space-opening animation and graceful exit effect
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StandbyIndicator(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Gentle bounce animation using Animatable
    val bounceOffset = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }
    
    LaunchedEffect(Unit) {
        // Bounce animation loop
        launch {
            while (true) {
                bounceOffset.animateTo(
                    targetValue = -8f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
                bounceOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
            }
        }
        // Pulse scale animation loop
        launch {
            while (true) {
                pulseScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(900, easing = FastOutSlowInEasing)
                )
                pulseScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(900, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .graphicsLayer {
                translationY = bounceOffset.value
                scaleX = pulseScale.value
                scaleY = pulseScale.value
            },
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(36.dp),
            color = color.copy(alpha = 0.85f),
            trackColor = color.copy(alpha = 0.15f),
            amplitude = WavyProgressIndicatorDefaults.indicatorAmplitude,
            wavelength = WavyProgressIndicatorDefaults.CircularWavelength,
            waveSpeed = WavyProgressIndicatorDefaults.CircularWavelength
        )
    }
}

@Composable
fun HierarchicalLyricsLine(
    line: LyricLine,
    isActive: Boolean,
    currentPosition: Long,
    textAlign: TextAlign,
    inactiveColor: Color,
    activeColor: Color,
    isBgLine: Boolean = false,
) {
    val lyricsGlowEffect by rememberPreference(LyricsGlowEffectKey, false)
    val textMeasurer = rememberTextMeasurer()
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    
    // BG lines are smaller
    val effectiveFontSize = if (isBgLine) lyricsTextSize * 0.7f else lyricsTextSize

    val textStyle = TextStyle(
        fontSize = effectiveFontSize.sp,
        fontWeight = if (isBgLine) FontWeight.Medium else FontWeight.Bold,
        textAlign = textAlign,
        lineHeight = (effectiveFontSize * lyricsLineSpacing).sp,
    )


    // Use raw playback position for smoother animation
    val activeWordIndex = if (isActive) {
        line.words.indexOfLast { (it.startTime * 1000) <= currentPosition }
    } else {
        val lineEndTime = (line.endTime * 1000f)
        if (currentPosition > lineEndTime) line.words.lastIndex else -1
    }


    val activeWord = line.words.getOrNull(activeWordIndex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {


        if (lyricsGlowEffect) {
             // Identify words that deserve "High Intensity" glow
             val highIntensityWords = remember(line.words) {
                 val highIntensitySet = mutableSetOf<com.metrolist.music.lyrics.Word>()
                 val chains = mutableListOf<MutableList<com.metrolist.music.lyrics.Word>>()
                 var currentChain: MutableList<com.metrolist.music.lyrics.Word>? = null

                 for (word in line.words) {
                     // Individual long words always get a glow
                     val wordDuration = (word.endTime * 1000) - (word.startTime * 1000)
                     if (wordDuration >= 1700) {
                         highIntensitySet.add(word)
                     }

                     if (currentChain == null) {
                         if (word.text.endsWith("-")) {
                             currentChain = mutableListOf(word)
                             chains.add(currentChain)
                         }
                     } else {
                         currentChain.add(word)
                         if (!word.text.endsWith("-")) {
                             currentChain = null
                         }
                     }
                 }

                 for (chain in chains) {
                     val chainStartMs = (chain.first().startTime * 1000).toLong()
                     val chainEndMs = (chain.last().endTime * 1000).toLong()
                     val chainDuration = chainEndMs - chainStartMs

                     if (chainDuration >= 1700) {
                         highIntensitySet.add(chain.last())
                     }
                 }

                 highIntensitySet
             }

             val glowingText = buildAnnotatedString {
                line.words.forEach { word ->
                    val wordStartMs = (word.startTime * 1000).toLong()
                    val wordEndMs = (word.endTime * 1000).toLong()
                    val wordDuration = wordEndMs - wordStartMs

                    // Determine intensity
                    val isHighIntensity = highIntensityWords.contains(word)
                    
                    // Config based on intensity
                    val maxAlpha = if (isHighIntensity) 1.0f else 0.6f
                    val maxRadius = if (isHighIntensity) MAX_GLOW_RADIUS else MAX_GLOW_RADIUS * 0.6f
                    val fadeInDuration = 200f // Fast attack for "bouncy" feel
                    val fadeOutDuration = if (isHighIntensity) 800f else 400f
                    
                    val timeSinceStart = (currentPosition - wordStartMs).toFloat()
                    val timeSinceEnd = (currentPosition - wordEndMs).toFloat()

                    val rawAlpha = when {
                        // Before word: No glow
                        currentPosition < wordStartMs -> 0f
                        
                        // Fade In (Fast)
                        currentPosition in wordStartMs..wordEndMs -> {
                            val progress = (timeSinceStart / fadeInDuration).coerceIn(0f, 1f)
                            // "Bouncy" ease out (overshoot-like or just quadOut)
                            // Using QuadOut for snappiness: 1 - (1-x)^2
                            1f - (1f - progress) * (1f - progress)
                        }
                        
                        // Fade Out
                        currentPosition > wordEndMs -> {
                            val progress = (timeSinceEnd / fadeOutDuration).coerceIn(0f, 1f)
                            1f - progress
                        }
                        
                        else -> 0f
                    }
                    
                    val finalAlpha = rawAlpha * maxAlpha

                    if (finalAlpha > 0.01f) {
                         val glowShadow = Shadow(
                            color = activeColor.copy(alpha = (finalAlpha * 0.8f).coerceIn(0f, 1f)),
                            offset = Offset.Zero,
                            blurRadius = maxRadius * finalAlpha // Scale radius with alpha for "pulse" effect
                        )
                        withStyle(style = SpanStyle(shadow = glowShadow)) {
                            append(word.text)
                        }
                    } else {
                        append(word.text)
                    }
                }
            }
            Text(
                text = glowingText,
                style = textStyle.copy(color = Color.Transparent),
                modifier = Modifier.matchParentSize()
            )
        }

        Text(
            text = line.text,
            style = textStyle,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val measuredText = textMeasurer.measure(
                        text = AnnotatedString(line.text),
                        style = textStyle,
                        constraints = androidx.compose.ui.unit.Constraints.fixedWidth(size.width.toInt())
                    )

                    onDrawBehind {
                        drawText(
                            textLayoutResult = measuredText,
                            color = inactiveColor,
                        )

                        if (activeWordIndex != -1) {
                            val wordToProcess = activeWord ?: line.words.last()
                            val activeWordStartOffset = line.words.take(activeWordIndex).sumOf { it.text.length }

                            val wordProgress = if (isActive) {
                                val wordStartTime = (wordToProcess.startTime * 1000f)
                                val wordEndTime = (wordToProcess.endTime * 1000f)
                                val wordDuration = wordEndTime - wordStartTime
                                if (wordDuration > 0) {
                                    ((currentPosition - wordStartTime) / wordDuration).coerceIn(0f, 1f)
                                } else 1f
                            } else 1f

                            val wordProgressFloat = wordToProcess.text.length * wordProgress
                            val currentCharIndex = wordProgressFloat.toInt()
                            val subCharProgress = wordProgressFloat - currentCharIndex

                            val totalCharOffsetStart = activeWordStartOffset + currentCharIndex
                            val totalCharOffsetEnd = (totalCharOffsetStart + 1).coerceAtMost(measuredText.layoutInput.text.length)

                            val clipStart = measuredText.getHorizontalPosition(totalCharOffsetStart, true)
                            val clipEnd = measuredText.getHorizontalPosition(totalCharOffsetEnd, true)

                            val startLine = measuredText.getLineForOffset(totalCharOffsetStart)
                            val endLine = measuredText.getLineForOffset(totalCharOffsetEnd)

                            val horizontalClip = if (startLine != endLine || clipEnd < clipStart) {
                                clipStart
                            } else {
                                clipStart + (clipEnd - clipStart) * subCharProgress
                            }

                            if (horizontalClip > 0) {
                                val pathForClipping = androidx.compose.ui.graphics.Path()
                                val currentLineIndex = measuredText.getLineForOffset(totalCharOffsetStart)
                                for (i in 0 until currentLineIndex) {
                                    pathForClipping.addRect(Rect(0f, measuredText.getLineTop(i), size.width, measuredText.getLineBottom(i)))
                                }
                                pathForClipping.addRect(Rect(0f, measuredText.getLineTop(currentLineIndex), horizontalClip, measuredText.getLineBottom(currentLineIndex)))

                                drawContext.canvas.save()
                                drawContext.canvas.clipPath(pathForClipping)
                                drawText(
                                    textLayoutResult = measuredText,
                                    color = activeColor
                                )
                                drawContext.canvas.restore()
                            }
                        }
                    }
                }
        )
    }
}


@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current // Get configuration

    val landscapeOffset =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val romanizeJapaneseLyrics by rememberPreference(LyricsRomanizeJapaneseKey, true)
    val romanizeKoreanLyrics by rememberPreference(LyricsRomanizeKoreanKey, true)
    val romanizeRussianLyrics by rememberPreference(LyricsRomanizeRussianKey, true)
    val romanizeUkrainianLyrics by rememberPreference(LyricsRomanizeUkrainianKey, true)
    val romanizeSerbianLyrics by rememberPreference(LyricsRomanizeSerbianKey, true)
    val romanizeBulgarianLyrics by rememberPreference(LyricsRomanizeBulgarianKey, true)
    val romanizeBelarusianLyrics by rememberPreference(LyricsRomanizeBelarusianKey, true)
    val romanizeKyrgyzLyrics by rememberPreference(LyricsRomanizeKyrgyzKey, true)
    val romanizeMacedonianLyrics by rememberPreference(LyricsRomanizeMacedonianKey, true)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val romanizeChineseLyrics by rememberPreference(LyricsRomanizeChineseKey, true)
    val lyricsGlowEffect by rememberPreference(LyricsGlowEffectKey, false)
    val lyricsHigherAnchor by rememberPreference(LyricsHigherAnchorKey, false)
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.APPLE)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val lyricsContent: LyricsContent = remember(lyrics, lyricsAnimationStyle) {
        val hasAppleMusicFormat = lyrics?.contains("<[0-9]{2}:[0-9]{2}\\.[0-9]{2,3}>".toRegex()) == true
        when {
            lyrics == null -> LyricsContent.Empty
            lyrics == LYRICS_NOT_FOUND -> LyricsContent.NotFound
            hasAppleMusicFormat && lyricsAnimationStyle == LyricsAnimationStyle.APPLE_ENHANCED -> {
                val parsedLines = AppleMusicLyricsParser.parse(lyrics)
                LyricsContent.Hierarchical(parsedLines)
            }
            hasAppleMusicFormat && lyricsAnimationStyle != LyricsAnimationStyle.NONE -> {
                // Parse Apple Music format and convert to Standard with word timings
                // This allows other animation styles to work with Apple Music format
                val parsedLines = AppleMusicLyricsParser.parse(lyrics)
                val standardLines = parsedLines.map { line ->
                    val wordTimestamps = line.words.map { word ->
                        WordTimestamp(
                            text = word.text,
                            startTime = word.startTime.toDouble(),
                            endTime = word.endTime.toDouble()
                        )
                    }
                    LyricsEntry(
                        time = (line.startTime * 1000).toLong(),
                        text = line.text,
                        words = wordTimestamps
                    )
                }
                LyricsContent.Standard(listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + standardLines)
            }
            hasAppleMusicFormat && lyricsAnimationStyle == LyricsAnimationStyle.NONE -> {
                // For NONE style, parse Apple Music format and display words without effects
                val parsedLines = AppleMusicLyricsParser.parse(lyrics)
                val standardLines = parsedLines.map { line ->
                    LyricsEntry(
                        time = (line.startTime * 1000).toLong(),
                        text = line.text,
                        words = null // No word timings for NONE style - just display text
                    )
                }
                LyricsContent.Standard(listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + standardLines)
            }
            else -> {
                val parsedLines = parseLyrics(lyrics)
                if (lyrics.startsWith("[")) {
                    val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
                    val isUkrainianLyrics =
                        romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
                    val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
                    val isBulgarianLyrics =
                        romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
                    val isBelarusianLyrics =
                        romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
                    val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
                    val isMacedonianLyrics =
                        romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)

                    LyricsContent.Standard(
                        parsedLines.map { entry ->
                            val newEntry = LyricsEntry(entry.time, entry.text)
                            scope.launchRomanization(
                                entry = newEntry,
                                romanizeJapaneseLyrics = romanizeJapaneseLyrics,
                                romanizeKoreanLyrics = romanizeKoreanLyrics,
                                romanizeRussianLyrics = romanizeRussianLyrics,
                                isRussianLyrics = isRussianLyrics,
                                romanizeUkrainianLyrics = romanizeUkrainianLyrics,
                                isUkrainianLyrics = isUkrainianLyrics,
                                romanizeSerbianLyrics = romanizeSerbianLyrics,
                                isSerbianLyrics = isSerbianLyrics,
                                romanizeBulgarianLyrics = romanizeBulgarianLyrics,
                                isBulgarianLyrics = isBulgarianLyrics,
                                romanizeBelarusianLyrics = romanizeBelarusianLyrics,
                                isBelarusianLyrics = isBelarusianLyrics,
                                romanizeKyrgyzLyrics = romanizeKyrgyzLyrics,
                                isKyrgyzLyrics = isKyrgyzLyrics,
                                romanizeMacedonianLyrics = romanizeMacedonianLyrics,
                                isMacedonianLyrics = isMacedonianLyrics,
                                romanizeCyrillicByLine = romanizeCyrillicByLine,
                                romanizeChineseLyrics = romanizeChineseLyrics
                            )
                            newEntry
                        }
                            .let {
                                listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
                            }
                    )
                } else {
                    val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
                    val isUkrainianLyrics =
                        romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
                    val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
                    val isBulgarianLyrics =
                        romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
                    val isBelarusianLyrics =
                        romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
                    val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
                    val isMacedonianLyrics =
                        romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)
                    LyricsContent.Standard(
                        lyrics.lines().mapIndexed { index, line ->
                            val newEntry = LyricsEntry(index * 100L, line)
                            scope.launchRomanization(
                                entry = newEntry,
                                text = line,
                                romanizeJapaneseLyrics = romanizeJapaneseLyrics,
                                romanizeKoreanLyrics = romanizeKoreanLyrics,
                                romanizeRussianLyrics = romanizeRussianLyrics,
                                isRussianLyrics = isRussianLyrics,
                                romanizeUkrainianLyrics = romanizeUkrainianLyrics,
                                isUkrainianLyrics = isUkrainianLyrics,
                                romanizeSerbianLyrics = romanizeSerbianLyrics,
                                isSerbianLyrics = isSerbianLyrics,
                                romanizeBulgarianLyrics = romanizeBulgarianLyrics,
                                isBulgarianLyrics = isBulgarianLyrics,
                                romanizeBelarusianLyrics = romanizeBelarusianLyrics,
                                isBelarusianLyrics = isBelarusianLyrics,
                                romanizeKyrgyzLyrics = romanizeKyrgyzLyrics,
                                isKyrgyzLyrics = isKyrgyzLyrics,
                                romanizeMacedonianLyrics = romanizeMacedonianLyrics,
                                isMacedonianLyrics = isMacedonianLyrics,
                                romanizeCyrillicByLine = romanizeCyrillicByLine,
                                romanizeChineseLyrics = romanizeChineseLyrics
                            )
                            newEntry
                        }
                    )
                }
            }
        }
    }

    val isSynced = lyricsContent is LyricsContent.Standard && lyrics?.startsWith("[") == true || lyricsContent is LyricsContent.Hierarchical

    val stepBlur = 3.dp
    val maxBlur = 15.dp
    val blurAnimationSpec = tween<Dp>(400)

    // Use Material 3 expressive accents and keep glow/text colors unified
    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
            // For blur/gradient backgrounds, always use light colors regardless of theme
            Color.White
        }
    }
    val textColor = expressiveAccent

    var activeLineIndices by remember { mutableStateOf(emptySet<Int>()) }
    val currentLineIndex = remember(activeLineIndices) { activeLineIndices.maxOrNull() ?: -1 }
    val scrollTargetMinIndex = remember(activeLineIndices) { activeLineIndices.minOrNull() ?: -1 }
    val scrollTargetMaxIndex = remember(activeLineIndices) { activeLineIndices.maxOrNull() ?: -1 }
    val midpointIndex = remember(activeLineIndices) {
        val min = activeLineIndices.minOrNull()
        val max = activeLineIndices.maxOrNull()
        if (min != null && max != null) {
            ((min.toFloat() + max.toFloat()) / 2f).roundToInt()
        } else {
            -1
        }
    }

    var currentPlaybackPosition by remember {
        mutableLongStateOf(0L)
    }
    // Because LaunchedEffect has delay, which leads to inconsistent with current line color and scroll animation,
    // we use deferredCurrentLineIndex when user is scrolling
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var previousScrollTargetMinIndex by rememberSaveable { mutableIntStateOf(-1) }
    var previousScrollTargetMaxIndex by rememberSaveable { mutableIntStateOf(-1) }
    
    // Track last known valid line index for resync when no active line
    var lastKnownActiveLineIndex by rememberSaveable { mutableIntStateOf(0) }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }

    var initialScrollDone by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldScrollToFirstLine by rememberSaveable {
        mutableStateOf(true)
    }

    var isAppMinimized by rememberSaveable {
        mutableStateOf(false)
    }

    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    // State for multi-selection
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) } // State for showing max selection toast

    val lazyListState = rememberLazyListState()
    
    // Professional animation states for smooth Metrolist-style transitions
    var isAnimating by remember { mutableStateOf(false) }
    var currentScrollJob by remember { mutableStateOf<Job?>(null) }
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
    var blurFocalPoint by rememberSaveable { mutableIntStateOf(0) }
    // Track if we need to re-sync after seeking
    var pendingResync by remember { mutableStateOf(false) }

    // Handle back button press - close selection mode instead of exiting screen
    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    // Define max selection limit
    val maxSelectionLimit = 5

    // Show toast when max selection is reached
    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen on while lyrics are visible
    DisposableEffect(showLyrics) {
        val activity = context as? Activity
        if (showLyrics) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if(event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset state when lyrics change
    LaunchedEffect(lyricsContent) {
        isSelectionModeActive = false
        selectedIndices.clear()
        // Reset scroll-related state for new lyrics
        previousScrollTargetMinIndex = -1
        previousScrollTargetMaxIndex = -1
        initialScrollDone = false
        shouldScrollToFirstLine = true
        lastKnownActiveLineIndex = 0
        pendingResync = false
        // Cancel any ongoing scroll animation
        currentScrollJob?.cancel()
        currentScrollJob = null
        isAnimating = false
    }

    LaunchedEffect(midpointIndex) {
        if (midpointIndex != -1) {
            blurFocalPoint = midpointIndex
        }
    }

    LaunchedEffect(lyricsContent) {
        if (lyrics.isNullOrEmpty()) {
            activeLineIndices = emptySet()
            return@LaunchedEffect
        }
        while (isActive) {
            delay(8) // Faster update for word-by-word animation
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: playerConnection.player.currentPosition
            currentPlaybackPosition = position
            val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
            val adjustedPosition = position + lyricsOffset

            when (lyricsContent) {
                is LyricsContent.Standard -> {
                    val newIndex = findCurrentLineIndex(lyricsContent.lines, adjustedPosition)
                    activeLineIndices = if (newIndex != -1) setOf(newIndex) else emptySet()
                    // Track last known active line for resync
                    if (newIndex != -1) {
                        lastKnownActiveLineIndex = newIndex
                    }
                }
                is LyricsContent.Hierarchical -> {
                    val lines = lyricsContent.lines
                    val indices = lines.indices.filter { index ->
                        val line = lines[index]
                        val startTimeMs = (line.startTime * 1000).toLong()
                        val endTimeMs = (line.endTime * 1000).toLong()
                        adjustedPosition in startTimeMs until endTimeMs
                    }.toSet()
                    activeLineIndices = indices
                    // Track last known active line for resync
                    indices.maxOrNull()?.let { lastKnownActiveLineIndex = it }
                }
                else -> {
                    activeLineIndices = emptySet()
                }
            }
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    suspend fun performSmoothPageScroll(minIndex: Int, maxIndex: Int, duration: Int = 1500, forceScroll: Boolean = false) {
        // Guard against invalid indices to prevent crashes
        if (minIndex < 0 || maxIndex < 0) return
        // If already animating and not forcing, skip this scroll request
        // This prevents interrupting ongoing animations
        if (isAnimating && !forceScroll) return
        
        // Cancel any pending scroll job if forcing a new scroll
        if (forceScroll) {
            currentScrollJob?.cancel()
        }
        
        isAnimating = true
        try {
            val minItemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == minIndex }
            val maxItemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == maxIndex }

            if (minItemInfo != null && maxItemInfo != null) {
                // Both items are visible. We can calculate the true midpoint.
                val groupTop = minItemInfo.offset
                val groupBottom = maxItemInfo.offset + maxItemInfo.size
                val groupCenter = groupTop + (groupBottom - groupTop) / 2

                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                // Use 0.25f (higher) when lyricsHigherAnchor is enabled, otherwise 0.5f (centered)
                val anchorPosition = if (lyricsHigherAnchor) 0.25f else 0.5f
                val anchor = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight * anchorPosition)
                val offset = groupCenter - anchor

                if (abs(offset) > 1f) {
                    lazyListState.animateScrollBy(
                        value = offset,
                        animationSpec = tween(durationMillis = duration)
                    )
                }
            } else {
                // One or both items are not visible. We scroll to the midpoint index to bring them into view.
                val midpointIndex = ((minIndex.toFloat() + maxIndex.toFloat()) / 2f).roundToInt().coerceAtLeast(0)
                lazyListState.scrollToItem(midpointIndex)
                // After scrolling to item, do a smooth centering animation
                delay(50) // Small delay to let the layout settle
                val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == midpointIndex }
                if (itemInfo != null) {
                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                    val anchorPosition = if (lyricsHigherAnchor) 0.25f else 0.5f
                    val anchor = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight * anchorPosition)
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val offset = itemCenter - anchor
                    if (abs(offset) > 1f) {
                        lazyListState.animateScrollBy(
                            value = offset,
                            animationSpec = tween(durationMillis = duration / 2)
                        )
                    }
                }
            }
        } finally {
            isAnimating = false
        }
    }
    // Handle seeking: when user stops seeking, trigger a resync scroll
    LaunchedEffect(isSeeking) {
        if (!isSeeking && pendingResync && isAutoScrollEnabled && isSynced) {
            pendingResync = false
            delay(100) // Small delay to let position settle
            val targetIndex = if (currentLineIndex >= 0) currentLineIndex else lastKnownActiveLineIndex
            if (targetIndex >= 0) {
                // Cancel any existing scroll and start a new one
                currentScrollJob?.cancel()
                currentScrollJob = scope.launch {
                    performSmoothPageScroll(targetIndex, targetIndex, 500, forceScroll = true)
                }
            }
        } else if (isSeeking) {
            pendingResync = true
        }
    }

    LaunchedEffect(scrollTargetMinIndex, scrollTargetMaxIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
        if (!isSynced || !isAutoScrollEnabled) return@LaunchedEffect

        if (lyricsContent is LyricsContent.Hierarchical) {
            if (scrollTargetMinIndex == -1) return@LaunchedEffect

            if (scrollTargetMinIndex != previousScrollTargetMinIndex || scrollTargetMaxIndex != previousScrollTargetMaxIndex) {
                val lines = lyricsContent.lines
                val previousLine = lines.getOrNull(previousScrollTargetMaxIndex)
                val currentLine = lines.getOrNull(scrollTargetMinIndex)

                if (previousScrollTargetMinIndex == -1 && currentLine != null) {
                    currentScrollJob = scope.launch {
                        performSmoothPageScroll(scrollTargetMinIndex, scrollTargetMaxIndex, 800)
                    }
                } else if (previousLine != null && currentLine != null) {
                    val previousLineEndTimeMs = (previousLine.endTime * 1000).toLong()
                    val scrollThresholdTimeMs = (currentLine.startTime * 1000).toLong() + 200
                    val scheduledScrollTimeMs = maxOf(previousLineEndTimeMs, scrollThresholdTimeMs)
                    val delayDuration = scheduledScrollTimeMs - currentPlaybackPosition
                    
                    // Calculate the animation duration based on word duration
                    // Use shorter animation if the word is short to ensure it completes
                    val wordDuration = (currentLine.endTime - currentLine.startTime) * 1000
                    val animDuration = minOf(1500, maxOf(500, (wordDuration * 0.8f).toInt()))
                    
                    if (delayDuration > 0) {
                        delay(delayDuration)
                    }

                    val originalMin = scrollTargetMinIndex
                    val originalMax = scrollTargetMaxIndex
                    val currentMinAfterDelay = activeLineIndices.minOrNull() ?: -1
                    val currentMaxAfterDelay = activeLineIndices.maxOrNull() ?: -1
                    if (isActive && originalMin == currentMinAfterDelay && originalMax == currentMaxAfterDelay) {
                        currentScrollJob = scope.launch {
                            performSmoothPageScroll(originalMin, originalMax, animDuration)
                        }
                    }
                }
                previousScrollTargetMinIndex = scrollTargetMinIndex
                previousScrollTargetMaxIndex = scrollTargetMaxIndex
            }
        } else {
            // Standard lyrics only have one active line, so min=max=midpoint
            if (midpointIndex == -1) return@LaunchedEffect
            val previousMidpointIndex = ((previousScrollTargetMinIndex.toFloat() + previousScrollTargetMaxIndex.toFloat()) / 2f).roundToInt()

            if ((midpointIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
                shouldScrollToFirstLine = false
                currentScrollJob = scope.launch {
                    performSmoothPageScroll(midpointIndex, midpointIndex, 800)
                }
                if (!isAppMinimized) {
                    initialScrollDone = true
                }
            } else {
                deferredCurrentLineIndex = midpointIndex
                if (isSeeking) {
                    // Don't scroll during seeking, will resync after
                    pendingResync = true
                } else if ((lastPreviewTime == 0L || midpointIndex != previousMidpointIndex) && scrollLyrics) {
                    if (midpointIndex != previousMidpointIndex) {
                        currentScrollJob = scope.launch {
                            performSmoothPageScroll(midpointIndex, midpointIndex, 1500)
                        }
                    }
                }
            }
            if (midpointIndex > 0) {
                shouldScrollToFirstLine = true
            }
            previousScrollTargetMinIndex = scrollTargetMinIndex
            previousScrollTargetMaxIndex = scrollTargetMaxIndex
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        when (lyricsContent) {
            is LyricsContent.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(0.5f)
                    )
                }
            }
            is LyricsContent.Empty -> {
                ShimmerHost {
                    repeat(10) {
                        Box(
                            contentAlignment = when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> Alignment.CenterStart
                                LyricsPosition.CENTER -> Alignment.Center
                                LyricsPosition.RIGHT -> Alignment.CenterEnd
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                        ) {
                            TextPlaceholder()
                        }
                    }
                }
            }
            is LyricsContent.Hierarchical -> {
                val lines = (lyricsContent as LyricsContent.Hierarchical).lines
                var lastPrimarySpeaker: SpeakerRole by remember { mutableStateOf(SpeakerRole.V1) }
                val hasV2 = remember(lines) { lines.any { it.speaker is SpeakerRole.V2 } }
                
                // Standby effect state
                val lyricsStandbyEffect by rememberPreference(LyricsStandbyEffectKey, false)
                val standbyState = rememberStandbyState(
                    lines = lines,
                    currentPosition = currentPlaybackPosition,
                    activeLineIndices = activeLineIndices
                )

                LazyColumn(
                    state = lazyListState,
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Top)
                        .add(WindowInsets(top = maxHeight / 3, bottom = maxHeight / 2))
                        .asPaddingValues(),
                    modifier = Modifier
                        .fadingEdge(vertical = 64.dp)
                        .nestedScroll(remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    if (source == NestedScrollSource.UserInput) {
                                        isAutoScrollEnabled = false
                                        // Cancel any ongoing auto-scroll animation
                                        currentScrollJob?.cancel()
                                        isAnimating = false
                                    }
                                    if (!isSelectionModeActive) { // Only update preview time if not selecting
                                        lastPreviewTime = System.currentTimeMillis()
                                    }
                                    return super.onPostScroll(consumed, available, source)
                                }

                                override suspend fun onPostFling(
                                    consumed: Velocity,
                                    available: Velocity
                                ): Velocity {
                                    isAutoScrollEnabled = false
                                    // Cancel any ongoing auto-scroll animation
                                    currentScrollJob?.cancel()
                                    isAnimating = false
                                    if (!isSelectionModeActive) { // Only update preview time if not selecting
                                        lastPreviewTime = System.currentTimeMillis()
                                    }
                                    return super.onPostFling(consumed, available)
                                }
                            }
                        })
                ) {
                    itemsIndexed(lines, key = { index, item -> "$index-${item.startTime}" }) { index, line ->
                        // Show standby indicator after the specified line
                        val showStandbyHere = lyricsStandbyEffect && 
                            standbyState.isVisible && 
                            standbyState.insertAfterIndex == index - 1
                        
                        AnimatedVisibility(
                            visible = showStandbyHere,
                            enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                                    expandVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        expandFrom = Alignment.CenterVertically
                                    ) +
                                    scaleIn(
                                        initialScale = 0.6f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ),
                            exit = scaleOut(
                                        targetScale = 1.2f,
                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                    ) +
                                    fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                    shrinkVertically(
                                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                                        shrinkTowards = Alignment.CenterVertically
                                    )
                        ) {
                            StandbyIndicator(
                                progress = standbyState.progress,
                                color = expressiveAccent
                            )
                        }
                        val isBgLine = line.speaker is SpeakerRole.BG
                        
                        // For BG lines, use parentSpeaker for alignment; otherwise use the line's speaker
                        val effectiveSpeakerForAlignment = if (isBgLine) {
                            line.parentSpeaker ?: SpeakerRole.V1
                        } else {
                            line.speaker
                        }

                        val textAlign = when (effectiveSpeakerForAlignment) {
                            is SpeakerRole.V1 -> if (hasV2) TextAlign.End else when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> TextAlign.Left
                                LyricsPosition.CENTER -> TextAlign.Center
                                LyricsPosition.RIGHT -> TextAlign.Right
                            }
                            is SpeakerRole.V2 -> TextAlign.Start
                            else -> when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> TextAlign.Left
                                LyricsPosition.CENTER -> TextAlign.Center
                                LyricsPosition.RIGHT -> TextAlign.Right
                            }
                        }

                        val isActiveLine = activeLineIndices.contains(index)
                        val isSelected = selectedIndices.contains(index)
                        
                        // BG line visibility: visible when active or not yet finished
                        val bgStartTimeMs = (line.startTime * 1000).toLong()
                        val bgEndTimeMs = (line.endTime * 1000).toLong()
                        val isBgVisible = if (isBgLine) {
                            // BG is visible from slightly before start until end
                            currentPlaybackPosition >= (bgStartTimeMs - 300) && currentPlaybackPosition <= bgEndTimeMs
                        } else true
                        
                        // Animate BG visibility
                        val bgAlpha by animateFloatAsState(
                            targetValue = if (isBgVisible) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = if (isBgVisible) 200 else 300,
                                easing = FastOutSlowInEasing
                            ),
                            label = "bgAlpha"
                        )
                        
                        // Bounce scale for BG lines
                        val bgScale by animateFloatAsState(
                            targetValue = if (isBgLine && isActiveLine) 1f else if (isBgLine) 0.95f else 1f,
                            animationSpec = tween(
                                durationMillis = 250,
                                easing = FastOutSlowInEasing
                            ),
                            label = "bgScale"
                        )

                        // Only apply blur for Apple Music (Enhanced) style
                        val animatedBlur = if (lyricsAnimationStyle == LyricsAnimationStyle.APPLE_ENHANCED) {
                            rememberAnimatedBlur(
                                lazyListState = lazyListState,
                                currentIndex = index,
                                activeLineIndices = activeLineIndices,
                                blurFocalPoint = blurFocalPoint,
                                isSynced = true, // Hierarchical is always synced
                                stepBlur = stepBlur,
                                maxBlur = maxBlur,
                                animationSpec = blurAnimationSpec,
                                isAnimating = isAnimating
                            )
                        } else {
                            0.dp
                        }
                        
                        // For BG lines, animate height to create space dynamically
                        AnimatedVisibility(
                            visible = !isBgLine || isBgVisible,
                            enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(200)),
                            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isBgLine) {
                                            Modifier
                                                .graphicsLayer {
                                                    alpha = bgAlpha
                                                    scaleX = bgScale
                                                    scaleY = bgScale
                                                }
                                        } else Modifier
                                    )
                                    .blur(radius = animatedBlur)
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        enabled = true,
                                        onClick = {
                                            if (isSelectionModeActive) {
                                                if (isSelected) {
                                                    selectedIndices.remove(index)
                                                    if (selectedIndices.isEmpty()) {
                                                        isSelectionModeActive = false
                                                    }
                                                } else {
                                                    if (selectedIndices.size < maxSelectionLimit) {
                                                        selectedIndices.add(index)
                                                    } else {
                                                        showMaxSelectionToast = true
                                                    }
                                                }
                                            } else if (isSynced && changeLyrics) {
                                                playerConnection.player.seekTo((line.startTime * 1000).toLong())
                                                scope.launch {
                                                    lazyListState.animateScrollToItem(index = index)
                                                }
                                                lastPreviewTime = 0L
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionModeActive) {
                                                isSelectionModeActive = true
                                                selectedIndices.add(index)
                                            }
                                        }
                                    )
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                            ) {
                                HierarchicalLyricsLine(
                                    line = line,
                                    isActive = isActiveLine,
                                    currentPosition = currentPlaybackPosition,
                                    textAlign = textAlign,
                                    inactiveColor = expressiveAccent.copy(alpha = if (isBgLine) 0.4f else 0.5f),
                                    activeColor = expressiveAccent.copy(alpha = if (isBgLine) 0.85f else 1f),
                                    isBgLine = isBgLine,
                                )
                            }
                        }
                    }
                }
            }
            is LyricsContent.Standard -> {
                val lines = (lyricsContent as LyricsContent.Standard).lines
                LazyColumn(
                state = lazyListState,
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Top)
                    .add(WindowInsets(top = maxHeight / 3, bottom = maxHeight / 2))
                    .asPaddingValues(),
                modifier = Modifier
                    .fadingEdge(vertical = 64.dp)
                    .nestedScroll(remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (source == NestedScrollSource.UserInput) {
                                    isAutoScrollEnabled = false
                                    // Cancel any ongoing auto-scroll animation
                                    currentScrollJob?.cancel()
                                    isAnimating = false
                                }
                                if (!isSelectionModeActive) { // Only update preview time if not selecting
                                    lastPreviewTime = System.currentTimeMillis()
                                }
                                return super.onPostScroll(consumed, available, source)
                            }

                            override suspend fun onPostFling(
                                consumed: Velocity,
                                available: Velocity
                            ): Velocity {
                                isAutoScrollEnabled = false
                                // Cancel any ongoing auto-scroll animation
                                currentScrollJob?.cancel()
                                isAnimating = false
                                if (!isSelectionModeActive) { // Only update preview time if not selecting
                                    lastPreviewTime = System.currentTimeMillis()
                                }
                                return super.onPostFling(consumed, available)
                            }
                        }
                    })
            ) {
                val displayedCurrentLineIndex = if (!isAutoScrollEnabled) {
                    currentLineIndex
                } else {
                    if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex
                }

                itemsIndexed(
                    items = lines,
                    key = { index, item -> "$index-${item.time}" } // Add stable key
                ) { index, item ->
                    val isSelected = selectedIndices.contains(index)
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) // Clip for background
                        .combinedClickable(
                            enabled = true,
                            onClick = {
                                if (isSelectionModeActive) {
                                    // Toggle selection
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive =
                                                false // Exit mode if last item deselected
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && changeLyrics) {
                                    // Professional seek action with smooth animation
                                    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                    playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                    // Smooth slow scroll when clicking on lyrics (3 seconds)
                                    scope.launch {
                                        // First scroll to the clicked item without animation
                                        lazyListState.scrollToItem(index = index)

                                        // Then animate it to center position slowly
                                        val itemInfo =
                                            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight =
                                                lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center =
                                                lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center

                                            if (kotlin.math.abs(offset) > 10) { // Only animate if not already centered
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = tween(durationMillis = 1500) // Reduced to half speed
                                                )
                                            }
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    // If already in selection mode and item not selected, add it if below limit
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    // If already at limit, show toast
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        .background(
                            if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            )
                            else Color.Transparent
                        )
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                    
                    val alpha by animateFloatAsState(
                        targetValue = when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            index == displayedCurrentLineIndex -> 1f
                            else -> 0.5f
                        },
                        animationSpec = tween(durationMillis = 400)
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (index == displayedCurrentLineIndex) 1.05f else 1f,
                        animationSpec = tween(durationMillis = 400)
                    )

                    // Standard lyrics should not have blur effect
                    Column(
                        modifier = itemModifier
                            .graphicsLayer {
                                this.alpha = alpha
                                this.scaleX = scale
                            this.scaleY = scale
                        },
                        horizontalAlignment = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> Alignment.Start
                            LyricsPosition.CENTER -> Alignment.CenterHorizontally
                            LyricsPosition.RIGHT -> Alignment.End
                        }
                    ) {
                        val isActiveLine = index == displayedCurrentLineIndex && isSynced
                        val lineColor = if (isActiveLine) expressiveAccent else expressiveAccent.copy(alpha = 0.7f)
                        val alignment = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left
                            LyricsPosition.CENTER -> TextAlign.Center
                            LyricsPosition.RIGHT -> TextAlign.Right
                        }
                        
                        val hasWordTimings = item.words?.isNotEmpty() == true
                        
                        // Word-by-word animation styles
                        if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.NONE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && currentPlaybackPosition > wordEndMs

                                    val transitionProgress = when {
                                        !isActiveLine -> 0f
                                        hasWordPassed -> 1f
                                        isWordActive && wordDuration > 0 -> {
                                            val elapsed = currentPlaybackPosition - wordStartMs
                                            val linear = (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                            linear * linear * (3f - 2f * linear)
                                        }
                                        else -> 0f
                                    }

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.7f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.5f + (0.5f * transitionProgress)
                                        else -> 0.35f
                                    }

                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Medium
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1 && !word.text.endsWith(" ")) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.FADE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && currentPlaybackPosition > wordEndMs

                                    val fadeProgress = if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = currentPlaybackPosition - wordStartMs
                                        val linear = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        // Smooth cubic easing
                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) 1f else 0f

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.55f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.4f + (0.6f * fadeProgress)
                                        else -> 0.4f
                                    }
                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Medium
                                    }
                                    // Enhanced shadow for active words
                                    val wordShadow = when {
                                        isWordActive && fadeProgress > 0.2f -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.35f * fadeProgress),
                                            offset = Offset.Zero,
                                            blurRadius = 10f * fadeProgress
                                        )
                                        hasWordPassed -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.15f),
                                            offset = Offset.Zero,
                                            blurRadius = 6f
                                        )
                                        else -> null
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1 && !word.text.endsWith(" ")) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.GLOW) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition in wordStartMs..wordEndMs
                                    val hasWordPassed = isActiveLine && currentPlaybackPosition > wordEndMs

                                    val fillProgress = if (isWordActive && wordDuration > 0) {
                                        val linear = ((currentPlaybackPosition - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) 1f else 0f

                                    val glowIntensity = fillProgress * fillProgress
                                    val brightness = 0.45f + (0.55f * fillProgress)

                                    val wordColor = when {
                                        !isActiveLine -> expressiveAccent.copy(alpha = 0.5f)
                                        isWordActive || hasWordPassed -> expressiveAccent.copy(alpha = brightness)
                                        else -> expressiveAccent.copy(alpha = 0.35f)
                                    }
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        hasWordPassed -> FontWeight.Bold
                                        else -> FontWeight.Medium
                                    }
                                    val wordShadow = if (isWordActive && glowIntensity > 0.05f) {
                                        Shadow(color = expressiveAccent.copy(alpha = 0.5f + (0.3f * glowIntensity)), offset = Offset.Zero, blurRadius = 16f + (12f * glowIntensity))
                                    } else if (hasWordPassed) {
                                        Shadow(color = expressiveAccent.copy(alpha = 0.25f), offset = Offset.Zero, blurRadius = 8f)
                                    } else null

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1 && !word.text.endsWith(" ")) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.SLIDE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && currentPlaybackPosition >= wordEndMs) || (!isActiveLine && index < displayedCurrentLineIndex)

                                    if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = currentPlaybackPosition - wordStartMs
                                        val fillProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        val breatheValue = (timeElapsed % 3000) / 3000f
                                        val breatheEffect = (kotlin.math.sin(breatheValue * Math.PI.toFloat() * 2f) * 0.03f).coerceIn(0f, 0.03f)
                                        val glowIntensity = (0.3f + fillProgress * 0.7f + breatheEffect).coerceIn(0f, 1.1f)

                                        val slideBrush = Brush.horizontalGradient(
                                            0.0f to expressiveAccent,
                                            (fillProgress * 0.95f).coerceIn(0f, 1f) to expressiveAccent,
                                            fillProgress to expressiveAccent.copy(alpha = 0.9f),
                                            (fillProgress + 0.02f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.5f),
                                            (fillProgress + 0.08f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.35f),
                                            1.0f to expressiveAccent.copy(alpha = 0.35f)
                                        )

                                        withStyle(style = SpanStyle(
                                            brush = slideBrush,
                                            fontWeight = FontWeight.ExtraBold,
                                            shadow = Shadow(color = expressiveAccent.copy(alpha = 0.4f * glowIntensity), offset = Offset(0f, 0f), blurRadius = 14f + (4f * fillProgress))
                                        )) {
                                            append(word.text)
                                        }
                                    } else if (hasWordPassed && isActiveLine) {
                                        withStyle(style = SpanStyle(
                                            color = expressiveAccent,
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(color = expressiveAccent.copy(alpha = 0.4f), offset = Offset(0f, 0f), blurRadius = 12f)
                                        )) {
                                            append(word.text)
                                        }
                                    } else {
                                        val wordColor = if (!isActiveLine) lineColor else expressiveAccent.copy(alpha = 0.35f)
                                        withStyle(style = SpanStyle(color = wordColor, fontWeight = FontWeight.Medium)) {
                                            append(word.text)
                                        }
                                    }
                                    if (wordIndex < item.words.size - 1 && !word.text.endsWith(" ")) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.KARAOKE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && currentPlaybackPosition >= wordEndMs) || (!isActiveLine && index < displayedCurrentLineIndex)

                                    if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = currentPlaybackPosition - wordStartMs
                                        val linearProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        // Smoother easing curve for more natural fill animation
                                        val fillProgress = linearProgress * linearProgress * (3f - 2f * linearProgress)
                                        
                                        // Enhanced glow intensity calculation
                                        val glowIntensity = fillProgress * fillProgress

                                        val wordBrush = Brush.horizontalGradient(
                                            0.0f to expressiveAccent.copy(alpha = 0.4f),
                                            (fillProgress * 0.6f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.75f),
                                            (fillProgress * 0.85f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.95f),
                                            fillProgress to expressiveAccent,
                                            (fillProgress + 0.03f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.85f),
                                            (fillProgress + 0.1f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.5f),
                                            1.0f to expressiveAccent.copy(alpha = if (fillProgress >= 0.9f) 0.95f else 0.4f)
                                        )

                                        // Improved shadow with better glow effect
                                        val wordShadow = Shadow(
                                            color = expressiveAccent.copy(alpha = 0.5f + (0.3f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 16f + (12f * glowIntensity)
                                        )

                                        withStyle(style = SpanStyle(
                                            brush = wordBrush,
                                            fontWeight = FontWeight.ExtraBold,
                                            shadow = wordShadow
                                        )) {
                                            append(word.text)
                                        }
                                    } else if (hasWordPassed && isActiveLine) {
                                        // Completed words with subtle glow
                                        withStyle(style = SpanStyle(
                                            color = expressiveAccent,
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(
                                                color = expressiveAccent.copy(alpha = 0.25f),
                                                offset = Offset.Zero,
                                                blurRadius = 8f
                                            )
                                        )) {
                                            append(word.text)
                                        }
                                    } else {
                                        // Inactive words
                                        val wordColor = if (!isActiveLine) lineColor else expressiveAccent.copy(alpha = 0.4f)
                                        withStyle(style = SpanStyle(color = wordColor, fontWeight = FontWeight.Medium)) {
                                            append(word.text)
                                        }
                                    }
                                    if (wordIndex < item.words.size - 1 && !word.text.endsWith(" ")) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.APPLE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && currentPlaybackPosition >= wordEndMs) || (!isActiveLine && index < displayedCurrentLineIndex)

                                    val rawProgress = if (isWordActive && wordDuration > 0) {
                                        val elapsed = currentPlaybackPosition - wordStartMs
                                        (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                    } else if (hasWordPassed) 1f else 0f

                                    // Smooth cubic easing for natural animation
                                    val smoothProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.55f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.55f + (0.45f * smoothProgress)
                                        else -> 0.4f
                                    }
                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.SemiBold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Normal
                                    }
                                    // Enhanced shadow with better glow intensity
                                    val glowIntensity = smoothProgress * smoothProgress
                                    val wordShadow = when {
                                        isWordActive -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.2f + (0.4f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 10f + (12f * glowIntensity)
                                        )
                                        hasWordPassed && isActiveLine -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.2f),
                                            offset = Offset.Zero,
                                            blurRadius = 8f
                                        )
                                        else -> null
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1 && !word.text.endsWith(" ")) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)
                        } else if (isActiveLine && lyricsGlowEffect) {
                            // Active line with glow effect - animated glow fill with bounce
                            val fillProgress = remember { Animatable(0f) }
                            val pulseProgress = remember { Animatable(0f) }

                            LaunchedEffect(index) {
                                fillProgress.snapTo(0f)
                                fillProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 1200,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    pulseProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 3000,
                                            easing = LinearEasing
                                        )
                                    )
                                    pulseProgress.snapTo(0f)
                                }
                            }

                            val fill = fillProgress.value
                            val pulse = pulseProgress.value

                            val pulseEffect = (kotlin.math.sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f)
                            val glowIntensity = (fill + pulseEffect).coerceIn(0f, 1.2f)

                            val glowBrush = Brush.horizontalGradient(
                                0.0f to expressiveAccent.copy(alpha = 0.3f),
                                (fill * 0.7f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.9f),
                                fill to expressiveAccent,
                                (fill + 0.1f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.7f),
                                1.0f to expressiveAccent.copy(alpha = if (fill >= 1f) 1f else 0.3f)
                            )

                            val styledText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        shadow = Shadow(
                                            color = expressiveAccent.copy(alpha = 0.8f * glowIntensity),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 28f * (1f + pulseEffect)
                                        ),
                                        brush = glowBrush
                                    )
                                ) {
                                    append(item.text)
                                }
                            }

                            // Bounce animation
                            val bounceScale = if (fill < 0.3f) {
                                1f + (kotlin.math.sin(fill * 3.33f * Math.PI.toFloat()) * 0.03f)
                            } else {
                                1f
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = bounceScale
                                        scaleY = bounceScale
                                    }
                            )
                        } else if (isActiveLine) {
                            // Active line without glow effect - just bold text
                            Text(
                                text = item.text,
                                fontSize = lyricsTextSize.sp,
                                color = expressiveAccent,
                                textAlign = alignment,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else {
                            // Inactive line
                            Text(
                                text = item.text,
                                fontSize = lyricsTextSize.sp,
                                color = lineColor,
                                textAlign = alignment,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        }
                        if (currentSong?.romanizeLyrics == true
                            && (romanizeJapaneseLyrics ||
                                    romanizeKoreanLyrics ||
                                    romanizeRussianLyrics ||
                                    romanizeUkrainianLyrics ||
                                    romanizeSerbianLyrics ||
                                    romanizeBulgarianLyrics ||
                                    romanizeBelarusianLyrics ||
                                    romanizeKyrgyzLyrics ||
                                    romanizeMacedonianLyrics ||
                                    romanizeChineseLyrics)) {
                            // Show romanized text if available
                            val romanizedText by item.romanizedTextFlow.collectAsState()
                            romanizedText?.let { romanized ->
                                Text(
                                    text = romanized,
                                    fontSize = 18.sp,
                                    color = expressiveAccent.copy(alpha = 0.6f),
                                    textAlign = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> TextAlign.Left
                                        LyricsPosition.CENTER -> TextAlign.Center
                                        LyricsPosition.RIGHT -> TextAlign.Right
                                    },
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FilledTonalButton(onClick = {
                // Enable auto-scroll first so the scroll animation works properly
                isAutoScrollEnabled = true
                scope.launch {
                    // Use last known active line if current line is -1 (no active line)
                    val targetIndex = if (currentLineIndex >= 0) currentLineIndex else lastKnownActiveLineIndex
                    // Force scroll to ensure it happens even if another animation is in progress
                    performSmoothPageScroll(targetIndex, targetIndex, 800, forceScroll = true)
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.sync),
                    contentDescription = stringResource(R.string.auto_scroll),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.auto_scroll))
            }
        }

        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        isSelectionModeActive = false
                        selectedIndices.clear()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalButton(
                    onClick = {
                        if (selectedIndices.isNotEmpty()) {
                            val sortedIndices = selectedIndices.sorted()
                            val content = lyricsContent
                            val selectedLyricsText = when (content) {
                                is LyricsContent.Standard -> sortedIndices
                                    .mapNotNull { content.lines.getOrNull(it)?.text }
                                    .joinToString("\n")
                                is LyricsContent.Hierarchical -> sortedIndices
                                    .mapNotNull { content.lines.getOrNull(it)?.text }
                                    .joinToString("\n")
                                else -> ""
                            }

                            if (selectedLyricsText.isNotBlank()) {
                                shareDialogData = Triple(
                                    selectedLyricsText,
                                    mediaMetadata?.title ?: "",
                                    mediaMetadata?.artists?.joinToString { it.name } ?: ""
                                )
                                showShareDialog = true
                            }
                            isSelectionModeActive = false
                            selectedIndices.clear()
                        }
                    },
                    enabled = selectedIndices.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(R.string.share_selected),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.share))
                }
            }
        }
        }
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* Don't dismiss */ }) {
            Card( // Use Card for better styling
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!! // Renamed 'lyrics' to 'lyricsText' for clarity
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Share as Text Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    val songLink =
                                        "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                    // Use the potentially multi-line lyricsText here
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.share_lyrics)
                                    )
                                )
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Share as Image Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Pass the potentially multi-line lyrics to the color picker
                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showColorPickerDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Cancel Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showShareDialog = false }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl
        val paletteColors = remember { mutableStateListOf<Color>() }

        val previewCardWidth = configuration.screenWidthDp.dp * 0.90f
        val previewPadding = 20.dp * 2
        val previewBoxPadding = 28.dp * 2
        val previewAvailableWidth = previewCardWidth - previewPadding - previewBoxPadding
        val previewBoxHeight = 340.dp
        val headerFooterEstimate = (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 28.dp * 2)
        val previewAvailableHeight = previewBoxHeight - headerFooterEstimate

        val textStyleForMeasurement = TextStyle(
            color = previewTextColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        val textMeasurer = rememberTextMeasurer()

        rememberAdjustedFontSize(
            text = lyricsText,
            maxWidth = previewAvailableWidth,
            maxHeight = previewAvailableHeight,
            density = density,
            initialFontSize = 50.sp,
            minFontSize = 22.sp,
            style = textStyleForMeasurement,
            textMeasurer = textMeasurer
        )

        LaunchedEffect(coverUrl) {
            if (coverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                        val result = loader.execute(req)
                        val bmp = result.image?.toBitmap()
                        if (bmp != null) {
                            val palette = Palette.from(bmp).generate()
                            val swatches = palette.swatches.sortedByDescending { it.population }
                            val colors = swatches.map { Color(it.rgb) }
                                .filter { color ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hsv[1] > 0.2f
                                }
                            paletteColors.clear()
                            paletteColors.addAll(colors.take(5))
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.customize_colors),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(8.dp)
                    ) {
                        LyricsImageCard(
                            lyricText = lyricsText,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            backgroundColor = previewBackgroundColor,
                            textColor = previewTextColor,
                            secondaryTextColor = previewSecondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(text = stringResource(id = R.string.background_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color(0xFF242424), Color(0xFF121212), Color.White, Color.Black, Color(0xFFF5F5F5))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewBackgroundColor = color }
                                    .border(
                                        2.dp,
                                        if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color.White, Color.Black, Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewTextColor = color }
                                    .border(
                                        2.dp,
                                        if (previewTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.secondary_text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors.map { it.copy(alpha = 0.7f) } + listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f), Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewSecondaryTextColor = color }
                                    .border(
                                        2.dp,
                                        if (previewSecondaryTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showColorPickerDialog = false
                            showProgressDialog = true
                            scope.launch {
                                try {
                                    val screenWidth = configuration.screenWidthDp
                                    val screenHeight = configuration.screenHeightDp

                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = coverUrl,
                                        songTitle = songTitle,
                                        artistName = artists,
                                        lyrics = lyricsText,
                                        width = (screenWidth * density.density).toInt(),
                                        height = (screenHeight * density.density).toInt(),
                                        backgroundColor = previewBackgroundColor.toArgb(),
                                        textColor = previewTextColor.toArgb(),
                                        secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                    )
                                    val timestamp = System.currentTimeMillis()
                                    val filename = "lyrics_$timestamp"
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.failed_to_create_image, e.message), Toast.LENGTH_SHORT).show()
                                } finally {
                                    showProgressDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.share))
                    }
                }
            }
        }
    }
}

// Professional page animation constants inspired by Metrolist design - slower for smoothness
private const val METROLIST_AUTO_SCROLL_DURATION = 1500L // Much slower auto-scroll for smooth transitions
private const val METROLIST_INITIAL_SCROLL_DURATION = 1000L // Slower initial positioning
private const val METROLIST_SEEK_DURATION = 800L // Slower user interaction
private const val METROLIST_FAST_SEEK_DURATION = 600L // Less aggressive seeking

// Lyrics constants
val LyricsPreviewTime = 2.seconds

private fun CoroutineScope.launchRomanization(
    entry: LyricsEntry,
    text: String = entry.text,
    romanizeJapaneseLyrics: Boolean,
    romanizeKoreanLyrics: Boolean,
    romanizeRussianLyrics: Boolean,
    isRussianLyrics: Boolean,
    romanizeUkrainianLyrics: Boolean,
    isUkrainianLyrics: Boolean,
    romanizeSerbianLyrics: Boolean,
    isSerbianLyrics: Boolean,
    romanizeBulgarianLyrics: Boolean,
    isBulgarianLyrics: Boolean,
    romanizeBelarusianLyrics: Boolean,
    isBelarusianLyrics: Boolean,
    romanizeKyrgyzLyrics: Boolean,
    isKyrgyzLyrics: Boolean,
    romanizeMacedonianLyrics: Boolean,
    isMacedonianLyrics: Boolean,
    romanizeCyrillicByLine: Boolean,
    romanizeChineseLyrics: Boolean
) {
    if (romanizeJapaneseLyrics && isJapanese(text) && !isChinese(text)) {
        launch { entry.romanizedTextFlow.value = romanizeJapanese(text) }
        return
    }
    if (romanizeKoreanLyrics && isKorean(text)) {
        launch { entry.romanizedTextFlow.value = romanizeKorean(text) }
        return
    }
    if (romanizeChineseLyrics && isChinese(text)) {
        launch { entry.romanizedTextFlow.value = romanizeChinese(text) }
        return
    }

    val isCyrillic = when {
        romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(text) else isRussianLyrics) -> true
        romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(text) else isUkrainianLyrics) -> true
        romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(text) else isSerbianLyrics) -> true
        romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(text) else isBulgarianLyrics) -> true
        romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(text) else isBelarusianLyrics) -> true
        romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(text) else isKyrgyzLyrics) -> true
        romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(text) else isMacedonianLyrics) -> true
        else -> false
    }

    if (isCyrillic) {
        launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
    }
}

/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.text.Layout
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AiProviderKey
import com.metrolist.music.constants.AiSystemPromptKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DeeplApiKey
import com.metrolist.music.constants.DeeplFormalityKey
import com.metrolist.music.constants.LyricsAnimationStyleKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsGlowEffectKey
import com.metrolist.music.constants.LyricsLineSpacingKey
import com.metrolist.music.constants.LyricsRomanizeAsMainKey
import com.metrolist.music.constants.LyricsRomanizeChineseKey
import com.metrolist.music.constants.LyricsRomanizeCyrillicByLineKey
import com.metrolist.music.constants.LyricsRomanizeHindiKey
import com.metrolist.music.constants.LyricsRomanizeJapaneseKey
import com.metrolist.music.constants.LyricsRomanizeKoreanKey
import com.metrolist.music.constants.LyricsRomanizeKyrgyzKey
import com.metrolist.music.constants.LyricsRomanizeList
import com.metrolist.music.constants.LyricsRomanizeMacedonianKey
import com.metrolist.music.constants.LyricsRomanizePunjabiKey
import com.metrolist.music.constants.LyricsRomanizeRussianKey
import com.metrolist.music.constants.LyricsRomanizeSerbianKey
import com.metrolist.music.constants.LyricsRomanizeUkrainianKey
import com.metrolist.music.constants.LyricsRomanizeBulgarianKey
import com.metrolist.music.constants.LyricsRomanizeBelarusianKey
import com.metrolist.music.constants.LyricsScrollKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.LyricsTextSizeKey
import com.metrolist.music.constants.OpenRouterApiKey
import com.metrolist.music.constants.OpenRouterBaseUrlKey
import com.metrolist.music.constants.OpenRouterModelKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.TranslateLanguageKey
import com.metrolist.music.constants.TranslateModeKey
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsTranslationHelper
import com.metrolist.music.lyrics.LyricsUtils.findActiveLineIndices
import com.metrolist.music.lyrics.LyricsUtils.isBelarusian
import com.metrolist.music.lyrics.LyricsUtils.isBulgarian
import com.metrolist.music.lyrics.LyricsUtils.isChinese
import com.metrolist.music.lyrics.LyricsUtils.isHindi
import com.metrolist.music.lyrics.LyricsUtils.isJapanese
import com.metrolist.music.lyrics.LyricsUtils.isKorean
import com.metrolist.music.lyrics.LyricsUtils.isKyrgyz
import com.metrolist.music.lyrics.LyricsUtils.isMacedonian
import com.metrolist.music.lyrics.LyricsUtils.isPunjabi
import com.metrolist.music.lyrics.LyricsUtils.isRussian
import com.metrolist.music.lyrics.LyricsUtils.isSerbian
import com.metrolist.music.lyrics.LyricsUtils.isUkrainian
import com.metrolist.music.lyrics.LyricsUtils.parseLyrics
import com.metrolist.music.lyrics.LyricsUtils.romanizeChinese
import com.metrolist.music.lyrics.LyricsUtils.romanizeCyrillic
import com.metrolist.music.lyrics.LyricsUtils.romanizeHindi
import com.metrolist.music.lyrics.LyricsUtils.romanizeJapanese
import com.metrolist.music.lyrics.LyricsUtils.romanizeKorean
import com.metrolist.music.lyrics.LyricsUtils.romanizePunjabi
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.ui.screens.settings.defaultList
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.utils.ComposeToImage
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

private sealed class LyricsListItem {
    data class Line(val index: Int, val entry: com.metrolist.music.lyrics.LyricsEntry) : LyricsListItem()
    data class Indicator(val afterLineIndex: Int, val gapMs: Long, val gapStartMs: Long, val gapEndMs: Long, val nextAgent: String?) : LyricsListItem()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IntervalIndicator(
    gapStartMs: Long,
    gapEndMs: Long,
    currentPositionMs: Long,
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val rowHeightPx = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rowHeightPx.animateTo(1f, tween(200))
            alpha.animateTo(1f, tween(200))
        } else {
            alpha.animateTo(0f, tween(200))
            rowHeightPx.animateTo(0f, tween(200))
        }
    }

    val density = LocalDensity.current
    val targetHeightDp = with(density) { (rowHeightPx.value * 72).dp }

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "intervalProgress"
    )

    Box(
        modifier = modifier
            .height(targetHeightDp)
            .padding(top = if (rowHeightPx.value > 0f) 16.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .size(36.dp)
                .alpha(alpha.value),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val romanizeLyricsList = rememberPreference(LyricsRomanizeList, "")
    val romanizeAsMain by rememberPreference(LyricsRomanizeAsMainKey, false)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val lyricsGlowEffect by rememberPreference(LyricsGlowEffectKey, false)
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.APPLE)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val deeplApiKey by rememberPreference(DeeplApiKey, "")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel by rememberPreference(OpenRouterModelKey, "google/gemini-2.5-flash-lite")
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    val deeplFormality by rememberPreference(DeeplFormalityKey, "default")
    val aiSystemPrompt by rememberPreference(AiSystemPromptKey, "")
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

    val decodedList =
        if (romanizeLyricsList.value.isEmpty()) {
            defaultList
        } else {
            romanizeLyricsList.value.split(",").map { entry ->
                val (lang, checked) = entry.split(":")
                Pair(lang, checked.toBoolean())
            }
        }

    val enabledLanguages = decodedList.filter { (_, checked) -> checked }.map { (lang, _) -> lang }

    val lines =
        remember(lyrics, scope) {
            if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                emptyList()
            } else if (lyrics.startsWith("[")) {
                val parsedLines = parseLyrics(lyrics)

                parsedLines
                    .map { entry ->
                        val newEntry =
                            LyricsEntry(entry.time, entry.text, entry.words, agent = entry.agent, isBackground = entry.isBackground)

                        scope.launch {
                            val text = if (romanizeCyrillicByLine) entry.text else lyrics
                            var value: String? = ""

                            when {
                                "Japanese" in enabledLanguages && isJapanese(text) && !isChinese(text) -> {
                                    value =
                                        romanizeJapanese(entry.text)
                                }

                                "Korean" in enabledLanguages && isKorean(text) -> {
                                    value = romanizeKorean(entry.text)
                                }

                                "Chinese" in enabledLanguages && isChinese(text) -> {
                                    value = romanizeChinese(entry.text)
                                }

                                "Hindi" in enabledLanguages && isHindi(text) -> {
                                    value = romanizeHindi(entry.text)
                                }

                                "Ukrainian" in enabledLanguages && isUkrainian(text) -> {
                                    value = romanizeCyrillic(entry.text)
                                }

                                "Russian" in enabledLanguages && isRussian(text) -> {
                                    value = romanizeCyrillic(entry.text)
                                }

                                "Serbian" in enabledLanguages && isSerbian(text) -> {
                                    value = romanizeCyrillic(entry.text)
                                }

                                "Bulgarian" in enabledLanguages && isBulgarian(text) -> {
                                    value = romanizeCyrillic(entry.text)
                                }

                                "Belarusian" in enabledLanguages && isBelarusian(text) -> {
                                    value = romanizeCyrillic(entry.text)
                                }

                                "Kyrgyz" in enabledLanguages && isKyrgyz(text) -> {
                                    value = romanizeCyrillic(entry.text)
                                }

                                "Macedonian" in enabledLanguages && isMacedonian(text) -> {
                                    value = romanizeCyrillic(entry.text)
                                }
                            }

                            newEntry.romanizedTextFlow.value = value
                        }

                        newEntry
                    }.let {
                        listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
                    }
            } else {
                lyrics.lines().mapIndexed { index, line ->
                    val newEntry = LyricsEntry(index * 100L, line)

                    scope.launch {
                        val text = if (romanizeCyrillicByLine) line else lyrics
                        var value: String? = ""

                        when {
                            "Japanese" in enabledLanguages && isJapanese(text) && !isChinese(text) -> value = romanizeJapanese(line)
                            "Korean" in enabledLanguages && isKorean(text) -> value = romanizeKorean(line)
                            "Chinese" in enabledLanguages && isChinese(text) -> value = romanizeChinese(line)
                            "Hindi" in enabledLanguages && isHindi(text) -> value = romanizeHindi(line)
                            "Ukrainian" in enabledLanguages && isUkrainian(text) -> value = romanizeCyrillic(line)
                            "Russian" in enabledLanguages && isRussian(text) -> value = romanizeCyrillic(line)
                            "Serbian" in enabledLanguages && isSerbian(text) -> value = romanizeCyrillic(line)
                            "Bulgarian" in enabledLanguages && isBulgarian(text) -> value = romanizeCyrillic(line)
                            "Belarusian" in enabledLanguages && isBelarusian(text) -> value = romanizeCyrillic(line)
                            "Kyrgyz" in enabledLanguages && isKyrgyz(text) -> value = romanizeCyrillic(line)
                            "Macedonian" in enabledLanguages && isMacedonian(text) -> value = romanizeCyrillic(line)
                        }

                        newEntry.romanizedTextFlow.value = value
                    }

                    newEntry
                }
            }
        }

    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    val mergedLyricsList: List<LyricsListItem> = remember(lines) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) return@remember result
        lines.forEachIndexed { i, entry ->
            result.add(LyricsListItem.Line(i, entry))
            if (i < lines.size - 1) {
                val hasWordTimings = !entry.words.isNullOrEmpty()
                if (!hasWordTimings) return@forEachIndexed
                val currentEnd = (entry.words.last().endTime * 1000).toLong()
                val nextStart = lines[i + 1].time
                val gap = nextStart - currentEnd
                if (gap > 4000L) {
                    result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                }
            }
        }
        result
    }

    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    
    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
        }
    }
    
    LaunchedEffect(lines, lyricsEntity, translateLanguage, translateMode) {
        if (lines.isNotEmpty() && lyricsEntity != null) {
            LyricsTranslationHelper.loadTranslationsFromDatabase(
                lyrics = lines,
                lyricsEntity = lyricsEntity,
                targetLanguage = translateLanguage,
                mode = translateMode
            )
        }
    }
    
    LaunchedEffect(showLyrics, lines.size) {
        LyricsTranslationHelper.manualTrigger.collect {
            val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
            if (showLyrics && lines.isNotEmpty() && effectiveApiKey.isNotBlank()) {
                LyricsTranslationHelper.translateLyrics(
                    lyrics = lines,
                    targetLanguage = translateLanguage,
                    apiKey = openRouterApiKey,
                    baseUrl = openRouterBaseUrl,
                    model = openRouterModel,
                    mode = translateMode,
                    scope = scope,
                    context = context,
                    provider = aiProvider,
                    deeplApiKey = deeplApiKey,
                    deeplFormality = deeplFormality,
                    useStreaming = true,
                    songId = currentSong?.id ?: "",
                    database = database,
                    systemPrompt = aiSystemPrompt,
                )
            } else if (effectiveApiKey.isBlank()) {
                Toast.makeText(context, context.getString(R.string.ai_api_key_required), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        LyricsTranslationHelper.clearTranslationsTrigger.collect {
            lines.forEach { it.translatedTextFlow.value = null }
        }
    }

    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> Color.White
    }

    var activeLineIndices by remember {
        mutableStateOf(emptySet<Int>())
    }
    var scrollTargetIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }
    var previousActiveLineIndices by remember {
        mutableStateOf(emptySet<Int>())
    }
    val currentLineIndex by remember {
        derivedStateOf {
            if (activeLineIndices.isEmpty()) -1
            else activeLineIndices
                .filter { lines.getOrNull(it)?.isBackground == false }
                .maxOrNull()
                ?: activeLineIndices.maxOrNull()
                ?: -1
        }
    }
    val currentPlaybackPosition = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    var currentPositionState by remember { mutableLongStateOf(0L) }
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var seekVersion by remember { mutableIntStateOf(0) }
    var isSeeking by remember {
        mutableStateOf(false)
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

    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }

    val isLyricsProviderShown = lyricsEntity?.provider != null && lyricsEntity?.provider != "Unknown" && !isSelectionModeActive

    val lazyListState = rememberLazyListState()
    
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    val maxSelectionLimit = 5

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

    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            activeLineIndices = emptySet()
            return@LaunchedEffect
        }
        while (isActive) {
            delay(8)
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: playerConnection.player.currentPosition
            currentPlaybackPosition.set(position)
            currentPositionState = position
            val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
            val newActiveIndices = findActiveLineIndices(lines, position + lyricsOffset)
            val newMax = newActiveIndices
                .filter { lines.getOrNull(it)?.isBackground == false }
                .maxOrNull() ?: (newActiveIndices.maxOrNull() ?: -1)
            val prevMax = previousActiveLineIndices
                .filter { lines.getOrNull(it)?.isBackground == false }
                .maxOrNull() ?: (previousActiveLineIndices.maxOrNull() ?: -1)

            val aLineJustEnded = previousActiveLineIndices.isNotEmpty() &&
                previousActiveLineIndices.any { idx ->
                    idx !in newActiveIndices && lines.getOrNull(idx)?.isBackground == false
                }

            val anyBgStillActive = newActiveIndices.any { lines.getOrNull(it)?.isBackground == true }

            val shouldScroll = when {
                anyBgStillActive && newMax == scrollTargetIndex -> false
                aLineJustEnded && newActiveIndices.isNotEmpty() && newMax != scrollTargetIndex -> true
                previousActiveLineIndices.size <= 1 && newActiveIndices.size <= 1 && newMax > prevMax -> true
                previousActiveLineIndices.isEmpty() && newActiveIndices.isNotEmpty() -> true
                else -> false
            }

            if (shouldScroll) {
                val nextLineIndex = newMax + 1
                val msUntilNextLine = if (nextLineIndex < lines.size) lines[nextLineIndex].time - (position + lyricsOffset) else Long.MAX_VALUE
                val preScrollDelay = when {
                    isSeeking -> 0L
                    msUntilNextLine > 800L -> 500L
                    msUntilNextLine > 400L -> 200L
                    else -> 0L
                }

                if (preScrollDelay > 0L) delay(preScrollDelay)
                scrollTargetIndex = newMax
            }
            previousActiveLineIndices = newActiveIndices
            activeLineIndices = newActiveIndices
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

    /**
     * Smoothly scrolls the lyrics list to center the item at [targetIndex].
     *
     * @param targetIndex The index of the lyrics line to scroll to.
     * @param duration The duration of the scroll animation in milliseconds.
     */
    suspend fun performSmoothPageScroll(
        targetIndex: Int,
        duration: Int = 1500,
    ) {
        // This function might need rewrite for the new custom scroll engine
        // but keeping it for now as a feature to be combined/adapted.
        scrollTargetIndex = targetIndex
    }

    LaunchedEffect(scrollTargetIndex) {
        if (scrollTargetIndex != -1 && isAutoScrollEnabled) {
            deferredCurrentLineIndex = scrollTargetIndex
        }
    }

    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(top = 56.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val status = translationStatus) {
                is LyricsTranslationHelper.TranslationStatus.Translating -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.ai_translating_lyrics),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                is LyricsTranslationHelper.TranslationStatus.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.error),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                is LyricsTranslationHelper.TranslationStatus.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.ai_lyrics_translated),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                is LyricsTranslationHelper.TranslationStatus.Idle -> {}
            }
        }

        if (lyrics == LYRICS_NOT_FOUND) {
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
        } else if (isSynced) {
            val displayedCurrentLineIndex = deferredCurrentLineIndex

            LaunchedEffect(isAutoScrollEnabled) {
                if (isAutoScrollEnabled) {
                    val start = userManualOffset
                    val dist = kotlin.math.abs(start)
                    if (dist < 1f) { userManualOffset = 0f; return@LaunchedEffect }
                    val duration = (dist / 4f).toInt().coerceIn(200, 600)
                    val anim = Animatable(start)
                    anim.animateTo(0f, tween(duration, easing = FastOutSlowInEasing)) {
                        userManualOffset = value
                    }
                }
            }

            val velocityTracker = remember { VelocityTracker() }
            val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
            val itemHeights = remember { mutableStateMapOf<Int, Int>() }
            var isInitialLayout by remember { mutableStateOf(true) }

            LaunchedEffect(showLyrics) {
                if (showLyrics) {
                    isInitialLayout = true
                    snapshotFlow { itemHeights.size }
                        .first { it >= minOf(mergedLyricsList.size, 5) }
                    isInitialLayout = false
                }
            }
            
            val activeListIndex = remember(displayedCurrentLineIndex, mergedLyricsList) {
                mergedLyricsList.indexOfFirst { it is LyricsListItem.Line && it.index == displayedCurrentLineIndex }.coerceAtLeast(0)
            }
            
            val positions = remember(itemHeights.toMap(), activeListIndex) {
                val map = mutableMapOf<Int, Float>()
                if (activeListIndex == -1) return@remember map
                
                val itemGapPx = with(density) { 16.dp.toPx() }
                map[activeListIndex] = 0f
                
                // Items above
                var currentY = 0f
                for (i in activeListIndex - 1 downTo 0) {
                    val height = itemHeights[i]?.toFloat() ?: with(density) { 68.dp.toPx() }
                    currentY -= (height + itemGapPx)
                    map[i] = currentY
                }
                
                // Items below
                currentY = 0f
                for (i in activeListIndex until mergedLyricsList.size - 1) {
                    val height = itemHeights[i]?.toFloat() ?: with(density) { 68.dp.toPx() }
                    currentY += (height + itemGapPx)
                    map[i + 1] = currentY
                }
                map
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(top = 130.dp, bottom = 160.dp)
                    .clipToBounds()
                    .nestedScroll(remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (source == NestedScrollSource.UserInput) isAutoScrollEnabled = false
                                if (!isSelectionModeActive) lastPreviewTime = System.currentTimeMillis()
                                return super.onPostScroll(consumed, available, source)
                            }

                            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                isAutoScrollEnabled = false
                                if (!isSelectionModeActive) lastPreviewTime = System.currentTimeMillis()
                                return super.onPostFling(consumed, available)
                            }
                        }
                    })
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                flingJob?.cancel()
                                velocityTracker.resetTracking()
                                isAutoScrollEnabled = false
                                lastPreviewTime = System.currentTimeMillis()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                verticalDrag(down.id) { change ->
                                    val dragAmount = change.positionChange().y
                                    userManualOffset += dragAmount
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                }

                                val velocity = velocityTracker.calculateVelocity().y
                                flingJob = scope.launch {
                                    AnimationState(
                                        initialValue = userManualOffset,
                                        initialVelocity = velocity
                                    ).animateDecay(decayAnimSpec) {
                                        userManualOffset = value
                                    }
                                }
                            }
                        }
                    }
            ) {
                val anchorY = with(density) { this@BoxWithConstraints.maxHeight.toPx() } * 0.35f
                val lineHeightPx = with(density) { 68.dp.toPx() }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLyricsProviderShown) {
                        val providerBase = anchorY + ((0 - activeListIndex) * lineHeightPx) - with(density) { 32.dp.toPx() }
                        Text(
                            text = "Lyrics from ${lyricsEntity?.provider}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, (providerBase + userManualOffset).roundToInt()) }
                                .padding(horizontal = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp
                                    else -> 24.dp
                            }, vertical = 4.dp)
                        )
                    }

                    mergedLyricsList.forEachIndexed { listIndex, listItem ->
                        val distance = abs(listIndex - activeListIndex)
                        val staggerDelay = (distance * 40).coerceAtMost(350)
                        
                        val frozenOffset = remember { mutableFloatStateOf(0f) }
                        val targetOffset = anchorY + positions.getOrDefault(listIndex, (listIndex - activeListIndex) * (lineHeightPx + with(density) { 16.dp.toPx() }))

                        val itemGapPx = with(density) { 16.dp.toPx() }
                        val prevTarget = positions[listIndex - 1]?.let { anchorY + it }
                        val nextTarget = positions[listIndex + 1]?.let { anchorY + it }
                        val prevHeight = itemHeights[listIndex - 1]?.toFloat() ?: with(density) { 68.dp.toPx() }
                        val clampedTarget = targetOffset.coerceAtLeast(
                            (prevTarget ?: Float.NEGATIVE_INFINITY) + prevHeight + itemGapPx
                        ).coerceAtMost(
                            (nextTarget ?: Float.POSITIVE_INFINITY) - (itemHeights[listIndex]?.toFloat() ?: with(density) { 68.dp.toPx() }) - itemGapPx
                        )

                        LaunchedEffect(isAutoScrollEnabled, clampedTarget) {
                            if (isAutoScrollEnabled) frozenOffset.floatValue = clampedTarget
                        }
                        val resolvedTarget = if (isAutoScrollEnabled) clampedTarget else frozenOffset.floatValue
                        val animatedOffset by animateFloatAsState(
                            targetValue = resolvedTarget,
                            animationSpec = if (isInitialLayout || !isAutoScrollEnabled) snap() else tween(600, staggerDelay, FastOutSlowInEasing),
                            label = "lyricStaggeredOffset_$listIndex"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
                                    layout(placeable.width, 0) {
                                        placeable.place(0, 0)
                                    }
                                }
                                .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                        ) {
                             Box(modifier = Modifier.onSizeChanged { itemHeights[listIndex] = it.height }) {
                                when (listItem) {
                                    is LyricsListItem.Indicator -> {
                                        val effectiveEndMs = listItem.gapEndMs - 650L
                                        val visible = currentPositionState > 0L &&
                                            currentPositionState >= listItem.gapStartMs &&
                                            currentPositionState <= effectiveEndMs
                                        IntervalIndicator(
                                            gapStartMs = listItem.gapStartMs,
                                            gapEndMs = effectiveEndMs,
                                            currentPositionMs = currentPositionState,
                                            visible = visible,
                                            color = expressiveAccent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = when (lyricsTextPosition) {
                                                    LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp
                                                    else -> 24.dp
                                                })
                                                .wrapContentWidth(when (lyricsTextPosition) {
                                                    LyricsPosition.LEFT -> Alignment.Start
                                                    LyricsPosition.RIGHT -> Alignment.End
                                                    else -> Alignment.CenterHorizontally
                                                })
                                        )
                                    }
                                    is LyricsListItem.Line -> {
                                        val index = listItem.index
                                        val item = listItem.entry
                                        val isSelected = selectedIndices.contains(index)
                                        
                                        val isActiveByIndex = activeLineIndices.contains(index)
                                        val pairedMainLineIndex = if (item.isBackground) {
                                            (index - 1 downTo 0).firstOrNull { lines.getOrNull(it)?.isBackground == false } ?: -1
                                        } else -1
                                        val bgVisible = item.isBackground && (activeLineIndices.contains(pairedMainLineIndex) || isActiveByIndex)
                                        
                                        val itemModifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelectionModeActive) {
                                                        if (isSelected) {
                                                            selectedIndices.remove(index)
                                                            if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                                        } else {
                                                            if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
                                                            else showMaxSelectionToast = true
                                                        }
                                                    } else if (changeLyrics && !isGuest) {
                                                        val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                                        playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                                        isAutoScrollEnabled = true
                                                        lastPreviewTime = 0L
                                                        seekVersion++
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isSelectionModeActive) {
                                                        isSelectionModeActive = true
                                                        selectedIndices.add(index)
                                                    } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                                        selectedIndices.add(index)
                                                    } else if (!isSelected) showMaxSelectionToast = true
                                                }
                                            )
                                            .background(if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                                            .padding(
                                                start = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp },
                                                end = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp },
                                                top = if (item.isBackground) (if (bgVisible) 2.dp else 0.dp) else 12.dp,
                                                bottom = if (item.isBackground) (if (bgVisible) 2.dp else 0.dp) else if (mergedLyricsList.getOrNull(listIndex + 1)?.let { it is LyricsListItem.Line && it.entry.isBackground } == true) 4.dp else 12.dp
                                            )
                                        
                                        val pairedAgent = if (item.isBackground && pairedMainLineIndex != -1) lines[pairedMainLineIndex].agent else null
                                        
                                        val agentAlignment = when {
                                            (if (item.isBackground) pairedAgent else item.agent) == "v1" -> Alignment.Start
                                            (if (item.isBackground) pairedAgent else item.agent) == "v2" -> Alignment.End
                                            (if (item.isBackground) pairedAgent else item.agent) == "v1000" -> Alignment.CenterHorizontally
                                            else -> when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> Alignment.Start
                                                LyricsPosition.CENTER -> Alignment.CenterHorizontally
                                                LyricsPosition.RIGHT -> Alignment.End
                                            }
                                        }
                                        val agentTextAlign = when {
                                            (if (item.isBackground) pairedAgent else item.agent) == "v1" -> TextAlign.Left
                                            (if (item.isBackground) pairedAgent else item.agent) == "v2" -> TextAlign.Right
                                            (if (item.isBackground) pairedAgent else item.agent) == "v1000" -> TextAlign.Center
                                            else -> when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> TextAlign.Left
                                                LyricsPosition.CENTER -> TextAlign.Center
                                                LyricsPosition.RIGHT -> TextAlign.Right
                                            }
                                        }

                                        Box(modifier = itemModifier, contentAlignment = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> Alignment.CenterStart
                                            LyricsPosition.RIGHT -> Alignment.CenterEnd
                                            else -> Alignment.Center
                                        }) {
                                            @Composable
                                            fun LyricContent() {
                                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = agentAlignment) {
                                                    if (index == 0) {
                                                        val firstRealLine = lines.getOrNull(1)
                                                        val prefirstGap = firstRealLine?.time ?: 0L
                                                        if (prefirstGap > 4000L) {
                                                            val effectiveEndMs = prefirstGap - 650L
                                                            val visible = currentPositionState > 0L && currentPositionState <= effectiveEndMs
                                                            IntervalIndicator(
                                                                gapStartMs = 0L, gapEndMs = effectiveEndMs, currentPositionMs = currentPositionState,
                                                                visible = visible, color = expressiveAccent,
                                                                modifier = Modifier.fillMaxWidth().padding(horizontal = when (lyricsTextPosition) {
                                                                    LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp
                                                                    else -> 24.dp
                                                                }).wrapContentWidth(when (lyricsTextPosition) {
                                                                    LyricsPosition.LEFT -> Alignment.Start
                                                                    LyricsPosition.RIGHT -> Alignment.End
                                                                    else -> Alignment.CenterHorizontally
                                                                })
                                                            )
                                                        }
                                                    }
                                                    val isActiveLine = isActiveByIndex
                                                    val baseAlpha = if (item.isBackground) 0.08f else 0.2f
                                                    val activeAlpha = if (item.isBackground) 1f else 1f
                                                    val targetAlpha = if (item.isBackground) {
                                                        activeAlpha
                                                    } else if (isActiveLine) {
                                                        activeAlpha
                                                    } else if (isAutoScrollEnabled && displayedCurrentLineIndex >= 0) {
                                                        when (abs(index - displayedCurrentLineIndex)) {
                                                            1 -> 0.2f; 2 -> 0.2f; 3 -> 0.15f; 4 -> 0.07f; else -> 0.05f
                                                        }
                                                    } else baseAlpha
                                                    val animatedAlpha by animateFloatAsState(targetAlpha, tween(250), label = "lyricsLineAlpha")
                                                    val lineColor = expressiveAccent.copy(alpha = if (item.isBackground) 0.5f else animatedAlpha)
                                                    val alignment = agentTextAlign
                                                    val romanizedTextState by item.romanizedTextFlow.collectAsState()
                                                    val isRomanizedAvailable = romanizedTextState != null
                                                    val mainTextRaw = if (romanizeAsMain && isRomanizedAvailable) romanizedTextState else item.text
                                                    val subTextRaw = if (romanizeAsMain && isRomanizedAvailable) item.text else romanizedTextState
                                                    val mainText = if (item.isBackground) mainTextRaw?.removePrefix("(")?.removeSuffix(")") else mainTextRaw
                                                    val subText = if (item.isBackground) subTextRaw?.removePrefix("(")?.removeSuffix(")") else subTextRaw
                                                    
                                                    if (item.words?.isNotEmpty() == true && abs(index - displayedCurrentLineIndex) <= 2 && isActiveLine) {
                                                        val textMeasurer = rememberTextMeasurer()
                                                        FlowRow(horizontalArrangement = when (alignment) {
                                                            TextAlign.Right -> Arrangement.End; TextAlign.Center -> Arrangement.Center; else -> Arrangement.Start
                                                        }) {
                                                            item.words.forEachIndexed { wordIndex, word ->
                                                                val rawWordText = word.text.let { 
                                                                    if (item.isBackground) {
                                                                        var t = it
                                                                        if (wordIndex == 0) t = t.removePrefix("(")
                                                                        if (wordIndex == item.words.size - 1) t = t.removeSuffix(")")
                                                                        t
                                                                    } else it
                                                                }
                                                                val wordText = rawWordText + if (word.hasTrailingSpace && wordIndex < item.words.size - 1) " " else ""
                                                                val wStartMs = (word.startTime * 1000).toLong()
                                                                val wEndMs = (word.endTime * 1000).toLong()

                                                                val progress = remember { Animatable(0f) }
                                                                LaunchedEffect(isActiveLine, seekVersion) {
                                                                    if (isActiveLine) {
                                                                        val offset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
                                                                        var pos = playerConnection.player.currentPosition + offset
                                                                        if (pos >= wEndMs) progress.snapTo(1f) else if (pos < wStartMs) progress.snapTo(0f)
                                                                        while (isActive && pos < wEndMs) {
                                                                            pos = withFrameMillis { playerConnection.player.currentPosition + offset }
                                                                            progress.snapTo(((pos - wStartMs).toFloat() / (wEndMs - wStartMs).toFloat()).coerceIn(0f, 1f))
                                                                        }
                                                                        if (pos >= wEndMs) progress.snapTo(1f)
                                                                    } else progress.snapTo(0f)
                                                                }
                                                                val textLayout = remember(wordText) { textMeasurer.measure(wordText, TextStyle(fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp, fontWeight = FontWeight.Bold, fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)) }
                                                                Row {
                                                                    wordText.indices.forEach { i ->
                                                                        val letterLayout = remember(wordText, i) { textMeasurer.measure(wordText[i].toString(), TextStyle(fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp, fontWeight = FontWeight.Bold, fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)) }

                                                                        val wordDuration = (wEndMs - wStartMs).coerceAtLeast(1L)
                                                                        val letterThreshold = i.toFloat() / wordText.length.coerceAtLeast(1)
                                                                        val letterEnd = (i + 1).toFloat() / wordText.length.coerceAtLeast(1)
                                                                        Box(modifier = Modifier.graphicsLayer {
                                                                            if (isActiveLine) {
                                                                                val lp = ((progress.value - letterThreshold) / (letterEnd - letterThreshold).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
                                                                                // ease out: fast rise, smooth settle
                                                                                val eased = 1f - (1f - lp) * (1f - lp)
                                                                                translationY = 8f * (1f - eased)
                                                                            } else {
                                                                                translationY = 0f
                                                                            }
                                                                            scaleX = 1f
                                                                            scaleY = 1f
                                                                        }) {
                                                                            Canvas(Modifier.size(with(density) { letterLayout.size.width.toDp() }, with(density) { letterLayout.size.height.toDp() })) {
                                                                                drawText(letterLayout, expressiveAccent.copy(alpha = if (isActiveLine) 0.25f else (if (item.isBackground) 0.5f else animatedAlpha)))
                                                                                if (isActiveLine) {
                                                                                    val letterProgress = ((progress.value - i.toFloat() / wordText.length) * wordText.length).coerceIn(0f, 1f)
                                                                                    if (letterProgress >= 1f) drawText(letterLayout, expressiveAccent)
                                                                                    else if (letterProgress > 0f) {
                                                                                        val fillX = size.width * letterProgress
                                                                                        val edge = (size.width * 0.18f).coerceAtMost(fillX)
                                                                                        val solid = (fillX - edge).coerceAtLeast(0f)
                                                                                        if (solid > 0f) clipRect(right = solid) { drawText(letterLayout, expressiveAccent) }
                                                                                        for (j in 0 until 8) {
                                                                                            val start = solid + j * (edge / 8); val end = start + (edge / 8)
                                                                                            clipRect(left = start, right = end) { drawText(letterLayout, expressiveAccent.copy(alpha = 1f - (j + 0.5f) / 8)) }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Text(mainText ?: "", fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp, fontWeight = FontWeight.Bold, fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal, color = if (isActiveLine && !lyricsGlowEffect) expressiveAccent else lineColor, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                                            modifier = Modifier.graphicsLayer(transformOrigin = when (alignment) {
                                                                TextAlign.Left -> TransformOrigin(0f, 0.5f); TextAlign.Right -> TransformOrigin(1f, 0.5f); else -> TransformOrigin.Center
                                                            }, scaleX = 1f, scaleY = 1f, clip = false))
                                                    }
                                                    
                                                    if (currentSong?.romanizeLyrics == true && enabledLanguages.isNotEmpty()) {
                                                        subText?.let { Text(it, fontSize = 18.sp, color = expressiveAccent.copy(alpha = 0.6f), textAlign = when (lyricsTextPosition) {
                                                            LyricsPosition.LEFT -> TextAlign.Left; LyricsPosition.CENTER -> TextAlign.Center; else -> TextAlign.Right
                                                        }, fontWeight = FontWeight.Normal, modifier = Modifier.padding(top = 2.dp)) }
                                                    }
                                                    val transText by item.translatedTextFlow.collectAsState()
                                                    transText?.let { Text(it, fontSize = 16.sp, color = expressiveAccent.copy(alpha = 0.5f), textAlign = when (lyricsTextPosition) {
                                                        LyricsPosition.LEFT -> TextAlign.Left; LyricsPosition.CENTER -> TextAlign.Center; else -> TextAlign.Right
                                                    }, fontWeight = FontWeight.Normal, modifier = Modifier.padding(top = 4.dp)) }
                                                }
                                            }

                                            if (item.isBackground) {
                                                AnimatedVisibility(
                                                    visible = bgVisible,
                                                    enter = fadeIn(tween(durationMillis = 250, delayMillis = 100)),
                                                    exit = fadeOut(tween(250))
                                                ) {
                                                    LyricContent()
                                                }
                                            } else {
                                                LyricContent()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.Top,
                contentPadding = WindowInsets(top = 0.dp, bottom = this@BoxWithConstraints.maxHeight / 2).asPaddingValues(),
                modifier = Modifier.fadingEdge(top = 130.dp, bottom = 160.dp).nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                            if (source == NestedScrollSource.UserInput) isAutoScrollEnabled = false
                            if (!isSelectionModeActive) lastPreviewTime = System.currentTimeMillis()
                            return super.onPostScroll(consumed, available, source)
                        }
                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            isAutoScrollEnabled = false
                            if (!isSelectionModeActive) lastPreviewTime = System.currentTimeMillis()
                            return super.onPostFling(consumed, available)
                        }
                    }
                })
            ) {
                if (lyrics == null) {
                    item { ShimmerHost { repeat(10) { Box(contentAlignment = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> Alignment.CenterStart; LyricsPosition.CENTER -> Alignment.Center; else -> Alignment.CenterEnd
                    }, modifier = Modifier.fillMaxWidth().padding(horizontal = when (lyricsTextPosition) {
                        LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp
                    }, vertical = 4.dp)) { TextPlaceholder() } } } }
                } else {
                    itemsIndexed(mergedLyricsList, key = { _, item -> when (item) { is LyricsListItem.Line -> "line_${item.index}"; is LyricsListItem.Indicator -> "indicator_${item.afterLineIndex}" } }) { listIndex, item ->
                        if (item is LyricsListItem.Line) {
                            val isSel = selectedIndices.contains(item.index)
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).combinedClickable(
                                onClick = { if (isSelectionModeActive) { if (isSel) { selectedIndices.remove(item.index); if (selectedIndices.isEmpty()) isSelectionModeActive = false } else { if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(item.index) else showMaxSelectionToast = true } } },
                                onLongClick = { if (!isSelectionModeActive) { isSelectionModeActive = true; selectedIndices.add(item.index) } }
                            ).background(if (isSel && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent).padding(
                                start = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp },
                                end = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp },
                                top = if (item.entry.isBackground) 2.dp else 12.dp,
                                bottom = if (item.entry.isBackground) 2.dp else if (mergedLyricsList.getOrNull(listIndex + 1)?.let { it is LyricsListItem.Line && it.entry.isBackground } == true) 4.dp else 12.dp
                            ), contentAlignment = when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> Alignment.CenterStart; LyricsPosition.RIGHT -> Alignment.CenterEnd; else -> Alignment.Center
                            }) { Text(item.entry.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = expressiveAccent, textAlign = when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> TextAlign.Left; LyricsPosition.CENTER -> TextAlign.Center; else -> TextAlign.Right
                            }) }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            AnimatedVisibility(visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                FilledTonalButton(onClick = { flingJob?.cancel(); flingJob = null; isAutoScrollEnabled = true }) {
                    Icon(painterResource(R.drawable.sync), stringResource(R.string.auto_scroll), Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.auto_scroll))
                }
            }
            AnimatedVisibility(visible = isSelectionModeActive, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { isSelectionModeActive = false; selectedIndices.clear() }) { Icon(painterResource(R.drawable.close), stringResource(R.string.cancel), Modifier.size(20.dp)) }
                    FilledTonalButton(onClick = {
                        if (selectedIndices.isNotEmpty()) {
                            val text = selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")
                            if (text.isNotBlank()) {
                                shareDialogData = Triple(text, mediaMetadata?.title ?: "", mediaMetadata?.artists?.joinToString { it.name } ?: "")
                                showShareDialog = true
                            }
                            isSelectionModeActive = false; selectedIndices.clear()
                        }
                    }, enabled = selectedIndices.isNotEmpty()) {
                        Icon(painterResource(R.drawable.share), stringResource(R.string.share_selected), Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.share))
                    }
                }
            }
        }
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = {}) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                Box(Modifier.padding(32.dp)) { Text(stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait), color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (txt, title, arts) = shareDialogData!!
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.padding(16.dp).fillMaxWidth(0.85f)) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.share_lyrics), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "\"$txt\"\n\n$title - $arts\nhttps://music.youtube.com/watch?v=${mediaMetadata?.id}") }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_lyrics)))
                        showShareDialog = false
                    }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.share), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.share_as_text), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable { shareDialogData = Triple(txt, title, arts); showColorPickerDialog = true; showShareDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.share), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.share_as_image), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End) {
                        Text(stringResource(R.string.cancel), fontSize = 16.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { showShareDialog = false }.padding(vertical = 8.dp, horizontal = 12.dp))
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (txt, title, arts) = shareDialogData!!
        val cover = mediaMetadata?.thumbnailUrl
        val pal = remember { mutableStateListOf<Color>() }
        var bgStyle by remember { mutableStateOf(LyricsBackgroundStyle.SOLID) }
        val previewCardWidth = configuration.screenWidthDp.dp * 0.90f
        val previewPadding = 40.dp; val previewBoxPadding = 56.dp
        val availW = previewCardWidth - previewPadding - previewBoxPadding
        val availH = 340.dp - (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 56.dp)
        val align = when (lyricsTextPosition) { LyricsPosition.LEFT -> TextAlign.Left; LyricsPosition.CENTER -> TextAlign.Center; else -> TextAlign.Right }
        val measurer = rememberTextMeasurer()
        rememberAdjustedFontSize(txt, availW, availH, density, 50.sp, 22.sp, TextStyle(color = previewTextColor, fontWeight = FontWeight.Bold, textAlign = align), measurer)
        LaunchedEffect(cover) {
            if (cover != null) withContext(Dispatchers.IO) {
                try {
                    val res = ImageLoader(context).execute(ImageRequest.Builder(context).data(cover).allowHardware(false).build())
                    val bmp = res.image?.toBitmap()
                    if (bmp != null) {
                        val swatches = Palette.from(bmp).generate().swatches.sortedByDescending { it.population }
                        pal.clear(); pal.addAll(swatches.map { Color(it.rgb) }.filter { val hsv = FloatArray(3); android.graphics.Color.colorToHSV(it.toArgb(), hsv); hsv[1] > 0.2f }.take(5))
                    }
                } catch (_: Exception) {}
            }
        }
        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text(stringResource(R.string.customize_colors), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp)); Text(stringResource(R.string.player_background_style), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        LyricsBackgroundStyle.entries.forEach { style ->
                            val label = when(style) { LyricsBackgroundStyle.SOLID -> stringResource(R.string.player_background_solid); LyricsBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur); else -> stringResource(R.string.gradient) }
                            androidx.compose.material3.FilterChip(selected = bgStyle == style, onClick = { bgStyle = style }, label = { Text(label) })
                        }
                    }
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp).clip(RoundedCornerShape(12.dp))) {
                        LyricsImageCard(
                            lyricText = txt,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            darkBackground = true,
                            backgroundColor = previewBackgroundColor,
                            backgroundStyle = bgStyle,
                            textColor = previewTextColor,
                            secondaryTextColor = previewSecondaryTextColor,
                            textAlign = align
                        )
                    }
                    Spacer(Modifier.height(18.dp)); Text(stringResource(R.string.background_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (pal + listOf(Color(0xFF242424), Color(0xFF121212), Color.White, Color.Black, Color(0xFFF5F5F5))).distinct().take(8).forEach { color ->
                            Box(Modifier.size(32.dp).background(color, RoundedCornerShape(8.dp)).clickable { previewBackgroundColor = color }.border(2.dp, if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)))
                        }
                    }
                    Text(stringResource(R.string.text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (pal + listOf(Color.White, Color.Black, Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(Modifier.size(32.dp).background(color, RoundedCornerShape(8.dp)).clickable { previewTextColor = color }.border(2.dp, if (previewTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)))
                        }
                    }
                    Text(stringResource(R.string.secondary_text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (pal.map { it.copy(alpha = 0.7f) } + listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f), Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(Modifier.size(32.dp).background(color, RoundedCornerShape(8.dp)).clickable { previewSecondaryTextColor = color }.border(2.dp, if (previewSecondaryTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)))
                        }
                    }
                    Spacer(Modifier.height(12.dp)); Button(onClick = {
                        showColorPickerDialog = false; showProgressDialog = true
                        scope.launch {
                            try {
                                val image = ComposeToImage.createLyricsImage(context, cover, title, arts, txt, (configuration.screenWidthDp * density.density).toInt(), (configuration.screenHeightDp * density.density).toInt(), previewBackgroundColor.toArgb(), bgStyle, previewTextColor.toArgb(), previewSecondaryTextColor.toArgb(), when (lyricsTextPosition) { LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL; LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER; else -> Layout.Alignment.ALIGN_OPPOSITE })
                                val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics_${System.currentTimeMillis()}")
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, context.getString(R.string.share_lyrics)))
                            } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.failed_to_create_image, e.message), Toast.LENGTH_SHORT).show() } finally { showProgressDialog = false }
                        }
                    }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.share)) }
                }
            }
        }
    }
}

val LyricsPreviewTime = 2.seconds

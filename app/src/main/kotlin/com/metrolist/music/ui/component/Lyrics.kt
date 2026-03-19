/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import android.text.Layout
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
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
import com.metrolist.music.constants.RespectAgentPositioningKey
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
import com.metrolist.music.ui.screens.settings.LyricsAnimationStyle
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

private sealed class LyricsListItem {
    data class Line(val index: Int, val entry: com.metrolist.music.lyrics.LyricsEntry) : LyricsListItem()
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
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
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.APPLE)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    val respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
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
    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    val currentLyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    var lastValidLyricsEntity by remember { mutableStateOf<com.metrolist.music.db.entities.LyricsEntity?>(null) }
    
    LaunchedEffect(currentLyricsEntity) {
        if (currentLyricsEntity != null) {
            lastValidLyricsEntity = currentLyricsEntity
        }
    }
    
    val lyricsEntity = remember(currentLyricsEntity, translationStatus) {
        if (currentLyricsEntity != null) {
            currentLyricsEntity
        } else if (translationStatus is LyricsTranslationHelper.TranslationStatus.Translating || translationStatus is LyricsTranslationHelper.TranslationStatus.Success) {
            lastValidLyricsEntity
        } else {
            null
        }
    }
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

    var lines by remember { mutableStateOf<List<com.metrolist.music.lyrics.LyricsEntry>>(emptyList()) }

    LaunchedEffect(lyrics, scope) {
        val processedLines = withContext(Dispatchers.Default) {
            if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                emptyList()
            } else if (lyrics.startsWith("[")) {
                val parsedLines = parseLyrics(lyrics)

                parsedLines
                    .map { entry ->
                        val newEntry =
                            LyricsEntry(
                                entry.time,
                                entry.text,
                                entry.words,
                                agent = entry.agent,
                                isBackground = entry.isBackground
                            )

                        val text = if (romanizeCyrillicByLine) entry.text else lyrics
                        var value: String? = ""

                        when {
                            "Japanese" in enabledLanguages && isJapanese(text) && !isChinese(text) -> {
                                value = romanizeJapanese(entry.text)
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
                        newEntry
                    }.let {
                        listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
                    }
            } else {
                lyrics.lines().mapIndexed { index, line ->
                    val newEntry = LyricsEntry(index * 100L, line)

                    val text = if (romanizeCyrillicByLine) line else lyrics
                    var value: String? = ""

                    when {
                        "Japanese" in enabledLanguages && isJapanese(text) && !isChinese(text) -> value =
                            romanizeJapanese(line)

                        "Korean" in enabledLanguages && isKorean(text) -> value = romanizeKorean(line)
                        "Chinese" in enabledLanguages && isChinese(text) -> value = romanizeChinese(line)
                        "Hindi" in enabledLanguages && isHindi(text) -> value = romanizeHindi(line)
                        "Ukrainian" in enabledLanguages && isUkrainian(text) -> value =
                            romanizeCyrillic(line)

                        "Russian" in enabledLanguages && isRussian(text) -> value = romanizeCyrillic(line)
                        "Serbian" in enabledLanguages && isSerbian(text) -> value = romanizeCyrillic(line)
                        "Bulgarian" in enabledLanguages && isBulgarian(text) -> value =
                            romanizeCyrillic(line)

                        "Belarusian" in enabledLanguages && isBelarusian(text) -> value =
                            romanizeCyrillic(line)

                        "Kyrgyz" in enabledLanguages && isKyrgyz(text) -> value = romanizeCyrillic(line)
                        "Macedonian" in enabledLanguages && isMacedonian(text) -> value =
                            romanizeCyrillic(line)
                    }

                    newEntry.romanizedTextFlow.value = value
                    newEntry
                }
            }
        }
        lines = processedLines
    }

    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    val mergedLyricsList: List<LyricsListItem> = remember(lines) {
        lines.mapIndexed { i, entry -> LyricsListItem.Line(i, entry) }
    }

    
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
    var previousScrollActiveIndices by remember {
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

    val isLyricsProviderShown = lyricsEntity != null && lyricsEntity.provider != "Unknown" && !isSelectionModeActive

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
        previousScrollActiveIndices = emptySet()
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
            val effectivePosition = position + lyricsOffset
            
            val newActiveIndices = findActiveLineIndices(lines, effectivePosition)
            val newScrollActiveIndices = findActiveLineIndices(lines, effectivePosition + 300L)

            val newMax = newScrollActiveIndices
                .filter { lines.getOrNull(it)?.isBackground == false }
                .maxOrNull() ?: (newScrollActiveIndices.maxOrNull() ?: -1)
            val prevMax = previousScrollActiveIndices
                .filter { lines.getOrNull(it)?.isBackground == false }
                .maxOrNull() ?: (previousScrollActiveIndices.maxOrNull() ?: -1)

            val aLineJustEnded = previousScrollActiveIndices.isNotEmpty() &&
                previousScrollActiveIndices.any { idx ->
                    idx !in newScrollActiveIndices && lines.getOrNull(idx)?.isBackground == false
                }

            val anyBgStillActive = newScrollActiveIndices.any { lines.getOrNull(it)?.isBackground == true }

            val shouldScroll = when {
                anyBgStillActive && newMax == scrollTargetIndex -> false
                aLineJustEnded && newMax != scrollTargetIndex -> true
                newMax > prevMax && newMax != -1 && newMax != scrollTargetIndex -> true
                previousScrollActiveIndices.isEmpty() && newScrollActiveIndices.isNotEmpty() && newMax != scrollTargetIndex -> true
                else -> false
            }

            if (shouldScroll) {
                val targetToScroll = if (newMax == -1 && aLineJustEnded) {
                    if (prevMax != -1 && prevMax + 1 < lines.size) prevMax + 1 else scrollTargetIndex
                } else {
                    newMax
                }
                if (targetToScroll != -1 && targetToScroll != scrollTargetIndex) {
                    scrollTargetIndex = targetToScroll
                }
            }
            previousScrollActiveIndices = newScrollActiveIndices
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

    LaunchedEffect(scrollTargetIndex, isAutoScrollEnabled) {
        if (scrollTargetIndex != -1 && isAutoScrollEnabled) {
            deferredCurrentLineIndex = scrollTargetIndex
        }
    }

    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val velocityTracker = remember { VelocityTracker() }
    val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var isInitialLayout by remember { mutableStateOf(true) }

    val displayedCurrentLineIndex = deferredCurrentLineIndex
    val activeListIndex = remember(displayedCurrentLineIndex, mergedLyricsList) {
        mergedLyricsList.indexOfFirst { it is LyricsListItem.Line && it.index == displayedCurrentLineIndex }.coerceAtLeast(0)
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val anchorY = maxHeightPx * 0.35f
        val lineHeightPx = with(density) { 68.dp.toPx() }

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

        val minOffset = remember(positions, itemHeights.toMap(), maxHeightPx, anchorY) {
            val lastIdx = mergedLyricsList.size - 1
            if (lastIdx < 0) return@remember -maxHeightPx
            val lastPos = positions[lastIdx] ?: ((lastIdx - activeListIndex) * lineHeightPx)
            val lastHeight = itemHeights[lastIdx]?.toFloat() ?: lineHeightPx
            val paddingTop = with(density) { 100.dp.toPx() }
            (paddingTop - anchorY - lastPos - lastHeight)
        }

        val maxOffset = remember(positions, itemHeights.toMap(), maxHeightPx, anchorY) {
            val firstPos = positions[0] ?: ((0 - activeListIndex) * lineHeightPx)
            val paddingBottom = with(density) { 150.dp.toPx() }
            (maxHeightPx - paddingBottom - anchorY - firstPos)
        }

        val safeMinOffset = minOf(minOffset, maxOffset - maxHeightPx)
        val safeMaxOffset = maxOf(maxOffset, 0f)

        LaunchedEffect(isAutoScrollEnabled) {
            if (isAutoScrollEnabled) {
                val start = userManualOffset
                val dist = kotlin.math.abs(start)
                if (dist < 1f) { userManualOffset = 0f; return@LaunchedEffect }
                
                isInitialLayout = true
                withFrameMillis { }
                
                val duration = (dist / 4f).toInt().coerceIn(200, 600)
                var lastValue = start
                val anim = Animatable(start)
                anim.animateTo(0f, tween(duration, easing = FastOutSlowInEasing)) {
                    val delta = value - lastValue
                    userManualOffset += delta
                    lastValue = value
                }
                isInitialLayout = false
            }
        }

        LaunchedEffect(showLyrics) {
            if (showLyrics) {
                isInitialLayout = true
                snapshotFlow { itemHeights.size }
                    .first { it >= minOf(mergedLyricsList.size, 5) }
                isInitialLayout = false
            }
        }

        var lastPositions by remember { mutableStateOf<Map<Int, Float>>(emptyMap()) }
        LaunchedEffect(activeListIndex) {
            if ((!isAutoScrollEnabled || isInitialLayout) && lastPositions.isNotEmpty()) {
                val shift = lastPositions[activeListIndex] ?: 0f
                userManualOffset += shift
            }
            lastPositions = positions
        }

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
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        } else if (isSynced) {
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
                                if (isInitialLayout) {
                                    // Discard input while layout is settling
                                    continue
                                }
                                flingJob?.cancel()
                                velocityTracker.resetTracking()
                                isAutoScrollEnabled = false
                                lastPreviewTime = System.currentTimeMillis()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                verticalDrag(down.id) { change ->
                                    val dragAmount = change.positionChange().y
                                    userManualOffset = (userManualOffset + dragAmount).coerceIn(safeMinOffset, safeMaxOffset)
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                }

                                val velocity = velocityTracker.calculateVelocity().y
                                flingJob = scope.launch {
                                    AnimationState(
                                        initialValue = userManualOffset,
                                        initialVelocity = velocity
                                    ).animateDecay(decayAnimSpec) {
                                        val clamped = value.coerceIn(safeMinOffset, safeMaxOffset)
                                        userManualOffset = clamped
                                        if (value != clamped) cancelAnimation()
                                    }
                                }
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLyricsProviderShown) {
                        val providerBase = anchorY + (positions[0] ?: 0f) - with(density) { 32.dp.toPx() }
                        Text(
                            text = "Lyrics from ${lyricsEntity.provider}",
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

                        LaunchedEffect(isAutoScrollEnabled, clampedTarget, isInitialLayout) {
                            if (isAutoScrollEnabled || isInitialLayout) frozenOffset.floatValue = clampedTarget
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
                                            respectAgentPositioning && (if (item.isBackground) pairedAgent else item.agent) == "v1" -> Alignment.Start
                                            respectAgentPositioning && (if (item.isBackground) pairedAgent else item.agent) == "v2" -> Alignment.End
                                            respectAgentPositioning && (if (item.isBackground) pairedAgent else item.agent) == "v1000" -> Alignment.CenterHorizontally
                                            else -> when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> Alignment.Start
                                                LyricsPosition.CENTER -> Alignment.CenterHorizontally
                                                LyricsPosition.RIGHT -> Alignment.End
                                            }
                                        }
                                        val agentTextAlign = when {
                                            respectAgentPositioning && (if (item.isBackground) pairedAgent else item.agent) == "v1" -> TextAlign.Left
                                            respectAgentPositioning && (if (item.isBackground) pairedAgent else item.agent) == "v2" -> TextAlign.Right
                                            respectAgentPositioning && (if (item.isBackground) pairedAgent else item.agent) == "v1000" -> TextAlign.Center
                                            else -> when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> TextAlign.Left
                                                LyricsPosition.CENTER -> TextAlign.Center
                                                LyricsPosition.RIGHT -> TextAlign.Right
                                            }
                                        }

                                        Box(modifier = itemModifier, contentAlignment = when {
                                            respectAgentPositioning && item.agent == "v1" -> Alignment.CenterStart
                                            respectAgentPositioning && item.agent == "v2" -> Alignment.CenterEnd
                                            item.isBackground -> Alignment.Center
                                            respectAgentPositioning && item.agent == "v1000" -> Alignment.Center
                                            else -> when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> Alignment.CenterStart
                                                LyricsPosition.RIGHT -> Alignment.CenterEnd
                                                else -> Alignment.Center
                                            }
                                        }) {
                                            @Composable
                                            fun LyricContent() {
                                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = agentAlignment) {
                                                    val isActiveLine = isActiveByIndex
                                                    val inactiveAlpha = if (item.isBackground) 0.08f else 0.2f
                                                    val activeAlpha = 1f
                                                    val focusedAlpha = if (item.isBackground) 0.5f else 0.3f
                                                    val targetAlpha = if (item.isBackground) {
                                                        activeAlpha
                                                    } else if (isActiveLine) {
                                                        activeAlpha
                                                    } else if (isAutoScrollEnabled && displayedCurrentLineIndex >= 0) {
                                                        when (abs(index - displayedCurrentLineIndex)) {
                                                            0 -> focusedAlpha
                                                            1 -> 0.2f; 2 -> 0.2f; 3 -> 0.15f; 4 -> 0.1f; else -> 0.08f
                                                        }
                                                    } else inactiveAlpha
                                                    val animatedAlpha by animateFloatAsState(targetAlpha, tween(250), label = "lyricsLineAlpha")
                                                    val lineColor = expressiveAccent.copy(alpha = if (item.isBackground) focusedAlpha else animatedAlpha)
                                                    val alignment = agentTextAlign
                                                    val romanizedTextState by item.romanizedTextFlow.collectAsState()
                                                    val isRomanizedAvailable = romanizedTextState != null
                                                    val mainTextRaw = if (romanizeAsMain && isRomanizedAvailable) romanizedTextState else item.text
                                                    val subTextRaw = if (romanizeAsMain && isRomanizedAvailable) item.text else romanizedTextState
                                                    val mainText = if (item.isBackground) mainTextRaw?.removePrefix("(")?.removeSuffix(")") else mainTextRaw
                                                    val subText = if (item.isBackground) subTextRaw?.removePrefix("(")?.removeSuffix(")") else subTextRaw

                                                    val lyricStyle = TextStyle(
                                                        fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal,
                                                        lineHeight = if (item.isBackground) (lyricsTextSize * 0.7f * lyricsLineSpacing).sp else (lyricsTextSize * lyricsLineSpacing).sp,
                                                        letterSpacing = (-0.5).sp,
                                                        textAlign = alignment,
                                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                        lineHeightStyle = LineHeightStyle(
                                                            alignment = LineHeightStyle.Alignment.Center,
                                                            trim = LineHeightStyle.Trim.None
                                                        )
                                                    )

                                                    val textMeasurer = rememberTextMeasurer()
                                                    if (item.words?.isNotEmpty() == true && (isActiveLine || abs(index - currentLineIndex) <= 3) && mainText != null) {
                                                        
                                                        // Smoothed player position interpolation to eliminate ExoPlayer jitter
                                                        val lyricsOffset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
                                                        var smoothPosition by remember { mutableLongStateOf(currentPositionState + lyricsOffset) }
                                                        LaunchedEffect(isActiveLine) {
                                                            if (isActiveLine) {
                                                                var lastPlayerPos = playerConnection.player.currentPosition
                                                                var lastUpdateTime = System.currentTimeMillis()
                                                                while (isActive) {
                                                                    withFrameMillis {
                                                                        val now = System.currentTimeMillis()
                                                                        val playerPos = playerConnection.player.currentPosition
                                                                        if (playerPos != lastPlayerPos) {
                                                                            lastPlayerPos = playerPos
                                                                            lastUpdateTime = now
                                                                        }
                                                                        val elapsed = now - lastUpdateTime
                                                                        smoothPosition = lastPlayerPos + lyricsOffset + (if (playerConnection.player.isPlaying) elapsed else 0)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        LaunchedEffect(isActiveLine, currentPositionState) {
                                                            if (!isActiveLine) {
                                                                smoothPosition = currentPositionState + lyricsOffset
                                                            }
                                                        }

                                                        val wordFactors = item.words.map { word ->
                                                            val wStartMs = (word.startTime * 1000).toLong()
                                                            val wEndMs = (word.endTime * 1000).toLong()
                                                            val isWordSung = smoothPosition > wEndMs
                                                            val isWordActive = smoothPosition in wStartMs..wEndMs
                                                            val sungFactor = if (isWordSung) 1f 
                                                                            else if (isWordActive) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                                                            else 0f
                                                            Triple(sungFactor, word, isWordSung)
                                                        }

                                                        val charToWordData = remember(mainText, item.words) {
                                                            val wordIdxMap = IntArray(mainText.length) { -1 }
                                                            val charInWordMap = IntArray(mainText.length) { 0 }
                                                            val wordLenMap = IntArray(mainText.length) { 1 }
                                                            var currentPos = 0
                                                            item.words.forEachIndexed { wordIdx, word ->
                                                                val rawWordText = word.text.let { 
                                                                    if (item.isBackground) {
                                                                        var t = it
                                                                        if (wordIdx == 0) t = t.removePrefix("(")
                                                                        if (wordIdx == item.words.size - 1) t = t.removeSuffix(")")
                                                                        t
                                                                    } else it
                                                                }
                                                                val indexInMain = mainText.indexOf(rawWordText, currentPos)
                                                                if (indexInMain != -1) {
                                                                    for (i in 0 until rawWordText.length) {
                                                                        val pos = indexInMain + i
                                                                        wordIdxMap[pos] = wordIdx
                                                                        charInWordMap[pos] = i
                                                                        wordLenMap[pos] = rawWordText.length
                                                                    }
                                                                    if (indexInMain + rawWordText.length < mainText.length && mainText[indexInMain + rawWordText.length] == ' ') {
                                                                        val pos = indexInMain + rawWordText.length
                                                                        wordIdxMap[pos] = wordIdx
                                                                        charInWordMap[pos] = rawWordText.length
                                                                        wordLenMap[pos] = rawWordText.length + 1
                                                                    }
                                                                    currentPos = indexInMain + rawWordText.length
                                                                }
                                                            }
                                                            Triple(wordIdxMap, charInWordMap, wordLenMap)
                                                        }

                                                        val lyricStyle = TextStyle(
                                                            fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal,
                                                            lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                                            letterSpacing = (-0.5).sp,
                                                            textAlign = alignment,
                                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                            lineHeightStyle = LineHeightStyle(
                                                                alignment = LineHeightStyle.Alignment.Center,
                                                                trim = LineHeightStyle.Trim.None
                                                            )
                                                        )

                                                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                                            val maxWidthPx = constraints.maxWidth
                                                            val layoutResult = remember(mainText, maxWidthPx) {
                                                                textMeasurer.measure(
                                                                    text = mainText,
                                                                    style = lyricStyle,
                                                                    constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
                                                                    softWrap = true
                                                                )
                                                            }
                                                            
                                                            val letterLayouts = remember(mainText) {
                                                                mainText.map { textMeasurer.measure(it.toString(), lyricStyle) }
                                                            }
                                                            
                                                            Canvas(modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(with(density) { layoutResult.size.height.toDp() })
                                                                .graphicsLayer(clip = false)
                                                            ) {
                                                                if (mainText.isEmpty()) return@Canvas
                                                                if (!isActiveLine) {
                                                                    drawText(layoutResult, color = lineColor)
                                                                } else {
                                                                    val (wordIdxMap, charInWordMap, wordLenMap) = charToWordData
                                                                    
                                                                    val lineTotalPushes = FloatArray(layoutResult.lineCount)
                                                                    val wordPushes = FloatArray(item.words.size)
                                                                    val wordWobbles = FloatArray(item.words.size)
                                                                    
                                                                    item.words.forEachIndexed { wordIdx, word ->
                                                                        val startMs = (word.startTime * 1000).toLong()
                                                                        val timeSinceStart = (smoothPosition - startMs).toFloat()
                                                                        val wobble = if (timeSinceStart in 0f..750f) {
                                                                            if (timeSinceStart < 125f) timeSinceStart / 125f
                                                                            else (1f - (timeSinceStart - 125f) / 625f).coerceAtLeast(0f)
                                                                        } else 0f
                                                                        wordWobbles[wordIdx] = wobble
                                                                        
                                                                        var wordWidth = 0f
                                                                        var firstCharIdxInWord = -1
                                                                        for (i in mainText.indices) {
                                                                            if (wordIdxMap[i] == wordIdx) {
                                                                                if (firstCharIdxInWord == -1) firstCharIdxInWord = i
                                                                                wordWidth += layoutResult.getBoundingBox(i).width
                                                                            }
                                                                        }
                                                                        if (firstCharIdxInWord != -1) {
                                                                            val push = wobble * 0.025f * wordWidth
                                                                            wordPushes[wordIdx] = push
                                                                            lineTotalPushes[layoutResult.getLineForOffset(firstCharIdxInWord)] += push
                                                                        }
                                                                    }

                                                                    val lineCurrentPushes = FloatArray(layoutResult.lineCount)
                                                                    var lastWordIdxAtLinePos = -2
                                                                    for (i in mainText.indices) {
                                                                        val lineIdx = layoutResult.getLineForOffset(i)
                                                                        val charBounds = layoutResult.getBoundingBox(i)
                                                                        val wordIdx = wordIdxMap[i]
                                                                        
                                                                        val alignShift = when(alignment) {
                                                                            TextAlign.Center -> -lineTotalPushes[lineIdx] / 2f
                                                                            TextAlign.Right -> -lineTotalPushes[lineIdx]
                                                                            else -> 0f
                                                                        }
                                                                        
                                                                        if (wordIdx != lastWordIdxAtLinePos && lastWordIdxAtLinePos >= 0) {
                                                                            lineCurrentPushes[lineIdx] += wordPushes[lastWordIdxAtLinePos]
                                                                        }
                                                                        lastWordIdxAtLinePos = wordIdx
                                                                        
                                                                        val wordPush = if (wordIdx != -1) wordPushes[wordIdx] else 0f
                                                                        val wobble = if (wordIdx != -1) wordWobbles[wordIdx] else 0f
                                                                        val (sungFactor, wordItem, isWordSung) = if (wordIdx != -1) wordFactors[wordIdx] else Triple(0f, null, false)
                                                                        
                                                                        // A word should glow if it's currently being sung (sungFactor > 0 and not fully sung)
                                                                        val shouldGlow = wordItem != null && !isWordSung && sungFactor > 0.001f

                                                                        withTransform({
                                                                            translate(left = alignShift + lineCurrentPushes[lineIdx] + wordPush / 2f + charBounds.left, top = charBounds.top)
                                                                            if (wordIdx != -1) {
                                                                                scale(1f + wobble * 0.025f, 1f + wobble * 0.015f, pivot = Offset(charBounds.width / 2f, charBounds.height))
                                                                            }
                                                                        }) {
                                                                            val charLp = if (wordItem != null) {
                                                                                val sMs = wordItem.startTime * 1000
                                                                                val dur = (wordItem.endTime * 1000 - wordItem.startTime * 1000).coerceAtLeast(100.0)
                                                                                val wProg = (smoothPosition.toDouble() - sMs) / dur
                                                                                val cInW = charInWordMap[i].toDouble()
                                                                                val wLen = wordLenMap[i].toDouble()
                                                                                ((wProg - cInW / wLen) * wLen).coerceIn(0.0, 1.0).toFloat()
                                                                            } else 0f

                                                                            if (shouldGlow) {
                                                                                val sMs = wordItem.startTime * 1000
                                                                                val eMs = wordItem.endTime * 1000
                                                                                val dur = eMs - sMs
                                                                                val wordLenText = wordItem.text.length.coerceAtLeast(1)
                                                                                val impactRatio = dur.toFloat() / wordLenText
                                                                                
                                                                                // Smooth fade-in: words fade in over the first 150ms of being sung
                                                                                // and fade out at the end
                                                                                val fadeFactor = (sungFactor * 5f).coerceIn(0f, 1f) * 
                                                                                               ((1f - sungFactor) * 8f).coerceIn(0f, 1f)

                                                                                // Impact calculation - more aggressive for visibility
                                                                                val impactFactor = (
                                                                                    ((impactRatio - 100f) / 250f).coerceIn(0f, 1f) * 0.6f +
                                                                                    ((dur.toFloat() - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f
                                                                                ).coerceIn(0f, 1f) * fadeFactor

                                                                                if (impactFactor > 0.01f) {
                                                                                    // Slightly adjusted alpha and reduced radius to prevent cropping
                                                                                    val glowAlpha = (0.9f * impactFactor).coerceIn(0f, 0.98f)
                                                                                    val baseGlowRadius = 20.dp.toPx() * impactFactor
                                                                                    
                                                                                    drawIntoCanvas { canvas ->
                                                                                        val paint = android.graphics.Paint()
                                                                                        paint.maskFilter = BlurMaskFilter(baseGlowRadius, BlurMaskFilter.Blur.NORMAL)
                                                                                        paint.color = expressiveAccent.copy(alpha = glowAlpha).toArgb()
                                                                                        paint.textSize = lyricStyle.fontSize.toPx()
                                                                                        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                                                                        
                                                                                        canvas.nativeCanvas.drawText(
                                                                                            letterLayouts[i].layoutInput.text.text,
                                                                                            0f,
                                                                                            letterLayouts[i].firstBaseline,
                                                                                            paint
                                                                                        )
                                                                                        paint.maskFilter = null
                                                                                    }
                                                                                }
                                                                            }

                                                                            // 1. Draw base layer
                                                                            val baseAlpha = if (isWordSung || charLp > 0.99f) 1f else (focusedAlpha + (1f - focusedAlpha) * sungFactor)
                                                                            drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = if (wordIdx == -1) focusedAlpha else baseAlpha))
                                                                            
                                                                            // 2. Draw highlight layer
                                                                            if (!isWordSung && charLp > 0f && charLp < 1f) {
                                                                                val fXL = charBounds.width * charLp
                                                                                val eW = (charBounds.width * 0.45f).coerceAtLeast(1f)
                                                                                val sWL = (fXL - eW).coerceAtLeast(0f)
                                                                                if (sWL > 0f) {
                                                                                    clipRect(left = 0f, top = 0f, right = sWL, bottom = charBounds.height) {
                                                                                        drawText(letterLayouts[i], color = expressiveAccent)
                                                                                    }
                                                                                }
                                                                                for (j in 0 until 12) {
                                                                                    val start = sWL + (j * eW / 12f)
                                                                                    val end = (sWL + ((j + 1) * eW / 12f) + 0.5f).coerceAtMost(fXL)
                                                                                    if (end > start) {
                                                                                        clipRect(left = start, top = 0f, right = end, bottom = charBounds.height) {
                                                                                            drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = 1f - (j + 0.5f) / 12f))
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
                                                        val lyricStyleSingle = TextStyle(

                                                            fontSize = if (item.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp,

                                                            fontWeight = FontWeight.Bold,

                                                            fontStyle = if (item.isBackground) FontStyle.Italic else FontStyle.Normal,

                                                            lineHeight = if (item.isBackground) (lyricsTextSize * 0.7f * lyricsLineSpacing).sp else (lyricsTextSize * lyricsLineSpacing).sp,

                                                            letterSpacing = (-0.5).sp,

                                                            textAlign = alignment,

                                                            platformStyle = PlatformTextStyle(includeFontPadding = false),

                                                            lineHeightStyle = LineHeightStyle(

                                                                alignment = LineHeightStyle.Alignment.Center,

                                                                trim = LineHeightStyle.Trim.None

                                                            )

                                                        )
                                                        Text(mainText ?: "", style = lyricStyleSingle.copy(color = if (isActiveLine) expressiveAccent else lineColor),
                                                            modifier = Modifier.fillMaxWidth().graphicsLayer(transformOrigin = when (alignment) {
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
                contentPadding = PaddingValues(top = 100.dp, bottom = 150.dp),
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
                if (lyrics == null && (translationStatus is LyricsTranslationHelper.TranslationStatus.Idle || translationStatus is LyricsTranslationHelper.TranslationStatus.Error)) {
                    item { ShimmerHost { repeat(10) { Box(contentAlignment = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> Alignment.CenterStart; LyricsPosition.CENTER -> Alignment.Center; else -> Alignment.CenterEnd
                    }, modifier = Modifier.fillMaxWidth().padding(horizontal = when (lyricsTextPosition) {
                        LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp
                    }, vertical = 4.dp)) { TextPlaceholder() } } } }
                } else {
                    itemsIndexed(mergedLyricsList, key = { _, item -> (item as LyricsListItem.Line).index }) { listIndex, item ->
                        val lineItem = item as LyricsListItem.Line
                        val isSel = selectedIndices.contains(lineItem.index)
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).combinedClickable(
                            onClick = { if (isSelectionModeActive) { if (isSel) { selectedIndices.remove(lineItem.index); if (selectedIndices.isEmpty()) isSelectionModeActive = false } else { if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(lineItem.index) else showMaxSelectionToast = true } } },
                            onLongClick = { if (!isSelectionModeActive) { isSelectionModeActive = true; selectedIndices.add(lineItem.index) } }
                        ).background(if (isSel && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent).padding(
                            start = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp },
                            end = when (lyricsTextPosition) { LyricsPosition.LEFT, LyricsPosition.RIGHT -> 11.dp; else -> 24.dp },
                            top = if (lineItem.entry.isBackground) 2.dp else 12.dp,
                            bottom = if (lineItem.entry.isBackground) 2.dp else if (mergedLyricsList.getOrNull(listIndex + 1)?.let { it is LyricsListItem.Line && it.entry.isBackground } == true) 4.dp else 12.dp
                        ), contentAlignment = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> Alignment.CenterStart; LyricsPosition.RIGHT -> Alignment.CenterEnd; else -> Alignment.Center
                        }) { Text(lineItem.entry.text, fontSize = 24.sp, fontWeight = FontWeight.Normal, color = expressiveAccent, textAlign = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left; LyricsPosition.CENTER -> TextAlign.Center; else -> TextAlign.Right
                        }) }
                    }
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            AnimatedVisibility(visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                FilledTonalButton(onClick = {
                    flingJob?.cancel()
                    flingJob = null
                    if (scrollTargetIndex != -1) {
                        val newActiveListIndex = mergedLyricsList.indexOfFirst { it is LyricsListItem.Line && it.index == scrollTargetIndex }.coerceAtLeast(0)
                        val itemGapPx = with(density) { 16.dp.toPx() }
                        val lineHeightPx = with(density) { 68.dp.toPx() }
                        val shift = positions[newActiveListIndex] ?: ((newActiveListIndex - activeListIndex) * (lineHeightPx + itemGapPx))
                        userManualOffset += shift
                        deferredCurrentLineIndex = scrollTargetIndex
                    }
                    isAutoScrollEnabled = true
                }) {
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
                    Text(stringResource(R.string.share_lyrics), fontWeight = FontWeight.Normal, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
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
        rememberAdjustedFontSize(txt, availW, availH, density, 50.sp, 22.sp, TextStyle(color = previewTextColor, fontWeight = FontWeight.Normal, textAlign = align), measurer)
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

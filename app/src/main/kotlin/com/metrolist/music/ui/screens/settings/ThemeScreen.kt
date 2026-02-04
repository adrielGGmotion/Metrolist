package com.metrolist.music.ui.screens.settings

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

val PaletteColors = listOf(
    Color(0xFFED5564), // Default Red
    Color(0xFFD81B60), // Pink
    Color(0xFF8E24AA), // Purple
    Color(0xFF5E35B1), // Deep Purple
    Color(0xFF3949AB), // Indigo
    Color(0xFF1E88E5), // Blue
    Color(0xFF039BE5), // Light Blue
    Color(0xFF00ACC1), // Cyan
    Color(0xFF00897B), // Teal
    Color(0xFF43A047), // Green
    Color(0xFF7CB342), // Light Green
    Color(0xFFC0CA33), // Lime
    Color(0xFFFDD835), // Yellow
    Color(0xFFFFB300), // Amber
    Color(0xFFFB8C00), // Orange
    Color(0xFFF4511E), // Deep Orange
    Color(0xFF6D4C41), // Brown
    Color(0xFF757575), // Grey
    Color(0xFF546E7A), // Blue Grey
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    navController: NavController,
) {
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (selectedThemeColorInt, onSelectedThemeColorChange) = rememberPreference(
        SelectedThemeColorKey,
        DefaultThemeColor.toArgb()
    )

    val selectedThemeColor = Color(selectedThemeColorInt)
    val useSystemColors = selectedThemeColor == DefaultThemeColor

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeMockup(
                            darkMode = darkMode,
                            pureBlack = pureBlack,
                            themeColor = selectedThemeColor
                        )
                    }
                    Card(
                        modifier = Modifier
                            .width(400.dp)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        ThemeControls(
                            darkMode = darkMode,
                            onDarkModeChange = onDarkModeChange,
                            pureBlack = pureBlack,
                            onPureBlackChange = onPureBlackChange,
                            selectedThemeColor = selectedThemeColor,
                            onSelectedThemeColorChange = { onSelectedThemeColorChange(it.toArgb()) },
                            useSystemColors = useSystemColors,
                            onUseSystemColorsChange = { useSystem ->
                                if (useSystem) {
                                    onSelectedThemeColorChange(DefaultThemeColor.toArgb())
                                } else {
                                    // If switching off system, pick first color if current is default
                                    if (selectedThemeColor == DefaultThemeColor) {
                                        onSelectedThemeColorChange(PaletteColors.first().toArgb())
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeMockup(
                            darkMode = darkMode,
                            pureBlack = pureBlack,
                            themeColor = selectedThemeColor
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        ThemeControls(
                            darkMode = darkMode,
                            onDarkModeChange = onDarkModeChange,
                            pureBlack = pureBlack,
                            onPureBlackChange = onPureBlackChange,
                            selectedThemeColor = selectedThemeColor,
                            onSelectedThemeColorChange = { onSelectedThemeColorChange(it.toArgb()) },
                            useSystemColors = useSystemColors,
                            onUseSystemColorsChange = { useSystem ->
                                if (useSystem) {
                                    onSelectedThemeColorChange(DefaultThemeColor.toArgb())
                                } else {
                                    if (selectedThemeColor == DefaultThemeColor) {
                                        onSelectedThemeColorChange(PaletteColors.first().toArgb())
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeControls(
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit,
    useSystemColors: Boolean,
    onUseSystemColorsChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ModeCircle(
                color = Color(0xFFE0E0E0), // Light Grey
                isSelected = darkMode == DarkMode.OFF,
                onClick = { onDarkModeChange(DarkMode.OFF) }
            )
            ModeCircle(
                color = Color(0xFF424242), // Dark Grey
                isSelected = darkMode == DarkMode.ON && !pureBlack,
                onClick = {
                    onDarkModeChange(DarkMode.ON)
                    onPureBlackChange(false)
                }
            )
            ModeCircle(
                color = Color(0xFF9E9E9E), // Medium Grey (System)
                isSelected = darkMode == DarkMode.AUTO,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "System",
                        tint = Color.Black
                    )
                },
                onClick = { onDarkModeChange(DarkMode.AUTO) }
            )
            ModeCircle(
                color = Color.Black,
                isSelected = pureBlack && darkMode == DarkMode.ON,
                onClick = {
                    onDarkModeChange(DarkMode.ON)
                    onPureBlackChange(true)
                }
            )
        }

        // System Colors Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUseSystemColorsChange(!useSystemColors) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.use_system_colors),
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = useSystemColors,
                onCheckedChange = onUseSystemColorsChange,
                thumbContent = if (useSystemColors) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else null
            )
        }

        // Palette List
        if (!useSystemColors) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(PaletteColors) { color ->
                    PaletteCircle(
                        seedColor = color,
                        isSelected = selectedThemeColor == color,
                        onClick = { onSelectedThemeColorChange(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModeCircle(
    color: Color,
    isSelected: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val shape = if (isSelected) RoundedCornerShape(16.dp) else CircleShape
    val borderStroke = if (isSelected) 3.dp else 0.dp
    val borderColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(color)
            .border(borderStroke, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon?.invoke()
    }
}

@Composable
fun PaletteCircle(
    seedColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 50.dp, // 50.dp for circle (half size), 16.dp for rect
        label = "cornerRadius"
    )
    val shape = RoundedCornerShape(cornerRadius)
    
    val colorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isSystemInDarkTheme(), // Use current system/app state for preview
        style = PaletteStyle.TonalSpot
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(Color.Transparent)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = colorScheme.primary,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top Half - Primary
            drawRect(
                color = colorScheme.primary,
                topLeft = Offset(0f, 0f),
                size = Size(width, height / 2)
            )
            // Bottom Left - Secondary
            drawRect(
                color = colorScheme.secondary,
                topLeft = Offset(0f, height / 2),
                size = Size(width / 2, height / 2)
            )
            // Bottom Right - Tertiary
            drawRect(
                color = colorScheme.tertiary,
                topLeft = Offset(width / 2, height / 2),
                size = Size(width / 2, height / 2)
            )
        }
    }
}

@Composable
fun ThemeMockup(
    darkMode: DarkMode,
    pureBlack: Boolean,
    themeColor: Color
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (darkMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }

    MetrolistTheme(
        darkTheme = useDark,
        pureBlack = pureBlack,
        themeColor = themeColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Mock App Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(120.dp)
                                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                        )
                    }
                }

                // Mock Content List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(6) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .height(12.dp)
                                        .fillMaxWidth(0.7f)
                                        .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .height(10.dp)
                                        .fillMaxWidth(0.4f)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }

                // Mock Mini Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        )
                        Column(
                             modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f),
                             verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                             Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .fillMaxWidth(0.5f)
                                    .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
                
                // Mock Nav Bar
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                             Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

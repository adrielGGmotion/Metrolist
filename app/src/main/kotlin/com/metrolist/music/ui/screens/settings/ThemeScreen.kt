package com.metrolist.music.ui.screens.settings

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

// Define 19 color palettes with descriptive names for accessibility
data class ThemePalette(
    val name: String,
    val seedColor: Color
)

val PaletteColors = listOf(
    ThemePalette("Crimson", Color(0xFFED5564)),
    ThemePalette("Rose", Color(0xFFD81B60)),
    ThemePalette("Purple", Color(0xFF8E24AA)),
    ThemePalette("Deep Purple", Color(0xFF5E35B1)),
    ThemePalette("Indigo", Color(0xFF3949AB)),
    ThemePalette("Blue", Color(0xFF1E88E5)),
    ThemePalette("Sky Blue", Color(0xFF039BE5)),
    ThemePalette("Cyan", Color(0xFF00ACC1)),
    ThemePalette("Teal", Color(0xFF00897B)),
    ThemePalette("Green", Color(0xFF43A047)),
    ThemePalette("Light Green", Color(0xFF7CB342)),
    ThemePalette("Lime", Color(0xFFC0CA33)),
    ThemePalette("Yellow", Color(0xFFFDD835)),
    ThemePalette("Amber", Color(0xFFFFB300)),
    ThemePalette("Orange", Color(0xFFFB8C00)),
    ThemePalette("Deep Orange", Color(0xFFF4511E)),
    ThemePalette("Brown", Color(0xFF6D4C41)),
    ThemePalette("Grey", Color(0xFF757575)),
    ThemePalette("Blue Grey", Color(0xFF546E7A)),
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.theme_colors)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeMockup(
                            darkMode = darkMode,
                            pureBlack = pureBlack,
                            themeColor = selectedThemeColor,
                            isLandscape = isLandscape
                        )
                    }
                    Card(
                        modifier = Modifier
                            .width(400.dp)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ThemeControls(
                            darkMode = darkMode,
                            onDarkModeChange = onDarkModeChange,
                            pureBlack = pureBlack,
                            onPureBlackChange = onPureBlackChange,
                            selectedThemeColor = selectedThemeColor,
                            onSelectedThemeColorChange = { onSelectedThemeColorChange(it.toArgb()) }
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
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeMockup(
                            darkMode = darkMode,
                            pureBlack = pureBlack,
                            themeColor = selectedThemeColor,
                            isLandscape = false
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ThemeControls(
                            darkMode = darkMode,
                            onDarkModeChange = onDarkModeChange,
                            pureBlack = pureBlack,
                            onPureBlackChange = onPureBlackChange,
                            selectedThemeColor = selectedThemeColor,
                            onSelectedThemeColorChange = { onSelectedThemeColorChange(it.toArgb()) }
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
    onSelectedThemeColorChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Theme Mode Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Mode Selector Row (Light/Dark/System/Pure Black)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                // Light Mode
                ModeCircle(
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    targetMode = DarkMode.OFF,
                    targetPureBlack = false,
                    onClick = {
                        onDarkModeChange(DarkMode.OFF)
                        onPureBlackChange(false)
                    },
                    showIcon = false
                )
                
                // Dark Mode
                ModeCircle(
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    targetMode = DarkMode.ON,
                    targetPureBlack = false,
                    onClick = {
                        onDarkModeChange(DarkMode.ON)
                        onPureBlackChange(false)
                    },
                    showIcon = false
                )
                
                // System Mode (with sync arrows)
                ModeCircle(
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    targetMode = DarkMode.AUTO,
                    targetPureBlack = false,
                    onClick = {
                        onDarkModeChange(DarkMode.AUTO)
                        onPureBlackChange(false)
                    },
                    showIcon = true
                )
                
                // Pure Black Mode
                ModeCircle(
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    targetMode = DarkMode.ON,
                    targetPureBlack = true,
                    onClick = {
                        onDarkModeChange(DarkMode.ON)
                        onPureBlackChange(true)
                    },
                    showIcon = false
                )
            }
        }

        // Color Palette Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.color_palette),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Palette List
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(PaletteColors) { palette ->
                    PaletteItem(
                        palette = palette,
                        isSelected = selectedThemeColor == palette.seedColor,
                        onClick = { onSelectedThemeColorChange(palette.seedColor) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModeCircle(
    darkMode: DarkMode,
    pureBlack: Boolean,
    targetMode: DarkMode,
    targetPureBlack: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isSelected = darkMode == targetMode && pureBlack == targetPureBlack
    
    // Determine effective dark theme for color generation
    val effectiveDark = when (targetMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    
    // Generate appropriate color scheme for this mode
    val modeColorScheme = rememberDynamicColorScheme(
        seedColor = DefaultThemeColor,
        isDark = effectiveDark,
        style = PaletteStyle.TonalSpot
    )
    
    // Choose the fill color based on mode
    val fillColor = when {
        targetPureBlack -> Color.Black
        effectiveDark -> modeColorScheme.surface
        else -> modeColorScheme.surface
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fillColor)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics {
                contentDescription = when {
                    targetPureBlack -> "Pure Black mode"
                    targetMode == DarkMode.OFF -> "Light mode"
                    targetMode == DarkMode.ON -> "Dark mode"
                    else -> "System mode"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            showIcon -> {
                // System mode - show sync icon
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null,
                    tint = modeColorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            isSelected -> {
                // Selected - show checkmark
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inversePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PaletteItem(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    // Generate color scheme from seed color
    val colorScheme = rememberDynamicColorScheme(
        seedColor = palette.seedColor,
        isDark = isSystemDark,
        style = PaletteStyle.TonalSpot
    )
    
    // Animate corner radius with bouncier spring animation
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 48.dp * 0.25f else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
    )
    
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics {
                contentDescription = "${palette.name} palette"
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Top Half - OnPrimary (0° to 180°)
            drawRect(
                color = colorScheme.onPrimary,
                topLeft = Offset(0f, 0f),
                size = Size(width, height / 2)
            )
            
            // Bottom Left Quarter - Secondary (180° to 270°)
            drawRect(
                color = colorScheme.secondary,
                topLeft = Offset(0f, height / 2),
                size = Size(width / 2, height / 2)
            )
            
            // Bottom Right Quarter - Tertiary (270° to 360°)
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
    themeColor: Color,
    isLandscape: Boolean
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (darkMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    
    val configuration = LocalConfiguration.current
    // Adapt aspect ratio to device form factor
    val mockupAspectRatio = if (isLandscape) {
        configuration.screenHeightDp.toFloat() / configuration.screenWidthDp.toFloat()
    } else {
        configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    }

    MetrolistTheme(
        darkTheme = useDark,
        pureBlack = pureBlack,
        themeColor = themeColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(mockupAspectRatio.coerceIn(0.5f, 0.7f))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        )
                    }
                }

                // Content area with color demonstration
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Secondary block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp))
                        )
                        
                        // Tertiary block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(6.dp))
                        )
                    }
                }

                // FAB
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    )
                }
            }
        }
    }
}

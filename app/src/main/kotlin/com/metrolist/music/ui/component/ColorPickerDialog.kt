package com.metrolist.music.ui.component

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.R
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val currentColor = remember(hue, saturation, value) {
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    val presets = remember {
        listOf(
            Color(0xFFF44336), // Red
            Color(0xFFE91E63), // Pink
            Color(0xFF9C27B0), // Purple
            Color(0xFF673AB7), // Deep Purple
            Color(0xFF3F51B5), // Indigo
            Color(0xFF2196F3), // Blue
            Color(0xFF03A9F4), // Light Blue
            Color(0xFF00BCD4), // Cyan
            Color(0xFF009688), // Teal
            Color(0xFF4CAF50), // Green
            Color(0xFF8BC34A), // Light Green
            Color(0xFFCDDC39), // Lime
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFFC107), // Amber
            Color(0xFFFF9800), // Orange
            Color(0xFFFF5722), // Deep Orange
            Color(0xFF795548), // Brown
            Color(0xFF607D8B), // Blue Grey
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Custom Color") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                )

                // Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (color == currentColor) 2.dp else 0.dp,
                                    color = if (color == currentColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(color.toArgb(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sliders
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Hue: ${hue.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f
                    )

                    Text(text = "Saturation: ${(saturation * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f
                    )

                    Text(text = "Value: ${(value * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

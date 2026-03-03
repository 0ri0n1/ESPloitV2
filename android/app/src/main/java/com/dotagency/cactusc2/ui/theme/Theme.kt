package com.dotagency.cactusc2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Dark-only Material 3 color scheme mapping Cactus palette to M3 roles. */
private val CactusDarkColorScheme = darkColorScheme(
    primary = CactusAccent,
    secondary = CactusGreen,
    tertiary = CactusYellow,
    background = CactusBg,
    surface = CactusSurface,
    onPrimary = CactusBg,
    onSecondary = CactusBg,
    onTertiary = CactusBg,
    onBackground = CactusText,
    onSurface = CactusText,
    error = CactusRed,
)

/** App-wide theme wrapper. Always dark — no light variant. */
@Composable
fun CactusC2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CactusDarkColorScheme,
        content = content
    )
}

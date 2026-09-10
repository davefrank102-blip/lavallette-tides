package com.lavallette.tides.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val OceanDeep = Color(0xFF062838)
val OceanMid = Color(0xFF0A4D6E)
val OceanBright = Color(0xFF1A7FA8)
val Foam = Color(0xFFB8E0F0)
val Sand = Color(0xFFE8D5A3)
val Coral = Color(0xFFFF8A65)
val Sun = Color(0xFFFFE08A)
val SeaGlass = Color(0xFF5EC8B0)
val CardGlass = Color(0xCC0D3F57)
val TextPrimary = Color(0xFFF5FBFF)
val TextMuted = Color(0xFFA9C7D6)

private val OceanColors = darkColorScheme(
    primary = SeaGlass,
    onPrimary = OceanDeep,
    secondary = Sand,
    onSecondary = OceanDeep,
    tertiary = Coral,
    background = OceanDeep,
    onBackground = TextPrimary,
    surface = CardGlass,
    onSurface = TextPrimary,
    surfaceVariant = OceanMid,
    onSurfaceVariant = TextMuted
)

val OceanGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0A3D5C),
        Color(0xFF062838),
        Color(0xFF041C28)
    )
)

@Composable
fun LavalletteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OceanColors,
        content = content
    )
}

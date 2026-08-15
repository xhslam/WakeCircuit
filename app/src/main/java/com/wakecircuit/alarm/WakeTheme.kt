package com.wakecircuit.alarm

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF070B18)
val Panel = Color(0xFF121A31)
val PanelAlt = Color(0xFF192442)
val Signal = Color(0xFFFF6B4A)
val SignalSoft = Color(0xFFFFB4A5)
val Mint = Color(0xFF5CE1C4)
val TextMain = Color(0xFFF5F7FF)
val TextMuted = Color(0xFFABB7D3)

private val WakeColors = darkColorScheme(
    primary = Signal,
    onPrimary = Color(0xFF2A0A00),
    secondary = Mint,
    onSecondary = Color(0xFF00382F),
    background = Ink,
    onBackground = TextMain,
    surface = Panel,
    onSurface = TextMain,
    surfaceVariant = PanelAlt,
    onSurfaceVariant = TextMuted,
    error = Color(0xFFFFB4AB)
)

@Composable
fun WakeCircuitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WakeColors, content = content)
}

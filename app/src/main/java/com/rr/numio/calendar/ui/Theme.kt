package com.rr.numio.calendar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Numio dark palette (same family as Clock, Notes and Decibel).
val NumioYellow = Color(0xFFF5C427)
val NumioInk = Color(0xFF1A1400)      // text on yellow
val NumioBg = Color(0xFF121212)
val NumioCard = Color(0xFF1C1C1C)
val NumioKey = Color(0xFF262626)
val NumioText = Color(0xFFF2F2F2)
val NumioMuted = Color(0xFF9A9A9A)
val NumioSunday = Color(0xFFF07A6A)   // Sundays and holidays

// Event colours from the mockup.
val EventPersonal = NumioYellow
val EventWork = Color(0xFF3CC4B4)
val EventGoogle = Color(0xFF5B9BF0)
val EventPrivate = Color(0xFFB08CF0)

private val scheme = darkColorScheme(
    primary = NumioYellow,
    onPrimary = NumioInk,
    background = NumioBg,
    onBackground = NumioText,
    surface = NumioCard,
    onSurface = NumioText,
    surfaceVariant = NumioKey,
    onSurfaceVariant = NumioMuted,
)

@Composable
fun NumioCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

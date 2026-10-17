package com.rr.numio.calendar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Numio colours, dark or light. Every colour below reads [Palette.dark], so when the theme
 * changes, everything that uses them is drawn again in the new colours.
 */
object Palette {
    var dark by mutableStateOf(true)
}

private fun pick(dark: Long, light: Long) = Color(if (Palette.dark) dark else light)

val NumioYellow get() = pick(0xFFF5C427, 0xFFD69E00)
val NumioInk = Color(0xFF1A1400)                        // text on yellow, both themes
val NumioBg get() = pick(0xFF121212, 0xFFF6F4EF)
val NumioCard get() = pick(0xFF1C1C1C, 0xFFFFFFFF)
val NumioKey get() = pick(0xFF262626, 0xFFE9E6DF)
val NumioText get() = pick(0xFFF2F2F2, 0xFF1B1B1B)
val NumioMuted get() = pick(0xFF9A9A9A, 0xFF6E6E6E)
val NumioSunday get() = pick(0xFFF07A6A, 0xFFD24A38)   // Sundays and holidays

// Event colours.
val EventPersonal get() = NumioYellow
val EventWork get() = pick(0xFF3CC4B4, 0xFF13998A)
val EventGoogle get() = pick(0xFF5B9BF0, 0xFF2F6FD0)
val EventFestival get() = pick(0xFFF29B4B, 0xFFDB7518)
val EventPrivate get() = pick(0xFFB08CF0, 0xFF7E57D0)

// Malayalam calendar.
val MalayalamGold get() = pick(0xFFE3B15E, 0xFF9C6A10)
val MalayalamDim get() = pick(0xFFA88A55, 0xFF9A8152)
val MalayalamSoft get() = pick(0xFFC9AE80, 0xFF7D6331)
val MalayalamCard get() = pick(0xFF261F12, 0xFFFBF0D9)

// Hijri calendar.
val HijriGreen get() = pick(0xFF7FCFA8, 0xFF23845A)
val HijriDim get() = pick(0xFF5E9C80, 0xFF5B8E76)
val HijriCard get() = pick(0xFF15241D, 0xFFE2F2E9)
val HijriLabel get() = pick(0xFF8FB8A4, 0xFF4D8268)
val HijriSoft get() = pick(0xFFA9C9B9, 0xFF3F6E58)
val HijriStrong get() = pick(0xFF9FE0BF, 0xFF1D7A50)

// Holiday groups and small controls.
val ChristianBlue get() = pick(0xFF8FB4F0, 0xFF3D73CC)
val NationalGrey get() = pick(0xFFB0B0B0, 0xFF7A7A7A)
val ChoiceOn get() = pick(0xFF2A2410, 0xFFFFF1C2)
val ChoiceOff get() = pick(0xFF161616, 0xFFF1EFEA)
val ChoiceBorder get() = pick(0xFF333333, 0xFFD9D5CC)

@Composable
fun NumioCalendarTheme(content: @Composable () -> Unit) {
    val scheme = if (Palette.dark) darkColorScheme(
        primary = NumioYellow, onPrimary = NumioInk, background = NumioBg, onBackground = NumioText,
        surface = NumioCard, onSurface = NumioText, surfaceVariant = NumioKey, onSurfaceVariant = NumioMuted,
    ) else lightColorScheme(
        primary = NumioYellow, onPrimary = NumioInk, background = NumioBg, onBackground = NumioText,
        surface = NumioCard, onSurface = NumioText, surfaceVariant = NumioKey, onSurfaceVariant = NumioMuted,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

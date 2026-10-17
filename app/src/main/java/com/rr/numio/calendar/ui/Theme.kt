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
 * One app theme. The palette themes (Bloom, Cosmic Latte, Sage…) use the same names and colours
 * as in Ezhuthola and Clock by Numio. [dark] picks the dark or light shades of the smaller colours
 * (Malayalam, Hijri, event colours) and the status bar icons.
 */
data class Look(
    val id: String,
    val name: String,
    val dark: Boolean,
    val bg: Long, val card: Long, val key: Long,
    val text: Long, val muted: Long,
    val accent: Long, val ink: Long,     // ink = text drawn on the accent (today's circle, buttons)
    val sunday: Long,
)

object Looks {
    val Numio = Look("dark", "Numio", true, 0xFF121212, 0xFF1C1C1C, 0xFF262626, 0xFFF2F2F2, 0xFF9A9A9A, 0xFFF5C427, 0xFF1A1400, 0xFFF07A6A)
    val Light = Look("light", "Light", false, 0xFFF6F4EF, 0xFFFFFFFF, 0xFFE9E6DF, 0xFF1B1B1B, 0xFF6E6E6E, 0xFFD69E00, 0xFF1A1400, 0xFFD24A38)
    val BlackWhite = Look("bw", "Black & White", true, 0xFF0B0B0B, 0xFF1A1A1A, 0xFF262626, 0xFFF2F2F2, 0xFF9E9E9E, 0xFFF2F2F2, 0xFF141414, 0xFFBDBDBD)
    val BlueNight = Look("blue", "Blue Night", true, 0xFF10151C, 0xFF18202A, 0xFF222B37, 0xFFEEF3F8, 0xFF8A9AAE, 0xFF4FA8F7, 0xFF0B1520, 0xFFF07A6A)
    val Bloom = Look("bloom", "Bloom", true, 0xFF021861, 0xFF0B2775, 0xFF0F2C80, 0xFFF4F1EA, 0xFF93A6DC, 0xFF4A7DE6, 0xFFFFFFFF, 0xFFFF6A5E)
    val BloomRed = Look("bloomred", "Bloom Red", true, 0xFFDE1110, 0xFFB80E0D, 0xFF8E0A0A, 0xFFFFF6F2, 0xFFFFD7D1, 0xFF021861, 0xFFFFFFFF, 0xFFFFE08A)
    val Latte = Look("latte", "Cosmic Latte", false, 0xFFE8DCC6, 0xFFFBF6EC, 0xFFDCCDB4, 0xFF3B1C21, 0xFF6E5D50, 0xFF722F37, 0xFFFBF6EC, 0xFFA3242F)
    val Sage = Look("sage", "Sage", false, 0xFF9CAF88, 0xFFF4F6F0, 0xFFB9C8A8, 0xFF1F2A1A, 0xFF33402B, 0xFFA34500, 0xFFFFF4EA, 0xFF8E2A10)
    val Atlas = Look("atlas", "Pocket Atlas", false, 0xFFB7D8F5, 0xFFFFFFFF, 0xFFDCEBF9, 0xFF1A2533, 0xFF2F4A66, 0xFFD00500, 0xFFFFFFFF, 0xFFD00500)

    val all = listOf(Numio, Light, BlackWhite, BlueNight, Bloom, BloomRed, Latte, Sage, Atlas)

    /** [id] "system" follows the phone: Numio when it's dark, Light when it's light. */
    fun of(id: String, phoneDark: Boolean): Look =
        if (id == "system") (if (phoneDark) Numio else Light) else all.firstOrNull { it.id == id } ?: Numio
}

/** A second time zone shown under event times, like "8:30 PM in India"; null = off. */
object SecondZone {
    var zone by mutableStateOf<java.time.ZoneId?>(null)
    var label by mutableStateOf("")

    /** Places people often want: home for expats, and family abroad. */
    val places = listOf(
        "Asia/Kolkata" to "India", "Asia/Dubai" to "UAE", "Asia/Riyadh" to "Saudi Arabia", "Asia/Qatar" to "Qatar",
        "Asia/Kuwait" to "Kuwait", "Asia/Muscat" to "Oman", "Asia/Bahrain" to "Bahrain", "Asia/Colombo" to "Sri Lanka",
        "Asia/Karachi" to "Pakistan", "Asia/Dhaka" to "Bangladesh", "Asia/Kathmandu" to "Nepal", "Asia/Singapore" to "Singapore",
        "Asia/Kuala_Lumpur" to "Malaysia", "Asia/Manila" to "Philippines", "Asia/Tokyo" to "Japan", "Australia/Sydney" to "Sydney",
        "Europe/London" to "UK", "Europe/Berlin" to "Germany", "Europe/Paris" to "France", "America/New_York" to "New York",
        "America/Chicago" to "Chicago", "America/Los_Angeles" to "California", "America/Toronto" to "Toronto",
    )
    fun labelOf(id: String) = places.firstOrNull { it.first == id }?.second ?: id.substringAfterLast('/').replace('_', ' ')
}

/** The theme on screen. Every colour below reads it, so a theme change redraws everything. */
object Palette {
    var current by mutableStateOf(Looks.Numio)
    val dark: Boolean get() = current.dark
}

private fun c(v: Long) = Color(v)
private fun pick(dark: Long, light: Long) = Color(if (Palette.dark) dark else light)

val NumioYellow get() = c(Palette.current.accent)      // the theme's accent (Numio yellow by default)
val NumioInk get() = c(Palette.current.ink)            // text on the accent
val NumioBg get() = c(Palette.current.bg)
val NumioCard get() = c(Palette.current.card)
val NumioKey get() = c(Palette.current.key)
val NumioText get() = c(Palette.current.text)
val NumioMuted get() = c(Palette.current.muted)
val NumioSunday get() = c(Palette.current.sunday)      // Sundays and holidays

// Event colours.
val EventPersonal get() = NumioYellow
val EventWork get() = pick(0xFF3CC4B4, 0xFF13998A)
val EventGoogle get() = pick(0xFF5B9BF0, 0xFF2F6FD0)
val EventFestival get() = pick(0xFFF29B4B, 0xFFDB7518)
val EventPrivate get() = pick(0xFFB08CF0, 0xFF7E57D0)

// Malayalam calendar.
val MalayalamGold get() = pick(0xFFE3B15E, 0xFF7E5206)
val MalayalamDim get() = pick(0xFFA88A55, 0xFF6E5528)
val MalayalamSoft get() = pick(0xFFC9AE80, 0xFF6A4F20)
val MalayalamCard get() = pick(0x33E3B15E, 0x66FBE9C4)

// Hijri calendar.
val HijriGreen get() = pick(0xFF7FCFA8, 0xFF23845A)
val HijriDim get() = pick(0xFF5E9C80, 0xFF3E6B55)
val HijriCard get() = pick(0x337FCFA8, 0x66D5F0E2)
val HijriLabel get() = pick(0xFF8FB8A4, 0xFF4D8268)
val HijriSoft get() = pick(0xFFA9C9B9, 0xFF3F6E58)
val HijriStrong get() = pick(0xFF9FE0BF, 0xFF1D7A50)

// Holiday groups and small controls.
val ChristianBlue get() = pick(0xFF8FB4F0, 0xFF3D73CC)
val NationalGrey get() = pick(0xFFB0B0B0, 0xFF6A6A6A)
val ChoiceOn get() = NumioYellow.copy(alpha = 0.18f)
val ChoiceOff get() = NumioKey
val ChoiceBorder get() = NumioMuted.copy(alpha = 0.35f)

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

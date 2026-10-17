package com.rr.numio.calendar.data

import android.content.Context

/** The calendar's settings, kept in the app's private preferences on this phone. */
data class CalendarSettings(
    val hijriLayer: Boolean = false,
    val hijriAdjust: Int = 0,        // −1, 0 or +1 day (moon sighting)
    val phoneCalendars: Boolean = false,
    val malayalamLayer: Boolean = true,
    val festivals: Set<FestivalGroup> = FestivalGroup.entries.filter { it.defaultOn }.toSet(),
    val regional: Boolean = true,      // Malayalam & Hijri calendars; off = a plain calendar
    val welcomed: Boolean = false,     // the first-launch question was answered
    val weekStart: java.time.DayOfWeek = java.time.DayOfWeek.SUNDAY,
    val countries: List<String> = emptyList(),   // public holidays of these countries, e.g. "AE", "IN-KL"
    val countdown: Boolean = true,               // festival countdown chips on the month screen
    val theme: String = "system",                // "system" (follow the phone), "dark" or "light"
) {
    /** Dark or light, given whether the phone itself is in dark mode. */
    fun isDark(phoneDark: Boolean) = when (theme) { "dark" -> true; "light" -> false; else -> phoneDark }

    /**
     * What the calendar actually shows. With [regional] off, the Malayalam and Hijri layers and the
     * Kerala and Islamic festivals are hidden, but their own switches are kept for when it's turned back on.
     */
    fun shown(): CalendarSettings = if (regional) this else copy(
        malayalamLayer = false,
        hijriLayer = false,
        festivals = festivals - FestivalGroup.Kerala - FestivalGroup.Islamic,
    )

    companion object {
        private const val FILE = "calendar"
        private const val HIJRI_LAYER = "hijri_layer"
        private const val HIJRI_ADJUST = "hijri_adjust"
        private const val PHONE_CALENDARS = "phone_calendars"
        private const val MALAYALAM_LAYER = "malayalam_layer"
        private const val FESTIVALS = "festivals"
        private const val REGIONAL = "regional"
        private const val WELCOMED = "welcomed"
        private const val WEEK_START = "week_start"
        private const val COUNTRIES = "countries"
        private const val COUNTDOWN = "countdown"
        private const val THEME = "theme"

        fun load(context: Context): CalendarSettings {
            val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            return CalendarSettings(
                hijriLayer = p.getBoolean(HIJRI_LAYER, false),
                hijriAdjust = p.getInt(HIJRI_ADJUST, 0).coerceIn(-1, 1),
                phoneCalendars = p.getBoolean(PHONE_CALENDARS, false),
                malayalamLayer = p.getBoolean(MALAYALAM_LAYER, true),
                festivals = p.getStringSet(FESTIVALS, null)
                    ?.let { ids -> FestivalGroup.entries.filter { it.id in ids }.toSet() }
                    ?: CalendarSettings().festivals,
                regional = p.getBoolean(REGIONAL, true),
                welcomed = p.getBoolean(WELCOMED, false),
                weekStart = java.time.DayOfWeek.of(p.getInt(WEEK_START, 7).coerceIn(1, 7)),
                // First time: the phone's own country (e.g. UAE), if there's a table for it.
                countries = p.getString(COUNTRIES, null)?.split(',')?.filter { it.isNotBlank() }
                    ?: listOfNotNull(CountryHolidays.phoneCountry(context)),
                countdown = p.getBoolean(COUNTDOWN, true),
                theme = p.getString(THEME, "system") ?: "system",
            )
        }

        fun save(context: Context, s: CalendarSettings) {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putBoolean(HIJRI_LAYER, s.hijriLayer)
                .putInt(HIJRI_ADJUST, s.hijriAdjust)
                .putBoolean(PHONE_CALENDARS, s.phoneCalendars)
                .putBoolean(MALAYALAM_LAYER, s.malayalamLayer)
                .putStringSet(FESTIVALS, s.festivals.map { it.id }.toSet())
                .putBoolean(REGIONAL, s.regional)
                .putBoolean(WELCOMED, s.welcomed)
                .putInt(WEEK_START, s.weekStart.value)
                .putString(COUNTRIES, s.countries.joinToString(","))
                .putBoolean(COUNTDOWN, s.countdown)
                .putString(THEME, s.theme)
                .apply()
        }
    }
}

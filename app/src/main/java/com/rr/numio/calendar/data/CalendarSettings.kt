package com.rr.numio.calendar.data

import android.content.Context

/** The calendar's settings, kept in the app's private preferences on this phone. */
data class CalendarSettings(
    val hijriLayer: Boolean = false,
    val hijriAdjust: Int = 0,        // −1, 0 or +1 day (moon sighting)
    val phoneCalendars: Boolean = false,
    val malayalamLayer: Boolean = true,
    val festivals: Set<FestivalGroup> = FestivalGroup.entries.filter { it.defaultOn }.toSet(),
) {
    companion object {
        private const val FILE = "calendar"
        private const val HIJRI_LAYER = "hijri_layer"
        private const val HIJRI_ADJUST = "hijri_adjust"
        private const val PHONE_CALENDARS = "phone_calendars"
        private const val MALAYALAM_LAYER = "malayalam_layer"
        private const val FESTIVALS = "festivals"

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
            )
        }

        fun save(context: Context, s: CalendarSettings) {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putBoolean(HIJRI_LAYER, s.hijriLayer)
                .putInt(HIJRI_ADJUST, s.hijriAdjust)
                .putBoolean(PHONE_CALENDARS, s.phoneCalendars)
                .putBoolean(MALAYALAM_LAYER, s.malayalamLayer)
                .putStringSet(FESTIVALS, s.festivals.map { it.id }.toSet())
                .apply()
        }
    }
}

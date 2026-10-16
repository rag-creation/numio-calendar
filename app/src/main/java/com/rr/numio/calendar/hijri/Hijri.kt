package com.rr.numio.calendar.hijri

import android.icu.util.Calendar
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import java.time.LocalDate
import java.time.ZoneOffset

/** A Hijri date. [month] is 1–12. */
data class HijriDate(val day: Int, val month: Int, val year: Int) {
    val monthNameAr: String get() = MONTHS_AR[month - 1]
    val monthNameEn: String get() = MONTHS_EN[month - 1]

    /** "٥ جمادى الأولى ١٤٤٨" */
    val arabic: String get() = "${arabicDigits(day)} $monthNameAr ${arabicDigits(year)}"

    /** "5 Jumada al-Ula 1448" */
    val english: String get() = "$day $monthNameEn $year"

    companion object {
        val MONTHS_AR = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        val MONTHS_EN = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Akhir", "Jumada al-Ula", "Jumada al-Akhirah",
            "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah"
        )
        private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"

        fun arabicDigits(n: Int): String = n.toString().map { AR_DIGITS[it - '0'] }.joinToString("")
    }
}

/**
 * Hijri dates from Android's built-in Umm al-Qura calendar: offline, no data files.
 * [adjust] is the user's moon-sighting setting (−1, 0 or +1 day), e.g. Kerala often
 * starts a month a day after the Gulf.
 */
object Hijri {

    fun of(date: LocalDate, adjust: Int = 0): HijriDate {
        val cal = IslamicCalendar(TimeZone.GMT_ZONE)
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        // Midday UTC, so the day never slips across a time-zone boundary.
        val millis = date.plusDays(adjust.toLong()).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        cal.timeInMillis = millis
        return HijriDate(
            day = cal.get(Calendar.DAY_OF_MONTH),
            month = cal.get(Calendar.MONTH) + 1,
            year = cal.get(Calendar.YEAR)
        )
    }
}

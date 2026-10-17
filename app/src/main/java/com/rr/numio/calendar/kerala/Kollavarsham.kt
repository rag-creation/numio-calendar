package com.rr.numio.calendar.kerala

import java.time.Duration
import java.time.LocalDate

/** A day in the Malayalam (Kollavarsham) calendar, with the day's star (nakshatram) at sunrise. */
data class MalayalamDate(val sign: Int, val day: Int, val year: Int, val star: Int) {
    val monthMl: String get() = Kollavarsham.MONTHS_ML[sign]
    val monthEn: String get() = Kollavarsham.MONTHS_EN[sign]
    val starMl: String get() = Kollavarsham.STARS_ML[star]
    val starEn: String get() = Kollavarsham.STARS_EN[star]

    /** "കന്നി 30, 1202" */
    val malayalam: String get() = "$monthMl $day, $year"
    /** "Kanni 30, 1202" */
    val english: String get() = "$monthEn $day, $year"
}

/**
 * The Kerala solar calendar. A month is the time the Sun spends in one sidereal sign. It starts on the day
 * the Sun enters the sign, if that happens before 3/5 of the daytime has passed (in Kerala); otherwise the next day.
 */
object Kollavarsham {
    // Indexed by the Sun's sidereal sign: 0 = Mesha = Medam.
    val MONTHS_ML = listOf("മേടം", "ഇടവം", "മിഥുനം", "കർക്കടകം", "ചിങ്ങം", "കന്നി", "തുലാം", "വൃശ്ചികം", "ധനു", "മകരം", "കുംഭം", "മീനം")
    val MONTHS_EN = listOf("Medam", "Edavam", "Mithunam", "Karkidakam", "Chingam", "Kanni", "Thulam", "Vrischikam", "Dhanu", "Makaram", "Kumbham", "Meenam")
    val STARS_ML = listOf(
        "അശ്വതി", "ഭരണി", "കാർത്തിക", "രോഹിണി", "മകയിരം", "തിരുവാതിര", "പുണർതം", "പൂയം", "ആയില്യം",
        "മകം", "പൂരം", "ഉത്രം", "അത്തം", "ചിത്തിര", "ചോതി", "വിശാഖം", "അനിഴം", "തൃക്കേട്ട",
        "മൂലം", "പൂരാടം", "ഉത്രാടം", "തിരുവോണം", "അവിട്ടം", "ചതയം", "പൂരുരുട്ടാതി", "ഉത്രട്ടാതി", "രേവതി",
    )
    val STARS_EN = listOf(
        "Ashwathi", "Bharani", "Karthika", "Rohini", "Makayiram", "Thiruvathira", "Punartham", "Pooyam", "Ayilyam",
        "Makam", "Pooram", "Uthram", "Atham", "Chithira", "Chothi", "Vishakham", "Anizham", "Thrikketta",
        "Moolam", "Pooradam", "Uthradam", "Thiruvonam", "Avittam", "Chathayam", "Pooruruttathi", "Uthrattathi", "Revathi",
    )

    const val CHINGAM = 4; const val DHANU = 8; const val MEDAM = 0; const val MAKARAM = 9
    const val VRISCHIKAM = 7; const val KARKIDAKAM = 3
    const val THIRUVONAM = 21; const val UTHRADAM = 20; const val THIRUVATHIRA = 5

    /** The month a day belongs to: the Sun's sign at 3/5 of that day's daytime. */
    fun signOfDay(date: LocalDate): Int {
        val (rise, set) = Sky.sunriseSunset(date)
        val cutoff = rise.plus(Duration.between(rise, set).multipliedBy(3).dividedBy(5))
        return Sky.sunSign(cutoff)
    }

    fun starOfDay(date: LocalDate): Int = Sky.star(Sky.sunriseSunset(date).first)

    /** The star kept for observances (birthdays, Shraddham): the one holding 6 nazhika (2 h 24 min) after sunrise. */
    fun observedStarOfDay(date: LocalDate): Int = Sky.star(Sky.sunriseSunset(date).first.plusSeconds(144 * 60))

    /** When the star at sunrise on [date] ends. */
    fun starEnds(date: LocalDate): java.time.Instant {
        var t = Sky.sunriseSunset(date).first
        val star = Sky.star(t)
        while (Sky.star(t.plusSeconds(1800)) == star) t = t.plusSeconds(1800)
        var lo = t; var hi = t.plusSeconds(1800)
        while (Duration.between(lo, hi).seconds > 30) {
            val mid = lo.plusSeconds(Duration.between(lo, hi).seconds / 2)
            if (Sky.star(mid) == star) lo = mid else hi = mid
        }
        return hi
    }

    fun tithiOfDay(date: LocalDate): Int = Sky.tithi(Sky.sunriseSunset(date).first)

    private fun year(date: LocalDate, sign: Int): Int =
        if (sign in CHINGAM..DHANU && date.monthValue >= 8) date.year - 824 else date.year - 825

    fun of(date: LocalDate): MalayalamDate = range(date, date).getValue(date)

    /** Every day from [from] to [to], worked out together (each month start is found once). */
    fun range(from: LocalDate, to: LocalDate): Map<LocalDate, MalayalamDate> {
        val out = LinkedHashMap<LocalDate, MalayalamDate>()
        // Walk back to the start of the month [from] is in.
        val firstSign = signOfDay(from)
        var start = from
        while (signOfDay(start.minusDays(1)) == firstSign) start = start.minusDays(1)
        var d = start
        var sign = firstSign
        var day = 0
        while (!d.isAfter(to)) {
            val s = if (d == start) firstSign else signOfDay(d)
            if (s != sign) { sign = s; day = 1 } else day++
            if (!d.isBefore(from)) out[d] = MalayalamDate(s, day, year(d, s), starOfDay(d))
            d = d.plusDays(1)
        }
        return out
    }

    /** The date (India time) the Sun enters [sign] in [year], e.g. Makara Sankranti. */
    fun sankranti(year: Int, sign: Int): LocalDate? {
        val ist = java.time.ZoneOffset.ofHoursMinutes(5, 30)
        var d = LocalDate.of(year, 1, 1)
        while (d.year == year) {
            val a = Sky.sunSign(d.atStartOfDay(ist).toInstant())
            val b = Sky.sunSign(d.plusDays(1).atStartOfDay(ist).toInstant())
            if (a != sign && b == sign) return d
            d = d.plusDays(1)
        }
        return null
    }

    /**
     * The day between [from] and [to] in [sign]'s month whose sunrise star is [star], like Thiruvonam in Chingam.
     * If the star comes twice in the month, the later one wins ([preferLast], as for Onam in 2024),
     * or else the one nearer the full moon.
     */
    fun starDay(from: LocalDate, to: LocalDate, sign: Int, star: Int, preferLast: Boolean = false): LocalDate? {
        val days = mutableListOf<LocalDate>()
        var d = from
        while (!d.isAfter(to)) {
            if (starOfDay(d) == star && signOfDay(d) == sign) days += d
            d = d.plusDays(1)
        }
        return if (preferLast) days.lastOrNull() else days.minByOrNull { kotlin.math.abs(tithiOfDay(it) - 15) }
    }
}

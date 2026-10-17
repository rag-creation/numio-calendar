package com.rr.numio.calendar.data

import com.rr.numio.calendar.hijri.Hijri
import com.rr.numio.calendar.kerala.Kollavarsham
import java.time.DayOfWeek
import java.time.LocalDate

/** Groups of festivals and holidays, each switched on or off in Settings. */
enum class FestivalGroup(val id: String, val label: String, val defaultOn: Boolean) {
    Kerala("kerala", "Kerala festivals", true),
    Christian("christian", "Christian festivals", true),
    Islamic("islamic", "Islamic festivals", true),
}

/**
 * Festivals and holidays, worked out on the phone: no internet, no yearly download.
 * Kerala festivals come from the Sun and Moon (see [Kollavarsham]); Islamic ones from the Hijri calendar,
 * so they are expected dates — the real day depends on the moon being sighted.
 */
object Festivals {
    private const val KUMBHAM = 10
    private const val KANNI = 5


    fun forYear(year: Int, groups: Set<FestivalGroup>, hijriAdjust: Int): List<Event> {
        val list = mutableListOf<Triple<LocalDate, Pair<String, String>, FestivalGroup>>() // date, (title, note), group
        fun add(g: FestivalGroup, d: LocalDate?, title: String, note: String = "") {
            if (d != null && d.year == year && g in groups) list += Triple(d, title to note, g)
        }
        if (FestivalGroup.Kerala in groups) kerala(year).forEach { (d, t) -> add(FestivalGroup.Kerala, d, t.first, t.second) }
        if (FestivalGroup.Christian in groups) christian(year).forEach { (d, t) -> add(FestivalGroup.Christian, d, t) }
        if (FestivalGroup.Islamic in groups) islamic(year, hijriAdjust).forEach { (d, t) ->
            add(FestivalGroup.Islamic, d, t, "Expected date. The actual day depends on the moon being sighted.")
        }
        return list.mapIndexed { i, (d, t, g) ->
            Event(
                id = -(5_000_000_000L + d.toEpochDay() * 100 + i),
                title = t.first,
                date = d,
                category = Category.Festival,
                note = t.second,
                external = true,
                calendarName = g.id, // which group, for its colour
            )
        }
    }

    /** Kerala festivals from the Malayalam calendar. */
    fun kerala(year: Int): List<Pair<LocalDate, Pair<String, String>>> {
        val out = mutableListOf<Pair<LocalDate, Pair<String, String>>>()
        fun add(d: LocalDate?, title: String, note: String = "") { if (d != null && d.year == year) out += d to (title to note) }
        val k = Kollavarsham

        add(k.sankranti(year, k.MAKARAM), "Makaravilakku", "Makara Sankranti")
        add(monthStart(year, 4, k.MEDAM), "Vishu", "Medam 1")
        add(monthStart(year, 7, k.KARKIDAKAM), "Karkidakam 1", "Ramayana month begins")
        add(monthStart(year, 8, k.CHINGAM), "Chingam 1", "Malayalam New Year")
        val onam = k.starDay(LocalDate.of(year, 8, 1), LocalDate.of(year, 9, 30), k.CHINGAM, k.THIRUVONAM, preferLast = true)
        add(onam?.minusDays(1), "Uthradam", "First Onam")
        add(onam, "Thiruvonam", "Onam")
        add(LocalDate.of(year, 11, 1), "Kerala Piravi", "Kerala's birthday")
        add(monthStart(year, 11, k.VRISCHIKAM), "Mandala season begins", "Vrischikam 1")
        // Festivals that follow the Moon. Each rule was checked against published Kerala dates for 2020–2026.
        val days = { from: LocalDate, to: LocalDate -> generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) } }
        // Sivarathri: the night of the 14th waning day (chaturdashi) in Kumbham.
        add(days(LocalDate.of(year, 1, 20), LocalDate.of(year, 3, 31)).firstOrNull {
            k.tithiAtMidnight(it) == 29 && k.signOfDay(it) == KUMBHAM
        }, "Sivarathri", "Maha Shivaratri")
        // Karkidaka Vavu: the new moon of Karkidakam, the day it holds at sunset.
        add(days(LocalDate.of(year, 6, 20), LocalDate.of(year, 8, 31)).firstOrNull {
            k.tithiAtSunset(it) == 30 && k.signOfDay(it) == k.KARKIDAKAM
        }, "Karkidaka Vavu", "Vavu bali")
        // Vinayaka Chathurthi: the 4th waxing day at noon, in the lunar month when the Sun enters Kanni.
        add(days(LocalDate.of(year, 8, 10), LocalDate.of(year, 9, 30)).firstOrNull {
            k.tithiAtNoon(it) == 4 && k.lunarMonth(it) == KANNI
        }, "Vinayaka Chathurthi")
        // Vijayadashami: the 10th waxing day at sunrise, in the lunar month when the Sun enters Thulam.
        add(days(LocalDate.of(year, 9, 15), LocalDate.of(year, 11, 10)).firstOrNull {
            k.tithiOfDay(it) == 10 && k.lunarMonth(it) == k.THULAM_SIGN
        }, "Vijayadashami", "Vidyarambham")
        // Deepavali: the 14th waning day (Naraka Chaturdashi) at sunrise in Thulam, as kept in Kerala.
        add(days(LocalDate.of(year, 10, 5), LocalDate.of(year, 11, 30)).firstOrNull {
            k.tithiOfDay(it) == 29 && k.signOfDay(it) == k.THULAM_SIGN
        }, "Deepavali", "Naraka Chaturdashi")

        // Dhanu runs from mid-December to mid-January, so a year can have a Thiruvathira at each end.
        for (y in listOf(year - 1, year)) {
            add(k.starDay(LocalDate.of(y, 12, 1), LocalDate.of(y + 1, 1, 31), k.DHANU, k.THIRUVATHIRA), "Thiruvathira", "Dhanu Thiruvathira")
        }
        return out
    }

    /** The first day of the Malayalam month [sign], looking from the start of [aroundMonth]. */
    private fun monthStart(year: Int, aroundMonth: Int, sign: Int): LocalDate? {
        var d = LocalDate.of(year, aroundMonth, 1)
        var prev = Kollavarsham.signOfDay(d.minusDays(1))
        repeat(45) {
            val s = Kollavarsham.signOfDay(d)
            if (s == sign && prev != sign) return d
            prev = s
            d = d.plusDays(1)
        }
        return null
    }

    fun christian(year: Int): List<Pair<LocalDate, String>> {
        val easter = easter(year)
        return listOf(
            easter.minusDays(7) to "Palm Sunday",
            easter.minusDays(3) to "Pesaha Vyazham",
            easter.minusDays(2) to "Good Friday",
            easter to "Easter",
            LocalDate.of(year, 12, 25) to "Christmas",
        )
    }

    /** Hijri festivals that fall in [year] (Umm al-Qura, with the moon-sighting adjustment). */
    fun islamic(year: Int, adjust: Int): List<Pair<LocalDate, String>> {
        val wanted = mapOf(
            (1 to 1) to "Islamic New Year",
            (1 to 10) to "Ashura",
            (3 to 12) to "Milad-un-Nabi",
            (9 to 1) to "Ramadan begins",
            (10 to 1) to "Eid al-Fitr",
            (12 to 10) to "Eid al-Adha",
        )
        val out = mutableListOf<Pair<LocalDate, String>>()
        var d = LocalDate.of(year, 1, 1)
        while (d.year == year) {
            val h = Hijri.of(d, adjust)
            wanted[h.month to h.day]?.let { out += d to it }
            d = d.plusDays(1)
        }
        return out
    }

    /** Western Easter Sunday (Anonymous Gregorian algorithm). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day).also { check(it.dayOfWeek == DayOfWeek.SUNDAY) }
    }
}

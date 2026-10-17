package com.rr.numio.calendar.data

import com.rr.numio.calendar.kerala.Kollavarsham
import com.rr.numio.calendar.kerala.MalayalamDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RepeatKind(val id: String) {
    None("none"), Weekly("weekly"), Monthly("monthly"), Yearly("yearly"),
    Star("star"),            // every year: a nakshatram in a Malayalam month (birthday star, Shraddham)
    Malayalam("malayalam"),  // every year: a Malayalam date, like Chingam 1
    Hijri("hijri");          // every year: a Hijri date, like 12 Rabi' al-Awwal

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: None
    }
}

/**
 * How an event repeats. For [RepeatKind.Star]: [month] is the Malayalam month (0 = Medam … 11 = Meenam)
 * and [day] the star (0 = Ashwathi … 26 = Revathi). For [RepeatKind.Malayalam]: month and day of month.
 * For [RepeatKind.Hijri]: month 1–12 and day 1–30. Unused for the others.
 */
data class Repeat(val kind: RepeatKind = RepeatKind.None, val month: Int = 0, val day: Int = 0) {
    val repeats: Boolean get() = kind != RepeatKind.None

    companion object {
        /** The rule of [kind] that matches [date], e.g. its star and Malayalam month. */
        fun from(kind: RepeatKind, date: LocalDate, hijri: (LocalDate) -> Pair<Int, Int>): Repeat = when (kind) {
            RepeatKind.Star -> Kollavarsham.of(date).let { Repeat(kind, it.sign, Kollavarsham.observedStarOfDay(date)) }
            RepeatKind.Malayalam -> Kollavarsham.of(date).let { Repeat(kind, it.sign, it.day) }
            RepeatKind.Hijri -> hijri(date).let { (m, d) -> Repeat(kind, m, d) }
            else -> Repeat(kind)
        }
    }
}

/** Works out on which days repeating events fall. */
object Recurrence {

    /**
     * Every event that happens between [from] and [to], repeats included. A repeat is a copy of
     * its event with the date changed (same id, so tapping it edits the whole series).
     */
    fun expand(events: List<Event>, from: LocalDate, to: LocalDate, hijri: (LocalDate) -> Pair<Int, Int>): List<Event> {
        val days = Days(from, to, hijri)
        return events.flatMap { e -> dates(e, from, to, days).map { d -> if (d == e.date) e else e.copy(date = d) } }
    }

    fun dates(e: Event, from: LocalDate, to: LocalDate, hijri: (LocalDate) -> Pair<Int, Int>): List<LocalDate> =
        dates(e, from, to, Days(from, to, hijri))

    private fun dates(e: Event, from: LocalDate, to: LocalDate, days: Days): List<LocalDate> {
        val r = e.repeat
        if (!r.repeats) return if (e.date in from..to) listOf(e.date) else emptyList()
        val start = maxOf(from, e.date)
        if (start.isAfter(to)) return emptyList()
        val out = mutableListOf<LocalDate>()
        when (r.kind) {
            RepeatKind.Weekly -> {
                val skip = ((7 - ChronoUnit.DAYS.between(e.date, start) % 7) % 7)
                var d = start.plusDays(skip)
                while (!d.isAfter(to)) { out += d; d = d.plusWeeks(1) }
            }
            RepeatKind.Monthly -> {
                var m = java.time.YearMonth.from(start)
                while (!m.atDay(1).isAfter(to)) {
                    val d = m.atDay(minOf(e.date.dayOfMonth, m.lengthOfMonth()))
                    if (d in start..to) out += d
                    m = m.plusMonths(1)
                }
            }
            RepeatKind.Yearly -> for (y in start.year..to.year) {
                val d = e.date.withYear(y) // 29 February becomes 28 February in other years
                if (d in start..to) out += d
            }
            RepeatKind.Malayalam -> days.runs().filter { it.sign == r.month }.forEach { run ->
                val d = run.first.plusDays((minOf(r.day, run.length) - 1).toLong())
                if (d in start..to) out += d
            }
            RepeatKind.Star -> days.runs().filter { it.sign == r.month }.forEach { run ->
                starDayIn(run, r.day, days)?.let { if (it in start..to) out += it }
            }
            RepeatKind.Hijri -> {
                var d = start
                while (!d.isAfter(to)) {
                    val (m, dd) = days.hijri(d)
                    val endOfShortMonth = r.day == 30 && m == r.month && dd == 29 && days.hijri(d.plusDays(1)).second == 1
                    if ((m == r.month && dd == r.day) || endOfShortMonth) out += d
                    d = d.plusDays(1)
                }
            }
            RepeatKind.None -> {}
        }
        return out
    }

    /**
     * The day of a Malayalam month kept for a star: the star must hold for 6 nazhika (2 h 24 min) after sunrise.
     * If it comes twice in the month, the later day is kept.
     */
    private fun starDayIn(run: Run, star: Int, days: Days): LocalDate? {
        val all = (0 until run.length).map { run.first.plusDays(it.toLong()) }
        return all.lastOrNull { days.observedStar(it) == star }
            ?: all.lastOrNull { days.mal(it)?.star == star }
    }

    /** One Malayalam month: its sign, first day and length. */
    private data class Run(val sign: Int, val first: LocalDate, val length: Int)

    /** Day facts for a window, worked out once and shared by every event. */
    private class Days(val from: LocalDate, val to: LocalDate, val hijriOf: (LocalDate) -> Pair<Int, Int>) {
        private val malMap: Map<LocalDate, MalayalamDate> by lazy { Kollavarsham.range(from.minusDays(33), to.plusDays(33)) }
        private val runList: List<Run> by lazy {
            val list = mutableListOf<Run>()
            var cur: Run? = null
            for ((d, m) in malMap) {
                cur = if (cur != null && cur.sign == m.sign) cur.copy(length = cur.length + 1)
                else { cur?.let { list += it }; Run(m.sign, d, 1) }
            }
            cur?.let { list += it }
            // The first and last runs may be cut off by the window, so they are not trusted.
            if (list.size > 2) list.subList(1, list.size - 1).toList() else emptyList()
        }
        private val stars = HashMap<LocalDate, Int>()
        private val hijris = HashMap<LocalDate, Pair<Int, Int>>()

        fun runs() = runList
        fun mal(d: LocalDate) = malMap[d]
        fun observedStar(d: LocalDate) = stars.getOrPut(d) { Kollavarsham.observedStarOfDay(d) }
        fun hijri(d: LocalDate) = hijris.getOrPut(d) { hijriOf(d) }
    }
}

package com.rr.numio.calendar.data

import android.content.Context
import java.time.LocalDate
import java.util.Locale

/**
 * Public holidays for many countries, 2000–2050, built into the app (assets/holidays.zip, one XX.txt per country).
 * Made from the open-source "holidays" library (MIT licence, Vacanza team and contributors).
 * Nothing is downloaded: the tables ship inside the app.
 */
object CountryHolidays {
    private const val ZIP = "holidays.zip"
    private var codes: List<String>? = null
    const val KERALA = "IN-KL"
    private val cache = HashMap<String, Map<Int, List<Pair<LocalDate, String>>>>()

    /** Codes of every country with a table, e.g. "AE", "IN", "IN-KL". */
    fun available(context: Context): List<String> = synchronized(this) {
        codes ?: try {
            java.util.zip.ZipInputStream(context.assets.open(ZIP)).use { zip ->
                generateSequence { zip.nextEntry }.map { it.name.removeSuffix(".txt") }.toList()
            }
        } catch (e: Exception) { emptyList() }.also { codes = it }
    }

    /** "United Arab Emirates", or "India · Kerala" for the Kerala list. */
    fun name(code: String, locale: Locale = Locale.getDefault()): String =
        if (code == KERALA) Locale("", "IN").getDisplayCountry(locale) + " · Kerala"
        else Locale("", code).getDisplayCountry(locale).ifBlank { code }

    /** The holidays of [code] in [year]. */
    fun forYear(context: Context, code: String, year: Int): List<Pair<LocalDate, String>> =
        synchronized(cache) { cache.getOrPut(code) { load(context, code) } }[year].orEmpty()

    private fun load(context: Context, code: String): Map<Int, List<Pair<LocalDate, String>>> = try {
        // File: holiday names, a "--" line, then "yyyymmdd<TAB>name number" lines.
        val lines = java.util.zip.ZipInputStream(context.assets.open(ZIP)).use { zip ->
            generateSequence { zip.nextEntry }.first { it.name == "$code.txt" }
            zip.bufferedReader().readLines()
        }
        val split = lines.indexOf("--")
        val names = lines.subList(0, split)
        lines.drop(split + 1).filter { it.isNotBlank() }.map { line ->
            val (d, n) = line.split('\t')
            LocalDate.of(d.substring(0, 4).toInt(), d.substring(4, 6).toInt(), d.substring(6, 8).toInt()) to names[n.toInt()]
        }.groupBy { it.first.year }
    } catch (e: Exception) {
        emptyMap()
    }

    /** The phone's own country, if there's a table for it — the default choice. */
    fun phoneCountry(context: Context): String? =
        Locale.getDefault().country.takeIf { it.isNotEmpty() && it in available(context) }
}

/**
 * Everything the calendar shows as holidays in a year: our own festivals (Kerala, Christian, Islamic)
 * plus the chosen countries' public holidays. When both have the same day (Onam, Christmas…),
 * it is shown once.
 */
object Holidays {
    private val aliases = listOf(
        setOf("onam", "thiruvonam"), setOf("prophet's birthday", "milad-un-nabi"),
        setOf("mahatma gandhi's birthday", "gandhi jayanti"), setOf("kerala foundation day", "kerala piravi"),
        setOf("islamic new year", "hijri new year"),
    )

    private fun norm(s: String) = s.lowercase().replace(" (estimated)", "").replace(" (observed)", "").trim()

    private fun same(a: String, b: String): Boolean {
        val x = norm(a); val y = norm(b)
        return x == y || x.contains(y) || y.contains(x) || aliases.any { set -> set.any { x.contains(it) } && set.any { y.contains(it) } }
    }

    fun forYear(context: Context, year: Int, settings: CalendarSettings): List<Event> {
        val own = Festivals.forYear(year, settings.festivals, settings.hijriAdjust)
        val countries = settings.countries.flatMapIndexed { ci, code ->
            CountryHolidays.forYear(context, code, year).mapIndexedNotNull { i, (d, name) ->
                Event(
                    id = -(7_000_000_000L + d.toEpochDay() * 1000 + ci * 100 + i % 100),
                    title = name.replace(" (estimated)", ""),
                    date = d,
                    category = Category.Festival,
                    note = CountryHolidays.name(code) + if (name.contains("(estimated)")) " · expected date" else "",
                    external = true,
                    calendarName = "country:$code",
                )
            }
        }
        val out = own.toMutableList()
        for (c in countries) {
            if (out.none { it.date == c.date && same(it.title, c.title) }) out += c
        }
        return out
    }
}

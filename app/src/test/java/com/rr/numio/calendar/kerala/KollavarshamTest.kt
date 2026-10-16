package com.rr.numio.calendar.kerala

import com.rr.numio.calendar.data.Festivals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Checked against a published Kerala calendar (Malayalam month, day and star at sunrise). */
class KollavarshamTest {

    private fun stars(year: Int, month: Int, names: String) {
        names.split(" ").forEachIndexed { i, name ->
            val d = LocalDate.of(year, month, i + 1)
            assertEquals("star on $d", name, Kollavarsham.STARS_EN[Kollavarsham.starOfDay(d)])
        }
    }

    @Test fun starsJanuary2026() = stars(2026, 1, "Rohini Makayiram Thiruvathira Punartham Pooyam Ayilyam Makam Pooram Uthram Atham Chithira Chothi Vishakham Anizham Thrikketta Moolam Moolam Pooradam Uthradam Thiruvonam Avittam Chathayam Pooruruttathi Uthrattathi Revathi Ashwathi Bharani Karthika Rohini Thiruvathira Punartham")
    @Test fun starsAugust2026() = stars(2026, 8, "Chathayam Pooruruttathi Uthrattathi Revathi Ashwathi Bharani Karthika Rohini Makayiram Thiruvathira Punartham Pooyam Makam Pooram Uthram Atham Chithira Chothi Chothi Vishakham Anizham Thrikketta Moolam Pooradam Uthradam Thiruvonam Avittam Chathayam Pooruruttathi Uthrattathi Revathi")
    @Test fun starsSeptember2026() = stars(2026, 9, "Ashwathi Bharani Karthika Rohini Makayiram Thiruvathira Punartham Pooyam Ayilyam Makam Pooram Uthram Atham Chithira Chothi Vishakham Anizham Thrikketta Moolam Pooradam Uthradam Uthradam Thiruvonam Avittam Chathayam Pooruruttathi Uthrattathi Revathi Ashwathi Bharani")
    @Test fun starsOctober2026() = stars(2026, 10, "Rohini Makayiram Thiruvathira Punartham Pooyam Ayilyam Makam Pooram Uthram Atham Chithira Chothi Vishakham Anizham Thrikketta Thrikketta Moolam Pooradam Uthradam Thiruvonam Avittam Chathayam Pooruruttathi Uthrattathi Revathi Ashwathi Bharani Karthika Rohini Makayiram Thiruvathira")

    @Test fun dates() {
        fun check(d: String, expected: String) = assertEquals(d, expected, Kollavarsham.of(LocalDate.parse(d)).english)
        check("2026-10-16", "Kanni 30, 1202")
        check("2026-10-17", "Kanni 31, 1202")
        check("2026-10-18", "Thulam 1, 1202")
        check("2026-08-16", "Karkidakam 31, 1201")
        check("2026-08-17", "Chingam 1, 1202")
        check("2026-09-16", "Chingam 31, 1202")
        check("2026-09-17", "Kanni 1, 1202")
        check("2026-01-14", "Dhanu 30, 1201")
        check("2026-01-15", "Makaram 1, 1201")
    }

    @Test fun range_matches_single_days() {
        val r = Kollavarsham.range(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertEquals(31, r.size)
        assertEquals("Kanni 15, 1202", r.getValue(LocalDate.of(2026, 10, 1)).english)
        assertEquals("Thulam 14, 1202", r.getValue(LocalDate.of(2026, 10, 31)).english)
    }

    @Test fun onam() {
        val known = mapOf(2020 to "2020-08-31", 2021 to "2021-08-21", 2022 to "2022-09-08", 2023 to "2023-08-29",
            2024 to "2024-09-15", 2025 to "2025-09-05", 2026 to "2026-08-26")
        known.forEach { (y, d) ->
            val k = Festivals.kerala(y).first { it.second.first == "Thiruvonam" }.first
            assertEquals("Onam $y", LocalDate.parse(d), k)
        }
    }

    @Test fun kerala2026() {
        val f = Festivals.kerala(2026).associate { it.second.first + "@" + it.first to true }.keys
        listOf(
            "Makaravilakku@2026-01-14", "Vishu@2026-04-14", "Chingam 1@2026-08-17",
            "Uthradam@2026-08-25", "Thiruvonam@2026-08-26", "Thiruvathira@2026-01-03", "Thiruvathira@2026-12-24",
        ).forEach { org.junit.Assert.assertTrue("$it missing from $f", it in f) }
    }

    @Test fun easter() {
        assertEquals(LocalDate.of(2025, 4, 20), Festivals.easter(2025))
        assertEquals(LocalDate.of(2026, 4, 5), Festivals.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Festivals.easter(2027))
    }
}

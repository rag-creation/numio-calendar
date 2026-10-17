package com.rr.numio.calendar

import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.EventStore
import com.rr.numio.calendar.data.Recurrence
import com.rr.numio.calendar.data.Repeat
import com.rr.numio.calendar.data.RepeatKind
import com.rr.numio.calendar.kerala.Kollavarsham
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {
    private val noHijri: (LocalDate) -> Pair<Int, Int> = { 1 to 1 }
    private fun d(s: String) = LocalDate.parse(s)
    private fun ev(date: String, repeat: Repeat) = Event(1, "x", d(date), repeat = repeat)
    private fun dates(e: Event, from: String, to: String) = Recurrence.dates(e, d(from), d(to), noHijri).map { it.toString() }

    @Test fun none() = assertEquals(listOf("2026-10-17"), dates(ev("2026-10-17", Repeat()), "2026-10-01", "2026-10-31"))

    @Test fun weekly() = assertEquals(
        listOf("2026-10-17", "2026-10-24", "2026-10-31"),
        dates(ev("2026-10-17", Repeat(RepeatKind.Weekly)), "2026-10-01", "2026-10-31")
    )

    @Test fun weekly_starts_mid_window() = assertEquals(
        listOf("2026-11-07", "2026-11-14"),
        dates(ev("2026-10-17", Repeat(RepeatKind.Weekly)), "2026-11-02", "2026-11-15")
    )

    @Test fun monthly_31st_uses_last_day() = assertEquals(
        listOf("2027-01-31", "2027-02-28", "2027-03-31"),
        dates(ev("2027-01-31", Repeat(RepeatKind.Monthly)), "2027-01-01", "2027-03-31")
    )

    @Test fun yearly_leap_day() = assertEquals(
        listOf("2025-02-28", "2026-02-28"),
        dates(ev("2024-02-29", Repeat(RepeatKind.Yearly)), "2025-01-01", "2026-12-31")
    )

    @Test fun malayalam_date() = assertEquals(
        listOf("2026-08-17"),
        dates(ev("2025-08-17", Repeat(RepeatKind.Malayalam, Kollavarsham.CHINGAM, 1)), "2026-08-01", "2026-08-31")
    )

    @Test fun star_in_month() {
        // Thiruvonam in Chingam is Onam: 26 Aug 2026, and the later of two in 2024 (15 Sep).
        val onam = ev("2020-08-31", Repeat(RepeatKind.Star, Kollavarsham.CHINGAM, Kollavarsham.THIRUVONAM))
        assertEquals(listOf("2026-08-26"), dates(onam, "2026-07-01", "2026-10-31"))
        assertEquals(listOf("2024-09-15"), dates(onam, "2024-07-01", "2024-10-31"))
    }

    @Test fun not_before_first_date() =
        assertEquals(emptyList<String>(), dates(ev("2026-10-17", Repeat(RepeatKind.Yearly)), "2025-01-01", "2025-12-31"))

    @Test fun repeat_and_reminder_saved() {
        val e = Event(5, "Amma", d("2026-10-17"), repeat = Repeat(RepeatKind.Star, 6, 18), reminder = 1440)
        assertEquals(e, EventStore.fromJson(EventStore.toJson(listOf(e))).single())
    }
}

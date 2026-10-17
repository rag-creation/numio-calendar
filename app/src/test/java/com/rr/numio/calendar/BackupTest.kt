package com.rr.numio.calendar

import com.rr.numio.calendar.data.Backup
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.Repeat
import com.rr.numio.calendar.data.RepeatKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class BackupTest {
    private val events = listOf(
        Event(1, "Amma's star birthday", LocalDate.of(1960, 5, 3), repeat = Repeat(RepeatKind.Star, 0, 3), reminder = 1440),
        Event(2, "Dentist", LocalDate.of(2026, 10, 20), LocalTime.of(17, 30), LocalTime.of(18, 0), note = "Bring the X-ray"),
    )

    @Test fun round_trip() {
        val settings = CalendarSettings(hijriLayer = true, hijriAdjust = -1, countries = listOf("AE", "IN-KL"), weekStart = DayOfWeek.MONDAY)
        val back = Backup.read(Backup.write(events, settings), CalendarSettings())
        assertEquals(events, back.events)
        assertEquals(settings.copy(welcomed = true), back.settings)
    }

    @Test fun merge_keeps_mine() {
        val mine = listOf(events[0].copy(title = "Edited"))
        val merged = Backup.merge(mine, events)
        assertEquals(listOf("Edited", "Dentist"), merged.map { it.title })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_other_files() { Backup.read("""{"kind":"something-else"}""", CalendarSettings()) }
}

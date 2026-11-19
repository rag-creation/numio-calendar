package com.rr.numio.calendar

import com.rr.numio.calendar.remind.Reminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderTextTest {
    private val nov19 = LocalDate.of(2026, 11, 19)

    @Test fun sameDayIsToday() {
        // An all-day event on the 19th, reminder set the day before: when it rings on the 19th it says Today
        assertEquals("Today · all day", Reminders.describe(nov19, null, now = nov19))
    }

    @Test fun nextDayIsTomorrow() {
        assertEquals("Tomorrow · all day", Reminders.describe(nov19.plusDays(1), null, now = nov19))
    }

    @Test fun laterDaysShowTheDate() {
        val text = Reminders.describe(nov19.plusDays(7), null, now = nov19)
        assertTrue(text, text.endsWith(" · all day") && !text.startsWith("Today") && !text.startsWith("Tomorrow"))
    }
}

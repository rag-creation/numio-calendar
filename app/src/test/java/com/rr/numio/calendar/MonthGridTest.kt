package com.rr.numio.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest {

    @Test fun october2026StartsOnThursday() {
        // 1 October 2026 is a Thursday: 4 blanks (Sun, Mon, Tue, Wed) before it.
        val cells = MonthGrid.cells(YearMonth.of(2026, 10))
        assertEquals(4 + 31, cells.size)
        (0 until 4).forEach { assertNull(cells[it]) }
        assertEquals(LocalDate.of(2026, 10, 1), cells[4])
        assertEquals(LocalDate.of(2026, 10, 31), cells.last())
    }

    @Test fun mondayStartShiftsTheBlanks() {
        val cells = MonthGrid.cells(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(3, cells.takeWhile { it == null }.size)
    }

    @Test fun leapFebruary() {
        assertEquals(29, MonthGrid.cells(YearMonth.of(2028, 2)).count { it != null })
    }

    @Test fun weekdaysStartOnTheChosenDay() {
        assertEquals(DayOfWeek.SUNDAY, MonthGrid.weekdays().first())
        assertEquals(DayOfWeek.SATURDAY, MonthGrid.weekdays().last())
    }
}

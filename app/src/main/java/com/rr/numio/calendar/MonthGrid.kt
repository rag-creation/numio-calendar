package com.rr.numio.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The days shown for one month: blanks before the 1st so it lands under the right weekday,
 * then the days of the month. Weeks start on [firstDay] (Sunday by default, like most
 * calendars in Kerala and the Gulf).
 */
object MonthGrid {

    /** One cell: null is an empty cell before the 1st. */
    fun cells(month: YearMonth, firstDay: DayOfWeek = DayOfWeek.SUNDAY): List<LocalDate?> {
        val first = month.atDay(1)
        val blanks = (first.dayOfWeek.value - firstDay.value + 7) % 7
        return List(blanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    }

    /** Weekday names in grid order, starting from [firstDay]. */
    fun weekdays(firstDay: DayOfWeek = DayOfWeek.SUNDAY): List<DayOfWeek> =
        (0 until 7).map { firstDay.plus(it.toLong()) }
}

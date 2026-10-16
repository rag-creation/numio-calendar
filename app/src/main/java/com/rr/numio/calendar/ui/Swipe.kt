package com.rr.numio.calendar.ui

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Page numbers for the swipeable month and day views: page 0 is January 2000 / 1 January 2000. */
object Pages {
    private val FIRST_MONTH = YearMonth.of(2000, 1)
    private val FIRST_DAY = LocalDate.of(2000, 1, 1)
    const val MONTHS = 200 * 12                  // 2000–2199
    val DAYS = ChronoUnit.DAYS.between(FIRST_DAY, LocalDate.of(2200, 1, 1)).toInt()

    fun ofMonth(m: YearMonth) = ChronoUnit.MONTHS.between(FIRST_MONTH, m).toInt().coerceIn(0, MONTHS - 1)
    fun month(page: Int): YearMonth = FIRST_MONTH.plusMonths(page.toLong())
    fun ofDay(d: LocalDate) = ChronoUnit.DAYS.between(FIRST_DAY, d).toInt().coerceIn(0, DAYS - 1)
    fun day(page: Int): LocalDate = FIRST_DAY.plusDays(page.toLong())
}

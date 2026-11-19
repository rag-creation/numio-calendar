package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.calendar.MonthGrid
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.hijri.Hijri
import com.rr.numio.calendar.kerala.Kollavarsham
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The whole year at a glance: twelve small months. Opens from the month title.
 * Tap a month to open it; arrows change the year; "Go to date" for far-off dates.
 * The Kollavarsham and Hijri year lines show only when those calendars are on.
 */
@Composable
fun YearView(
    year: Int,
    today: LocalDate,
    settings: CalendarSettings,
    onYear: (Int) -> Unit,
    onPickMonth: (YearMonth) -> Unit,
    onGoToDate: () -> Unit,
) {
    val locale = Locale.getDefault()
    val first = LocalDate.of(year, 1, 1)
    val last = LocalDate.of(year, 12, 31)
    val kollam = remember(year, settings.malayalamLayer) {
        if (!settings.malayalamLayer) null else {
            val a = Kollavarsham.of(first).year
            val b = Kollavarsham.of(last).year
            if (a == b) "$a" else "$a – $b"
        }
    }
    val hijri = remember(year, settings.hijriLayer, settings.hijriAdjust) {
        if (!settings.hijriLayer) null else {
            val a = Hijri.of(first, settings.hijriAdjust).year
            val b = Hijri.of(last, settings.hijriAdjust).year
            if (a == b) "$a AH" else "$a – $b AH"
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // Header: the year, the regional years under it, arrows
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(year.toString(), color = NumioYellow, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                kollam?.let { Text("കൊല്ലവർഷം $it", color = MalayalamGold, fontSize = 14.sp) }
                hijri?.let { Text(it, color = HijriGreen, fontSize = 14.sp) }
            }
            ArrowButton(left = true, label = "Previous year") { onYear(year - 1) }
            Spacer(Modifier.size(6.dp))
            ArrowButton(left = false, label = "Next year") { onYear(year + 1) }
        }

        Row(Modifier.padding(start = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Go to date", filled = false, onClick = onGoToDate)
            if (year != today.year) Pill("Back to ${today.year}", filled = true) { onYear(today.year) }
        }

        // Twelve months, three per row. It scrolls, so big phone fonts never cut the numbers off.
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            (1..12).chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { m ->
                        MiniMonth(
                            month = YearMonth.of(year, m),
                            today = today,
                            weekStart = settings.weekStart,
                            locale = locale,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onPickMonth(YearMonth.of(year, m)) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(88.dp)) // room for the + button
        }
    }
}

@Composable
private fun Pill(text: String, filled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (filled) NumioInk else NumioText,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .then(if (filled) Modifier.background(NumioYellow) else Modifier.border(1.5.dp, ChoiceBorder, RoundedCornerShape(18.dp)))
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun MiniMonth(
    month: YearMonth,
    today: LocalDate,
    weekStart: DayOfWeek,
    locale: Locale,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val current = YearMonth.from(today) == month
    val name = month.month.getDisplayName(TextStyle.FULL, locale)
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NumioCard)
            .clickable(role = Role.Button, onClickLabel = "Open $name ${month.year}") { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Text(
            month.month.getDisplayName(TextStyle.SHORT, locale),
            color = if (current) NumioYellow else NumioText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)
        )
        val cells = MonthGrid.cells(month, weekStart)
        val letters = MonthGrid.weekdays(weekStart).map { d -> d.getDisplayName(TextStyle.NARROW, locale) to (d == DayOfWeek.SUNDAY) }
        Row(Modifier.fillMaxWidth()) {
            letters.forEach { (letter, sunday) ->
                Text(
                    letter,
                    color = if (sunday) NumioSunday else NumioMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val isToday = date == today
                    val ring = NumioYellow
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 1.dp)
                            // Today's circle fits inside its own cell, so it never covers the days beside it
                            .then(if (isToday) Modifier.drawBehind { drawCircle(ring, radius = minOf(size.width, size.height) / 2f) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            Text(
                                maxLines = 1,
                                softWrap = false,
                                text = date.dayOfMonth.toString(),
                                color = when {
                                    isToday -> NumioInk
                                    date.dayOfWeek == DayOfWeek.SUNDAY -> NumioSunday
                                    else -> NumioText
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.calendar.MonthGrid
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.sortedForDay
import com.rr.numio.calendar.hijri.Hijri
import com.rr.numio.calendar.hijri.HijriDate
import com.rr.numio.calendar.kerala.Kollavarsham
import com.rr.numio.calendar.kerala.MalayalamDate
import com.rr.numio.calendar.data.Category
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

val HijriGreen = Color(0xFF7FCFA8)
private val HijriDim = Color(0xFF5E9C80)

/**
 * The month grid. Arrows change the month; tap a day to select it, tap it again to open it.
 * With the Hijri layer on, each day shows its Hijri day under the date.
 */
@Composable
fun MonthScreen(
    today: LocalDate,
    month: YearMonth,
    selected: LocalDate,
    settings: CalendarSettings,
    events: Map<LocalDate, List<Event>>,
    onOpenEvent: (Event) -> Unit,
    onMonth: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val locale = Locale.getDefault()
    val hijriOn = settings.hijriLayer
    val adjust = settings.hijriAdjust
    val malayalam = remember(month, settings.malayalamLayer) {
        if (settings.malayalamLayer) Kollavarsham.range(month.atDay(1), month.atEndOfMonth()) else emptyMap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Header: month + year (and the Hijri months it covers), arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = month.month.getDisplayName(TextStyle.FULL, locale),
                        color = NumioText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = month.year.toString(),
                        color = NumioYellow,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (malayalam.isNotEmpty()) {
                    Text(text = malayalamMonthsTitle(malayalam.values), color = MalayalamGold, fontSize = 14.sp)
                }
                if (hijriOn) {
                    Text(
                        text = hijriMonthsTitle(month, adjust),
                        color = HijriGreen,
                        fontSize = 14.sp
                    )
                }
            }
            ArrowButton(left = true, label = "Previous month") { onMonth(month.minusMonths(1)) }
            Spacer(Modifier.size(6.dp))
            ArrowButton(left = false, label = "Next month") { onMonth(month.plusMonths(1)) }
        }

        // Weekday row
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            MonthGrid.weekdays().forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = if (day == DayOfWeek.SUNDAY) NumioSunday else NumioMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Day grid: the weeks share the free height, so the month fills the screen.
        // Swipe left/right for the next/previous month; the grid slides the same way.
        AnimatedContent(
            targetState = month,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipeSideways(onPrevious = { onMonth(month.minusMonths(1)) }, onNext = { onMonth(month.plusMonths(1)) }),
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { w -> if (forward) w / 3 else -w / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { w -> if (forward) -w / 3 else w / 3 } + fadeOut())
            },
            label = "month"
        ) { shown ->
        val shownMalayalam = remember(shown, settings.malayalamLayer) {
            if (shown == month) malayalam
            else if (settings.malayalamLayer) Kollavarsham.range(shown.atDay(1), shown.atEndOfMonth()) else emptyMap()
        }
        Column(Modifier.fillMaxSize()) {
            MonthGrid.cells(shown).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            if (date != null) {
                                DayCell(
                                    date = date,
                                    malayalam = shownMalayalam[date],
                                    hijri = if (hijriOn) Hijri.of(date, adjust) else null,
                                    dots = events[date].orEmpty().sortedForDay().take(3).map { it.category.color() },
                                    isToday = date == today,
                                    isSelected = date == selected,
                                    onClick = { if (date == selected) onOpenDay(date) else onSelect(date) }
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        }

        Spacer(Modifier.size(12.dp))
        SelectedDayCard(selected, today, settings, events[selected].orEmpty().sortedForDay(), locale, onOpenEvent, onOpen = { onOpenDay(selected) }) {
            onMonth(YearMonth.from(today))
            onSelect(today)
        }
        Spacer(Modifier.size(12.dp))
    }
}

/** "കന്നി – തുലാം 1202": the Malayalam months this Gregorian month covers. */
private fun malayalamMonthsTitle(days: Collection<MalayalamDate>): String {
    val first = days.first()
    val last = days.last()
    return when {
        first.sign == last.sign -> "${first.monthMl} ${first.year}"
        first.year == last.year -> "${first.monthMl} – ${last.monthMl} ${last.year}"
        else -> "${first.monthMl} ${first.year} – ${last.monthMl} ${last.year}"
    }
}

/** "Rabi' al-Akhir – Jumada al-Ula 1448": the Hijri months this Gregorian month covers. */
private fun hijriMonthsTitle(month: YearMonth, adjust: Int): String {
    val first = Hijri.of(month.atDay(1), adjust)
    val last = Hijri.of(month.atEndOfMonth(), adjust)
    return when {
        first.month == last.month -> "${first.monthNameEn} ${first.year}"
        first.year == last.year -> "${first.monthNameEn} – ${last.monthNameEn} ${last.year}"
        else -> "${first.monthNameEn} ${first.year} – ${last.monthNameEn} ${last.year}"
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    malayalam: MalayalamDate?,
    hijri: HijriDate?,
    dots: List<Color>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
    val background = when {
        isToday -> NumioYellow
        isSelected -> NumioKey
        else -> Color.Transparent
    }
    val textColor = when {
        isToday -> NumioInk
        isSunday -> NumioSunday
        else -> NumioText
    }
    Column(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = "Select") { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (malayalam != null) {
            // The 1st of a Malayalam month shows the month's name, other days just the number.
            Text(
                text = if (malayalam.day == 1) malayalam.monthMl else malayalam.day.toString(),
                color = if (malayalam.day == 1) MalayalamGold else MalayalamDim,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        if (hijri != null) {
            // The 1st of a Hijri month shows the month's name, other days just the number.
            Text(
                text = if (hijri.day == 1) hijri.monthNameAr else HijriDate.arabicDigits(hijri.day),
                color = if (hijri.day == 1) HijriGreen else HijriDim,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        if (dots.isNotEmpty()) {
            Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                dots.forEach { c -> Box(Modifier.size(5.dp).clip(CircleShape).background(c)) }
            }
        }
    }
}

@Composable
private fun SelectedDayCard(
    day: LocalDate,
    today: LocalDate,
    settings: CalendarSettings,
    dayEvents: List<Event>,
    locale: Locale,
    onOpenEvent: (Event) -> Unit,
    onOpen: () -> Unit,
    onToday: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NumioCard)
            .clickable(role = Role.Button, onClickLabel = "Open day") { onOpen() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (if (day == today) "Today · " else "") +
                        day.dayOfWeek.getDisplayName(TextStyle.FULL, locale) + " " +
                        day.dayOfMonth + " " + day.month.getDisplayName(TextStyle.SHORT, locale),
                color = NumioText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (day != today) {
                Text(
                    text = "Today",
                    color = NumioYellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onToday() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
        if (settings.malayalamLayer) {
            val m = remember(day) { Kollavarsham.of(day) }
            Text(text = m.malayalam + " · " + m.starMl, color = MalayalamGold, fontSize = 15.sp)
        }
        if (settings.hijriLayer) {
            val h = Hijri.of(day, settings.hijriAdjust)
            Text(text = h.arabic, color = HijriGreen, fontSize = 15.sp)
        }
        if (dayEvents.isEmpty()) {
            Text(text = "No events · tap + to add one", color = NumioMuted, fontSize = 14.sp)
        } else {
            dayEvents.take(3).forEach { e -> EventRow(e) { onOpenEvent(e) } }
            if (dayEvents.size > 3) {
                Text("+${dayEvents.size - 3} more", color = NumioMuted, fontSize = 13.sp)
            }
        }
    }
}

/** One event line: colour bar, title, time. Used in the Today card and on the Day screen. */
@Composable
fun EventRow(e: Event, big: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(end = 64.dp) // keep clear of the + button
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Edit") { onClick() }
            .padding(vertical = if (big) 10.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = if (big) 40.dp else 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(e.category.color())
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(e.title, color = NumioText, fontSize = if (big) 16.sp else 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                e.timeText() + " · " + when {
                    e.category == Category.Festival -> e.note.ifEmpty { "Festival" }
                    e.external && e.calendarName.isNotEmpty() -> e.calendarName
                    else -> e.category.label
                },
                color = NumioMuted,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ArrowButton(left: Boolean, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NumioCard)
            .clickable(role = Role.Button, onClickLabel = label) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = if (left) "‹" else "›", color = NumioText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

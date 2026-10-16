package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.sortedForDay
import com.rr.numio.calendar.hijri.Hijri
import com.rr.numio.calendar.kerala.Kollavarsham
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** One day: the big date, the Hijri card (if the layer is on), then the day's events. */
@Composable
fun DayScreen(
    day: LocalDate,
    today: LocalDate,
    settings: CalendarSettings,
    events: Map<LocalDate, List<Event>>,
    onOpenEvent: (Event) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    // A pager of days: swipe to the next or previous day, it follows your finger.
    val pager = rememberPagerState(initialPage = Pages.ofDay(day)) { Pages.DAYS }
    val currentDay = rememberUpdatedState(day)
    val onDayState = rememberUpdatedState(onDay)
    LaunchedEffect(day) {
        val target = Pages.ofDay(day)
        if (pager.currentPage != target) {
            if (kotlin.math.abs(pager.currentPage - target) == 1) pager.animateScrollToPage(target)
            else pager.scrollToPage(target)
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val d = Pages.day(page)
            if (d != currentDay.value) onDayState.value(d)
        }
    }
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { it }) { page ->
        val d = Pages.day(page)
        DayPage(d, today, settings, events[d].orEmpty(), onOpenEvent, onDay)
    }
}

@Composable
private fun DayPage(
    day: LocalDate,
    today: LocalDate,
    settings: CalendarSettings,
    events: List<Event>,
    onOpenEvent: (Event) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val locale = Locale.getDefault()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = day.month.getDisplayName(TextStyle.FULL, locale) + " " + day.year,
                color = NumioMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            ArrowButton(left = true, label = "Previous day") { onDay(day.minusDays(1)) }
            Spacer(Modifier.size(6.dp))
            ArrowButton(left = false, label = "Next day") { onDay(day.plusDays(1)) }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = day.dayOfMonth.toString(),
                color = NumioYellow,
                fontSize = 72.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.size(14.dp))
            Column(Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                    color = NumioText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (day == today) Text("Today", color = NumioMuted, fontSize = 14.sp)
            }
        }

        if (settings.malayalamLayer) {
            val m = remember(day) { Kollavarsham.of(day) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF261F12))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "KOLLAVARSHAM",
                        color = MalayalamDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Star at sunrise", color = MalayalamDim, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(m.malayalam, color = MalayalamGold, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(m.starMl, color = MalayalamGold, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Row {
                    Text(m.english, color = Color(0xFFC9AE80), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(m.starEn, color = Color(0xFFC9AE80), fontSize = 14.sp)
                }
            }
        }

        if (settings.hijriLayer) {
            val h = Hijri.of(day, settings.hijriAdjust)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF15241D))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "HIJRI",
                        color = Color(0xFF8FB8A4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    val a = settings.hijriAdjust
                    Text(
                        text = if (a == 0) "Umm al-Qura" else "Umm al-Qura ${if (a > 0) "+1" else "−1"} day",
                        color = Color(0xFFA9C9B9),
                        fontSize = 12.sp
                    )
                }
                Text(h.arabic, color = Color(0xFF9FE0BF), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(h.english, color = Color(0xFFA9C9B9), fontSize = 14.sp)
            }
        }

        Text(
            "EVENTS",
            color = NumioMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        if (events.isEmpty()) {
            Text("No events · tap + to add one", color = NumioMuted, fontSize = 15.sp)
        } else {
            Column {
                events.sortedForDay().forEach { e -> EventRow(e, big = true) { onOpenEvent(e) } }
            }
        }
        Spacer(Modifier.size(80.dp)) // room to scroll past the + button
    }
}

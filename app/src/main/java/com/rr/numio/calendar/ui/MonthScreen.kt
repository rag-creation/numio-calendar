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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * First screen: the month grid. Swipe-free for now; arrows change the month,
 * tapping a day selects it and shows it in the card below.
 * The Malayalam and Hijri layers come later, as optional lines under each day.
 */
@Composable
fun MonthScreen() {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    val locale = Locale.getDefault()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioBg)
            .safeDrawingPadding()
            .padding(horizontal = 12.dp)
    ) {
        // Header: month + year, arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
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
            ArrowButton(left = true, label = "Previous month") { month = month.minusMonths(1) }
            Spacer(Modifier.size(6.dp))
            ArrowButton(left = false, label = "Next month") { month = month.plusMonths(1) }
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

        // Day grid, a week per row. The rows share the free height, so the month fills the
        // screen (and has room for the Malayalam / Hijri lines and event dots later).
        Column(Modifier.fillMaxWidth().weight(1f)) {
        MonthGrid.cells(month).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selected,
                                onClick = { selected = date }
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        }

        Spacer(Modifier.size(12.dp))
        SelectedDayCard(selected, today, locale) {
            month = YearMonth.from(today)
            selected = today
        }
        Spacer(Modifier.size(12.dp))
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
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
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClickLabel = "Select") { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SelectedDayCard(day: LocalDate, today: LocalDate, locale: Locale, onToday: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NumioCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
        Text(text = "No events yet", color = NumioMuted, fontSize = 14.sp)
    }
}

@Composable
private fun ArrowButton(left: Boolean, label: String, onClick: () -> Unit) {
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

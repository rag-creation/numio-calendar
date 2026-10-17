package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.FestivalGroup
import com.rr.numio.calendar.data.Festivals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The big festivals that get a countdown chip, with the short name shown on it. */
private val majors = mapOf(
    "Thiruvonam" to "Onam",
    "Vishu" to "Vishu",
    "Makaravilakku" to "Makaravilakku",
    "Christmas" to "Christmas",
    "Easter" to "Easter",
    "Eid al-Fitr" to "Eid al-Fitr",
    "Eid al-Adha" to "Eid al-Adha",
    "Ramadan begins" to "Ramadan",
    "Independence Day" to "Independence Day",
    "National Day" to "National Day",
    "Diwali (Deepavali)" to "Deepavali",
    "Diwali" to "Diwali",
)

private data class Countdown(val name: String, val date: LocalDate, val color: Color)

/** "Onam · 312 days" chips for the next big festivals; tap one to go to its day. */
@Composable
fun CountdownRow(today: LocalDate, settings: CalendarSettings, onGo: (LocalDate) -> Unit) {
    var chips by remember { mutableStateOf(emptyList<Countdown>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(today, settings.festivals, settings.hijriAdjust, settings.countries) {
        chips = withContext(Dispatchers.Default) {
            (com.rr.numio.calendar.data.Holidays.forYear(context, today.year, settings) +
                    com.rr.numio.calendar.data.Holidays.forYear(context, today.year + 1, settings))
                .filter { it.title in majors && !it.date.isBefore(today) }
                .groupBy { it.title }
                .map { (title, list) -> list.minBy { it.date }.let { Countdown(majors.getValue(title), it.date, chipColor(it.calendarName)) } }
                .sortedBy { it.date }
        }
    }
    if (chips.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { c ->
            val days = ChronoUnit.DAYS.between(today, c.date)
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(NumioCard)
                    .clickable(role = Role.Button, onClickLabel = "Go to ${c.name}") { onGo(c.date) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.color))
                Spacer(Modifier.size(7.dp))
                Text(c.name, color = NumioText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    when (days) { 0L -> " · today 🎉"; 1L -> " · tomorrow"; else -> " · $days days" },
                    color = if (days <= 1) c.color else NumioMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun chipColor(group: String) = when (group) {
    FestivalGroup.Kerala.id -> MalayalamGold
    FestivalGroup.Islamic.id -> HijriGreen
    FestivalGroup.Christian.id -> Color(0xFF8FB4F0)
    else -> Color(0xFFB0B0B0)
}

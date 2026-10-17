package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.Festivals
import com.rr.numio.calendar.kerala.Kollavarsham
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Jump to any date: type the year, tap a month, and (if you like) type the day.
 * Below that: the year's festivals, and every day of a chosen star, each one tap away.
 */
@Composable
fun GoToDateDialog(
    start: LocalDate,
    today: LocalDate,
    settings: CalendarSettings,
    onGo: (LocalDate) -> Unit,
    onClose: () -> Unit,
) {
    var yearText by remember { mutableStateOf(start.year.toString()) }
    var month by remember { mutableStateOf(start.month) }
    var dayText by remember { mutableStateOf("") }

    val year = yearText.toIntOrNull()?.takeIf { it in 1900..2199 }
    val ym = year?.let { YearMonth.of(it, month) }
    val day = dayText.toIntOrNull()
    val dayOk = dayText.isEmpty() || (ym != null && day != null && day in 1..ym.lengthOfMonth())
    val target = if (ym != null && dayOk) ym.atDay(day ?: 1) else null

    // The year's festivals and star days, worked out in the background when the year changes.
    var festivals by remember { mutableStateOf(emptyList<Event>()) }
    LaunchedEffect(year, settings.festivals, settings.hijriAdjust) {
        festivals = if (year == null || settings.festivals.isEmpty()) emptyList() else withContext(Dispatchers.Default) {
            Festivals.forYear(year, settings.festivals, settings.hijriAdjust).sortedBy { it.date }
        }
    }
    var star by remember { mutableStateOf(-1) }
    var pickStar by remember { mutableStateOf(false) }
    var starDays by remember { mutableStateOf(emptyList<LocalDate>()) }
    LaunchedEffect(year, star) {
        starDays = if (year == null || star < 0) emptyList() else withContext(Dispatchers.Default) {
            Kollavarsham.range(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .filter { it.value.star == star }.keys.toList()
        }
    }
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEE d MMM") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NumioYellow, focusedLabelColor = NumioYellow, cursorColor = NumioYellow,
        unfocusedBorderColor = NumioKey, unfocusedLabelColor = NumioMuted,
        focusedTextColor = NumioText, unfocusedTextColor = NumioText,
    )

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = NumioCard,
        title = { Text("Go to date", color = NumioText) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Year: type it, or step with the arrows.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArrowButton(left = true, label = "Previous year") {
                        yearText = ((yearText.toIntOrNull() ?: start.year) - 1).coerceIn(1900, 2199).toString()
                    }
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Year") },
                        singleLine = true,
                        isError = year == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    ArrowButton(left = false, label = "Next year") {
                        yearText = ((yearText.toIntOrNull() ?: start.year) + 1).coerceIn(1900, 2199).toString()
                    }
                }
                if (year == null) Text("A year from 1900 to 2199", color = NumioSunday, fontSize = 13.sp)

                // Months: 4 rows of 3.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Month.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { m ->
                                val on = m == month
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (on) NumioYellow else Color(0xFF161616))
                                        .border(1.dp, if (on) NumioYellow else NumioKey, RoundedCornerShape(12.dp))
                                        .clickable(role = Role.RadioButton) { month = m },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        m.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        color = if (on) NumioInk else NumioText,
                                        fontSize = 15.sp,
                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Day (optional)") },
                    singleLine = true,
                    isError = !dayOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    modifier = Modifier.width(160.dp)
                )

                if (festivals.isNotEmpty()) {
                    Label("FESTIVALS IN $year")
                    Column {
                        festivals.forEach { f ->
                            JumpRow(f.date.format(dayFormat), f.title, EventFestival) { onGo(f.date) }
                        }
                    }
                }

                if (settings.malayalamLayer && year != null) {
                    Label("FIND A STAR IN $year")
                    JumpRow(
                        "Star",
                        if (star < 0) "Choose…" else Kollavarsham.STARS_ML[star] + " · " + Kollavarsham.STARS_EN[star],
                        MalayalamGold
                    ) { pickStar = true }
                    starDays.forEach { d ->
                        val m = Kollavarsham.of(d)
                        JumpRow(d.format(dayFormat), m.malayalam, MalayalamGold) { onGo(d) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = target != null, onClick = { target?.let(onGo) }) {
                Text("Go", color = if (target != null) NumioYellow else NumioMuted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onGo(today) }) { Text("Today", color = NumioText) }
                TextButton(onClick = onClose) { Text("Cancel", color = NumioMuted) }
            }
        }
    )
    if (pickStar) {
        AlertDialog(
            onDismissRequest = { pickStar = false },
            containerColor = NumioCard,
            title = { Text("Star", color = NumioText) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    (0 until 27).forEach { i ->
                        Text(
                            Kollavarsham.STARS_ML[i] + " · " + Kollavarsham.STARS_EN[i],
                            color = if (i == star) NumioYellow else NumioText,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.RadioButton) { star = i; pickStar = false }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickStar = false }) { Text("Cancel", color = NumioMuted) } }
        )
    }
}

@Composable
private fun Label(text: String) =
    Text(text, color = NumioMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

/** A tappable line: date on the left, what it is on the right. */
@Composable
private fun JumpRow(left: String, right: String, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Text(left, color = NumioText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp).width(96.dp))
        Text(right, color = NumioMuted, fontSize = 14.sp, maxLines = 1)
    }
}

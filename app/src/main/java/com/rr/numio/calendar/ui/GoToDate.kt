package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.FestivalGroup
import com.rr.numio.calendar.data.Festivals
import com.rr.numio.calendar.kerala.Kollavarsham
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class GoTab(val label: String) { Date("Date"), Festivals("Festivals"), Stars("Stars") }

/** One line in the Festivals or Stars list. */
private data class Entry(val date: LocalDate, val title: String, val subtitle: String, val color: Color)


private fun groupColor(id: String) = when (id) {
    FestivalGroup.Kerala.id -> MalayalamGold
    FestivalGroup.Islamic.id -> HijriGreen
    FestivalGroup.Christian.id -> ChristianBlue
    else -> NationalGrey
}

/**
 * Full-screen "Go to": the year at the top, then three tabs — pick a month and day, jump to a festival,
 * or find every day of a star. Festivals and stars are shown as little tear-off calendar tiles.
 */
@Composable
fun GoToDateDialog(
    start: LocalDate,
    today: LocalDate,
    settings: CalendarSettings,
    onGo: (LocalDate) -> Unit,
    onClose: () -> Unit,
) {
    val locale = Locale.getDefault()
    var yearText by remember { mutableStateOf(start.year.toString()) }
    var month by remember { mutableStateOf(start.month) }
    var dayText by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(GoTab.Date) }
    val tabs = GoTab.entries.filter { it != GoTab.Stars || settings.malayalamLayer }

    val year = yearText.toIntOrNull()?.takeIf { it in 1900..2199 }
    val ym = year?.let { YearMonth.of(it, month) }
    val day = dayText.toIntOrNull()
    val dayOk = dayText.isEmpty() || (ym != null && day != null && day in 1..ym.lengthOfMonth())
    val target = if (ym != null && dayOk) ym.atDay(day ?: 1) else null

    // Festivals of the year, worked out in the background.
    var festivals by remember { mutableStateOf<List<Entry>?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(year, settings.festivals, settings.hijriAdjust, settings.countries) {
        festivals = null
        festivals = if (year == null) emptyList() else withContext(Dispatchers.Default) {
            com.rr.numio.calendar.data.Holidays.forYear(context, year, settings).sortedBy { it.date }.map { f ->
                Entry(
                    f.date,
                    f.title,
                    listOf(f.note, f.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)).filter { it.isNotBlank() }.joinToString(" · "),
                    groupColor(f.calendarName)
                )
            }
        }
    }
    // Every day of the chosen star in the year.
    var star by remember { mutableStateOf(-1) }
    var pickStar by remember { mutableStateOf(false) }
    var starDays by remember { mutableStateOf<List<Entry>?>(emptyList()) }
    LaunchedEffect(year, star) {
        if (year == null || star < 0) { starDays = emptyList(); return@LaunchedEffect }
        starDays = null
        starDays = withContext(Dispatchers.Default) {
            Kollavarsham.range(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .filter { it.value.star == star }
                .map { (d, m) -> Entry(d, m.malayalam, m.english + " · " + d.dayOfWeek.getDisplayName(TextStyle.FULL, locale), MalayalamGold) }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NumioYellow, focusedLabelColor = NumioYellow, cursorColor = NumioYellow,
        unfocusedBorderColor = NumioKey, unfocusedLabelColor = NumioMuted,
        focusedTextColor = NumioText, unfocusedTextColor = NumioText,
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(NumioBg)
                .systemBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("Close", color = NumioMuted, fontSize = 16.sp) }
                Text("Go to", color = NumioText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                TextButton(onClick = { onGo(today) }) { Text("Today", color = NumioYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }

            // Year, shared by all tabs
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ArrowButton(left = true, label = "Previous year") {
                    yearText = ((yearText.toIntOrNull() ?: start.year) - 1).coerceIn(1900, 2199).toString()
                }
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    singleLine = true,
                    isError = year == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = NumioYellow
                    ),
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
                ArrowButton(left = false, label = "Next year") {
                    yearText = ((yearText.toIntOrNull() ?: start.year) + 1).coerceIn(1900, 2199).toString()
                }
            }
            if (year == null) {
                Text("A year from 1900 to 2199", color = NumioSunday, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 6.dp))
            }

            // Tabs
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NumioCard)
                    .padding(4.dp)
            ) {
                tabs.forEach { t ->
                    val on = t == tab
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) NumioKey else Color.Transparent)
                            .clickable(role = Role.Tab) { tab = t },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(t.label, color = if (on) NumioYellow else NumioMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (tab) {
                GoTab.Date -> Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Month.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { m ->
                                val on = m == month
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (on) NumioYellow else NumioCard)
                                        .clickable(role = Role.RadioButton) { month = m },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        m.getDisplayName(TextStyle.SHORT, locale),
                                        color = if (on) NumioInk else NumioText,
                                        fontSize = 16.sp,
                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = dayText,
                            onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                            label = { Text("Day (optional)") },
                            singleLine = true,
                            isError = !dayOk,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (target != null) NumioYellow else NumioKey)
                                .clickable(enabled = target != null, role = Role.Button) { target?.let(onGo) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Go", color = if (target != null) NumioInk else NumioMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                GoTab.Festivals -> TileList(
                    entries = festivals,
                    today = today,
                    empty = if (settings.festivals.isEmpty() && settings.countries.isEmpty()) "No holidays are switched on. Choose some in Settings." else "No holidays this year.",
                    onGo = onGo
                )

                GoTab.Stars -> Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NumioCard)
                            .clickable(role = Role.Button) { pickStar = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Star", color = NumioText, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (star < 0) "Choose ▾" else Kollavarsham.STARS_ML[star] + " · " + Kollavarsham.STARS_EN[star] + " ▾",
                            color = MalayalamGold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    TileList(
                        entries = starDays,
                        today = today,
                        empty = if (star < 0) "Choose a star to see every day it falls on in $yearText." else "",
                        onGo = onGo
                    )
                }
            }
        }
    }

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

/** Entries grouped under month headings, each with a tear-off date tile. Null = still working it out. */
@Composable
private fun TileList(entries: List<Entry>?, today: LocalDate, empty: String, onGo: (LocalDate) -> Unit) {
    when {
        entries == null -> Text("Working it out…", color = NumioMuted, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
        entries.isEmpty() -> if (empty.isNotEmpty()) Text(empty, color = NumioMuted, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
        else -> {
            val next = entries.firstOrNull { !it.date.isBefore(today) }?.date
            val byMonth = entries.groupBy { it.date.month }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                byMonth.forEach { (m, list) ->
                    item(key = "h$m") {
                        Text(
                            m.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
                            color = NumioMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(list, key = { it.date.toString() + it.title }) { e ->
                        TileRow(e, today, isNext = e.date == next) { onGo(e.date) }
                    }
                }
                item { Spacer(Modifier.size(32.dp)) }
            }
        }
    }
}

@Composable
private fun TileRow(e: Entry, today: LocalDate, isNext: Boolean, onClick: () -> Unit) {
    val past = e.date.isBefore(today)
    val days = ChronoUnit.DAYS.between(today, e.date)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isNext) NumioCard else Color.Transparent)
            .then(if (isNext) Modifier.border(1.dp, e.color.copy(alpha = 0.6f), RoundedCornerShape(16.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp)
            .alpha(if (past) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateTile(e.date, e.color)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(e.title, color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(e.subtitle, color = NumioMuted, fontSize = 13.sp, maxLines = 1)
        }
        val badge = when {
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days in 2..99 -> "in $days days"
            else -> ""
        }
        if (badge.isNotEmpty()) {
            Text(badge, color = if (isNext) e.color else NumioMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** A tear-off calendar page: coloured strip with the month, the day number below. */
@Composable
private fun DateTile(date: LocalDate, color: Color) {
    Column(
        Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NumioKey),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().background(color).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
            Text(
                date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                color = NumioInk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
            )
        }
        Text(
            date.dayOfMonth.toString(),
            color = NumioText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )
    }
}

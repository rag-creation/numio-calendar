package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.EventStore
import com.rr.numio.calendar.data.PhoneCalendar
import com.rr.numio.calendar.data.Festivals
import com.rr.numio.calendar.data.Category
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import java.time.LocalDate
import java.time.YearMonth

private enum class Tab(val label: String, val icon: ImageVector) {
    Month("Month", Icons.Filled.DateRange),
    Day("Day", Icons.Filled.List),
    Settings("Settings", Icons.Filled.Settings),
}

/** The whole app: the current screen on top, the bottom bar below. */
@Composable
fun CalendarApp() {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var tab by remember { mutableStateOf(Tab.Month) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var settings by remember { mutableStateOf(CalendarSettings.load(context)) }
    var events by remember { mutableStateOf(EventStore.load(context)) }
    var editing by remember { mutableStateOf<Event?>(null) }   // the event in the editor
    var editingIsNew by remember { mutableStateOf(false) }
    var phoneEvents by remember { mutableStateOf(emptyList<Event>()) }
    var phoneVersion by remember { mutableStateOf(0) }            // bump to re-read after permission
    var showingPhoneEvent by remember { mutableStateOf<Event?>(null) }
    var festivalEvents by remember { mutableStateOf(emptyList<Event>()) }
    val festivalCache = remember { HashMap<String, List<Event>>() }
    val byDate = remember(events, phoneEvents, festivalEvents) {
        (events + phoneEvents + festivalEvents).groupBy { it.date }
    }

    // Festivals: worked out per year in the background, then kept.
    LaunchedEffect(month, settings.festivals, settings.hijriAdjust) {
        val years = listOf(month.atDay(1).minusDays(7).year, month.atEndOfMonth().plusDays(7).year).distinct()
        festivalEvents = withContext(Dispatchers.Default) {
            years.flatMap { y ->
                val key = "$y/${settings.festivals.sorted()}/${settings.hijriAdjust}"
                festivalCache.getOrPut(key) { Festivals.forYear(y, settings.festivals, settings.hijriAdjust) }
            }
        }
    }

    // Phone calendars: read the shown month (plus a week either side) in the background.
    LaunchedEffect(month, settings.phoneCalendars, phoneVersion) {
        phoneEvents = if (settings.phoneCalendars) {
            withContext(Dispatchers.IO) {
                PhoneCalendar.events(context, month.atDay(1).minusDays(7), month.atEndOfMonth().plusDays(7))
            }
        } else emptyList()
    }

    fun saveEvents(list: List<Event>) {
        events = list
        EventStore.save(context, list)
    }
    fun newEvent(on: java.time.LocalDate) {
        editing = Event(id = System.currentTimeMillis(), title = "", date = on)
        editingIsNew = true
    }
    fun openEvent(e: Event) {
        if (e.external) { showingPhoneEvent = e; return }
        editing = e
        editingIsNew = false
    }

    fun update(s: CalendarSettings) {
        settings = s
        CalendarSettings.save(context, s)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(NumioBg)
            .statusBarsPadding()
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                Tab.Month -> MonthScreen(
                    today = today,
                    month = month,
                    selected = selected,
                    settings = settings,
                    events = byDate,
                    onOpenEvent = ::openEvent,
                    onMonth = { month = it },
                    onSelect = { selected = it },
                    onOpenDay = { selected = it; tab = Tab.Day }
                )
                Tab.Day -> DayScreen(
                    day = selected,
                    today = today,
                    settings = settings,
                    events = byDate,
                    onOpenEvent = ::openEvent,
                    onDay = { selected = it; month = YearMonth.from(it) }
                )
                Tab.Settings -> SettingsScreen(settings, onPermission = { phoneVersion++ }, onChange = ::update)
            }
            // The yellow + : a new event on the selected day.
            if (tab != Tab.Settings) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp)
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NumioYellow)
                        .clickable(role = Role.Button, onClickLabel = "New event") { newEvent(selected) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New event", tint = NumioInk, modifier = Modifier.size(30.dp))
                }
            }
        }
        BottomBar(tab) { tab = it }
    }

    showingPhoneEvent?.let { e ->
        AlertDialog(
            onDismissRequest = { showingPhoneEvent = null },
            containerColor = NumioCard,
            title = { Text(e.title, color = NumioText) },
            text = {
                Column {
                    Text(e.date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")) + " · " + e.timeText(), color = e.category.color(), fontSize = 15.sp)
                    val festival = e.category == Category.Festival
                    if (!festival && e.calendarName.isNotEmpty()) Text("From " + e.calendarName, color = NumioMuted, fontSize = 13.sp)
                    if (e.note.isNotBlank()) Text(e.note, color = NumioText, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        if (festival) "Worked out on your phone, no internet. You can choose which festivals show in Settings."
                        else "From your phone's calendars. Edit it in the app it came from.",
                        color = NumioMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showingPhoneEvent = null }) { Text("OK", color = NumioYellow) } }
        )
    }

    editing?.let { e ->
        EventEditor(
            event = e,
            isNew = editingIsNew,
            onSave = { saved ->
                saveEvents(events.filter { it.id != saved.id } + saved)
                selected = saved.date
                month = YearMonth.from(saved.date)
                editing = null
            },
            onDelete = {
                saveEvents(events.filter { it.id != e.id })
                editing = null
            },
            onClose = { editing = null }
        )
    }
}

@Composable
private fun BottomBar(current: Tab, onTab: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(NumioCard).navigationBarsPadding()) {
        HorizontalDivider(color = NumioKey)
        Row(
            Modifier.fillMaxWidth().height(68.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { t ->
                val on = t == current
                Column(
                    modifier = Modifier
                        .clickable(role = Role.Tab) { onTab(t) }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        t.icon,
                        contentDescription = null,
                        tint = if (on) NumioYellow else NumioMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        t.label,
                        color = if (on) NumioYellow else NumioMuted,
                        fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

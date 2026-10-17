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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rr.numio.calendar.data.Category
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.Repeat
import com.rr.numio.calendar.data.RepeatKind
import com.rr.numio.calendar.hijri.Hijri
import com.rr.numio.calendar.hijri.HijriDate
import com.rr.numio.calendar.kerala.Kollavarsham
import com.rr.numio.calendar.remind.Reminders
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun Category.color(): Color = when (this) {
    Category.Personal -> EventPersonal
    Category.Work -> EventWork
    Category.Phone -> EventGoogle
    Category.Festival -> EventFestival
}

val timeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

/** "9:00 AM – 9:30 AM", "9:00 AM" or "All day". */
fun Event.timeText(): String = when {
    start == null -> "All day"
    end == null -> start.format(timeFormat)
    else -> start.format(timeFormat) + " – " + end.format(timeFormat)
}

/** "Every year: Thiruvonam in Thulam" and so on. */
fun repeatLabel(r: Repeat, date: LocalDate): String = when (r.kind) {
    RepeatKind.None -> "Does not repeat"
    RepeatKind.Weekly -> "Every " + date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    RepeatKind.Monthly -> "Every month on the ${date.dayOfMonth}"
    RepeatKind.Yearly -> "Every year on " + date.format(DateTimeFormatter.ofPattern("d MMMM"))
    RepeatKind.Star -> "Every year: ${Kollavarsham.STARS_EN[r.day]} in ${Kollavarsham.MONTHS_EN[r.month]}"
    RepeatKind.Malayalam -> "Every year: ${Kollavarsham.MONTHS_EN[r.month]} ${r.day}"
    RepeatKind.Hijri -> "Every year: ${r.day} ${HijriDate.MONTHS_EN[r.month - 1]}"
}

private val timedReminders = listOf(-1 to "No reminder", 0 to "At the time", 10 to "10 minutes before",
    30 to "30 minutes before", 60 to "1 hour before", 1440 to "1 day before")
private val allDayReminders = listOf(-1 to "No reminder", 0 to "On the day, 9:00 AM",
    1440 to "Day before, 9:00 AM", 10080 to "Week before, 9:00 AM")

fun reminderLabel(minutes: Int, allDay: Boolean): String =
    (if (allDay) allDayReminders else timedReminders).firstOrNull { it.first == minutes }?.second
        ?: if (minutes < 0) "No reminder" else "$minutes minutes before"

/**
 * Full-screen editor for a new event ([event] with a blank title) or an existing one.
 * [onSave] gets the finished event; [onDelete] is only shown for an existing event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditor(
    event: Event,
    isNew: Boolean,
    hijriAdjust: Int,
    onSave: (Event) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var title by remember { mutableStateOf(event.title) }
    var date by remember { mutableStateOf(event.date) }
    var allDay by remember { mutableStateOf(event.allDay) }
    var start by remember { mutableStateOf(event.start ?: LocalTime.of(9, 0)) }
    var end by remember { mutableStateOf(event.end ?: (event.start ?: LocalTime.of(9, 0)).plusHours(1)) }
    var category by remember { mutableStateOf(event.category) }
    var note by remember { mutableStateOf(event.note) }
    var repeat by remember { mutableStateOf(event.repeat) }
    var reminder by remember { mutableStateOf(event.reminder) }
    val hijriOf: (LocalDate) -> Pair<Int, Int> = { d -> Hijri.of(d, hijriAdjust).let { it.month to it.day } }
    var pickRepeat by remember { mutableStateOf(false) }
    var pickReminder by remember { mutableStateOf(false) }
    var pickStarMonth by remember { mutableStateOf(false) }
    var pickStar by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf<String?>(null) }   // "start" or "end"
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank()

    fun save() {
        if (!canSave) return
        val fixedEnd = if (end.isBefore(start)) start else end
        onSave(
            event.copy(
                title = title.trim(),
                date = date,
                start = if (allDay) null else start,
                end = if (allDay) null else fixedEnd,
                category = category,
                note = note.trim(),
                repeat = repeat,
                reminder = reminder,
            )
        )
    }

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
            // Top bar: Cancel · title · Save
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) { Text("Cancel", color = NumioMuted, fontSize = 16.sp) }
                Text(
                    if (isNew) "New event" else "Edit event",
                    color = NumioText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) NumioYellow else NumioKey)
                        .clickable(enabled = canSave, role = Role.Button) { save() }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Save",
                        color = if (canSave) NumioInk else NumioMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                Card {
                    RowItem("Date", date.format(dateFormat)) { pickDate = true }
                    Divider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = allDay, role = Role.Switch, onValueChange = { allDay = it })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("All day", color = NumioText, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = allDay,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedThumbColor = NumioInk, checkedTrackColor = NumioYellow)
                        )
                    }
                    if (!allDay) {
                        Divider()
                        RowItem("Starts", start.format(timeFormat)) { pickTime = "start" }
                        Divider()
                        RowItem("Ends", end.format(timeFormat)) { pickTime = "end" }
                    }
                }

                Card {
                    RowItem("Repeat", repeatLabel(repeat, date)) { pickRepeat = true }
                    if (repeat.kind == RepeatKind.Star) {
                        Divider()
                        RowItem("Malayalam month", Kollavarsham.MONTHS_ML[repeat.month] + " · " + Kollavarsham.MONTHS_EN[repeat.month]) { pickStarMonth = true }
                        Divider()
                        RowItem("Star", Kollavarsham.STARS_ML[repeat.day] + " · " + Kollavarsham.STARS_EN[repeat.day]) { pickStar = true }
                        Text(
                            "Kept on the day this star holds at sunrise in this month (6 nazhika rule), as for a pirannal or Shraddham.",
                            color = NumioMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Divider()
                    RowItem("Reminder", reminderLabel(reminder, allDay)) { pickReminder = true }
                }

                Text("TYPE", color = NumioMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Category.Personal, Category.Work).forEach { c ->
                        val on = c == category
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (on) c.color().copy(alpha = 0.18f) else NumioCard)
                                .border(if (on) 2.dp else 1.dp, if (on) c.color() else NumioKey, RoundedCornerShape(14.dp))
                                .clickable(role = Role.RadioButton) { category = c }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(c.color()))
                            Spacer(Modifier.size(8.dp))
                            Text(c.label, color = NumioText, fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                if (!isNew) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, NumioSunday, RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { confirmDelete = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Delete event", color = NumioSunday, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        // A star, Malayalam or Hijri repeat follows the new date.
                        if (repeat.kind in listOf(RepeatKind.Star, RepeatKind.Malayalam, RepeatKind.Hijri)) {
                            repeat = Repeat.from(repeat.kind, date, hijriOf)
                        }
                    }
                    pickDate = false
                }) { Text("OK", color = NumioYellow) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel", color = NumioMuted) } },
            colors = DatePickerDefaults.colors(containerColor = NumioCard)
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = NumioCard,
                    selectedDayContainerColor = NumioYellow,
                    selectedDayContentColor = NumioInk,
                    todayDateBorderColor = NumioYellow,
                    todayContentColor = NumioYellow,
                )
            )
        }
    }

    pickTime?.let { which ->
        val initial = if (which == "start") start else end
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
        AlertDialog(
            onDismissRequest = { pickTime = null },
            containerColor = NumioCard,
            confirmButton = {
                TextButton(onClick = {
                    val t = LocalTime.of(state.hour, state.minute)
                    if (which == "start") {
                        // Moving the start keeps the event's length.
                        val length = java.time.Duration.between(start, end).let { if (it.isNegative) java.time.Duration.ofHours(1) else it }
                        start = t
                        end = t.plus(length).let { if (it.isBefore(t)) LocalTime.of(23, 59) else it }
                    } else {
                        end = t
                    }
                    pickTime = null
                }) { Text("OK", color = NumioYellow) }
            },
            dismissButton = { TextButton(onClick = { pickTime = null }) { Text("Cancel", color = NumioMuted) } },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = NumioKey,
                        selectorColor = NumioYellow,
                        timeSelectorSelectedContainerColor = NumioYellow.copy(alpha = 0.25f),
                        timeSelectorSelectedContentColor = NumioYellow,
                    )
                )
            }
        )
    }

    if (pickRepeat) {
        val options = remember(date) {
            listOf(
                RepeatKind.None to "",
                RepeatKind.Weekly to "",
                RepeatKind.Monthly to "",
                RepeatKind.Yearly to "Birthdays, anniversaries",
                RepeatKind.Star to "Star birthday (പിറന്നാൾ), Shraddham",
                RepeatKind.Malayalam to "Same Malayalam date every year",
                RepeatKind.Hijri to "Same Hijri date every year",
            ).map { (k, hint) -> (if (k == repeat.kind) repeat else Repeat.from(k, date, hijriOf)) to hint }
        }
        ChoiceDialog(
            title = "Repeat",
            options = options.map { (r, hint) -> repeatLabel(r, date) to hint },
            selected = options.indexOfFirst { it.first.kind == repeat.kind },
            onPick = { i -> repeat = options[i].first; pickRepeat = false },
            onClose = { pickRepeat = false }
        )
    }

    if (pickReminder) {
        val list = if (allDay) allDayReminders else timedReminders
        ChoiceDialog(
            title = "Reminder",
            options = list.map { it.second to "" },
            selected = list.indexOfFirst { it.first == reminder },
            onPick = { i ->
                reminder = list[i].first
                pickReminder = false
                if (reminder >= 0 && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
                    askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onClose = { pickReminder = false }
        )
    }

    if (pickStarMonth) {
        // Chingam first, as in the Malayalam year.
        val order = (0 until 12).map { (it + Kollavarsham.CHINGAM) % 12 }
        ChoiceDialog(
            title = "Malayalam month",
            options = order.map { Kollavarsham.MONTHS_ML[it] + " · " + Kollavarsham.MONTHS_EN[it] to "" },
            selected = order.indexOf(repeat.month),
            onPick = { i -> repeat = repeat.copy(month = order[i]); pickStarMonth = false },
            onClose = { pickStarMonth = false }
        )
    }

    if (pickStar) {
        ChoiceDialog(
            title = "Star",
            options = (0 until 27).map { Kollavarsham.STARS_ML[it] + " · " + Kollavarsham.STARS_EN[it] to "" },
            selected = repeat.day,
            onPick = { i -> repeat = repeat.copy(day = i); pickStar = false },
            onClose = { pickStar = false }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = NumioCard,
            title = { Text("Delete this event?", color = NumioText) },
            text = { Text(title.ifBlank { "This event" } + " will be removed from this phone.", color = NumioMuted) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = NumioSunday) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = NumioMuted) } }
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NumioYellow,
    focusedLabelColor = NumioYellow,
    cursorColor = NumioYellow,
    unfocusedBorderColor = NumioKey,
    unfocusedLabelColor = NumioMuted,
    focusedTextColor = NumioText,
    unfocusedTextColor = NumioText,
)

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NumioCard)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) { content() }
}

@Composable
private fun RowItem(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NumioText, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = NumioYellow, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(NumioKey))
}

/** A list of choices in a dialog; each has a label and an optional hint under it. */
@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: Int,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = NumioCard,
        title = { Text(title, color = NumioText) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { i, (label, hint) ->
                    val on = i == selected
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) NumioYellow.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable(role = Role.RadioButton) { onPick(i) }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text(label, color = if (on) NumioYellow else NumioText, fontSize = 16.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                        if (hint.isNotEmpty()) Text(hint, color = NumioMuted, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Cancel", color = NumioMuted) } }
    )
}

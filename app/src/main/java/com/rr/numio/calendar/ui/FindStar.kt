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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.Recurrence
import com.rr.numio.calendar.data.Repeat
import com.rr.numio.calendar.data.RepeatKind
import com.rr.numio.calendar.kerala.Kollavarsham
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/** Where the person was born, for the clock time of the birth. */
private enum class BornIn(val label: String, val zone: ZoneId) {
    India("India", ZoneOffset.ofHoursMinutes(5, 30)),
    Uae("UAE", ZoneOffset.ofHours(4)),
    Phone("This phone's time", ZoneId.systemDefault()),
}

/**
 * Find my star: from a birth date and time, the janma nakshatram, its pada, the kooru and the Malayalam
 * birth date, and when the next star birthday (പിറന്നാൾ) is. One tap adds it as a yearly event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindStarDialog(start: LocalDate, today: LocalDate, onAdd: (Event) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(start) }
    var knowTime by remember { mutableStateOf(true) }
    var time by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var bornIn by remember { mutableStateOf(BornIn.India) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    // Without a time, the star at sunrise in Kerala (6 nazhika rule) is used.
    val moment: Instant = remember(date, time, knowTime, bornIn) {
        if (knowTime) date.atTime(time).atZone(bornIn.zone).toInstant()
        else com.rr.numio.calendar.kerala.Sky.sunriseSunset(date).first.plusSeconds(144 * 60)
    }
    val birth = remember(moment) { Kollavarsham.birth(moment) }
    val keralaDay = remember(moment, knowTime) { if (knowTime) Kollavarsham.keralaDay(moment) else date }
    val mal = remember(keralaDay) { Kollavarsham.of(keralaDay) }
    val repeat = Repeat(RepeatKind.Star, mal.sign, birth.star)
    val anchor = Event(id = 0, title = "", date = keralaDay, repeat = repeat)
    val next = remember(repeat, keralaDay) {
        Recurrence.dates(anchor, maxOf(today, keralaDay), maxOf(today, keralaDay).plusDays(400)) { 1 to 1 }.firstOrNull()
    }
    // Close to a star change, a few minutes matter; say so.
    val nearChange = remember(moment) {
        listOf(-30L, 30L).any { m -> Kollavarsham.birth(moment.plusSeconds(m * 60)).star != birth.star }
    }

    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NumioYellow, focusedLabelColor = NumioYellow, cursorColor = NumioYellow,
        unfocusedBorderColor = NumioKey, unfocusedLabelColor = NumioMuted,
        focusedTextColor = NumioText, unfocusedTextColor = NumioText,
    )

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(NumioBg).systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("Close", color = NumioMuted, fontSize = 16.sp) }
                Text("Find my star", color = NumioText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // The answer first, big.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MalayalamCard)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("JANMA NAKSHATRAM", color = MalayalamDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(Kollavarsham.STARS_ML[birth.star], color = MalayalamGold, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${Kollavarsham.STARS_EN[birth.star]} · pada ${birth.pada} · ${Kollavarsham.KOORU_ML[birth.kooru]}",
                        color = MalayalamSoft, fontSize = 15.sp
                    )
                    Text("Born on " + mal.malayalam + " · " + mal.english, color = MalayalamSoft, fontSize = 14.sp)
                    if (next != null) {
                        val days = ChronoUnit.DAYS.between(today, next)
                        Text(
                            "Next പിറന്നാൾ: " + next.format(dateFormat) +
                                when (days) { 0L -> " · today 🎉"; 1L -> " · tomorrow"; else -> " · in $days days" },
                            color = NumioYellow, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    if (!knowTime) {
                        Text("Without a birth time this is the day's star. Add the time for the exact one.", color = MalayalamDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    } else if (nearChange) {
                        Text("The star changes within half an hour of this time, so check the birth time.", color = NumioSunday, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }

                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors
                )

                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NumioCard).padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PickRow("Birth date", date.format(dateFormat)) { pickDate = true }
                    Row(
                        Modifier.fillMaxWidth().toggleable(value = knowTime, role = Role.Switch, onValueChange = { knowTime = it }).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("I know the birth time", color = NumioText, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Switch(checked = knowTime, onCheckedChange = null, colors = SwitchDefaults.colors(checkedThumbColor = NumioInk, checkedTrackColor = NumioYellow))
                    }
                    if (knowTime) {
                        PickRow("Birth time", time.format(timeFormat)) { pickTime = true }
                        Text("Born in", color = NumioText, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BornIn.entries.forEach { b ->
                                val on = b == bornIn
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (on) NumioYellow else NumioKey)
                                        .clickable(role = Role.RadioButton) { bornIn = b }
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) { Text(b.label, color = if (on) NumioInk else NumioText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NumioYellow)
                        .clickable(role = Role.Button) {
                            onAdd(
                                Event(
                                    id = System.currentTimeMillis(),
                                    title = if (name.isBlank()) "Star birthday (പിറന്നാൾ)" else name.trim() + "'s star birthday",
                                    date = keralaDay,
                                    repeat = repeat,
                                    reminder = 1440,
                                    note = "${Kollavarsham.STARS_EN[birth.star]} (${Kollavarsham.STARS_ML[birth.star]}) pada ${birth.pada}, " +
                                        "${Kollavarsham.KOORU_ML[birth.kooru]}. Every year on ${Kollavarsham.STARS_EN[birth.star]} in ${Kollavarsham.MONTHS_EN[mal.sign]}.",
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add yearly പിറന്നാൾ", color = NumioInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Adds an event every year on ${Kollavarsham.STARS_EN[birth.star]} in ${Kollavarsham.MONTHS_EN[mal.sign]}, with a reminder the day before.",
                    color = NumioMuted, fontSize = 13.sp
                )
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("OK", color = NumioYellow) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel", color = NumioMuted) } },
            colors = DatePickerDefaults.colors(containerColor = NumioCard)
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = NumioCard, selectedDayContainerColor = NumioYellow, selectedDayContentColor = NumioInk,
                    todayDateBorderColor = NumioYellow, todayContentColor = NumioYellow,
                )
            )
        }
    }
    if (pickTime) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = NumioCard,
            confirmButton = { TextButton(onClick = { time = LocalTime.of(state.hour, state.minute); pickTime = false }) { Text("OK", color = NumioYellow) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel", color = NumioMuted) } },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = NumioKey, selectorColor = NumioYellow,
                        timeSelectorSelectedContainerColor = NumioYellow.copy(alpha = 0.25f), timeSelectorSelectedContentColor = NumioYellow,
                    )
                )
            }
        )
    }
}

@Composable
private fun PickRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NumioText, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = NumioYellow, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

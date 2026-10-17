package com.rr.numio.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.PhoneCalendar
import com.rr.numio.calendar.data.FestivalGroup
import com.rr.numio.calendar.kerala.Kollavarsham
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rr.numio.calendar.hijri.Hijri
import java.time.LocalDate

@Composable
fun SettingsScreen(settings: CalendarSettings, onPermission: () -> Unit, onChange: (CalendarSettings) -> Unit) {
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) {
            onChange(settings.copy(phoneCalendars = true))
            onPermission()
        }
    }
    fun setPhoneCalendars(on: Boolean) {
        if (!on) { onChange(settings.copy(phoneCalendars = false)); return }
        if (PhoneCalendar.hasPermission(context)) {
            onChange(settings.copy(phoneCalendars = true))
            onPermission()
        } else {
            askCalendar.launch(Manifest.permission.READ_CALENDAR)
        }
    }
    val phoneOn = settings.phoneCalendars && PhoneCalendar.hasPermission(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", color = NumioText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

        Card {
            ToggleRow(
                title = "Malayalam & Hijri calendars",
                text = if (settings.regional) "Malayalam date and star, Hijri date, Kerala and Islamic festivals. Turn off for a plain calendar."
                       else "Off: a plain calendar. Turn on for the Malayalam date and star, Hijri date, Kerala and Islamic festivals.",
                checked = settings.regional,
                onChange = { onChange(settings.copy(regional = it)) }
            )
        }

        if (settings.regional) {
            SectionLabel("MALAYALAM CALENDAR")
            Card {
                ToggleRow(
                    title = "Malayalam layer",
                    text = "Kollavarsham date under each day, and the day's star (nakshatram). Worked out on the phone for Kerala.",
                    checked = settings.malayalamLayer,
                    onChange = { onChange(settings.copy(malayalamLayer = it)) }
                )
                if (settings.malayalamLayer) {
                    val m = Kollavarsham.of(LocalDate.now())
                    Text(
                        "Today: " + m.malayalam + " · " + m.starMl,
                        color = MalayalamGold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

        }

        SectionLabel("FESTIVALS")
        Card {
            FestivalGroup.entries.filter { settings.regional || it !in listOf(FestivalGroup.Kerala, FestivalGroup.Islamic) }.forEach { g ->
                ToggleRow(
                    title = g.label,
                    text = festivalExamples(g),
                    checked = g in settings.festivals,
                    onChange = { on ->
                        onChange(settings.copy(festivals = if (on) settings.festivals + g else settings.festivals - g))
                    }
                )
            }
        }

        SectionLabel("PHONE CALENDARS")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(NumioCard)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = phoneOn, role = Role.Switch, onValueChange = ::setPhoneCalendars)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Show phone calendars", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Events from the calendars on this phone, like Google or Samsung, in blue. Read-only: Calendar never changes them.",
                        color = NumioMuted,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = phoneOn,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedThumbColor = NumioInk, checkedTrackColor = NumioYellow)
                )
            }
            if (denied) {
                Text(
                    "Calendar permission was not given. You can allow it in Android Settings → Apps → Calendar → Permissions.",
                    color = NumioSunday,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        if (settings.regional) {
            SectionLabel("HIJRI CALENDAR")
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(NumioCard)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = settings.hijriLayer,
                            role = Role.Switch,
                            onValueChange = { onChange(settings.copy(hijriLayer = it)) }
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Hijri layer", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Islamic date under each day (Umm al-Qura, built into Android, works offline)",
                            color = NumioMuted,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = settings.hijriLayer,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NumioInk,
                            checkedTrackColor = NumioYellow
                        )
                    )
                }

                if (settings.hijriLayer) {
                    Column(Modifier.padding(top = 6.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Moon sighting adjustment", color = NumioText, fontSize = 15.sp)
                        Text(
                            "Match your country or mosque. Kerala often starts a month a day after the Gulf: choose −1 day.",
                            color = NumioMuted,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(-1 to "−1 day", 0 to "0", 1 to "+1 day").forEach { (value, label) ->
                                val on = settings.hijriAdjust == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (on) Color(0xFF2A2410) else Color(0xFF161616))
                                        .border(
                                            if (on) 2.dp else 1.dp,
                                            if (on) NumioYellow else Color(0xFF333333),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(role = Role.RadioButton) {
                                            onChange(settings.copy(hijriAdjust = value))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (on) NumioYellow else NumioText,
                                        fontSize = 14.sp,
                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Text(
                            "Today: " + Hijri.of(LocalDate.now(), settings.hijriAdjust).english,
                            color = HijriGreen,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Text(
            "No ads · No tracking · No internet",
            color = NumioMuted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = NumioMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

private fun festivalExamples(g: FestivalGroup) = when (g) {
    FestivalGroup.Kerala -> "Onam, Vishu, Thiruvathira, Makaravilakku, Chingam 1"
    FestivalGroup.India -> "Republic Day, Independence Day, Gandhi Jayanti"
    FestivalGroup.Christian -> "Christmas, Easter, Good Friday, Pesaha"
    FestivalGroup.Islamic -> "Eid al-Fitr, Eid al-Adha, Ramadan, Milad-un-Nabi (expected dates)"
    FestivalGroup.Uae -> "UAE National Day, Commemoration Day"
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NumioCard)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) { content() }
}

@Composable
private fun ToggleRow(title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text, color = NumioMuted, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedThumbColor = NumioInk, checkedTrackColor = NumioYellow)
        )
    }
}

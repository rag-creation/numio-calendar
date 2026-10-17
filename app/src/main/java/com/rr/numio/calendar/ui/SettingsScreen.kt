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
import androidx.compose.foundation.layout.Spacer
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
fun SettingsScreen(settings: CalendarSettings, onPermission: () -> Unit, onFindStar: () -> Unit, onChange: (CalendarSettings) -> Unit) {
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

    var regionalPage by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = regionalPage) { regionalPage = false }
    if (regionalPage) {
        RegionalCalendarsPage(settings, onFindStar, onBack = { regionalPage = false }, onChange = onChange)
        return
    }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", color = NumioText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

        // Regional calendars: one switch here, everything else on its own page.
        Card {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClickLabel = "Open regional calendars") { regionalPage = true }
                        .padding(vertical = 14.dp)
                ) {
                    Text("Regional calendars ›", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (!settings.regional) "Off · a plain calendar"
                        else listOfNotNull(
                            "Malayalam".takeIf { settings.malayalamLayer },
                            "Hijri".takeIf { settings.hijriLayer },
                        ).ifEmpty { listOf("On") }.joinToString(" · ") + " · festivals and more",
                        color = if (settings.regional) MalayalamGold else NumioMuted,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = settings.regional,
                    onCheckedChange = { onChange(settings.copy(regional = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NumioInk, checkedTrackColor = NumioYellow)
                )
            }
        }

        SectionLabel("GENERAL")
        Card {
            Text("Week starts on", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.SATURDAY).forEach { d ->
                    Choice(
                        d.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
                        settings.weekStart == d,
                        Modifier.weight(1f)
                    ) { onChange(settings.copy(weekStart = d)) }
                }
            }
        }

        SectionLabel("HOLIDAYS")
        Card {
            listOf(FestivalGroup.India, FestivalGroup.Christian, FestivalGroup.Uae).forEach { g ->
                FestivalToggle(g, settings, onChange)
            }
        }

        SectionLabel("PHONE CALENDARS")
        Card {
            ToggleRow(
                title = "Show phone calendars",
                text = "Events from the calendars on this phone, like Google or Samsung, in blue. Read-only: Calendar never changes them.",
                checked = phoneOn,
                onChange = ::setPhoneCalendars
            )
            if (denied) {
                Text(
                    "Calendar permission was not given. You can allow it in Android Settings → Apps → Calendar → Permissions.",
                    color = NumioSunday,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        SectionLabel("ABOUT")
        Card {
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Calendar by Numio" + if (version.isNotEmpty()) " · $version" else "", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("No ads · No tracking · No internet. Everything is worked out and kept on this phone.", color = NumioMuted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Malayalam and Hijri calendars, Find my star and their festivals, all on one page. */
@Composable
private fun RegionalCalendarsPage(
    settings: CalendarSettings,
    onFindStar: () -> Unit,
    onBack: () -> Unit,
    onChange: (CalendarSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(left = true, label = "Back", onClick = onBack)
            Text("Regional calendars", color = NumioText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 12.dp))
        }
        Card {
            ToggleRow(
                title = "Use regional calendars",
                text = if (settings.regional) "On. Turn off for a plain calendar; your choices below are kept."
                       else "Off: a plain calendar. Turn on for the Malayalam and Hijri calendars and their festivals.",
                checked = settings.regional,
                onChange = { onChange(settings.copy(regional = it)) }
            )
        }
        if (!settings.regional) return@Column

        SectionLabel("MALAYALAM · മലയാളം")
        Card {
            ToggleRow(
                title = "Malayalam layer",
                text = "Kollavarsham date under each day, and the day's star (nakshatram). Worked out on the phone for Kerala.",
                checked = settings.malayalamLayer,
                onChange = { onChange(settings.copy(malayalamLayer = it)) }
            )
            if (settings.malayalamLayer) {
                val m = remember { Kollavarsham.of(LocalDate.now()) }
                Text("Today: " + m.malayalam + " · " + m.starMl, color = MalayalamGold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
            FestivalToggle(FestivalGroup.Kerala, settings, onChange)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onFindStar)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Find my star · ജന്മനക്ഷത്രം", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Birth star, pada and kooru from a birth date and time, and a yearly പിറന്നാൾ.", color = NumioMuted, fontSize = 13.sp)
                }
                Text("›", color = MalayalamGold, fontSize = 24.sp)
            }
        }

        SectionLabel("HIJRI · هجري")
        Card {
            ToggleRow(
                title = "Hijri layer",
                text = "Islamic date under each day (Umm al-Qura, built into Android, works offline).",
                checked = settings.hijriLayer,
                onChange = { onChange(settings.copy(hijriLayer = it)) }
            )
            Text("Moon sighting adjustment", color = NumioText, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            Text(
                "Match your country or mosque. Kerala often starts a month a day after the Gulf: choose −1 day. Also moves the Islamic festivals.",
                color = NumioMuted,
                fontSize = 13.sp
            )
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-1 to "−1 day", 0 to "0", 1 to "+1 day").forEach { (value, label) ->
                    Choice(label, settings.hijriAdjust == value, Modifier.weight(1f)) { onChange(settings.copy(hijriAdjust = value)) }
                }
            }
            Text("Today: " + Hijri.of(LocalDate.now(), settings.hijriAdjust).english, color = HijriGreen, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            FestivalToggle(FestivalGroup.Islamic, settings, onChange)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FestivalToggle(g: FestivalGroup, settings: CalendarSettings, onChange: (CalendarSettings) -> Unit) {
    ToggleRow(
        title = g.label,
        text = festivalExamples(g),
        checked = g in settings.festivals,
        onChange = { on -> onChange(settings.copy(festivals = if (on) settings.festivals + g else settings.festivals - g)) }
    )
}

@Composable
private fun Choice(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) Color(0xFF2A2410) else Color(0xFF161616))
            .border(if (on) 2.dp else 1.dp, if (on) NumioYellow else Color(0xFF333333), RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (on) NumioYellow else NumioText, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
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

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
fun SettingsScreen(
    settings: CalendarSettings,
    events: List<com.rr.numio.calendar.data.Event>,
    onPermission: () -> Unit,
    onFindStar: () -> Unit,
    onRestore: (events: List<com.rr.numio.calendar.data.Event>, settings: CalendarSettings?) -> Unit,
    onChange: (CalendarSettings) -> Unit,
) {
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
    var countriesPage by remember { mutableStateOf(false) }
    var creditsPage by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = regionalPage || countriesPage || creditsPage) {
        regionalPage = false; countriesPage = false; creditsPage = false
    }
    if (creditsPage) {
        CreditsPage(onBack = { creditsPage = false })
        return
    }

    // Backup: write a .numio file where the person chooses; restore reads one back.
    var pendingRestore by remember { mutableStateOf<com.rr.numio.calendar.data.Backup.Contents?>(null) }
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(com.rr.numio.calendar.data.Backup.write(events, settings).toByteArray())
            }
        }.isSuccess
        android.widget.Toast.makeText(context, if (ok) "Backup saved: ${events.size} events" else "Couldn't save the backup", android.widget.Toast.LENGTH_SHORT).show()
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val read = runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            com.rr.numio.calendar.data.Backup.read(text, settings)
        }
        read.onSuccess { pendingRestore = it }
            .onFailure { android.widget.Toast.makeText(context, "That file isn't a Calendar backup", android.widget.Toast.LENGTH_SHORT).show() }
    }
    pendingRestore?.let { b ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRestore = null },
            containerColor = NumioCard,
            title = { Text("Restore backup?", color = NumioText) },
            text = {
                Text(
                    "The backup has ${b.events.size} events. You have ${events.size} now.\n\n" +
                        "Replace: use the backup's events and settings instead of yours.\n" +
                        "Add: keep yours and add the backup's events you don't have.",
                    color = NumioMuted
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onRestore(b.events, b.settings); pendingRestore = null
                    android.widget.Toast.makeText(context, "Restored ${b.events.size} events", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Replace", color = NumioSunday) }
            },
            dismissButton = {
                Row {
                    androidx.compose.material3.TextButton(onClick = { pendingRestore = null }) { Text("Cancel", color = NumioMuted) }
                    androidx.compose.material3.TextButton(onClick = {
                        val merged = com.rr.numio.calendar.data.Backup.merge(events, b.events)
                        onRestore(merged, null); pendingRestore = null
                        android.widget.Toast.makeText(context, "Added ${merged.size - events.size} events", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text("Add", color = NumioYellow) }
                }
            }
        )
    }
    if (regionalPage) {
        RegionalCalendarsPage(settings, onFindStar, onBack = { regionalPage = false }, onChange = onChange)
        return
    }
    if (countriesPage) {
        CountryHolidaysPage(settings, onBack = { countriesPage = false }, onChange = onChange)
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
            ToggleRow(
                title = "Festival countdown",
                text = "Chips under the month name, like \"Christmas · 69 days\". Tap one to go to its day.",
                checked = settings.countdown,
                onChange = { onChange(settings.copy(countdown = it)) }
            )
        }

        SectionLabel("HOLIDAYS")
        Card {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { countriesPage = true }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Country holidays", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (settings.countries.isEmpty()) "None chosen · public holidays of 249 countries"
                        else settings.countries.joinToString(", ") { com.rr.numio.calendar.data.CountryHolidays.name(it) },
                        color = if (settings.countries.isEmpty()) NumioMuted else NumioYellow,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                }
                Text("›", color = NumioYellow, fontSize = 24.sp)
            }
            FestivalToggle(FestivalGroup.Christian, settings, onChange)
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

        SectionLabel("BACKUP")
        Card {
            LinkRow("Back up to a file", "Your events and settings in one .numio file. Keep it somewhere safe, like Downloads or a USB stick.") {
                saveBackup.launch("Calendar-" + LocalDate.now() + ".numio")
            }
            LinkRow("Restore from a file", "Bring back a Calendar backup, on this phone or a new one.") {
                openBackup.launch(arrayOf("*/*"))
            }
        }

        SectionLabel("ABOUT")
        Card {
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Calendar by Numio" + if (version.isNotEmpty()) " · $version" else "", color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("No ads · No tracking · No internet. Everything is worked out and kept on this phone.", color = NumioMuted, fontSize = 13.sp)
            }
            LinkRow("Credits & licences", "Where the data and maths come from, and the app's own licence.") { creditsPage = true }
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

/** Pick countries whose public holidays are shown: search, tick, done. */
@Composable
private fun CountryHolidaysPage(settings: CalendarSettings, onBack: () -> Unit, onChange: (CalendarSettings) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val all = remember {
        com.rr.numio.calendar.data.CountryHolidays.available(context)
            .map { it to com.rr.numio.calendar.data.CountryHolidays.name(it) }
            .sortedBy { it.second.lowercase() }
    }
    val chosen = settings.countries
    val shown = all.filter { (code, name) ->
        query.isBlank() || name.contains(query.trim(), ignoreCase = true) || code.equals(query.trim(), ignoreCase = true)
    }.sortedBy { if (it.first in chosen) 0 else 1 }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(left = true, label = "Back", onClick = onBack)
            Text("Country holidays", color = NumioText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 12.dp))
        }
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search countries", color = NumioMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NumioYellow, cursorColor = NumioYellow, unfocusedBorderColor = NumioKey,
                focusedTextColor = NumioText, unfocusedTextColor = NumioText,
            )
        )
        androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(NumioCard)) {
            items(shown.size, key = { shown[it].first }) { i ->
                val (code, name) = shown[i]
                val on = code in chosen
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = on, role = Role.Checkbox) { tick ->
                            onChange(settings.copy(countries = if (tick) chosen + code else chosen - code))
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = if (on) NumioYellow else NumioText, fontSize = 16.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Checkbox(
                        checked = on, onCheckedChange = null,
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = NumioYellow, checkmarkColor = NumioInk, uncheckedColor = NumioMuted)
                    )
                }
            }
        }
        Text(
            "Built into the app, no internet: public holidays 2000–2050 from the open-source holidays library (MIT licence). Moon-based dates are expected dates.",
            color = NumioMuted, fontSize = 12.sp
        )
    }
}

@Composable
private fun LinkRow(title: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text, color = NumioMuted, fontSize = 13.sp)
        }
        Text("›", color = NumioYellow, fontSize = 24.sp)
    }
}

/** Credits & licences: who made what this app is built on. */
@Composable
private fun CreditsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var showMit by remember { mutableStateOf(false) }
    val mit = remember {
        runCatching { context.assets.open("holidays-LICENSE.txt").bufferedReader().readText() }.getOrDefault("")
    }
    fun open(url: String) {
        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(left = true, label = "Back", onClick = onBack)
            Text("Credits & licences", color = NumioText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 12.dp))
        }
        Card {
            Credit(
                "Calendar by Numio",
                "Made by RR. Free and open source under the GNU GPL v3.0.",
                "Source code ›"
            ) { open("https://github.com/rag-creation/numio-calendar") }
        }
        Card {
            Credit(
                "Country holidays",
                "Public holidays for 249 countries are made from the holidays library by the Vacanza team and contributors (MIT licence). They are built into the app; nothing is downloaded.",
                if (showMit) "Hide licence" else "Show licence"
            ) { showMit = !showMit }
            if (showMit) Text(mit, color = NumioMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 14.dp))
        }
        Card {
            Credit(
                "Malayalam calendar",
                "Malayalam dates, stars, sunrise and festival days are worked out on the phone with formulas from Jean Meeus, Astronomical Algorithms, and the Lahiri ayanamsa. Checked against published Kerala calendars.",
                null
            ) {}
        }
        Card {
            Credit(
                "Hijri calendar",
                "Hijri dates come from the Umm al-Qura calendar built into Android (ICU, Unicode licence).",
                null
            ) {}
        }
        Card {
            Credit(
                "Android & Jetpack Compose",
                "Built with AndroidX and Jetpack Compose by Google (Apache licence 2.0).",
                null
            ) {}
        }
    }
}

@Composable
private fun Credit(title: String, text: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text, color = NumioMuted, fontSize = 13.sp)
        if (action != null) {
            Text(
                action, color = NumioYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onAction).padding(vertical = 6.dp)
            )
        }
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
    FestivalGroup.Christian -> "Christmas, Easter, Good Friday, Pesaha"
    FestivalGroup.Islamic -> "Eid al-Fitr, Eid al-Adha, Ramadan, Milad-un-Nabi (expected dates)"
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

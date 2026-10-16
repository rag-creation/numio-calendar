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
import com.rr.numio.calendar.hijri.Hijri
import java.time.LocalDate

@Composable
fun SettingsScreen(settings: CalendarSettings, onChange: (CalendarSettings) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", color = NumioText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

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

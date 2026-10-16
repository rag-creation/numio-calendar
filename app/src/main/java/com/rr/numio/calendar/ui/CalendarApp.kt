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
                    onMonth = { month = it },
                    onSelect = { selected = it },
                    onOpenDay = { selected = it; tab = Tab.Day }
                )
                Tab.Day -> DayScreen(
                    day = selected,
                    today = today,
                    settings = settings,
                    onDay = { selected = it; month = YearMonth.from(it) }
                )
                Tab.Settings -> SettingsScreen(settings, ::update)
            }
        }
        BottomBar(tab) { tab = it }
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

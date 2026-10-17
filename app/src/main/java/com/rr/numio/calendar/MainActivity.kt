package com.rr.numio.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rr.numio.calendar.ui.CalendarApp
import com.rr.numio.calendar.ui.NumioCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start in the right theme, so a light-theme user never sees a dark flash.
        val phoneDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        com.rr.numio.calendar.ui.Palette.current =
            com.rr.numio.calendar.ui.Looks.of(com.rr.numio.calendar.data.CalendarSettings.load(this).theme, phoneDark)
        setContent {
            NumioCalendarTheme {
                CalendarApp()
            }
        }
    }
}

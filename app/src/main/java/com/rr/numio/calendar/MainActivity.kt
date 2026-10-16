package com.rr.numio.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rr.numio.calendar.ui.CalendarApp
import com.rr.numio.calendar.ui.NumioCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NumioCalendarTheme {
                CalendarApp()
            }
        }
    }
}

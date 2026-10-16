package com.rr.numio.calendar.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Reads events from the phone's own calendars (Google, Samsung, holidays…) through Android's
 * calendar storage. Read-only: Calendar by Numio never adds, changes or deletes anything there.
 */
object PhoneCalendar {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    /** Every event happening between [from] and [to] (both included), repeats expanded. */
    fun events(context: Context, from: LocalDate, to: LocalDate): List<Event> {
        if (!hasPermission(context)) return emptyList()
        val zone = ZoneId.systemDefault()
        val begin = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, begin)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.DESCRIPTION,
        )

        val out = mutableListOf<Event>()
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(No title)"
                    val startMs = c.getLong(2)
                    val endMs = c.getLong(3)
                    val allDay = c.getInt(4) == 1
                    val calName = c.getString(5).orEmpty()
                    val note = c.getString(6).orEmpty()

                    if (allDay) {
                        // All-day events are stored at UTC midnight; the end day is not included.
                        var day = Instant.ofEpochMilli(startMs).atZone(ZoneOffset.UTC).toLocalDate()
                        val last = Instant.ofEpochMilli(endMs).atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
                        while (!day.isAfter(last.coerceAtLeast(day))) {
                            if (!day.isBefore(from) && !day.isAfter(to)) {
                                out += Event(
                                    id = -(id * 1000 + out.size), // negative: never clashes with own events
                                    title = title, date = day, category = Category.Phone,
                                    note = note, external = true, calendarName = calName
                                )
                            }
                            if (!day.isBefore(last)) break
                            day = day.plusDays(1)
                        }
                    } else {
                        val s = Instant.ofEpochMilli(startMs).atZone(zone)
                        val e = Instant.ofEpochMilli(endMs).atZone(zone)
                        out += Event(
                            id = -(id * 1000 + out.size),
                            title = title,
                            date = s.toLocalDate(),
                            start = s.toLocalTime().withSecond(0).withNano(0),
                            end = if (e.toLocalDate() == s.toLocalDate()) e.toLocalTime().withSecond(0).withNano(0) else null,
                            category = Category.Phone,
                            note = note,
                            external = true,
                            calendarName = calName,
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            return emptyList() // permission was taken away
        }
        return out
    }
}

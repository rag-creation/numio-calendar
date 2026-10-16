package com.rr.numio.calendar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

enum class Category(val id: String, val label: String) {
    Personal("personal", "Personal"),
    Work("work", "Work"),
    Phone("phone", "Phone calendar");   // events read from the phone's calendars (read-only)

    companion object {
        fun byId(id: String?): Category = entries.firstOrNull { it.id == id } ?: Personal
    }
}

/** One event. No [start] means all day. */
data class Event(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val start: LocalTime? = null,
    val end: LocalTime? = null,
    val category: Category = Category.Personal,
    val note: String = "",
    val external: Boolean = false,      // from the phone's calendars: shown, never changed
    val calendarName: String = "",
) {
    val allDay: Boolean get() = start == null
}

/** Sorts a day's events: all-day first, then by start time, then by title. */
fun List<Event>.sortedForDay(): List<Event> =
    sortedWith(compareBy<Event>({ it.start != null }, { it.start }, { it.title.lowercase() }))

/**
 * Events are kept in one small JSON file in the app's private storage.
 * They never leave the phone: the app has no internet permission.
 */
object EventStore {
    private const val FILE = "events.json"

    fun load(context: Context): List<Event> {
        val f = File(context.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return try { fromJson(f.readText()) } catch (e: Exception) { emptyList() }
    }

    fun save(context: Context, events: List<Event>) {
        val f = File(context.filesDir, FILE)
        val tmp = File(context.filesDir, "$FILE.tmp")
        tmp.writeText(toJson(events))
        tmp.renameTo(f) // replace in one step, so a crash never leaves half a file
    }

    fun toJson(events: List<Event>): String {
        val arr = JSONArray()
        events.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("date", e.date.toString())
                e.start?.let { put("start", it.toString()) }
                e.end?.let { put("end", it.toString()) }
                put("category", e.category.id)
                if (e.note.isNotEmpty()) put("note", e.note)
            })
        }
        return arr.toString()
    }

    fun fromJson(text: String): List<Event> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Event(
                id = o.getLong("id"),
                title = o.getString("title"),
                date = LocalDate.parse(o.getString("date")),
                start = o.optString("start").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                end = o.optString("end").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                category = Category.byId(o.optString("category")),
                note = o.optString("note"),
            )
        }
    }
}

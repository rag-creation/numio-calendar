package com.rr.numio.calendar.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * A Calendar backup: your events and settings in one small ".numio" file (JSON inside).
 * Made and read only on the phone — you choose where the file goes (Downloads, a USB stick, a chat to yourself).
 */
object Backup {
    private const val KIND = "numio-calendar-backup"
    private const val VERSION = 1

    data class Contents(val events: List<Event>, val settings: CalendarSettings?)

    fun write(events: List<Event>, settings: CalendarSettings): String = JSONObject().apply {
        put("kind", KIND)
        put("version", VERSION)
        put("events", JSONArray(EventStore.toJson(events)))
        put("settings", JSONObject().apply {
            put("regional", settings.regional)
            put("malayalamLayer", settings.malayalamLayer)
            put("hijriLayer", settings.hijriLayer)
            put("hijriAdjust", settings.hijriAdjust)
            put("festivals", JSONArray(settings.festivals.map { it.id }))
            put("countries", JSONArray(settings.countries))
            put("weekStart", settings.weekStart.value)
            put("countdown", settings.countdown)
            put("theme", settings.theme)
            put("secondZone", settings.secondZone)
        })
    }.toString(2)

    /** Reads a backup; throws if the file isn't a Calendar backup. */
    fun read(text: String, current: CalendarSettings): Contents {
        val o = JSONObject(text)
        require(o.optString("kind") == KIND) { "Not a Calendar backup" }
        val events = EventStore.fromJson(o.getJSONArray("events").toString())
        val s = o.optJSONObject("settings")?.let { j ->
            fun ids(name: String) = j.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } }
            current.copy(
                regional = j.optBoolean("regional", current.regional),
                malayalamLayer = j.optBoolean("malayalamLayer", current.malayalamLayer),
                hijriLayer = j.optBoolean("hijriLayer", current.hijriLayer),
                hijriAdjust = j.optInt("hijriAdjust", current.hijriAdjust).coerceIn(-1, 1),
                festivals = ids("festivals")?.let { list -> FestivalGroup.entries.filter { it.id in list }.toSet() } ?: current.festivals,
                countries = ids("countries") ?: current.countries,
                weekStart = java.time.DayOfWeek.of(j.optInt("weekStart", current.weekStart.value).coerceIn(1, 7)),
                countdown = j.optBoolean("countdown", current.countdown),
                theme = j.optString("theme", current.theme),
                secondZone = j.optString("secondZone", current.secondZone),
                welcomed = true,
            )
        }
        return Contents(events, s)
    }

    /** Your events plus the backup's, keeping yours when both have the same event. */
    fun merge(mine: List<Event>, backup: List<Event>): List<Event> {
        val ids = mine.map { it.id }.toSet()
        return mine + backup.filter { it.id !in ids }
    }
}

package com.rr.numio.calendar

import com.rr.numio.calendar.data.Category
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.EventStore
import com.rr.numio.calendar.data.sortedForDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventStoreTest {

    private val day = LocalDate.of(2026, 10, 16)

    @Test fun savesAndReadsBack() {
        val events = listOf(
            Event(1, "Team standup", day, LocalTime.of(9, 0), LocalTime.of(9, 30), Category.Work),
            Event(2, "Amma's birthday", day, note = "Call in the morning"),
        )
        assertEquals(events, EventStore.fromJson(EventStore.toJson(events)))
    }

    @Test fun allDayFirstThenByTime() {
        val a = Event(1, "Late", day, LocalTime.of(19, 0))
        val b = Event(2, "Early", day, LocalTime.of(8, 0))
        val c = Event(3, "Holiday", day)
        assertEquals(listOf(c, b, a), listOf(a, b, c).sortedForDay())
    }

    @Test fun unknownCategoryFallsBackToPersonal() {
        val json = """[{"id":5,"title":"x","date":"2026-10-16","category":"other"}]"""
        assertEquals(Category.Personal, EventStore.fromJson(json).single().category)
    }
}

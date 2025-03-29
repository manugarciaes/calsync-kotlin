package com.calsync.infrastructure.util

import com.calsync.domain.model.Event
import com.calsync.domain.model.EventId
import com.calsync.domain.model.CalendarId
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import java.io.StringReader
import java.net.URL
import java.time.ZoneId

/**
 * Utility class for handling ICS calendar files
 */
object IcsUtil {
    /**
     * Fetches an ICS calendar from a URL
     * @param url URL to fetch the calendar from
     * @return ICS calendar content as a string
     */
    fun fetchCalendarFromUrl(url: String): String {
        return URL(url).openStream().bufferedReader().use { it.readText() }
    }
    
    /**
     * Parses ICS calendar content into an ical4j Calendar object
     * @param content ICS calendar content
     * @return Parsed Calendar object
     */
    fun parseIcsContent(content: String): Calendar {
        val builder = CalendarBuilder()
        return builder.build(StringReader(content))
    }
    
    /**
     * Extract events from an ical4j Calendar object
     * @param calendar Parsed Calendar object
     * @param calendarId ID of the calendar these events belong to
     * @return List of domain Event objects
     */
    fun extractEvents(calendar: Calendar, calendarId: CalendarId): List<Event> {
        val events = mutableListOf<Event>()
        
        for (component in calendar.components) {
            if (component is VEvent) {
                val event = convertToEvent(component, calendarId)
                events.add(event)
            }
        }
        
        return events
    }
    
    /**
     * Converts an ical4j VEvent to a domain Event object
     * @param vevent ical4j VEvent object
     * @param calendarId ID of the calendar this event belongs to
     * @return Domain Event object
     */
    private fun convertToEvent(vevent: VEvent, calendarId: CalendarId): Event {
        val uid = vevent.uid?.value ?: ""
        val summary = vevent.summary?.value ?: ""
        val description = vevent.description?.value ?: ""
        val location = vevent.location?.value ?: ""
        
        // Get start and end times
        val startDate = vevent.startDate?.date
        val endDate = vevent.endDate?.date
        
        val startInstant = if (startDate != null) {
            java.time.Instant.from(startDate.toInstant()).toKotlinInstant()
        } else {
            Instant.fromEpochMilliseconds(0)
        }
        
        val endInstant = if (endDate != null) {
            java.time.Instant.from(endDate.toInstant()).toKotlinInstant()
        } else {
            startInstant
        }
        
        // Check if it's an all-day event
        val isAllDay = vevent.startDate.isUtc.not() && vevent.startDate.date !is java.util.Date
        
        // Get or default timezone
        val timeZone = vevent.startDate?.timeZone?.id ?: ZoneId.systemDefault().id
        
        return Event(
            id = EventId.generate(),
            calendarId = calendarId,
            uid = uid,
            title = summary,
            description = description,
            location = location,
            startTime = startInstant,
            endTime = endInstant,
            timeZone = timeZone,
            isAllDay = isAllDay
        )
    }
}
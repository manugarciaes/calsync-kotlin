package com.calsync.domain.repository

import com.calsync.domain.model.CalendarId
import com.calsync.domain.model.Event
import com.calsync.domain.model.EventId
import kotlinx.datetime.Instant

/**
 * Repository interface for Event entities
 */
interface EventRepository {
    /**
     * Find an event by ID
     */
    suspend fun findById(id: EventId): Event?
    
    /**
     * Find events by calendar ID
     */
    suspend fun findByCalendarId(calendarId: CalendarId): List<Event>
    
    /**
     * Find events by multiple calendar IDs
     */
    suspend fun findByCalendarIds(calendarIds: List<CalendarId>): List<Event>
    
    /**
     * Find events by calendar IDs within a time range
     */
    suspend fun findByCalendarIdsAndTimeRange(
        calendarIds: List<CalendarId>,
        startTime: Instant,
        endTime: Instant
    ): List<Event>
    
    /**
     * Save an event (create or update)
     */
    suspend fun save(event: Event): Event
    
    /**
     * Delete an event by ID
     */
    suspend fun delete(id: EventId): Boolean
    
    /**
     * Delete all events for a calendar
     */
    suspend fun deleteByCalendarId(calendarId: CalendarId): Boolean
}
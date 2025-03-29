package com.calsync.infrastructure.repository

import com.calsync.domain.model.CalendarId
import com.calsync.domain.model.Event
import com.calsync.domain.model.EventId
import com.calsync.domain.repository.EventRepository
import com.calsync.infrastructure.database.DatabaseConfig
import com.calsync.infrastructure.database.EventsTable
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Implementation of EventRepository
 */
class EventRepositoryImpl : EventRepository {
    private val logger = LoggerFactory.getLogger(EventRepositoryImpl::class.java)
    
    /**
     * Find an event by ID
     */
    override suspend fun findById(id: EventId): Event? = DatabaseConfig.dbQuery {
        EventsTable.select { EventsTable.id eq id.value }
            .singleOrNull()
            ?.toEvent()
    }
    
    /**
     * Find events by calendar ID
     */
    override suspend fun findByCalendarId(calendarId: CalendarId): List<Event> = DatabaseConfig.dbQuery {
        EventsTable.select { EventsTable.calendarId eq calendarId.value }
            .map { it.toEvent() }
    }
    
    /**
     * Find events by multiple calendar IDs
     */
    override suspend fun findByCalendarIds(calendarIds: List<CalendarId>): List<Event> = DatabaseConfig.dbQuery {
        // Skip query if no calendar IDs provided
        if (calendarIds.isEmpty()) {
            return@dbQuery emptyList()
        }
        
        val calendarValues = calendarIds.map { it.value }
        EventsTable.select { EventsTable.calendarId inList calendarValues }
            .map { it.toEvent() }
    }
    
    /**
     * Find events by calendar IDs within a time range
     */
    override suspend fun findByCalendarIdsAndTimeRange(
        calendarIds: List<CalendarId>,
        startTime: Instant,
        endTime: Instant
    ): List<Event> = DatabaseConfig.dbQuery {
        // Skip query if no calendar IDs provided
        if (calendarIds.isEmpty()) {
            return@dbQuery emptyList()
        }
        
        val calendarValues = calendarIds.map { it.value }
        
        // Convert Instant to java.sql.Timestamp
        val start = java.sql.Timestamp.from(startTime.toJavaInstant())
        val end = java.sql.Timestamp.from(endTime.toJavaInstant())
        
        EventsTable.select {
            (EventsTable.calendarId inList calendarValues) and
                    (
                            // Event starts within range
                            (EventsTable.startTime greaterEq start and (EventsTable.startTime less end)) or
                                    // Event ends within range
                                    (EventsTable.endTime greater start and (EventsTable.endTime lessEq end)) or
                                    // Event spans the entire range
                                    (EventsTable.startTime lessEq start and (EventsTable.endTime greaterEq end))
                    )
        }
            .map { it.toEvent() }
    }
    
    /**
     * Save an event (create or update)
     */
    override suspend fun save(event: Event): Event = DatabaseConfig.dbQuery {
        // Check if event exists
        val exists = EventsTable.select { EventsTable.id eq event.id.value }.count() > 0
        
        val now = LocalDateTime.now().toInstant(ZoneOffset.UTC).toKotlinInstant()
        
        if (exists) {
            // Update existing event
            EventsTable.update({ EventsTable.id eq event.id.value }) {
                it[calendarId] = event.calendarId.value
                it[uid] = event.uid
                it[title] = event.title
                it[description] = event.description
                it[location] = event.location
                it[startTime] = java.sql.Timestamp.from(event.startTime.toJavaInstant())
                it[endTime] = java.sql.Timestamp.from(event.endTime.toJavaInstant())
                it[timeZone] = event.timeZone
                it[isAllDay] = event.isAllDay
                it[recurrenceRule] = event.recurrenceRule
                it[updatedAt] = java.sql.Timestamp.from(now.toJavaInstant())
            }
        } else {
            // Insert new event
            EventsTable.insert {
                it[id] = event.id.value
                it[calendarId] = event.calendarId.value
                it[uid] = event.uid
                it[title] = event.title
                it[description] = event.description
                it[location] = event.location
                it[startTime] = java.sql.Timestamp.from(event.startTime.toJavaInstant())
                it[endTime] = java.sql.Timestamp.from(event.endTime.toJavaInstant())
                it[timeZone] = event.timeZone
                it[isAllDay] = event.isAllDay
                it[recurrenceRule] = event.recurrenceRule
                it[createdAt] = java.sql.Timestamp.from(now.toJavaInstant())
                it[updatedAt] = java.sql.Timestamp.from(now.toJavaInstant())
            }
        }
        
        // Return updated event
        event.copy(updatedAt = now)
    }
    
    /**
     * Delete an event by ID
     */
    override suspend fun delete(id: EventId): Boolean = DatabaseConfig.dbQuery {
        EventsTable.deleteWhere { EventsTable.id eq id.value } > 0
    }
    
    /**
     * Delete all events for a calendar
     */
    override suspend fun deleteByCalendarId(calendarId: CalendarId): Boolean = DatabaseConfig.dbQuery {
        EventsTable.deleteWhere { EventsTable.calendarId eq calendarId.value } > 0
    }
    
    /**
     * Extension function to convert a ResultRow to an Event
     */
    private fun ResultRow.toEvent(): Event {
        return Event(
            id = EventId(this[EventsTable.id].toString()),
            calendarId = CalendarId(this[EventsTable.calendarId].toString()),
            uid = this[EventsTable.uid],
            title = this[EventsTable.title],
            description = this[EventsTable.description],
            location = this[EventsTable.location],
            startTime = this[EventsTable.startTime].toInstant().toKotlinInstant(),
            endTime = this[EventsTable.endTime].toInstant().toKotlinInstant(),
            timeZone = this[EventsTable.timeZone],
            isAllDay = this[EventsTable.isAllDay],
            recurrenceRule = this[EventsTable.recurrenceRule],
            createdAt = this[EventsTable.createdAt].toInstant().toKotlinInstant(),
            updatedAt = this[EventsTable.updatedAt].toInstant().toKotlinInstant()
        )
    }
}
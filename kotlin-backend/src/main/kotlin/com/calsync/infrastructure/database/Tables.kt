package com.calsync.infrastructure.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Database table for users
 */
object UsersTable : UUIDTable(name = "users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 100)
    val name = varchar("name", 100)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/**
 * Database table for calendars
 */
object CalendarsTable : UUIDTable(name = "calendars") {
    val userId = reference("user_id", UsersTable)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val color = varchar("color", 20).nullable()
    val sourceType = varchar("source_type", 20)
    val sourceUrl = text("source_url").nullable()
    val sourceFile = text("source_file").nullable()
    val lastSynced = timestamp("last_synced").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/**
 * Database table for events
 */
object EventsTable : UUIDTable(name = "events") {
    val calendarId = reference("calendar_id", CalendarsTable)
    val uid = varchar("uid", 255)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val location = text("location").nullable()
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val timeZone = varchar("time_zone", 50)
    val isAllDay = bool("is_all_day")
    val recurrenceRule = text("recurrence_rule").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        uniqueIndex("events_calendar_uid_idx", calendarId, uid)
    }
}

/**
 * Database table for free slots
 */
object FreeSlotsTable : UUIDTable(name = "free_slots") {
    val userId = reference("user_id", UsersTable)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val shareUrl = varchar("share_url", 100).uniqueIndex()
    val slotDuration = integer("slot_duration") // in minutes
    val bufferTime = integer("buffer_time") // in minutes
    val timeZone = varchar("time_zone", 50)
    val startDate = timestamp("start_date").nullable()
    val endDate = timestamp("end_date").nullable()
    val maxBookingsPerDay = integer("max_bookings_per_day").nullable()
    val active = bool("active")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/**
 * Junction table for free slots and days of week
 */
object FreeSlotDaysTable : Table(name = "free_slot_days") {
    val freeSlotId = reference("free_slot_id", FreeSlotsTable)
    val dayOfWeek = enumeration<DayOfWeek>("day_of_week")
    
    override val primaryKey = PrimaryKey(freeSlotId, dayOfWeek)
}

/**
 * Junction table for free slots and calendars
 */
object FreeSlotCalendarsTable : Table(name = "free_slot_calendars") {
    val freeSlotId = reference("free_slot_id", FreeSlotsTable)
    val calendarId = reference("calendar_id", CalendarsTable)
    
    override val primaryKey = PrimaryKey(freeSlotId, calendarId)
}

/**
 * Database table for free slot time ranges
 */
object FreeSlotTimeRangesTable : UUIDTable(name = "free_slot_time_ranges") {
    val freeSlotId = reference("free_slot_id", FreeSlotsTable)
    val startTime = varchar("start_time", 10) // HH:mm format
    val endTime = varchar("end_time", 10) // HH:mm format
}

/**
 * Database table for bookings
 */
object BookingsTable : UUIDTable(name = "bookings") {
    val freeSlotId = reference("free_slot_id", FreeSlotsTable)
    val name = varchar("name", 100)
    val email = varchar("email", 100)
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val timeZone = varchar("time_zone", 50)
    val notes = text("notes").nullable()
    val status = enumeration<BookingStatus>("status")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/**
 * Booking status enum
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
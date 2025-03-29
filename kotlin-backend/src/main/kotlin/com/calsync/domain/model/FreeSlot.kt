package com.calsync.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Time range value object
 * Represents a time range within a day (e.g., 9:00-17:00)
 */
data class TimeRange(
    val startTime: String,  // HH:mm format
    val endTime: String,    // HH:mm format
    
    // For convenience, we add these properties for service code
    val startHour: Int = startTime.split(":")[0].toInt(),
    val startMinute: Int = startTime.split(":")[1].toInt(),
    val endHour: Int = endTime.split(":")[0].toInt(),
    val endMinute: Int = endTime.split(":")[1].toInt()
)

/**
 * FreeSlot entity
 * Represents a configurable set of available time slots for bookings
 */
data class FreeSlot(
    val id: FreeSlotId,
    val userId: UserId,
    val name: String,
    val description: String? = null,
    val shareUrl: String,
    val slotDuration: Int,  // Duration in minutes
    val bufferTime: Int,    // Buffer time in minutes
    val timeZone: String,
    val availableDays: Set<DayOfWeek>,
    val timeRanges: List<TimeRange>,
    val startDate: java.time.LocalDate? = null,
    val endDate: java.time.LocalDate? = null,
    val maxBookingsPerDay: Int? = null,
    val calendarIds: List<CalendarId>,
    val active: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
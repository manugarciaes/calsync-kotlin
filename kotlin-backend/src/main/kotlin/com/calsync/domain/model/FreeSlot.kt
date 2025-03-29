package com.calsync.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

/**
 * FreeSlot ID value object
 */
@JvmInline
value class FreeSlotId(val value: String) {
    companion object {
        fun generate(): FreeSlotId = FreeSlotId(UUID.randomUUID().toString())
    }
}

/**
 * Time range value object
 * Represents a time range within a day (e.g., 9:00-17:00)
 */
data class TimeRange(
    val startTime: String,  // HH:mm format
    val endTime: String     // HH:mm format
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
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val maxBookingsPerDay: Int? = null,
    val calendarIds: List<CalendarId>,
    val active: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
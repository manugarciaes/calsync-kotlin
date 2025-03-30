package com.calsync.infrastructure.api.dto

import com.calsync.domain.model.FreeSlot
import com.calsync.domain.model.TimeRange
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DTO for time range in a free slot
 */
@Serializable
data class TimeRangeDto(
    val startTime: String,  // HH:mm format
    val endTime: String     // HH:mm format
) {
    companion object {
        fun fromTimeRange(timeRange: TimeRange): TimeRangeDto {
            return TimeRangeDto(
                startTime = timeRange.startTime,
                endTime = timeRange.endTime
            )
        }
        
        fun toTimeRange(dto: TimeRangeDto): TimeRange {
            return TimeRange(
                startTime = dto.startTime,
                endTime = dto.endTime
            )
        }
    }
}

/**
 * DTO for free slot creation/update requests
 */
@Serializable
data class FreeSlotRequest(
    val name: String,
    val description: String? = null,
    val slotDuration: Int,   // Duration in minutes
    val bufferTime: Int,     // Buffer time in minutes
    val timeZone: String,
    val availableDays: Set<DayOfWeek>,
    val timeRanges: List<TimeRangeDto>,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val maxBookingsPerDay: Int? = null,
    val calendarIds: List<String>,
    val active: Boolean = true
)

/**
 * DTO for free slot responses
 */
@Serializable
data class FreeSlotResponse(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val shareUrl: String,
    val slotDuration: Int,   // Duration in minutes
    val bufferTime: Int,     // Buffer time in minutes
    val timeZone: String,
    val availableDays: List<DayOfWeek>,
    val timeRanges: List<TimeRangeDto>,
    val startDate: String? = null,
    val endDate: String? = null,
    val maxBookingsPerDay: Int? = null,
    val calendarIds: List<String>,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        /**
         * Convert FreeSlot domain entity to FreeSlotResponse DTO
         */
        fun fromFreeSlot(freeSlot: FreeSlot): FreeSlotResponse {
            return FreeSlotResponse(
                id = freeSlot.id.toString(),
                userId = freeSlot.userId.toString(),
                name = freeSlot.name,
                description = freeSlot.description,
                shareUrl = freeSlot.shareUrl,
                slotDuration = freeSlot.slotDuration,
                bufferTime = freeSlot.bufferTime,
                timeZone = freeSlot.timeZone,
                availableDays = freeSlot.availableDays.toList(),
                timeRanges = freeSlot.timeRanges.map { TimeRangeDto.fromTimeRange(it) },
                startDate = freeSlot.startDate?.format(dateFormatter),
                endDate = freeSlot.endDate?.format(dateFormatter),
                maxBookingsPerDay = freeSlot.maxBookingsPerDay,
                calendarIds = freeSlot.calendarIds.map { it.toString() },
                active = freeSlot.active,
                createdAt = freeSlot.createdAt,
                updatedAt = freeSlot.updatedAt
            )
        }
    }
}

/**
 * DTO for available time slot
 */
@Serializable
data class AvailableTimeSlotDto(
    val startTime: String,    // ISO format (e.g., "2023-05-15T09:00:00")
    val endTime: String       // ISO format (e.g., "2023-05-15T09:30:00")
)
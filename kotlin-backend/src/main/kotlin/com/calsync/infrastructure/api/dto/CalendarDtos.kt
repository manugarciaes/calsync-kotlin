package com.calsync.infrastructure.api.dto

import com.calsync.domain.model.Calendar
import com.calsync.domain.model.Event
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * DTO for calendar creation/update requests
 */
@Serializable
data class CalendarRequest(
    val name: String,
    val url: String? = null,
    val color: String? = null,
    val description: String? = null,
    val syncIntervalMinutes: Int = 60
)

/**
 * DTO for calendar responses
 */
@Serializable
data class CalendarResponse(
    val id: String,
    val userId: String,
    val name: String,
    val url: String? = null,
    val color: String? = null,
    val description: String? = null,
    val lastSynced: Instant? = null,
    val syncIntervalMinutes: Int,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        /**
         * Convert Calendar domain entity to CalendarResponse DTO
         */
        fun fromCalendar(calendar: Calendar): CalendarResponse {
            return CalendarResponse(
                id = calendar.id.toString(),
                userId = calendar.userId.toString(),
                name = calendar.name,
                url = calendar.url,
                color = calendar.color,
                description = calendar.description,
                lastSynced = calendar.lastSynced,
                syncIntervalMinutes = calendar.syncIntervalMinutes,
                active = calendar.active,
                createdAt = calendar.createdAt,
                updatedAt = calendar.updatedAt
            )
        }
    }
}

/**
 * DTO for calendar file upload response
 */
@Serializable
data class CalendarUploadResponse(
    val calendar: CalendarResponse,
    val eventsImported: Int
)

/**
 * DTO for event responses
 */
@Serializable
data class EventResponse(
    val id: String,
    val calendarId: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Instant,
    val endTime: Instant,
    val allDay: Boolean,
    val recurringEventId: String? = null,
    val externalId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        /**
         * Convert Event domain entity to EventResponse DTO
         */
        fun fromEvent(event: Event): EventResponse {
            return EventResponse(
                id = event.id.toString(),
                calendarId = event.calendarId.toString(),
                title = event.title,
                description = event.description,
                location = event.location,
                startTime = event.startTime,
                endTime = event.endTime,
                allDay = event.allDay,
                recurringEventId = event.recurringEventId?.toString(),
                externalId = event.externalId,
                createdAt = event.createdAt,
                updatedAt = event.updatedAt
            )
        }
    }
}
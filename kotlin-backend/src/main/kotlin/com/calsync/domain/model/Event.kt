package com.calsync.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Event entity
 * Represents a calendar event
 */
data class Event(
    val id: EventId,
    val calendarId: CalendarId,
    val uid: String,  // UID from iCal
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: String,
    val isAllDay: Boolean = false,
    val recurrenceRule: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
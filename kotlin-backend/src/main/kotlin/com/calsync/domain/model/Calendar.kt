package com.calsync.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Calendar source type enum
 */
enum class CalendarSourceType {
    URL,    // Calendar from external URL (e.g., iCal URL)
    FILE,   // Calendar from uploaded file
    MANUAL  // Manually created calendar
}

/**
 * Calendar entity
 * Represents a calendar with events
 */
data class Calendar(
    val id: CalendarId,
    val userId: UserId,
    val name: String,
    val description: String? = null,
    val color: String? = null,
    val sourceType: CalendarSourceType,
    val sourceUrl: String? = null,
    val sourceFile: String? = null,
    val lastSynced: Instant? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
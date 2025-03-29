package com.calsync.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Booking status enum
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}

/**
 * Booking entity
 * Represents a booking made by a user
 */
data class Booking(
    val id: BookingId,
    val freeSlotId: FreeSlotId,
    val name: String,
    val email: String,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: String,
    val notes: String? = null,
    val status: BookingStatus = BookingStatus.PENDING,
    val cancelReason: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    /**
     * Cancel this booking
     */
    fun cancel(reason: String? = null): Booking {
        return this.copy(
            status = BookingStatus.CANCELLED,
            cancelReason = reason,
            updatedAt = Clock.System.now()
        )
    }
    
    /**
     * Confirm this booking
     */
    fun confirm(): Booking {
        return this.copy(
            status = BookingStatus.CONFIRMED,
            updatedAt = Clock.System.now()
        )
    }
}
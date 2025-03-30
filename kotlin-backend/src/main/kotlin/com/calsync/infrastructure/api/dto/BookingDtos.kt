package com.calsync.infrastructure.api.dto

import com.calsync.domain.model.Booking
import com.calsync.domain.model.BookingStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter

/**
 * DTO for booking creation requests
 */
@Serializable
data class BookingRequest(
    val freeSlotShareUrl: String,
    val name: String,
    val email: String,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: String,
    val notes: String? = null
)

/**
 * DTO for booking responses
 */
@Serializable
data class BookingResponse(
    val id: String,
    val freeSlotId: String,
    val name: String,
    val email: String,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: String,
    val notes: String? = null,
    val status: BookingStatus,
    val cancelReason: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        /**
         * Convert Booking domain entity to BookingResponse DTO
         */
        fun fromBooking(booking: Booking): BookingResponse {
            return BookingResponse(
                id = booking.id.toString(),
                freeSlotId = booking.freeSlotId.toString(),
                name = booking.name,
                email = booking.email,
                startTime = booking.startTime,
                endTime = booking.endTime,
                timeZone = booking.timeZone,
                notes = booking.notes,
                status = booking.status,
                cancelReason = booking.cancelReason,
                createdAt = booking.createdAt,
                updatedAt = booking.updatedAt
            )
        }
    }
}
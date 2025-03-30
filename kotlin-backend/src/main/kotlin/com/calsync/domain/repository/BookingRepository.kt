package com.calsync.domain.repository

import com.calsync.domain.model.Booking
import com.calsync.domain.model.BookingId
import com.calsync.domain.model.FreeSlotId
import com.calsync.domain.model.BookingStatus
import kotlinx.datetime.Instant

/**
 * Repository interface for Booking entities
 */
interface BookingRepository {
    /**
     * Get all bookings for a free slot
     */
    suspend fun getBookingsByFreeSlotId(freeSlotId: FreeSlotId): List<Booking>
    
    /**
     * Get bookings for a free slot within a time range
     */
    suspend fun getBookingsByFreeSlotIdAndTimeRange(
        freeSlotId: FreeSlotId,
        startTime: Instant,
        endTime: Instant
    ): List<Booking>
    
    /**
     * Get a booking by ID
     */
    suspend fun getBookingById(id: BookingId): Booking?
    
    /**
     * Create a new booking
     */
    suspend fun createBooking(booking: Booking): Booking
    
    /**
     * Update a booking
     */
    suspend fun updateBooking(booking: Booking): Booking
    
    /**
     * Update booking status
     */
    suspend fun updateBookingStatus(id: BookingId, status: BookingStatus, cancelReason: String? = null): Booking?
    
    /**
     * Delete a booking
     */
    suspend fun deleteBooking(id: BookingId): Boolean
}
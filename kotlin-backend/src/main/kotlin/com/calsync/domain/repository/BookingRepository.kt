package com.calsync.domain.repository

import com.calsync.domain.model.Booking
import com.calsync.domain.model.BookingId
import com.calsync.domain.model.BookingStatus
import com.calsync.domain.model.FreeSlotId
import kotlinx.datetime.Instant

/**
 * Repository interface for Booking entities
 */
interface BookingRepository {
    /**
     * Find a booking by ID
     */
    suspend fun findById(id: BookingId): Booking?
    
    /**
     * Find bookings by free slot ID
     */
    suspend fun findByFreeSlotId(freeSlotId: FreeSlotId): List<Booking>
    
    /**
     * Find bookings by free slot ID and status
     */
    suspend fun findByFreeSlotIdAndStatus(freeSlotId: FreeSlotId, status: BookingStatus): List<Booking>
    
    /**
     * Find bookings for a specific date range
     */
    suspend fun findByTimeRange(startTime: Instant, endTime: Instant): List<Booking>
    
    /**
     * Find bookings for a free slot in a specific date range
     */
    suspend fun findByFreeSlotIdAndTimeRange(
        freeSlotId: FreeSlotId,
        startTime: Instant,
        endTime: Instant
    ): List<Booking>
    
    /**
     * Count bookings for a free slot on a specific date
     */
    suspend fun countBookingsByFreeSlotAndDate(
        freeSlotId: FreeSlotId,
        startOfDay: Instant,
        endOfDay: Instant
    ): Int
    
    /**
     * Save a booking (create or update)
     */
    suspend fun save(booking: Booking): Booking
    
    /**
     * Update the status of a booking
     */
    suspend fun updateStatus(id: BookingId, status: BookingStatus, reason: String? = null): Booking?
    
    /**
     * Delete a booking by ID
     */
    suspend fun delete(id: BookingId): Boolean
}
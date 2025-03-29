package com.calsync.application.service

import com.calsync.domain.model.*
import com.calsync.domain.repository.BookingRepository
import com.calsync.domain.repository.FreeSlotRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Service for managing bookings
 */
class BookingService(
    private val bookingRepository: BookingRepository,
    private val freeSlotRepository: FreeSlotRepository,
    private val freeSlotService: FreeSlotService,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(BookingService::class.java)
    
    /**
     * Create a new booking
     */
    suspend fun createBooking(request: BookingRequest): Result<Booking> {
        try {
            // Validate the free slot exists and is active
            val freeSlotResult = freeSlotService.getFreeSlotByShareUrl(request.freeSlotUrl)
            if (freeSlotResult.isFailure) {
                return Result.failure(freeSlotResult.exceptionOrNull() ?: IllegalArgumentException("Invalid free slot URL"))
            }
            
            val freeSlot = freeSlotResult.getOrThrow()
            
            // Validate that the slot is within the allowed time range
            val startLocalDate = request.startTime.toLocalDateTime(TimeZone.of(request.timeZone)).date
            
            val availableSlotsResult = freeSlotService.calculateAvailableSlots(
                freeSlot.id,
                startLocalDate.toJavaLocalDate()
            )
            
            if (availableSlotsResult.isFailure) {
                return Result.failure(availableSlotsResult.exceptionOrNull() 
                    ?: IllegalArgumentException("Failed to calculate available slots"))
            }
            
            val availableSlots = availableSlotsResult.getOrThrow()
            
            // Check if the requested slot is available
            val requestedSlot = availableSlots.find { slot ->
                slot.start == request.startTime && slot.end == request.endTime
            } ?: return Result.failure(IllegalArgumentException("The requested time slot is not available"))
            
            // Create booking
            val booking = Booking(
                id = BookingId.generate(),
                freeSlotId = freeSlot.id,
                name = request.name,
                email = request.email,
                startTime = request.startTime,
                endTime = request.endTime,
                timeZone = request.timeZone,
                notes = request.notes,
                status = BookingStatus.PENDING
            )
            
            // Save booking
            val savedBooking = bookingRepository.save(booking)
            
            // Send confirmation emails
            emailService.sendBookingConfirmationToCustomer(savedBooking)
            emailService.sendBookingNotificationToOwner(savedBooking)
            
            return Result.success(savedBooking)
        } catch (e: Exception) {
            logger.error("Error creating booking", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Get a booking by ID
     */
    suspend fun getBooking(id: BookingId): Result<Booking> {
        return try {
            val booking = bookingRepository.findById(id)
                ?: return Result.failure(IllegalArgumentException("Booking not found: $id"))
            
            Result.success(booking)
        } catch (e: Exception) {
            logger.error("Error retrieving booking: $id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get bookings for a free slot
     */
    suspend fun getBookingsForFreeSlot(freeSlotId: FreeSlotId): Result<List<Booking>> {
        return try {
            val bookings = bookingRepository.findByFreeSlotId(freeSlotId)
            Result.success(bookings)
        } catch (e: Exception) {
            logger.error("Error retrieving bookings for free slot: $freeSlotId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a booking
     */
    suspend fun cancelBooking(id: BookingId, reason: String? = null): Result<Booking> {
        return try {
            val booking = bookingRepository.findById(id)
                ?: return Result.failure(IllegalArgumentException("Booking not found: $id"))
            
            if (booking.status == BookingStatus.CANCELLED) {
                return Result.success(booking)
            }
            
            // Update booking status
            val cancelledBooking = bookingRepository.updateStatus(
                id,
                BookingStatus.CANCELLED,
                reason
            ) ?: return Result.failure(IllegalStateException("Failed to update booking status"))
            
            // Send cancellation notification
            emailService.sendBookingCancellationNotification(cancelledBooking)
            
            Result.success(cancelledBooking)
        } catch (e: Exception) {
            logger.error("Error cancelling booking: $id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Confirm a booking
     */
    suspend fun confirmBooking(id: BookingId): Result<Booking> {
        return try {
            val booking = bookingRepository.findById(id)
                ?: return Result.failure(IllegalArgumentException("Booking not found: $id"))
            
            if (booking.status == BookingStatus.CONFIRMED) {
                return Result.success(booking)
            }
            
            // Update booking status
            val confirmedBooking = bookingRepository.updateStatus(
                id,
                BookingStatus.CONFIRMED
            ) ?: return Result.failure(IllegalStateException("Failed to update booking status"))
            
            Result.success(confirmedBooking)
        } catch (e: Exception) {
            logger.error("Error confirming booking: $id", e)
            Result.failure(e)
        }
    }
}

/**
 * Data class for booking creation request
 */
data class BookingRequest(
    val freeSlotUrl: String,
    val name: String,
    val email: String,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: String,
    val notes: String? = null
)
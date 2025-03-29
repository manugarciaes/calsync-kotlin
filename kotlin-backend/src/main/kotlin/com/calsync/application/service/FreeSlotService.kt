package com.calsync.application.service

import com.calsync.domain.model.*
import com.calsync.domain.repository.BookingRepository
import com.calsync.domain.repository.CalendarRepository
import com.calsync.domain.repository.EventRepository
import com.calsync.domain.repository.FreeSlotRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Service for managing free slots and calculating available time slots
 */
class FreeSlotService(
    private val freeSlotRepository: FreeSlotRepository,
    private val calendarRepository: CalendarRepository,
    private val eventRepository: EventRepository,
    private val bookingRepository: BookingRepository
) {
    private val logger = LoggerFactory.getLogger(FreeSlotService::class.java)
    
    /**
     * Create a new free slot
     */
    suspend fun createFreeSlot(request: FreeSlotRequest): Result<FreeSlot> {
        return try {
            logger.info("Creating free slot: ${request.name}")
            
            // Generate a unique share URL
            val shareUrl = generateShareUrl()
            
            // Create free slot
            val freeSlot = FreeSlot(
                id = FreeSlotId.generate(),
                userId = request.userId,
                name = request.name,
                description = request.description,
                shareUrl = shareUrl,
                slotDuration = request.slotDuration,
                bufferTime = request.bufferTime,
                timeZone = request.timeZone,
                availableDays = request.availableDays,
                timeRanges = request.timeRanges,
                startDate = request.startDate,
                endDate = request.endDate,
                maxBookingsPerDay = request.maxBookingsPerDay,
                calendarIds = request.calendarIds,
                active = request.active
            )
            
            // Save free slot
            val savedFreeSlot = freeSlotRepository.save(freeSlot)
            Result.success(savedFreeSlot)
        } catch (e: Exception) {
            logger.error("Error creating free slot", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update a free slot
     */
    suspend fun updateFreeSlot(id: FreeSlotId, request: UpdateFreeSlotRequest): Result<FreeSlot> {
        return try {
            // Get existing free slot
            val existingFreeSlot = freeSlotRepository.findById(id)
                ?: return Result.failure(IllegalArgumentException("Free slot not found: $id"))
            
            logger.info("Updating free slot: ${existingFreeSlot.name} (${existingFreeSlot.id})")
            
            // Create updated free slot
            val updatedFreeSlot = existingFreeSlot.copy(
                name = request.name ?: existingFreeSlot.name,
                description = request.description ?: existingFreeSlot.description,
                slotDuration = request.slotDuration ?: existingFreeSlot.slotDuration,
                bufferTime = request.bufferTime ?: existingFreeSlot.bufferTime,
                timeZone = request.timeZone ?: existingFreeSlot.timeZone,
                availableDays = request.availableDays ?: existingFreeSlot.availableDays,
                timeRanges = request.timeRanges ?: existingFreeSlot.timeRanges,
                startDate = request.startDate ?: existingFreeSlot.startDate,
                endDate = request.endDate ?: existingFreeSlot.endDate,
                maxBookingsPerDay = request.maxBookingsPerDay ?: existingFreeSlot.maxBookingsPerDay,
                calendarIds = request.calendarIds ?: existingFreeSlot.calendarIds,
                active = request.active ?: existingFreeSlot.active
            )
            
            // Save updated free slot
            val savedFreeSlot = freeSlotRepository.save(updatedFreeSlot)
            Result.success(savedFreeSlot)
        } catch (e: Exception) {
            logger.error("Error updating free slot: $id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a free slot by ID
     */
    suspend fun getFreeSlot(id: FreeSlotId): Result<FreeSlot> {
        return try {
            val freeSlot = freeSlotRepository.findById(id)
                ?: return Result.failure(IllegalArgumentException("Free slot not found: $id"))
            
            Result.success(freeSlot)
        } catch (e: Exception) {
            logger.error("Error retrieving free slot: $id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a free slot by share URL
     */
    suspend fun getFreeSlotByShareUrl(shareUrl: String): Result<FreeSlot> {
        return try {
            val freeSlot = freeSlotRepository.findByShareUrl(shareUrl)
                ?: return Result.failure(IllegalArgumentException("Free slot not found for URL: $shareUrl"))
            
            // Check if the free slot is active
            if (!freeSlot.active) {
                return Result.failure(IllegalArgumentException("Free slot is not active"))
            }
            
            Result.success(freeSlot)
        } catch (e: Exception) {
            logger.error("Error retrieving free slot by share URL: $shareUrl", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get free slots for a user
     */
    suspend fun getFreeSlotsByUser(userId: UserId): Result<List<FreeSlot>> {
        return try {
            val freeSlots = freeSlotRepository.findByUserId(userId)
            Result.success(freeSlots)
        } catch (e: Exception) {
            logger.error("Error retrieving free slots for user: $userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a free slot
     */
    suspend fun deleteFreeSlot(id: FreeSlotId): Result<Boolean> {
        return try {
            val success = freeSlotRepository.delete(id)
            if (!success) {
                return Result.failure(IllegalArgumentException("Failed to delete free slot: $id"))
            }
            Result.success(true)
        } catch (e: Exception) {
            logger.error("Error deleting free slot: $id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate a unique share URL
     */
    private suspend fun generateShareUrl(): String {
        // Generate a random string of characters
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val length = 8
        
        // Try up to 10 times to generate a unique URL
        for (i in 0 until 10) {
            val shareUrl = (1..length)
                .map { chars.random() }
                .joinToString("")
            
            // Check if URL already exists
            if (freeSlotRepository.findByShareUrl(shareUrl) == null) {
                return shareUrl
            }
        }
        
        // If we couldn't generate a unique URL, use a UUID-based one
        return UUID.randomUUID().toString().take(8)
    }
    
    /**
     * Calculate available time slots for a free slot on a specific date
     */
    suspend fun calculateAvailableSlots(
        freeSlotId: FreeSlotId,
        date: java.time.LocalDate
    ): Result<List<AvailableSlot>> {
        return try {
            val freeSlot = freeSlotRepository.findById(freeSlotId)
                ?: return Result.failure(IllegalArgumentException("Free slot not found: $freeSlotId"))
            
            if (!freeSlot.active) {
                return Result.failure(IllegalArgumentException("Free slot is not active"))
            }
            
            // Convert java.time.LocalDate to kotlinx.datetime.LocalDate
            val kotlinDate = LocalDate(date.year, date.monthValue, date.dayOfMonth)
            
            // Check if the date is within the allowed date range
            if (freeSlot.startDate != null && kotlinDate < freeSlot.startDate) {
                return Result.failure(IllegalArgumentException("Date is before start date"))
            }
            if (freeSlot.endDate != null && kotlinDate > freeSlot.endDate) {
                return Result.failure(IllegalArgumentException("Date is after end date"))
            }
            
            // Check if the day of week is allowed
            val dayOfWeek = date.dayOfWeek
            if (!freeSlot.availableDays.contains(dayOfWeek)) {
                return Result.success(emptyList())
            }
            
            // Get the time zone for the free slot
            val timeZone = TimeZone.of(freeSlot.timeZone)
            
            // Get events from all calendars for this date
            val busyTimes = getBusyTimesForDate(freeSlot, kotlinDate, timeZone)
            
            // Calculate available slots based on busy times
            val availableSlots = calculateTimeSlots(freeSlot, kotlinDate, busyTimes, timeZone)
            
            Result.success(availableSlots)
        } catch (e: Exception) {
            logger.error("Error calculating available slots for free slot: $freeSlotId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get busy times (events and bookings) for a specific date
     */
    private suspend fun getBusyTimesForDate(
        freeSlot: FreeSlot,
        date: LocalDate,
        timeZone: TimeZone
    ): List<BusyTime> {
        val busyTimes = mutableListOf<BusyTime>()
        
        // Convert date to start and end of day in free slot's timezone
        val startOfDay = LocalDateTime(date, LocalTime(0, 0, 0)).toInstant(timeZone)
        val endOfDay = LocalDateTime(date, LocalTime(23, 59, 59)).toInstant(timeZone)
        
        // Get calendar events
        if (freeSlot.calendarIds.isNotEmpty()) {
            val events = eventRepository.findByCalendarIdsAndTimeRange(
                freeSlot.calendarIds, startOfDay, endOfDay
            )
            
            // Add events to busy times
            for (event in events) {
                busyTimes.add(
                    BusyTime(
                        start = event.startTime,
                        end = event.endTime
                    )
                )
            }
        }
        
        // Get existing bookings
        val bookings = bookingRepository.findByFreeSlotIdAndTimeRange(
            freeSlot.id, startOfDay, endOfDay
        )
        
        // Add bookings to busy times
        for (booking in bookings) {
            if (booking.status != BookingStatus.CANCELLED) {
                busyTimes.add(
                    BusyTime(
                        start = booking.startTime,
                        end = booking.endTime
                    )
                )
            }
        }
        
        return busyTimes
    }
    
    /**
     * Calculate available time slots based on busy times
     */
    private fun calculateTimeSlots(
        freeSlot: FreeSlot,
        date: LocalDate,
        busyTimes: List<BusyTime>,
        timeZone: TimeZone
    ): List<AvailableSlot> {
        val availableSlots = mutableListOf<AvailableSlot>()
        
        // Get slot duration in minutes
        val slotDuration = freeSlot.slotDuration.minutes
        
        // Get buffer time in minutes
        val bufferTime = freeSlot.bufferTime.minutes
        
        // Process each time range for the day
        for (timeRange in freeSlot.timeRanges) {
            // Create start and end times for this range on this date
            val rangeStart = LocalDateTime(
                date,
                LocalTime(timeRange.startHour, timeRange.startMinute)
            ).toInstant(timeZone)
            
            val rangeEnd = LocalDateTime(
                date,
                LocalTime(timeRange.endHour, timeRange.endMinute)
            ).toInstant(timeZone)
            
            // Skip if range is already past (for today)
            if (rangeEnd < Clock.System.now()) {
                continue
            }
            
            // Generate potential slots within this range
            var slotStart = rangeStart
            val totalSlotDuration = slotDuration + bufferTime
            
            while (slotStart.plus(slotDuration) <= rangeEnd) {
                val slotEnd = slotStart.plus(slotDuration)
                
                // Check if this slot overlaps with any busy time
                val isAvailable = busyTimes.none { busyTime ->
                    // Check for overlap
                    (slotStart < busyTime.end && slotEnd > busyTime.start)
                }
                
                if (isAvailable) {
                    availableSlots.add(
                        AvailableSlot(
                            start = slotStart,
                            end = slotEnd
                        )
                    )
                }
                
                // Move to next slot start time
                slotStart = slotStart.plus(totalSlotDuration)
            }
        }
        
        // Check if we've exceeded max bookings per day
        if (freeSlot.maxBookingsPerDay != null && freeSlot.maxBookingsPerDay > 0) {
            // Get start and end of day
            val startOfDay = LocalDateTime(date, LocalTime(0, 0, 0)).toInstant(timeZone)
            val endOfDay = LocalDateTime(date, LocalTime(23, 59, 59)).toInstant(timeZone)
            
            // Count existing bookings
            val bookingCount = bookingRepository.countBookingsByFreeSlotAndDate(
                freeSlot.id, startOfDay, endOfDay
            )
            
            // If we already have the maximum bookings, return empty list
            if (bookingCount >= freeSlot.maxBookingsPerDay) {
                return emptyList()
            }
            
            // Otherwise, limit the number of available slots
            val remainingSlots = freeSlot.maxBookingsPerDay - bookingCount
            if (availableSlots.size > remainingSlots) {
                return availableSlots.take(remainingSlots)
            }
        }
        
        return availableSlots
    }
}

/**
 * Data class for a free slot creation request
 */
data class FreeSlotRequest(
    val userId: UserId,
    val name: String,
    val description: String?,
    val slotDuration: Int,
    val bufferTime: Int,
    val timeZone: String,
    val availableDays: Set<DayOfWeek>,
    val timeRanges: List<TimeRange>,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val maxBookingsPerDay: Int?,
    val calendarIds: List<CalendarId>,
    val active: Boolean = true
)

/**
 * Data class for a free slot update request with all fields optional
 */
data class UpdateFreeSlotRequest(
    val name: String? = null,
    val description: String? = null,
    val slotDuration: Int? = null,
    val bufferTime: Int? = null,
    val timeZone: String? = null,
    val availableDays: Set<DayOfWeek>? = null,
    val timeRanges: List<TimeRange>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val maxBookingsPerDay: Int? = null,
    val calendarIds: List<CalendarId>? = null,
    val active: Boolean? = null
)

/**
 * Data class for a time slot with start and end time
 */
data class AvailableSlot(
    val start: Instant,
    val end: Instant
)

/**
 * Data class for busy time periods
 */
data class BusyTime(
    val start: Instant,
    val end: Instant
)
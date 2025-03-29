package com.calsync.application.service

import com.calsync.domain.model.*
import com.calsync.domain.repository.CalendarRepository
import com.calsync.domain.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.RRule
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.StringReader
import java.net.URL
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Service for synchronizing calendars from external sources (ICS)
 */
class CalendarSyncService(
    private val calendarRepository: CalendarRepository,
    private val eventRepository: EventRepository
) {
    private val logger = LoggerFactory.getLogger(CalendarSyncService::class.java)
    
    /**
     * Sync a calendar from its source
     */
    suspend fun syncCalendar(calendarId: CalendarId): Result<Calendar> {
        return try {
            // Get calendar by ID
            val calendar = calendarRepository.findById(calendarId)
                ?: return Result.failure(IllegalArgumentException("Calendar not found: $calendarId"))
            
            logger.info("Syncing calendar: ${calendar.name} (${calendar.id})")
            
            // Sync based on source type
            when (calendar.sourceType) {
                CalendarSourceType.URL -> syncFromUrl(calendar)
                CalendarSourceType.FILE -> syncFromFile(calendar)
                CalendarSourceType.MANUAL -> Result.success(calendar) // Manual calendars don't need syncing
            }
        } catch (e: Exception) {
            logger.error("Error syncing calendar: $calendarId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync a calendar from a URL
     */
    private suspend fun syncFromUrl(calendar: Calendar): Result<Calendar> {
        return try {
            val url = calendar.sourceUrl
                ?: return Result.failure(IllegalArgumentException("Calendar URL is missing"))
            
            logger.info("Syncing calendar from URL: $url")
            
            // Fetch ICS data from URL
            val icsData = withContext(Dispatchers.IO) {
                URL(url).readText()
            }
            
            // Process the ICS data and update events
            processIcsData(calendar, icsData)
        } catch (e: Exception) {
            logger.error("Error syncing calendar from URL: ${calendar.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync a calendar from a file
     */
    private suspend fun syncFromFile(calendar: Calendar): Result<Calendar> {
        return try {
            val sourceFile = calendar.sourceFile
                ?: return Result.failure(IllegalArgumentException("Calendar file is missing"))
            
            logger.info("Syncing calendar from file: $sourceFile")
            
            // Process the ICS data from file and update events
            processIcsData(calendar, sourceFile)
        } catch (e: Exception) {
            logger.error("Error syncing calendar from file: ${calendar.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process ICS data and update events in the database
     */
    private suspend fun processIcsData(calendar: Calendar, icsData: String): Result<Calendar> {
        return try {
            val calendarBuilder = CalendarBuilder()
            val icsCalendar = withContext(Dispatchers.IO) {
                calendarBuilder.build(StringReader(icsData))
            }
            
            // Track processed event UIDs for cleanup
            val processedEventUids = mutableSetOf<String>()
            
            // Process all events from the ICS file
            for (component in icsCalendar.components) {
                if (component is VEvent) {
                    val event = component.toEvent(calendar.id)
                    processedEventUids.add(event.uid)
                    
                    // Save event
                    eventRepository.save(event)
                }
            }
            
            // Delete events that no longer exist in the ICS
            val existingEvents = eventRepository.findByCalendarId(calendar.id)
            val eventsToDelete = existingEvents.filter { !processedEventUids.contains(it.uid) }
            
            for (event in eventsToDelete) {
                eventRepository.delete(event.id)
            }
            
            // Update calendar with last synced time
            val now = Clock.System.now()
            val updatedCalendar = calendar.copy(lastSynced = now)
            
            // Update last synced time in the database
            val savedCalendar = calendarRepository.save(updatedCalendar)
            
            logger.info("Successfully synced calendar: ${calendar.name} (${calendar.id}), " +
                    "processed ${processedEventUids.size} events, deleted ${eventsToDelete.size} events")
            
            Result.success(savedCalendar)
        } catch (e: Exception) {
            logger.error("Error processing ICS data for calendar: ${calendar.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a new calendar from a URL
     */
    suspend fun createCalendarFromUrl(userId: UserId, name: String, url: String): Result<Calendar> {
        return try {
            logger.info("Creating calendar from URL: $url")
            
            // Create a new calendar
            val calendar = Calendar(
                id = CalendarId.generate(),
                userId = userId,
                name = name,
                description = null,
                color = null,
                sourceType = CalendarSourceType.URL,
                sourceUrl = url,
                sourceFile = null,
                lastSynced = null
            )
            
            // Save the calendar
            val savedCalendar = calendarRepository.save(calendar)
            
            // Sync the calendar
            val syncResult = syncFromUrl(savedCalendar)
            
            if (syncResult.isFailure) {
                logger.warn("Initial sync failed for new calendar: ${savedCalendar.id}", 
                    syncResult.exceptionOrNull())
            }
            
            Result.success(savedCalendar)
        } catch (e: Exception) {
            logger.error("Error creating calendar from URL", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a new calendar from ICS file data
     */
    suspend fun createCalendarFromIcsData(userId: UserId, name: String, icsData: String): Result<Calendar> {
        return try {
            logger.info("Creating calendar from ICS data")
            
            // Create a new calendar
            val calendar = Calendar(
                id = CalendarId.generate(),
                userId = userId,
                name = name,
                description = null,
                color = null,
                sourceType = CalendarSourceType.FILE,
                sourceUrl = null,
                sourceFile = icsData,
                lastSynced = null
            )
            
            // Save the calendar
            val savedCalendar = calendarRepository.save(calendar)
            
            // Process the ICS data
            val syncResult = processIcsData(savedCalendar, icsData)
            
            if (syncResult.isFailure) {
                logger.warn("Initial processing failed for new calendar: ${savedCalendar.id}", 
                    syncResult.exceptionOrNull())
            }
            
            Result.success(savedCalendar)
        } catch (e: Exception) {
            logger.error("Error creating calendar from ICS data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extension function to convert an iCal4j VEvent to our domain Event
     */
    private fun VEvent.toEvent(calendarId: CalendarId): Event {
        // Get UID from event or generate one if not present
        val uid = getProperty<Property>(Property.UID)?.value ?: generateUID()
        
        // Get event title
        val title = summary?.value ?: "Untitled Event"
        
        // Get event description
        val description = description?.value
        
        // Get event location
        val location = location?.value
        
        // Get timezone from event or use UTC as default
        val timeZone = startDate?.timeZone?.id ?: "UTC"
        
        // Determine if event is all day
        val isAllDay = startDate?.isUtc == false && startDate?.date != null && endDate?.date != null
        
        // Get start and end times
        val startTime: Instant
        val endTime: Instant
        
        if (isAllDay) {
            // Handle all-day events
            val zone = TimeZone.of(timeZone)
            
            // Convert java.util.Date to kotlinx.datetime.LocalDate at midnight in event's timezone
            val startDate = LocalDate.parse(
                DateTimeFormatter.ISO_LOCAL_DATE.format(this.startDate.date.toInstant().atZone(ZoneId.of(timeZone)))
            )
            val startDateTime = LocalDateTime(startDate, LocalTime(0, 0, 0))
            startTime = startDateTime.toInstant(zone)
            
            // End date might be exclusive in iCal format, so we use the same time
            val endDate = if (this.endDate != null) {
                LocalDate.parse(
                    DateTimeFormatter.ISO_LOCAL_DATE.format(this.endDate.date.toInstant().atZone(ZoneId.of(timeZone)))
                )
            } else {
                startDate
            }
            val endDateTime = LocalDateTime(endDate, LocalTime(23, 59, 59))
            endTime = endDateTime.toInstant(zone)
        } else {
            // Handle timed events
            startTime = this.startDate.date.toInstant().toKotlinInstant()
            endTime = (this.endDate?.date ?: this.startDate.date).toInstant().toKotlinInstant()
        }
        
        // Get recurrence rule if present
        val recurrenceRule = getProperty<RRule>(Property.RRULE)?.value
        
        return Event(
            id = EventId.generate(),
            calendarId = calendarId,
            uid = uid,
            title = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            timeZone = timeZone,
            isAllDay = isAllDay,
            recurrenceRule = recurrenceRule
        )
    }
    
    /**
     * Generate a UID for events that don't have one
     */
    private fun generateUID(): String {
        return "generated-${java.util.UUID.randomUUID()}"
    }
}

/**
 * Extension function to convert java.util.Date.toInstant() to kotlinx.datetime.Instant
 */
private fun java.time.Instant.toKotlinInstant(): Instant {
    return Instant.fromEpochMilliseconds(this.toEpochMilli())
}
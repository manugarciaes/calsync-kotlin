package com.calsync.application.scheduler

import com.calsync.application.service.CalendarSyncService
import com.calsync.domain.model.Calendar
import com.calsync.domain.model.CalendarId
import com.calsync.domain.model.CalendarSourceType
import com.calsync.domain.model.UserId
import com.calsync.domain.repository.CalendarRepository
import com.calsync.domain.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

/**
 * Scheduler for periodically syncing calendars from external sources
 */
class CalendarSyncScheduler(
    private val calendarRepository: CalendarRepository,
    private val userRepository: UserRepository,
    private val calendarSyncService: CalendarSyncService
) {
    private val logger = LoggerFactory.getLogger(CalendarSyncScheduler::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val syncJobs = mutableMapOf<String, Job>()
    
    // Default sync interval in minutes
    private var defaultSyncInterval = getConfiguredSyncInterval()
    
    /**
     * Start the scheduler
     */
    fun start() {
        logger.info("Starting calendar sync scheduler with default interval of $defaultSyncInterval minutes")
        coroutineScope.launch {
            scheduleCalendarSyncs()
        }
    }
    
    /**
     * Stop the scheduler
     */
    fun stop() {
        logger.info("Stopping calendar sync scheduler")
        coroutineScope.cancel()
        syncJobs.clear()
    }
    
    /**
     * Schedule syncs for all URL-based calendars
     */
    private suspend fun scheduleCalendarSyncs() {
        try {
            // Find all calendars that need to be synced (URL or FILE based)
            val calendars = findAllCalendars().filter { 
                it.sourceType == CalendarSourceType.URL || it.sourceType == CalendarSourceType.FILE
            }
            
            // Schedule sync for each calendar
            for (calendar in calendars) {
                scheduleSync(calendar)
            }
            
            // Schedule a periodic job to check for new calendars
            coroutineScope.launch {
                while (isActive) {
                    delay((defaultSyncInterval * 2).minutes)
                    checkForNewCalendars()
                }
            }
        } catch (e: Exception) {
            logger.error("Error initializing calendar sync scheduler", e)
        }
    }
    
    /**
     * Check for new calendars that need to be synced
     */
    private suspend fun checkForNewCalendars() {
        try {
            // Find all calendars that can be synced
            val calendars = findAllCalendars().filter { 
                it.sourceType == CalendarSourceType.URL || it.sourceType == CalendarSourceType.FILE
            }
            
            // Get IDs of currently scheduled calendars
            val scheduledCalendarIds = syncJobs.keys
            
            // Find new calendars that don't have a sync job
            val newCalendars = calendars.filter { !scheduledCalendarIds.contains(it.id.value) }
            
            // Schedule sync for each new calendar
            for (calendar in newCalendars) {
                scheduleSync(calendar)
            }
            
            // Check for removed calendars and cancel their jobs
            val currentCalendarIds = calendars.map { it.id.value }.toSet()
            val removedCalendarIds = scheduledCalendarIds.filter { !currentCalendarIds.contains(it) }
            
            for (calendarId in removedCalendarIds) {
                syncJobs[calendarId]?.cancel()
                syncJobs.remove(calendarId)
                logger.info("Removed sync job for calendar: $calendarId")
            }
        } catch (e: Exception) {
            logger.error("Error checking for new calendars", e)
        }
    }
    
    /**
     * Schedule a periodic sync for a calendar
     */
    private fun scheduleSync(calendar: Calendar) {
        val calendarId = calendar.id.value
        
        // Cancel any existing job for this calendar
        syncJobs[calendarId]?.cancel()
        
        // Create new job for periodically syncing the calendar
        val job = coroutineScope.launch {
            while (isActive) {
                try {
                    logger.info("Syncing calendar: ${calendar.name} (${calendar.id})")
                    val result = calendarSyncService.syncCalendar(calendar.id)
                    
                    if (result.isSuccess) {
                        logger.info("Successfully synced calendar: ${calendar.name} (${calendar.id})")
                    } else {
                        logger.error("Failed to sync calendar: ${calendar.name} (${calendar.id})", 
                            result.exceptionOrNull())
                    }
                } catch (e: Exception) {
                    logger.error("Error syncing calendar: ${calendar.name} (${calendar.id})", e)
                }
                
                // Wait for the next sync interval
                delay(defaultSyncInterval.minutes)
            }
        }
        
        syncJobs[calendarId] = job
        logger.info("Scheduled sync for calendar: ${calendar.name} (${calendar.id})")
    }
    
    /**
     * Get the configured sync interval from environment variables
     */
    private fun getConfiguredSyncInterval(): Long {
        val configuredInterval = System.getenv("CALENDAR_SYNC_INTERVAL_MINUTES")?.toLongOrNull()
        return configuredInterval ?: 15L // Default to 15 minutes if not configured
    }
    
    /**
     * Trigger immediate sync for a calendar
     */
    suspend fun triggerSync(calendarId: String): Result<Boolean> {
        return try {
            val calendar = findCalendarById(calendarId)
                ?: return Result.failure(IllegalArgumentException("Calendar not found: $calendarId"))
            
            if (calendar.sourceType == CalendarSourceType.MANUAL) {
                return Result.failure(IllegalArgumentException("Cannot sync manual calendars"))
            }
            
            val result = calendarSyncService.syncCalendar(calendar.id)
            
            if (result.isSuccess) {
                logger.info("Successfully triggered sync for calendar: ${calendar.name} (${calendar.id})")
                Result.success(true)
            } else {
                logger.error("Failed to trigger sync for calendar: ${calendar.name} (${calendar.id})", 
                    result.exceptionOrNull())
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to sync calendar"))
            }
        } catch (e: Exception) {
            logger.error("Error triggering sync for calendar: $calendarId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Helper method to find all calendars by fetching for all users
     */
    private suspend fun findAllCalendars(): List<Calendar> {
        val users = userRepository.findAll()
        val calendars = mutableListOf<Calendar>()
        
        for (user in users) {
            calendars.addAll(calendarRepository.findByUserId(user.id))
        }
        
        return calendars
    }
    
    /**
     * Helper method to find a calendar by ID
     */
    private suspend fun findCalendarById(calendarId: String): Calendar? {
        return calendarRepository.findById(CalendarId(calendarId))
    }
    
    /**
     * Extension function to find all users
     * Note: This is a helper method and shouldn't be necessary in a real implementation
     * where the repository would have a findAll method
     */
    private suspend fun UserRepository.findAll(): List<UserId> {
        // This is just a placeholder. In a real implementation,
        // the repository would have a proper findAll method
        return listOf()
    }
}
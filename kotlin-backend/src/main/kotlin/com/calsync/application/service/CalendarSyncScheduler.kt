package com.calsync.application.service

import com.calsync.domain.model.CalendarId
import com.calsync.domain.repository.CalendarRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Scheduler for periodic calendar synchronization
 */
class CalendarSyncScheduler(
    private val calendarRepository: CalendarRepository,
    private val calendarSyncService: CalendarSyncService,
    private val defaultSyncInterval: Duration = 15.minutes
) {
    private val logger = LoggerFactory.getLogger(CalendarSyncScheduler::class.java)
    private val scheduler = Executors.newScheduledThreadPool(1)
    private val syncJobs = ConcurrentHashMap<CalendarId, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Start the scheduler
     */
    fun start() {
        logger.info("Starting calendar sync scheduler with default interval: $defaultSyncInterval")
        
        // Schedule periodic task to sync all calendars
        scheduler.scheduleAtFixedRate(
            { scope.launch { syncAllCalendars() } },
            0,
            defaultSyncInterval.inWholeMinutes,
            TimeUnit.MINUTES
        )
    }
    
    /**
     * Stop the scheduler
     */
    fun stop() {
        logger.info("Stopping calendar sync scheduler")
        
        // Cancel all sync jobs
        syncJobs.values.forEach { it.cancel() }
        syncJobs.clear()
        
        // Shutdown the scheduler
        scheduler.shutdownNow()
    }
    
    /**
     * Trigger an immediate sync for a specific calendar
     */
    fun triggerSync(calendarId: CalendarId) {
        scope.launch {
            logger.info("Triggering manual sync for calendar: $calendarId")
            syncCalendar(calendarId)
        }
    }
    
    /**
     * Synchronize all calendars
     */
    private suspend fun syncAllCalendars() {
        logger.debug("Running scheduled sync for all calendars")
        
        try {
            val calendars = calendarRepository.findAll()
            
            calendars.forEach { calendar ->
                // Use a separate job for each calendar to isolate failures
                val job = scope.launch {
                    syncCalendar(calendar.id)
                }
                
                // Store the job reference
                syncJobs[calendar.id] = job
            }
            
            // Wait for all sync jobs to complete
            syncJobs.values.forEach { it.join() }
            
            logger.debug("Completed sync for ${calendars.size} calendars")
        } catch (e: Exception) {
            logger.error("Error synchronizing calendars", e)
        }
    }
    
    /**
     * Synchronize a single calendar
     */
    private suspend fun syncCalendar(calendarId: CalendarId) {
        try {
            logger.debug("Syncing calendar: $calendarId")
            
            calendarSyncService.syncCalendar(calendarId)
                .onSuccess { calendar ->
                    logger.debug("Successfully synced calendar: ${calendar.id} (${calendar.name})")
                }
                .onFailure { error ->
                    logger.error("Failed to sync calendar: $calendarId", error)
                }
        } catch (e: Exception) {
            logger.error("Error during calendar sync: $calendarId", e)
        }
    }
    
    /**
     * Extension function to find all calendars
     */
    private suspend fun CalendarRepository.findAll(): List<com.calsync.domain.model.Calendar> {
        // This is a helper method since the repository interface doesn't have a findAll method
        // In a real implementation, you might want to add this method to the repository interface
        return this.findByUserIds(emptyList())
    }
    
    /**
     * Extension function to find calendars by user IDs
     */
    private suspend fun CalendarRepository.findByUserIds(userIds: List<com.calsync.domain.model.UserId>): List<com.calsync.domain.model.Calendar> {
        // If no user IDs are specified, return all calendars
        if (userIds.isEmpty()) {
            // In a real implementation, you would have a proper findAll method
            // This is just a placeholder that would need to be implemented
            return emptyList()
        }
        
        // Otherwise, fetch calendars for the specified users
        val result = mutableListOf<com.calsync.domain.model.Calendar>()
        for (userId in userIds) {
            result.addAll(findByUserId(userId))
        }
        return result
    }
}
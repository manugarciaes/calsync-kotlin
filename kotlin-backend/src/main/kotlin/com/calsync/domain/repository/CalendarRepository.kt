package com.calsync.domain.repository

import com.calsync.domain.model.Calendar
import com.calsync.domain.model.CalendarId
import com.calsync.domain.model.UserId
import kotlinx.datetime.Instant

/**
 * Repository interface for Calendar entities
 */
interface CalendarRepository {
    /**
     * Find a calendar by ID
     */
    suspend fun findById(id: CalendarId): Calendar?
    
    /**
     * Find calendars by user ID
     */
    suspend fun findByUserId(userId: UserId): List<Calendar>
    
    /**
     * Save a calendar (create or update)
     */
    suspend fun save(calendar: Calendar): Calendar
    
    /**
     * Delete a calendar by ID
     */
    suspend fun delete(id: CalendarId): Boolean
    
    /**
     * Update the last synced timestamp for a calendar
     */
    suspend fun updateLastSynced(id: CalendarId, timestamp: Instant): Calendar?
}
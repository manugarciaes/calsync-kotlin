package com.calsync.domain.repository

import com.calsync.domain.model.FreeSlot
import com.calsync.domain.model.FreeSlotId
import com.calsync.domain.model.UserId

/**
 * Repository interface for FreeSlot entities
 */
interface FreeSlotRepository {
    /**
     * Find a free slot by ID
     */
    suspend fun findById(id: FreeSlotId): FreeSlot?
    
    /**
     * Find a free slot by share URL
     */
    suspend fun findByShareUrl(shareUrl: String): FreeSlot?
    
    /**
     * Find free slots by user ID
     */
    suspend fun findByUserId(userId: UserId): List<FreeSlot>
    
    /**
     * Save a free slot (create or update)
     */
    suspend fun save(freeSlot: FreeSlot): FreeSlot
    
    /**
     * Delete a free slot by ID
     */
    suspend fun delete(id: FreeSlotId): Boolean
    
    /**
     * Update the active status of a free slot
     */
    suspend fun updateActiveStatus(id: FreeSlotId, active: Boolean): Boolean
}
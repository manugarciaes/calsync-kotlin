package com.calsync.domain.repository

import com.calsync.domain.model.FreeSlot
import com.calsync.domain.model.FreeSlotId
import com.calsync.domain.model.UserId
import java.time.LocalDate

/**
 * Repository interface for FreeSlot entities
 */
interface FreeSlotRepository {
    /**
     * Get all free slots for a user
     */
    suspend fun getFreeSlotsByUserId(userId: UserId): List<FreeSlot>
    
    /**
     * Get a free slot by ID
     */
    suspend fun getFreeSlotById(id: FreeSlotId): FreeSlot?
    
    /**
     * Get a free slot by share URL
     */
    suspend fun getFreeSlotByShareUrl(shareUrl: String): FreeSlot?
    
    /**
     * Create a new free slot
     */
    suspend fun createFreeSlot(freeSlot: FreeSlot): FreeSlot
    
    /**
     * Update an existing free slot
     */
    suspend fun updateFreeSlot(freeSlot: FreeSlot): FreeSlot
    
    /**
     * Delete a free slot
     */
    suspend fun deleteFreeSlot(id: FreeSlotId): Boolean
}
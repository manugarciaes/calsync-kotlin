package com.calsync.domain.repository

import com.calsync.domain.model.User
import com.calsync.domain.model.UserId

/**
 * Repository interface for User entities
 */
interface UserRepository {
    /**
     * Find a user by ID
     */
    suspend fun findById(id: UserId): User?
    
    /**
     * Find a user by username
     */
    suspend fun findByUsername(username: String): User?
    
    /**
     * Find a user by email
     */
    suspend fun findByEmail(email: String): User?
    
    /**
     * Save a user (create or update)
     */
    suspend fun save(user: User): User
    
    /**
     * Delete a user by ID
     */
    suspend fun delete(id: UserId): Boolean
    
    /**
     * Check if a username exists
     */
    suspend fun existsByUsername(username: String): Boolean
    
    /**
     * Check if an email exists
     */
    suspend fun existsByEmail(email: String): Boolean
}
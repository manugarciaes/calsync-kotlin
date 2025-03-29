package com.calsync.domain.model

/**
 * Base entity class that all domain entities extend
 * Implements the identity concept from DDD
 */
abstract class Entity<ID : Any> {
    abstract val id: ID
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity<*>) return false
        
        return id == other.id
    }
    
    override fun hashCode(): Int = id.hashCode()
}
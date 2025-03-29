package com.calsync.domain.model

import java.util.*
import kotlinx.serialization.Serializable

/**
 * Type-safe ID wrappers to prevent accidental mixing of ID types
 */
@JvmInline
@Serializable
value class UserId(val value: UUID) : ValueObject {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class CalendarId(val value: UUID) : ValueObject {
    companion object {
        fun generate(): CalendarId = CalendarId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class EventId(val value: UUID) : ValueObject {
    companion object {
        fun generate(): EventId = EventId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class FreeSlotId(val value: UUID) : ValueObject {
    companion object {
        fun generate(): FreeSlotId = FreeSlotId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class BookingId(val value: UUID) : ValueObject {
    companion object {
        fun generate(): BookingId = BookingId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}
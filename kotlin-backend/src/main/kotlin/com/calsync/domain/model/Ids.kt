package com.calsync.domain.model

import com.calsync.infrastructure.serialization.UUIDSerializer
import java.util.*
import kotlinx.serialization.Serializable

/**
 * Type-safe ID wrappers to prevent accidental mixing of ID types
 */
@JvmInline
@Serializable
value class UserId(@Serializable(with = UUIDSerializer::class) val value: UUID) : ValueObject {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class CalendarId(@Serializable(with = UUIDSerializer::class) val value: UUID) : ValueObject {
    companion object {
        fun generate(): CalendarId = CalendarId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class EventId(@Serializable(with = UUIDSerializer::class) val value: UUID) : ValueObject {
    companion object {
        fun generate(): EventId = EventId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class FreeSlotId(@Serializable(with = UUIDSerializer::class) val value: UUID) : ValueObject {
    companion object {
        fun generate(): FreeSlotId = FreeSlotId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class BookingId(@Serializable(with = UUIDSerializer::class) val value: UUID) : ValueObject {
    companion object {
        fun generate(): BookingId = BookingId(UUID.randomUUID())
    }
    
    override fun toString(): String = value.toString()
}
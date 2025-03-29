package com.calsync.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * User ID value object
 */
@JvmInline
value class UserId(val value: String) {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID().toString())
    }
}

/**
 * User entity
 * Represents a user of the system
 */
data class User(
    val id: UserId,
    val username: String,
    val email: String,
    val password: String,
    val name: String,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
package com.calsync.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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
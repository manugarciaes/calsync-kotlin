package com.calsync.infrastructure.api.dto

import com.calsync.domain.model.User
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * DTO for user registration requests
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String? = null
)

/**
 * DTO for user login requests
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * DTO for authentication responses
 */
@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

/**
 * DTO for user responses
 */
@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String? = null,
    val createdAt: Instant
) {
    companion object {
        /**
         * Convert User domain entity to UserResponse DTO
         */
        fun fromUser(user: User): UserResponse {
            return UserResponse(
                id = user.id.toString(),
                username = user.username,
                email = user.email,
                fullName = user.fullName,
                createdAt = user.createdAt
            )
        }
    }
}
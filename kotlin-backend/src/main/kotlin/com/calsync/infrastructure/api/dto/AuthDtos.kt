package com.calsync.infrastructure.api.dto

import com.calsync.domain.model.User
import kotlinx.serialization.Serializable

/**
 * DTO for login requests
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * DTO for registration requests
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val name: String
)

/**
 * DTO for user response (without password)
 */
@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val name: String
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
                name = user.name
            )
        }
    }
}
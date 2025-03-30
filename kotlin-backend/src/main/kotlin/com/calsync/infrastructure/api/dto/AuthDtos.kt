package com.calsync.infrastructure.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO for error responses
 */
@Serializable
data class ErrorResponse(
    val error: String
)
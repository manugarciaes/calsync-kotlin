package com.calsync.infrastructure.api.dto

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/**
 * DTO for creating a booking
 */
@Serializable
data class BookingDto(
    val shareUrl: String,
    val name: String,
    val email: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val notes: String? = null
)

/**
 * DTO for updating a booking status
 */
@Serializable
data class BookingStatusUpdateDto(
    val status: String
)
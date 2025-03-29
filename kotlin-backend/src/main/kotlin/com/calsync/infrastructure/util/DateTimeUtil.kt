package com.calsync.infrastructure.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility class for date/time conversions between Kotlin and Java date/time types
 */
object DateTimeUtil {
    /**
     * Convert Kotlin LocalDate to Java LocalDate
     */
    fun KotlinLocalDate.toJavaLocalDate(): LocalDate {
        return LocalDate.of(year, monthNumber, dayOfMonth)
    }
    
    /**
     * Convert Java LocalDate to Kotlin LocalDate
     */
    fun LocalDate.toKotlinLocalDate(): KotlinLocalDate {
        return KotlinLocalDate(year, monthValue, dayOfMonth)
    }
    
    /**
     * Format a Kotlin Instant for display with timezone
     */
    fun formatDateTime(instant: Instant, timezone: String): String {
        val timeZone = TimeZone.of(timezone)
        val localDateTime = instant.toLocalDateTime(timeZone)
        val javaLocalDateTime = localDateTime.toJavaLocalDateTime()
        
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a (z)")
                .withZone(ZoneId.of(timezone))
        return formatter.format(javaLocalDateTime)
    }
}
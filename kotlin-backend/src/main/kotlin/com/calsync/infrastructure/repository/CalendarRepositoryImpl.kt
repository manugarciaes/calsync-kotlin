package com.calsync.infrastructure.repository

import com.calsync.domain.model.Calendar
import com.calsync.domain.model.CalendarId
import com.calsync.domain.model.CalendarSourceType
import com.calsync.domain.model.UserId
import com.calsync.domain.repository.CalendarRepository
import com.calsync.infrastructure.database.CalendarsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Implementation of CalendarRepository using Exposed
 */
class CalendarRepositoryImpl : CalendarRepository {
    
    override suspend fun findById(id: CalendarId): Calendar? = dbQuery {
        CalendarsTable.select { CalendarsTable.id eq UUID.fromString(id.value) }
            .singleOrNull()
            ?.toCalendar()
    }
    
    override suspend fun findByUserId(userId: UserId): List<Calendar> = dbQuery {
        CalendarsTable.select { CalendarsTable.userId eq UUID.fromString(userId.value) }
            .map { it.toCalendar() }
    }
    
    override suspend fun save(calendar: Calendar): Calendar = dbQuery {
        val calendarId = UUID.fromString(calendar.id.value)
        val userId = UUID.fromString(calendar.userId.value)
        
        // Check if calendar exists
        val exists = CalendarsTable.select { CalendarsTable.id eq calendarId }.singleOrNull() != null
        
        if (exists) {
            // Update existing calendar
            CalendarsTable.update({ CalendarsTable.id eq calendarId }) {
                it[CalendarsTable.userId] = userId
                it[name] = calendar.name
                it[description] = calendar.description
                it[color] = calendar.color
                it[sourceType] = calendar.sourceType.name
                it[sourceUrl] = calendar.sourceUrl
                it[sourceFile] = calendar.sourceFile
                it[lastSynced] = calendar.lastSynced?.toJavaInstant()
                it[updatedAt] = calendar.updatedAt.toJavaInstant()
            }
        } else {
            // Insert new calendar
            CalendarsTable.insert {
                it[id] = calendarId
                it[CalendarsTable.userId] = userId
                it[name] = calendar.name
                it[description] = calendar.description
                it[color] = calendar.color
                it[sourceType] = calendar.sourceType.name
                it[sourceUrl] = calendar.sourceUrl
                it[sourceFile] = calendar.sourceFile
                it[lastSynced] = calendar.lastSynced?.toJavaInstant()
                it[createdAt] = calendar.createdAt.toJavaInstant()
                it[updatedAt] = calendar.updatedAt.toJavaInstant()
            }
        }
        
        // Return updated calendar
        CalendarsTable.select { CalendarsTable.id eq calendarId }
            .single()
            .toCalendar()
    }
    
    override suspend fun delete(id: CalendarId): Boolean = dbQuery {
        CalendarsTable.deleteWhere { CalendarsTable.id eq UUID.fromString(id.value) } > 0
    }
    
    override suspend fun updateLastSynced(id: CalendarId, timestamp: Instant): Calendar? = dbQuery {
        val calendarId = UUID.fromString(id.value)
        
        // Update lastSynced timestamp
        CalendarsTable.update({ CalendarsTable.id eq calendarId }) {
            it[lastSynced] = timestamp.toJavaInstant()
            it[updatedAt] = java.time.Instant.now()
        }
        
        // Return updated calendar
        CalendarsTable.select { CalendarsTable.id eq calendarId }
            .singleOrNull()
            ?.toCalendar()
    }
    
    /**
     * Extension function to convert ResultRow to Calendar domain entity
     */
    private fun ResultRow.toCalendar(): Calendar {
        return Calendar(
            id = CalendarId(this[CalendarsTable.id].toString()),
            userId = UserId(this[CalendarsTable.userId].toString()),
            name = this[CalendarsTable.name],
            description = this[CalendarsTable.description],
            color = this[CalendarsTable.color],
            sourceType = CalendarSourceType.valueOf(this[CalendarsTable.sourceType]),
            sourceUrl = this[CalendarsTable.sourceUrl],
            sourceFile = this[CalendarsTable.sourceFile],
            lastSynced = this[CalendarsTable.lastSynced]?.toKotlinInstant(),
            createdAt = this[CalendarsTable.createdAt].toKotlinInstant(),
            updatedAt = this[CalendarsTable.updatedAt].toKotlinInstant()
        )
    }
    
    /**
     * Helper function to execute database operations in a transaction
     */
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
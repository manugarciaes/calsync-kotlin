package com.calsync.infrastructure.repository

import com.calsync.domain.model.*
import com.calsync.domain.repository.FreeSlotRepository
import com.calsync.infrastructure.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneId
import java.util.*

/**
 * Implementation of FreeSlotRepository using Exposed ORM
 */
class FreeSlotRepositoryImpl : FreeSlotRepository {

    /**
     * Convert database rows to a FreeSlot entity
     */
    private suspend fun resultRowToFreeSlot(row: ResultRow): FreeSlot {
        val id = FreeSlotId(row[FreeSlotsTable.id].value)
        
        // Get available days
        val availableDays = FreeSlotDaysTable
            .select { FreeSlotDaysTable.freeSlotId eq row[FreeSlotsTable.id] }
            .map { it[FreeSlotDaysTable.dayOfWeek] }
            .toSet()
        
        // Get time ranges
        val timeRanges = FreeSlotTimeRangesTable
            .select { FreeSlotTimeRangesTable.freeSlotId eq row[FreeSlotsTable.id] }
            .map { 
                TimeRange(
                    startTime = it[FreeSlotTimeRangesTable.startTime],
                    endTime = it[FreeSlotTimeRangesTable.endTime]
                )
            }
        
        // Get calendar IDs
        val calendarIds = FreeSlotCalendarsTable
            .select { FreeSlotCalendarsTable.freeSlotId eq row[FreeSlotsTable.id] }
            .map { CalendarId(it[FreeSlotCalendarsTable.calendarId].value) }
        
        return FreeSlot(
            id = id,
            userId = UserId(row[FreeSlotsTable.userId].value),
            name = row[FreeSlotsTable.name],
            description = row[FreeSlotsTable.description],
            shareUrl = row[FreeSlotsTable.shareUrl],
            slotDuration = row[FreeSlotsTable.slotDuration],
            bufferTime = row[FreeSlotsTable.bufferTime],
            timeZone = row[FreeSlotsTable.timeZone],
            availableDays = availableDays,
            timeRanges = timeRanges,
            startDate = row[FreeSlotsTable.startDate]?.toLocalDateTime()?.toLocalDate(),
            endDate = row[FreeSlotsTable.endDate]?.toLocalDateTime()?.toLocalDate(),
            maxBookingsPerDay = row[FreeSlotsTable.maxBookingsPerDay],
            calendarIds = calendarIds,
            active = row[FreeSlotsTable.active],
            createdAt = row[FreeSlotsTable.createdAt].toInstant().toKotlinInstant(),
            updatedAt = row[FreeSlotsTable.updatedAt].toInstant().toKotlinInstant()
        )
    }

    override suspend fun getFreeSlotsByUserId(userId: UserId): List<FreeSlot> = newSuspendedTransaction(Dispatchers.IO) {
        FreeSlotsTable
            .select { FreeSlotsTable.userId eq userId.value }
            .map { resultRowToFreeSlot(it) }
    }

    override suspend fun getFreeSlotById(id: FreeSlotId): FreeSlot? = newSuspendedTransaction(Dispatchers.IO) {
        FreeSlotsTable
            .select { FreeSlotsTable.id eq id.value }
            .map { resultRowToFreeSlot(it) }
            .singleOrNull()
    }

    override suspend fun getFreeSlotByShareUrl(shareUrl: String): FreeSlot? = newSuspendedTransaction(Dispatchers.IO) {
        FreeSlotsTable
            .select { FreeSlotsTable.shareUrl eq shareUrl }
            .map { resultRowToFreeSlot(it) }
            .singleOrNull()
    }

    override suspend fun createFreeSlot(freeSlot: FreeSlot): FreeSlot = newSuspendedTransaction(Dispatchers.IO) {
        // Insert free slot main data
        val id = FreeSlotsTable.insert {
            it[id] = freeSlot.id.value
            it[userId] = freeSlot.userId.value
            it[name] = freeSlot.name
            it[description] = freeSlot.description
            it[shareUrl] = freeSlot.shareUrl
            it[slotDuration] = freeSlot.slotDuration
            it[bufferTime] = freeSlot.bufferTime
            it[timeZone] = freeSlot.timeZone
            it[startDate] = freeSlot.startDate?.atStartOfDay()?.atZone(ZoneId.of(freeSlot.timeZone))?.toInstant()?.let { instant -> java.sql.Timestamp.from(instant) }
            it[endDate] = freeSlot.endDate?.atStartOfDay()?.atZone(ZoneId.of(freeSlot.timeZone))?.toInstant()?.let { instant -> java.sql.Timestamp.from(instant) }
            it[maxBookingsPerDay] = freeSlot.maxBookingsPerDay
            it[active] = freeSlot.active
            it[createdAt] = java.sql.Timestamp.from(freeSlot.createdAt.toEpochMilliseconds().let { millis -> java.time.Instant.ofEpochMilli(millis) })
            it[updatedAt] = java.sql.Timestamp.from(freeSlot.updatedAt.toEpochMilliseconds().let { millis -> java.time.Instant.ofEpochMilli(millis) })
        } get FreeSlotsTable.id
        
        // Insert available days
        freeSlot.availableDays.forEach { dayOfWeek ->
            FreeSlotDaysTable.insert {
                it[freeSlotId] = id
                it[FreeSlotDaysTable.dayOfWeek] = dayOfWeek
            }
        }
        
        // Insert time ranges
        freeSlot.timeRanges.forEach { timeRange ->
            FreeSlotTimeRangesTable.insert {
                it[id] = UUID.randomUUID()
                it[freeSlotId] = id
                it[startTime] = timeRange.startTime
                it[endTime] = timeRange.endTime
            }
        }
        
        // Insert calendar associations
        freeSlot.calendarIds.forEach { calendarId ->
            FreeSlotCalendarsTable.insert {
                it[freeSlotId] = id
                it[FreeSlotCalendarsTable.calendarId] = calendarId.value
            }
        }
        
        freeSlot
    }

    override suspend fun updateFreeSlot(freeSlot: FreeSlot): FreeSlot = newSuspendedTransaction(Dispatchers.IO) {
        // Update free slot main data
        FreeSlotsTable.update({ FreeSlotsTable.id eq freeSlot.id.value }) {
            it[name] = freeSlot.name
            it[description] = freeSlot.description
            it[shareUrl] = freeSlot.shareUrl
            it[slotDuration] = freeSlot.slotDuration
            it[bufferTime] = freeSlot.bufferTime
            it[timeZone] = freeSlot.timeZone
            it[startDate] = freeSlot.startDate?.atStartOfDay()?.atZone(ZoneId.of(freeSlot.timeZone))?.toInstant()?.let { instant -> java.sql.Timestamp.from(instant) }
            it[endDate] = freeSlot.endDate?.atStartOfDay()?.atZone(ZoneId.of(freeSlot.timeZone))?.toInstant()?.let { instant -> java.sql.Timestamp.from(instant) }
            it[maxBookingsPerDay] = freeSlot.maxBookingsPerDay
            it[active] = freeSlot.active
            it[updatedAt] = java.sql.Timestamp.from(freeSlot.updatedAt.toEpochMilliseconds().let { millis -> java.time.Instant.ofEpochMilli(millis) })
        }
        
        // Delete and re-insert available days
        FreeSlotDaysTable.deleteWhere { FreeSlotDaysTable.freeSlotId eq freeSlot.id.value }
        freeSlot.availableDays.forEach { dayOfWeek ->
            FreeSlotDaysTable.insert {
                it[freeSlotId] = freeSlot.id.value
                it[FreeSlotDaysTable.dayOfWeek] = dayOfWeek
            }
        }
        
        // Delete and re-insert time ranges
        FreeSlotTimeRangesTable.deleteWhere { FreeSlotTimeRangesTable.freeSlotId eq freeSlot.id.value }
        freeSlot.timeRanges.forEach { timeRange ->
            FreeSlotTimeRangesTable.insert {
                it[id] = UUID.randomUUID()
                it[freeSlotId] = freeSlot.id.value
                it[startTime] = timeRange.startTime
                it[endTime] = timeRange.endTime
            }
        }
        
        // Delete and re-insert calendar associations
        FreeSlotCalendarsTable.deleteWhere { FreeSlotCalendarsTable.freeSlotId eq freeSlot.id.value }
        freeSlot.calendarIds.forEach { calendarId ->
            FreeSlotCalendarsTable.insert {
                it[freeSlotId] = freeSlot.id.value
                it[FreeSlotCalendarsTable.calendarId] = calendarId.value
            }
        }
        
        freeSlot
    }

    override suspend fun deleteFreeSlot(id: FreeSlotId): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        // Delete all associated records first
        FreeSlotDaysTable.deleteWhere { FreeSlotDaysTable.freeSlotId eq id.value }
        FreeSlotTimeRangesTable.deleteWhere { FreeSlotTimeRangesTable.freeSlotId eq id.value }
        FreeSlotCalendarsTable.deleteWhere { FreeSlotCalendarsTable.freeSlotId eq id.value }
        
        // Delete the free slot
        FreeSlotsTable.deleteWhere { FreeSlotsTable.id eq id.value } > 0
    }
}
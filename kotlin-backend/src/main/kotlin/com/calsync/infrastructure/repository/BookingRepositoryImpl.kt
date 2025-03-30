package com.calsync.infrastructure.repository

import com.calsync.domain.model.*
import com.calsync.domain.repository.BookingRepository
import com.calsync.infrastructure.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.sql.Timestamp

/**
 * Implementation of BookingRepository using Exposed ORM
 */
class BookingRepositoryImpl : BookingRepository {

    /**
     * Convert database rows to a Booking entity
     */
    private fun resultRowToBooking(row: ResultRow): Booking {
        return Booking(
            id = BookingId(row[BookingsTable.id].value),
            freeSlotId = FreeSlotId(row[BookingsTable.freeSlotId].value),
            name = row[BookingsTable.name],
            email = row[BookingsTable.email],
            startTime = row[BookingsTable.startTime].toInstant().toKotlinInstant(),
            endTime = row[BookingsTable.endTime].toInstant().toKotlinInstant(),
            timeZone = row[BookingsTable.timeZone],
            notes = row[BookingsTable.notes],
            status = row[BookingsTable.status],
            cancelReason = row[BookingsTable.cancelReason],
            createdAt = row[BookingsTable.createdAt].toInstant().toKotlinInstant(),
            updatedAt = row[BookingsTable.updatedAt].toInstant().toKotlinInstant()
        )
    }

    override suspend fun getBookingsByFreeSlotId(freeSlotId: FreeSlotId): List<Booking> = newSuspendedTransaction(Dispatchers.IO) {
        BookingsTable
            .select { BookingsTable.freeSlotId eq freeSlotId.value }
            .map { resultRowToBooking(it) }
    }

    override suspend fun getBookingsByFreeSlotIdAndTimeRange(
        freeSlotId: FreeSlotId,
        startTime: Instant,
        endTime: Instant
    ): List<Booking> = newSuspendedTransaction(Dispatchers.IO) {
        val startTimestamp = Timestamp.from(java.time.Instant.ofEpochMilli(startTime.toEpochMilliseconds()))
        val endTimestamp = Timestamp.from(java.time.Instant.ofEpochMilli(endTime.toEpochMilliseconds()))
        
        BookingsTable
            .select {
                (BookingsTable.freeSlotId eq freeSlotId.value) and
                (
                    // Check for overlapping time ranges
                    ((BookingsTable.startTime lessEq endTimestamp) and (BookingsTable.endTime greaterEq startTimestamp))
                ) and
                // Only include confirmed and pending bookings
                (BookingsTable.status neq BookingStatus.CANCELLED)
            }
            .map { resultRowToBooking(it) }
    }

    override suspend fun getBookingById(id: BookingId): Booking? = newSuspendedTransaction(Dispatchers.IO) {
        BookingsTable
            .select { BookingsTable.id eq id.value }
            .map { resultRowToBooking(it) }
            .singleOrNull()
    }

    override suspend fun createBooking(booking: Booking): Booking = newSuspendedTransaction(Dispatchers.IO) {
        BookingsTable.insert {
            it[id] = booking.id.value
            it[freeSlotId] = booking.freeSlotId.value
            it[name] = booking.name
            it[email] = booking.email
            it[startTime] = Timestamp.from(java.time.Instant.ofEpochMilli(booking.startTime.toEpochMilliseconds()))
            it[endTime] = Timestamp.from(java.time.Instant.ofEpochMilli(booking.endTime.toEpochMilliseconds()))
            it[timeZone] = booking.timeZone
            it[notes] = booking.notes
            it[status] = booking.status
            it[cancelReason] = booking.cancelReason
            it[createdAt] = Timestamp.from(java.time.Instant.ofEpochMilli(booking.createdAt.toEpochMilliseconds()))
            it[updatedAt] = Timestamp.from(java.time.Instant.ofEpochMilli(booking.updatedAt.toEpochMilliseconds()))
        }
        
        booking
    }

    override suspend fun updateBooking(booking: Booking): Booking = newSuspendedTransaction(Dispatchers.IO) {
        BookingsTable.update({ BookingsTable.id eq booking.id.value }) {
            it[name] = booking.name
            it[email] = booking.email
            it[startTime] = Timestamp.from(java.time.Instant.ofEpochMilli(booking.startTime.toEpochMilliseconds()))
            it[endTime] = Timestamp.from(java.time.Instant.ofEpochMilli(booking.endTime.toEpochMilliseconds()))
            it[timeZone] = booking.timeZone
            it[notes] = booking.notes
            it[status] = booking.status
            it[cancelReason] = booking.cancelReason
            it[updatedAt] = Timestamp.from(java.time.Instant.ofEpochMilli(booking.updatedAt.toEpochMilliseconds()))
        }
        
        booking
    }

    override suspend fun updateBookingStatus(id: BookingId, status: BookingStatus, cancelReason: String?): Booking? = newSuspendedTransaction(Dispatchers.IO) {
        val booking = getBookingById(id) ?: return@newSuspendedTransaction null
        
        BookingsTable.update({ BookingsTable.id eq id.value }) {
            it[BookingsTable.status] = status
            it[BookingsTable.cancelReason] = cancelReason
            it[updatedAt] = Timestamp.from(java.time.Instant.now())
        }
        
        booking.copy(
            status = status,
            cancelReason = cancelReason,
            updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
    }

    override suspend fun deleteBooking(id: BookingId): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        BookingsTable.deleteWhere { BookingsTable.id eq id.value } > 0
    }
}
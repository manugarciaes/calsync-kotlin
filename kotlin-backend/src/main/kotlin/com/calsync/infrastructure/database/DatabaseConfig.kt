package com.calsync.infrastructure.database

import com.calsync.domain.model.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import javax.sql.DataSource
import com.google.gson.Gson

/**
 * Database configuration
 */
object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    
    /**
     * Initialize the database
     */
    fun init() {
        logger.info("Initializing database")
        
        val config = HikariConfig().apply {
            // Get configuration from environment variables
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/calsync"
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("DB_USERNAME") ?: "postgres"
            password = System.getenv("DB_PASSWORD") ?: "postgres"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        
        val dataSource = HikariDataSource(config)
        
        // Initialize database connection
        Database.connect(dataSource)
        
        // Create tables
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                CalendarsTable,
                EventsTable,
                FreeSlotsTable,
                BookingsTable
            )
        }
        
        logger.info("Database initialized successfully")
    }
    
    /**
     * Execute a database query
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

/**
 * Users table
 */
object UsersTable : Table("users") {
    val id = uuid("id").primaryKey()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/**
 * Calendars table
 */
object CalendarsTable : Table("calendars") {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").references(UsersTable.id)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val color = varchar("color", 50).nullable()
    val sourceType = varchar("source_type", 50)
    val sourceUrl = text("source_url").nullable()
    val sourceFile = text("source_file").nullable()
    val lastSynced = timestamp("last_synced").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        index(isUnique = false, userId)
    }
}

/**
 * Events table
 */
object EventsTable : Table("events") {
    val id = uuid("id").primaryKey()
    val calendarId = uuid("calendar_id").references(CalendarsTable.id, onDelete = ReferenceOption.CASCADE)
    val uid = varchar("uid", 255)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val location = varchar("location", 255).nullable()
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val timeZone = varchar("time_zone", 50)
    val isAllDay = bool("is_all_day")
    val recurrenceRule = text("recurrence_rule").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        index(isUnique = false, calendarId)
        index(isUnique = false, calendarId, startTime, endTime)
        // Create a unique constraint for calendarId + uid to prevent duplicates during sync
        uniqueIndex(calendarId, uid)
    }
}

/**
 * Free slots table
 */
object FreeSlotsTable : Table("free_slots") {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").references(UsersTable.id)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val shareUrl = varchar("share_url", 50).uniqueIndex()
    val slotDuration = integer("slot_duration")
    val bufferTime = integer("buffer_time")
    val timeZone = varchar("time_zone", 50)
    val availableDays = text("available_days") // Stored as JSON
    val timeRanges = text("time_ranges") // Stored as JSON
    val startDate = date("start_date").nullable()
    val endDate = date("end_date").nullable()
    val maxBookingsPerDay = integer("max_bookings_per_day").nullable()
    val calendarIds = text("calendar_ids") // Stored as JSON
    val active = bool("active")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        index(isUnique = false, userId)
    }
}

/**
 * Bookings table
 */
object BookingsTable : Table("bookings") {
    val id = uuid("id").primaryKey()
    val freeSlotId = uuid("free_slot_id").references(FreeSlotsTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val timeZone = varchar("time_zone", 50)
    val notes = text("notes").nullable()
    val status = varchar("status", 50)
    val cancelReason = text("cancel_reason").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        index(isUnique = false, freeSlotId)
        index(isUnique = false, freeSlotId, startTime, endTime)
    }
}

/**
 * JSON serialization/deserialization helpers
 */
private val gson = Gson()

/**
 * Extension functions for JSON serialization/deserialization
 */
fun Set<DayOfWeek>.toJson(): String = gson.toJson(this.map { it.name })

fun String.toDayOfWeekSet(): Set<DayOfWeek> = 
    gson.fromJson(this, Array<String>::class.java)
        .map { DayOfWeek.valueOf(it) }
        .toSet()

fun List<TimeRange>.toJson(): String = gson.toJson(this)

fun String.toTimeRangeList(): List<TimeRange> = 
    gson.fromJson(this, Array<TimeRange>::class.java).toList()

fun List<CalendarId>.toJson(): String = gson.toJson(this.map { it.value })

fun String.toCalendarIdList(): List<CalendarId> = 
    gson.fromJson(this, Array<String>::class.java)
        .map { CalendarId(it) }
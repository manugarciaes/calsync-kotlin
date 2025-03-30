package com.calsync.infrastructure.api

import com.calsync.application.service.FreeSlotService
import com.calsync.domain.model.FreeSlotId
import com.calsync.infrastructure.api.dto.FreeSlotRequest
import com.calsync.infrastructure.api.dto.FreeSlotResponse
import com.calsync.infrastructure.api.utils.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = LoggerFactory.getLogger("FreeSlotRoutes")

/**
 * Setup routes for free slot operations
 */
fun Route.freeSlotRoutes(freeSlotService: FreeSlotService) {
    route("/free-slots") {
        authenticate {
            // Get all free slots for the authenticated user
            get {
                try {
                    val userId = call.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val freeSlots = freeSlotService.getFreeSlotsByUserId(userId)
                    call.respond(freeSlots.map { FreeSlotResponse.fromFreeSlot(it) })
                } catch (e: Exception) {
                    logger.error("Failed to get free slots", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Get a specific free slot by ID
            get("/{id}") {
                try {
                    val userId = call.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val freeSlotId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid free slot ID"))
                    
                    val freeSlot = freeSlotService.getFreeSlotById(FreeSlotId(freeSlotId))
                    if (freeSlot != null && freeSlot.userId == userId) {
                        call.respond(FreeSlotResponse.fromFreeSlot(freeSlot))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Free slot not found"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get free slot", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Create a new free slot
            post {
                try {
                    val userId = call.userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val request = call.receive<FreeSlotRequest>()
                    
                    val freeSlot = freeSlotService.createFreeSlot(
                        userId = userId,
                        name = request.name,
                        description = request.description,
                        slotDuration = request.slotDuration,
                        bufferTime = request.bufferTime,
                        timeZone = request.timeZone,
                        availableDays = request.availableDays,
                        timeRanges = request.timeRanges,
                        startDate = request.startDate,
                        endDate = request.endDate,
                        maxBookingsPerDay = request.maxBookingsPerDay,
                        calendarIds = request.calendarIds.map { UUID.fromString(it) },
                        active = request.active
                    )
                    
                    call.respond(HttpStatusCode.Created, FreeSlotResponse.fromFreeSlot(freeSlot))
                } catch (e: Exception) {
                    logger.error("Failed to create free slot", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
            
            // Update an existing free slot
            put("/{id}") {
                try {
                    val userId = call.userId ?: return@put call.respond(HttpStatusCode.Unauthorized)
                    val freeSlotId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid free slot ID"))
                    val request = call.receive<FreeSlotRequest>()
                    
                    val freeSlot = freeSlotService.updateFreeSlot(
                        freeSlotId = FreeSlotId(freeSlotId),
                        userId = userId,
                        name = request.name,
                        description = request.description,
                        slotDuration = request.slotDuration,
                        bufferTime = request.bufferTime,
                        timeZone = request.timeZone,
                        availableDays = request.availableDays,
                        timeRanges = request.timeRanges,
                        startDate = request.startDate,
                        endDate = request.endDate,
                        maxBookingsPerDay = request.maxBookingsPerDay,
                        calendarIds = request.calendarIds.map { UUID.fromString(it) },
                        active = request.active
                    )
                    
                    if (freeSlot != null) {
                        call.respond(FreeSlotResponse.fromFreeSlot(freeSlot))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Free slot not found or not owned by user"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update free slot", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
            
            // Delete a free slot
            delete("/{id}") {
                try {
                    val userId = call.userId ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                    val freeSlotId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid free slot ID"))
                    
                    val success = freeSlotService.deleteFreeSlot(FreeSlotId(freeSlotId), userId)
                    if (success) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Free slot not found or not owned by user"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete free slot", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
        }
        
        // Public routes for shared free slots
        
        // Get a shared free slot by share URL
        get("/public/{shareUrl}") {
            try {
                val shareUrl = call.parameters["shareUrl"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Share URL is required"))
                
                val freeSlot = freeSlotService.getFreeSlotByShareUrl(shareUrl)
                if (freeSlot != null && freeSlot.active) {
                    call.respond(FreeSlotResponse.fromFreeSlot(freeSlot))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Free slot not found or not active"))
                }
            } catch (e: Exception) {
                logger.error("Failed to get shared free slot", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        // Get available time slots for a specific date
        get("/public/{shareUrl}/available-times") {
            try {
                val shareUrl = call.parameters["shareUrl"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Share URL is required"))
                val dateParam = call.parameters["date"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Date parameter is required"))
                
                val date = LocalDate.parse(dateParam)
                val freeSlot = freeSlotService.getFreeSlotByShareUrl(shareUrl)
                
                if (freeSlot != null && freeSlot.active) {
                    val availableTimes = freeSlotService.getAvailableTimesForDate(FreeSlotId(freeSlot.id.value), date)
                    call.respond(availableTimes)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Free slot not found or not active"))
                }
            } catch (e: Exception) {
                logger.error("Failed to get available times", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
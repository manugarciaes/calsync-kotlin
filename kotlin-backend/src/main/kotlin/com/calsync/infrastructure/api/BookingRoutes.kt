package com.calsync.infrastructure.api

import com.calsync.application.service.BookingService
import com.calsync.application.service.FreeSlotService
import com.calsync.domain.model.BookingId
import com.calsync.domain.model.FreeSlotId
import com.calsync.infrastructure.api.dto.BookingRequest
import com.calsync.infrastructure.api.dto.BookingResponse
import com.calsync.infrastructure.api.utils.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = LoggerFactory.getLogger("BookingRoutes")

/**
 * Setup routes for booking operations
 */
fun Route.bookingRoutes(bookingService: BookingService, freeSlotService: FreeSlotService) {
    route("/bookings") {
        authenticate {
            // Get all bookings for a free slot (owner access)
            get("/free-slot/{freeSlotId}") {
                try {
                    val userId = call.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val freeSlotId = call.parameters["freeSlotId"]?.let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid free slot ID"))
                    
                    // Verify the free slot belongs to the user
                    val freeSlot = freeSlotService.getFreeSlotById(FreeSlotId(freeSlotId))
                    if (freeSlot == null || freeSlot.userId != userId) {
                        return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Free slot not found or not owned by user"))
                    }
                    
                    val bookings = bookingService.getBookingsByFreeSlotId(FreeSlotId(freeSlotId))
                    call.respond(bookings.map { BookingResponse.fromBooking(it) })
                } catch (e: Exception) {
                    logger.error("Failed to get bookings", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Get a specific booking (owner access)
            get("/{id}") {
                try {
                    val userId = call.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val bookingId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid booking ID"))
                    
                    val booking = bookingService.getBookingById(BookingId(bookingId))
                    if (booking == null) {
                        return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Booking not found"))
                    }
                    
                    // Verify the booking's free slot belongs to the user
                    val freeSlot = freeSlotService.getFreeSlotById(booking.freeSlotId)
                    if (freeSlot == null || freeSlot.userId != userId) {
                        return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Booking not found"))
                    }
                    
                    call.respond(BookingResponse.fromBooking(booking))
                } catch (e: Exception) {
                    logger.error("Failed to get booking", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Confirm a booking (owner action)
            post("/{id}/confirm") {
                try {
                    val userId = call.userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val bookingId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid booking ID"))
                    
                    val booking = bookingService.getBookingById(BookingId(bookingId))
                    if (booking == null) {
                        return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Booking not found"))
                    }
                    
                    // Verify the booking's free slot belongs to the user
                    val freeSlot = freeSlotService.getFreeSlotById(booking.freeSlotId)
                    if (freeSlot == null || freeSlot.userId != userId) {
                        return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Booking not found"))
                    }
                    
                    val updatedBooking = bookingService.confirmBooking(BookingId(bookingId))
                    call.respond(BookingResponse.fromBooking(updatedBooking))
                } catch (e: Exception) {
                    logger.error("Failed to confirm booking", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Cancel a booking (owner action)
            post("/{id}/cancel") {
                try {
                    val userId = call.userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val bookingId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid booking ID"))
                    val request = call.receive<Map<String, String>>()
                    val reason = request["reason"]
                    
                    val booking = bookingService.getBookingById(BookingId(bookingId))
                    if (booking == null) {
                        return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Booking not found"))
                    }
                    
                    // Verify the booking's free slot belongs to the user
                    val freeSlot = freeSlotService.getFreeSlotById(booking.freeSlotId)
                    if (freeSlot == null || freeSlot.userId != userId) {
                        return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Booking not found"))
                    }
                    
                    val updatedBooking = bookingService.cancelBooking(BookingId(bookingId), reason)
                    call.respond(BookingResponse.fromBooking(updatedBooking))
                } catch (e: Exception) {
                    logger.error("Failed to cancel booking", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
        }
        
        // Public routes
        
        // Create a booking (public access)
        post("/public") {
            try {
                val request = call.receive<BookingRequest>()
                
                // Verify the free slot exists and is active
                val freeSlot = freeSlotService.getFreeSlotByShareUrl(request.freeSlotShareUrl)
                if (freeSlot == null || !freeSlot.active) {
                    return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Free slot not found or not active"))
                }
                
                val booking = bookingService.createBooking(
                    freeSlotId = freeSlot.id,
                    name = request.name,
                    email = request.email,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    timeZone = request.timeZone,
                    notes = request.notes
                )
                
                call.respond(HttpStatusCode.Created, BookingResponse.fromBooking(booking))
            } catch (e: Exception) {
                logger.error("Failed to create booking", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        // Cancel a booking (public access via token)
        post("/public/{id}/cancel") {
            try {
                val bookingId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid booking ID"))
                val token = call.request.queryParameters["token"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cancellation token is required"))
                val request = call.receive<Map<String, String>>()
                val reason = request["reason"]
                
                val booking = bookingService.getBookingById(BookingId(bookingId))
                if (booking == null) {
                    return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Booking not found"))
                }
                
                // Verify the cancellation token
                if (!bookingService.validateCancellationToken(BookingId(bookingId), token)) {
                    return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid cancellation token"))
                }
                
                val updatedBooking = bookingService.cancelBooking(BookingId(bookingId), reason)
                call.respond(BookingResponse.fromBooking(updatedBooking))
            } catch (e: Exception) {
                logger.error("Failed to cancel booking", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
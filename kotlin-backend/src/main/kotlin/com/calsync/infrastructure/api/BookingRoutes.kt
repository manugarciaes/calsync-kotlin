package com.calsync.infrastructure.api

import com.calsync.application.service.BookingService
import com.calsync.application.service.FreeSlotService
import com.calsync.domain.model.Booking
import com.calsync.infrastructure.api.dto.BookingDto
import com.calsync.infrastructure.api.dto.BookingStatusUpdateDto
import com.calsync.infrastructure.api.utils.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Routes for booking management
 */
fun Route.bookingRoutes(bookingService: BookingService, freeSlotService: FreeSlotService) {
    
    // Public routes
    route("/public/book") {
        // Book a free slot
        post {
            val bookingDto = call.receive<BookingDto>()
            
            // Validate free slot exists
            val freeSlot = freeSlotService.getFreeSlotByShareUrl(bookingDto.shareUrl)
                ?: return@post call.respond(HttpStatusCode.NotFound, "Free slot not found")
            
            // Create booking
            val booking = bookingService.createBooking(
                freeSlotId = freeSlot.id,
                name = bookingDto.name,
                email = bookingDto.email,
                date = bookingDto.date,
                startTime = bookingDto.startTime,
                endTime = bookingDto.endTime,
                notes = bookingDto.notes
            )
            
            call.respond(HttpStatusCode.Created, booking)
        }
    }
    
    // Protected routes
    authenticate {
        route("/bookings") {
            // Get all bookings for a free slot
            get("/{freeSlotId}") {
                val freeSlotId = call.parameters["freeSlotId"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid free slot ID")
                
                // Verify ownership of free slot
                val freeSlot = freeSlotService.getFreeSlot(freeSlotId)
                if (freeSlot == null || freeSlot.userId != call.userId) {
                    return@get call.respond(HttpStatusCode.NotFound, "Free slot not found")
                }
                
                val bookings = bookingService.getBookings(freeSlotId)
                call.respond(bookings)
            }
            
            // Update booking status
            put("/{id}/status") {
                val userId = call.userId
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid booking ID")
                
                val statusUpdate = call.receive<BookingStatusUpdateDto>()
                
                // Get the booking
                val booking = bookingService.getBooking(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Booking not found")
                
                // Verify ownership via free slot
                val freeSlot = freeSlotService.getFreeSlot(booking.freeSlotId)
                if (freeSlot == null || freeSlot.userId != userId) {
                    return@put call.respond(HttpStatusCode.Forbidden, "Access denied")
                }
                
                // Update status
                val updatedBooking = bookingService.updateBookingStatus(id, statusUpdate.status)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Booking not found")
                
                call.respond(updatedBooking)
            }
        }
    }
}
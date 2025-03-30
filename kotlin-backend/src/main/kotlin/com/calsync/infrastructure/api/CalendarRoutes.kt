package com.calsync.infrastructure.api

import com.calsync.application.service.CalendarSyncService
import com.calsync.domain.model.CalendarId
import com.calsync.infrastructure.api.dto.CalendarRequest
import com.calsync.infrastructure.api.dto.CalendarResponse
import com.calsync.infrastructure.api.dto.CalendarUploadResponse
import com.calsync.infrastructure.api.dto.EventResponse
import com.calsync.infrastructure.api.utils.userId
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.*

private val logger = LoggerFactory.getLogger("CalendarRoutes")

/**
 * Setup routes for calendar operations
 */
fun Route.calendarRoutes(calendarSyncService: CalendarSyncService) {
    route("/calendars") {
        authenticate {
            // Get all calendars for the authenticated user
            get {
                try {
                    val userId = call.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val calendars = calendarSyncService.getCalendarsByUserId(userId)
                    call.respond(calendars.map { CalendarResponse.fromCalendar(it) })
                } catch (e: Exception) {
                    logger.error("Failed to get calendars", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Get calendar by ID
            get("/{id}") {
                try {
                    val userId = call.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val calendarId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid calendar ID"))
                    
                    val calendar = calendarSyncService.getCalendarById(CalendarId(calendarId))
                    if (calendar != null && calendar.userId == userId) {
                        call.respond(CalendarResponse.fromCalendar(calendar))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Calendar not found"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get calendar", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Get events for a specific calendar
            get("/{id}/events") {
                try {
                    val userId = call.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val calendarId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid calendar ID"))
                    
                    val calendar = calendarSyncService.getCalendarById(CalendarId(calendarId))
                    if (calendar == null || calendar.userId != userId) {
                        return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Calendar not found"))
                    }
                    
                    val events = calendarSyncService.getEventsByCalendarId(CalendarId(calendarId))
                    call.respond(events.map { EventResponse.fromEvent(it) })
                } catch (e: Exception) {
                    logger.error("Failed to get events", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Create a new calendar
            post {
                try {
                    val userId = call.userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val request = call.receive<CalendarRequest>()
                    
                    val calendar = calendarSyncService.createCalendar(
                        userId = userId,
                        name = request.name,
                        url = request.url,
                        color = request.color,
                        description = request.description,
                        syncIntervalMinutes = request.syncIntervalMinutes
                    )
                    
                    // If URL is provided, trigger a sync
                    if (!request.url.isNullOrBlank()) {
                        calendarSyncService.syncCalendar(calendar.id)
                    }
                    
                    call.respond(HttpStatusCode.Created, CalendarResponse.fromCalendar(calendar))
                } catch (e: Exception) {
                    logger.error("Failed to create calendar", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
            
            // Update an existing calendar
            put("/{id}") {
                try {
                    val userId = call.userId ?: return@put call.respond(HttpStatusCode.Unauthorized)
                    val calendarId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid calendar ID"))
                    val request = call.receive<CalendarRequest>()
                    
                    val calendar = calendarSyncService.updateCalendar(
                        calendarId = CalendarId(calendarId),
                        userId = userId,
                        name = request.name,
                        url = request.url,
                        color = request.color,
                        description = request.description,
                        syncIntervalMinutes = request.syncIntervalMinutes
                    )
                    
                    if (calendar != null) {
                        call.respond(CalendarResponse.fromCalendar(calendar))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Calendar not found or not owned by user"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update calendar", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
            
            // Delete a calendar
            delete("/{id}") {
                try {
                    val userId = call.userId ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                    val calendarId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid calendar ID"))
                    
                    val success = calendarSyncService.deleteCalendar(CalendarId(calendarId), userId)
                    if (success) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Calendar not found or not owned by user"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete calendar", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Manually sync a calendar
            post("/{id}/sync") {
                try {
                    val userId = call.userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val calendarId = call.parameters["id"]?.let { UUID.fromString(it) } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid calendar ID"))
                    
                    val calendar = calendarSyncService.getCalendarById(CalendarId(calendarId))
                    if (calendar == null || calendar.userId != userId) {
                        return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Calendar not found"))
                    }
                    
                    if (calendar.url.isNullOrBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Calendar has no URL to sync from"))
                    }
                    
                    val eventsCount = calendarSyncService.syncCalendar(CalendarId(calendarId))
                    call.respond(mapOf("message" to "Calendar synced successfully", "eventsImported" to eventsCount))
                } catch (e: Exception) {
                    logger.error("Failed to sync calendar", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            // Upload an ICS file to create a new calendar
            post("/upload") {
                try {
                    val userId = call.userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    
                    val multipart = call.receiveMultipart()
                    var fileName = ""
                    var fileBytes: ByteArray? = null
                    var calendarName = "Imported Calendar"
                    var tempFile: File? = null
                    
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "name") {
                                    calendarName = part.value
                                }
                            }
                            is PartData.FileItem -> {
                                fileName = part.originalFileName ?: "calendar.ics"
                                val fileContent = part.streamProvider().readBytes()
                                fileBytes = fileContent
                                
                                // Also save to temp file for processing
                                tempFile = withContext(Dispatchers.IO) {
                                    Files.createTempFile("calendar", ".ics").toFile().apply {
                                        writeBytes(fileContent)
                                        deleteOnExit()
                                    }
                                }
                            }
                            else -> {}
                        }
                        part.dispose()
                    }
                    
                    if (fileBytes == null || tempFile == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file uploaded"))
                        return@post
                    }
                    
                    val icsContent = String(fileBytes!!)
                    
                    // Create the calendar
                    val calendar = calendarSyncService.createCalendar(
                        userId = userId,
                        name = calendarName,
                        description = "Imported from file: $fileName"
                    )
                    
                    // Import events from the ICS file
                    val eventsImported = calendarSyncService.importCalendarFromIcsData(
                        calendarId = calendar.id,
                        icsData = icsContent
                    )
                    
                    call.respond(
                        HttpStatusCode.Created,
                        CalendarUploadResponse(
                            calendar = CalendarResponse.fromCalendar(calendar),
                            eventsImported = eventsImported
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Failed to upload calendar", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
            
            // Create a calendar from an URL
            post("/url") {
                try {
                    val userId = call.userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val request = call.receive<CalendarRequest>()
                    
                    if (request.url.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "URL is required"))
                        return@post
                    }
                    
                    // Create the calendar with URL
                    val calendar = calendarSyncService.createCalendar(
                        userId = userId,
                        name = request.name,
                        url = request.url,
                        color = request.color,
                        description = request.description,
                        syncIntervalMinutes = request.syncIntervalMinutes
                    )
                    
                    // Sync events from the URL
                    val eventsImported = calendarSyncService.syncCalendar(calendar.id)
                    
                    call.respond(
                        HttpStatusCode.Created,
                        CalendarUploadResponse(
                            calendar = CalendarResponse.fromCalendar(calendar),
                            eventsImported = eventsImported
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Failed to create calendar from URL", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
        }
    }
}
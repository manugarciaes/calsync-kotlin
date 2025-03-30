package com.calsync.infrastructure.api.utils

import com.calsync.infrastructure.api.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ErrorHandling")

/**
 * Set up global error handling for the application
 */
fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            
            val status = when (cause) {
                is IllegalArgumentException -> HttpStatusCode.BadRequest
                is NoSuchElementException -> HttpStatusCode.NotFound
                is SecurityException -> HttpStatusCode.Unauthorized
                else -> HttpStatusCode.InternalServerError
            }
            
            call.respond(status, ErrorResponse(error = cause.message ?: "An error occurred"))
        }
        
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(error = "Resource not found"))
        }
        
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error = "Authentication required"))
        }
        
        status(HttpStatusCode.Forbidden) { call, _ ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(error = "Access denied"))
        }
    }
}
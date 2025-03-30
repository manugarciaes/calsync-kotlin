package com.calsync.infrastructure.api

import com.calsync.application.service.UserService
import com.calsync.infrastructure.api.dto.AuthResponse
import com.calsync.infrastructure.api.dto.LoginRequest
import com.calsync.infrastructure.api.dto.RegisterRequest
import com.calsync.infrastructure.api.dto.UserResponse
import com.calsync.infrastructure.api.utils.JwtConfig
import com.calsync.infrastructure.api.utils.userDomainId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("UserRoutes")

/**
 * Setup routes for user operations
 */
fun Route.userRoutes(userService: UserService) {
    // Register a new user
    post("/register") {
        try {
            val registerRequest = call.receive<RegisterRequest>()
            
            // Check if username or email already exists
            val existingUser = userService.getUserByEmail(registerRequest.email)
            if (existingUser != null) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "Email already registered")
                )
            }
            
            val existingUsername = userService.getUserByUsername(registerRequest.username)
            if (existingUsername != null) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "Username already taken")
                )
            }
            
            // Create the user
            val user = userService.registerUser(
                username = registerRequest.username,
                email = registerRequest.email,
                password = registerRequest.password,
                fullName = registerRequest.fullName
            )
            
            // Generate JWT token
            val token = JwtConfig.generateToken(user.id)
            
            // Return the authentication response
            call.respond(
                HttpStatusCode.Created,
                AuthResponse(token = token, user = UserResponse.fromUser(user))
            )
        } catch (e: Exception) {
            logger.error("Failed to register user", e)
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (e.message ?: "Unknown error during registration"))
            )
        }
    }
    
    // Login an existing user
    post("/login") {
        try {
            val loginRequest = call.receive<LoginRequest>()
            
            // Authenticate the user
            val user = userService.authenticateUser(loginRequest.email, loginRequest.password)
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Invalid email or password")
                )
            
            // Generate JWT token
            val token = JwtConfig.generateToken(user.id)
            
            // Return the authentication response
            call.respond(
                AuthResponse(token = token, user = UserResponse.fromUser(user))
            )
        } catch (e: Exception) {
            logger.error("Failed to login user", e)
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (e.message ?: "Unknown error during login"))
            )
        }
    }
    
    // Get the current authenticated user
    authenticate {
        get("/me") {
            try {
                val userId = call.userDomainId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val user = userService.getUserById(userId)
                if (user != null) {
                    call.respond(UserResponse.fromUser(user))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            } catch (e: Exception) {
                logger.error("Failed to get current user", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }
    }
}
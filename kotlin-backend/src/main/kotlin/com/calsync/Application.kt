package com.calsync

import com.calsync.application.scheduler.CalendarSyncScheduler
import com.calsync.application.service.BookingService
import com.calsync.application.service.CalendarSyncService
import com.calsync.application.service.EmailService
import com.calsync.application.service.FreeSlotService
import com.calsync.application.service.UserService
import com.calsync.domain.repository.BookingRepository
import com.calsync.domain.repository.CalendarRepository
import com.calsync.domain.repository.EventRepository
import com.calsync.domain.repository.FreeSlotRepository
import com.calsync.domain.repository.UserRepository
import com.calsync.infrastructure.api.bookingRoutes
import com.calsync.infrastructure.api.calendarRoutes
import com.calsync.infrastructure.api.freeSlotRoutes
import com.calsync.infrastructure.api.userRoutes
import com.calsync.infrastructure.api.utils.JwtConfig
import com.calsync.infrastructure.api.utils.configureErrorHandling
import com.calsync.infrastructure.database.DatabaseConfig
import com.calsync.infrastructure.repository.BookingRepositoryImpl
import com.calsync.infrastructure.repository.CalendarRepositoryImpl
import com.calsync.infrastructure.repository.EventRepositoryImpl
import com.calsync.infrastructure.repository.FreeSlotRepositoryImpl
import com.calsync.infrastructure.repository.UserRepositoryImpl
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("Application")

/**
 * Main application entry point
 */
fun main() {
    logger.info("Starting CalSync application")
    
    // Initialize database
    DatabaseConfig.init()
    
    // Create repositories
    val userRepository: UserRepository = UserRepositoryImpl()
    val calendarRepository: CalendarRepository = CalendarRepositoryImpl()
    val eventRepository: EventRepository = EventRepositoryImpl()
    val freeSlotRepository: FreeSlotRepository = FreeSlotRepositoryImpl()
    val bookingRepository: BookingRepository = BookingRepositoryImpl()
    
    // Create services
    val userService = UserService(userRepository)
    val calendarSyncService = CalendarSyncService(calendarRepository, eventRepository)
    val freeSlotService = FreeSlotService(freeSlotRepository, eventRepository)
    val bookingService = BookingService(bookingRepository, freeSlotRepository)
    val emailService = EmailService()
    
    // Start the calendar sync scheduler
    val scheduler = CalendarSyncScheduler(calendarSyncService)
    scheduler.start()

    // Start the Ktor server
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting(userService, calendarSyncService, freeSlotService, bookingService)
        configureHTTP()
        configureSerialization()
        configureAuthentication()
        configureErrorHandling()
    }.start(wait = true)
}

/**
 * Configure the application routing
 */
fun Application.configureRouting(
    userService: UserService,
    calendarSyncService: CalendarSyncService,
    freeSlotService: FreeSlotService,
    bookingService: BookingService
) {
    routing {
        route("/api") {
            userRoutes(userService)
            calendarRoutes(calendarSyncService)
            freeSlotRoutes(freeSlotService)
            bookingRoutes(bookingService, freeSlotService)
        }
    }
}

/**
 * Configure HTTP-related features
 */
fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }
}

/**
 * Configure serialization
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

/**
 * Configure JWT authentication
 */
fun Application.configureAuthentication() {
    install(Authentication) {
        jwt {
            verifier(JwtConfig.verifier)
            realm = JwtConfig.realm
            validate { credential ->
                if (credential.payload.audience.contains(JwtConfig.audience) &&
                    credential.payload.getClaim("id").asString().isNotEmpty()
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
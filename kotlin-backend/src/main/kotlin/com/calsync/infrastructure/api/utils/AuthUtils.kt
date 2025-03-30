package com.calsync.infrastructure.api.utils

import com.calsync.domain.model.UserId
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

/**
 * Extension property to get the authenticated user's ID from the JWT token
 */
val ApplicationCall.userId: UUID?
    get() {
        val principal = this.authentication.principal<JWTPrincipal>()
        val userId = principal?.payload?.getClaim("id")?.asString()
        return userId?.let { UUID.fromString(it) }
    }

/**
 * Extension property to get the authenticated user's ID as a domain UserId object
 */
val ApplicationCall.userDomainId: UserId?
    get() {
        val uuid = this.userId ?: return null
        return UserId(uuid)
    }
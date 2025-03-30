package com.calsync.infrastructure.api.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.calsync.domain.model.UserId
import java.util.*

/**
 * JWT configuration and utility functions
 */
object JwtConfig {
    private const val SECRET = "calsync-secret-key" // In production, use environment variable
    const val issuer = "calsync"
    const val audience = "calsync-users"
    const val realm = "calsync-api"
    private const val validityInMs = 36_000_00 * 24 // 24 hours
    
    private val algorithm = Algorithm.HMAC256(SECRET)
    
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
    
    /**
     * Generate a JWT token for a user
     */
    fun generateToken(userId: UserId): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("id", userId.toString())
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}
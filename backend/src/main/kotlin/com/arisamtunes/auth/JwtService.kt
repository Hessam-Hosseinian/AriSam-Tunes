package com.arisamtunes.auth

import com.arisamtunes.config.JwtConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)
    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(config.issuer).withAudience(config.audience).build()

    fun accessToken(userId: UUID): String {
        val now = Instant.now()
        return JWT.create()
            .withIssuer(config.issuer).withAudience(config.audience).withSubject(userId.toString())
            .withClaim("type", "access").withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(15, ChronoUnit.MINUTES))).sign(algorithm)
    }
}

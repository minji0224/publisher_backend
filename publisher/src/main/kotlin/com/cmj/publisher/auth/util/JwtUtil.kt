package com.cmj.publisher.auth.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.cmj.publisher.auth.AuthProfile
import java.util.*

object JwtUtil {
    var secret = "your-secret"
    val TOKEN_TIMEOUT = (1000 * 60 * 60 * 24 * 7).toLong()

    fun createToken(id: Long, publisherName: String, businessRegistrationNumber: String): String {
        val now = Date()
        val exp = Date(now.time + TOKEN_TIMEOUT)
        val algorithm = Algorithm.HMAC256(secret)

        return JWT.create().withSubject(id.toString()).withClaim("publisherName", publisherName)
                .withClaim("businessRegistrationNumber", businessRegistrationNumber).withIssuedAt(now)
                .withExpiresAt(exp).sign(algorithm)
    }

    fun validateToken(token: String) :AuthProfile? {
        val algorithm = Algorithm.HMAC256(secret)
        val verifier: JWTVerifier = JWT.require(algorithm).build()

        return try {
            val decodedJWT: DecodedJWT = verifier.verify(token)
            val id: Long = java.lang.Long.valueOf(decodedJWT.subject)
            val publisherName: String = decodedJWT.getClaim("publisherName").asString()
            val businessRegistrationNumber: String = decodedJWT.getClaim("businessRegistrationNumber").asString()

            AuthProfile(id, publisherName, businessRegistrationNumber)
        } catch (e: JWTVerificationException) {
            null
        }
    }
}
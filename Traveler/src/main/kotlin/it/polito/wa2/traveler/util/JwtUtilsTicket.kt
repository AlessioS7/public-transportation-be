package it.polito.wa2.traveler.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import it.polito.wa2.traveler.exceptions.InvalidJwtException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtUtilsTicket {
    @Value("\${traveler.jwt.key}")
    lateinit var key: String

    fun validateJwt(authToken: String): Boolean {
        try {
            val jwt = getJwtClaims(authToken)

            jwt.body.subject ?: return false
            jwt.body.issuedAt ?: return false
            jwt.body.expiration ?: return false
            jwt.body.notBefore ?: return false
            jwt.body["type"] ?: return false
            jwt.body["zid"] ?: return false
        } catch (e: Exception) {
            println(e.toString())
            return false
        }
        return true
    }

    fun getJwtTicketType(authToken: String): String {
        if (!validateJwt(authToken))
            throw InvalidJwtException()

        val jwt = getJwtClaims(authToken)

        return jwt.body["type"] as String
    }

    fun getJwtClaims(token: String): Jws<Claims> {
        return Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
    }

}

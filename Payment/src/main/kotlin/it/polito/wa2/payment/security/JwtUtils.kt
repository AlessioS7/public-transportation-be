package it.polito.wa2.payment.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import it.polito.wa2.payment.dto.UserDetailsDTO
import it.polito.wa2.payment.exceptions.InvalidJwtException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtUtils {
    @Value("\${login.jwt.key}")
    lateinit var key: String

    fun validateJwt(authToken: String): Boolean {
        try {
            val jwt = getJwtClaims(authToken)

            jwt.body.id ?: return false
            jwt.body.subject ?: return false
            jwt.body.issuedAt ?: return false
            jwt.body.expiration ?: return false
            jwt.body["roles"] ?: return false
        } catch (e: Exception) {
            println(e.toString())
            return false
        }
        return true
    }

    fun getDetailsJwt(authToken: String): UserDetailsDTO {
        if (!validateJwt(authToken))
            throw InvalidJwtException()

        val jwt = getJwtClaims(authToken)

        return UserDetailsDTO(
            username = jwt.body.subject,
            roles = jwt.body["roles"].toString()
        )
    }

    //this function is not required, introduced to avoid code repetition
    fun getJwtClaims(token: String): Jws<Claims> {
        return Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
    }

}
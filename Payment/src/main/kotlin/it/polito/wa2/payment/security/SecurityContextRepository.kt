package it.polito.wa2.payment.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.stream.Collectors
import kotlin.streams.toList

@Component
class SecurityContextRepository : ServerSecurityContextRepository {

    @Autowired
    lateinit var jwtUtils: JwtUtils

    override fun save(exchange: ServerWebExchange?, context: SecurityContext?): Mono<Void> {
        throw UnsupportedOperationException("Not supported yet")
    }

    override fun load(exchange: ServerWebExchange?): Mono<SecurityContext> {
        if (exchange!!.request.path.toString() == "/tickets")
            return guestContext()

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: return guestContext()

        if (!authHeader.startsWith("Bearer "))
            return guestContext()

        val authToken = authHeader.substring(7)

        if (!jwtUtils.validateJwt(authToken))
            return guestContext()

        val user = jwtUtils.getDetailsJwt(authToken)
        val roles = listOf(user.roles)

        val authentication = UsernamePasswordAuthenticationToken(
            user.username,
            authHeader,
            roles.stream().map { SimpleGrantedAuthority(it) }.collect(Collectors.toList())
        )

        return Mono.just(SecurityContextImpl(authentication))
    }
}

fun guestContext(): Mono<SecurityContext> {
    val roles = listOf("GUEST").map { SimpleGrantedAuthority(it) }.toList()
    val guest = AnonymousAuthenticationToken("guest-user", "guest", roles)
    return Mono.just(SecurityContextImpl(guest))
}
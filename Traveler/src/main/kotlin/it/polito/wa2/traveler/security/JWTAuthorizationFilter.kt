package it.polito.wa2.traveler.security

import io.jsonwebtoken.Jwts
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import it.polito.wa2.traveler.config.SecurityProperties
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(
    authManager: AuthenticationManager,
    private val securityProperties: SecurityProperties,
    val jwtUtils: JwtUtils
) : BasicAuthenticationFilter(authManager) {

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {
        val header = req.getHeader(securityProperties.headerString)
        if (header == null || !header.startsWith(securityProperties.tokenPrefix)) {
            chain.doFilter(req, res)
            return
        }

        val token = header.replace(securityProperties.tokenPrefix, "")

        if(!jwtUtils.validateJwt(token)) {
            chain.doFilter(req, res)
            return
        }

        getAuthentication(token)?.also {
            SecurityContextHolder.getContext().authentication = it
        }
        chain.doFilter(req, res)
    }

    private fun getAuthentication(token: String): UsernamePasswordAuthenticationToken? {
        return try {
            val userDetails = jwtUtils.getDetailsJwt(token)

            val authorities = ArrayList<GrantedAuthority>()
            authorities.add(SimpleGrantedAuthority(userDetails.roles))
            UsernamePasswordAuthenticationToken(userDetails.username, null, authorities)
        } catch (e: Exception) {
            println(e.toString())
            return null
        }
    }
}
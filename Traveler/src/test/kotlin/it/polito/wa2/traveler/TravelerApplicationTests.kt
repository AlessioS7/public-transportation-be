package it.polito.wa2.traveler

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.traveler.exceptions.InvalidJwtException
import it.polito.wa2.traveler.exceptions.UserNotFoundException
import it.polito.wa2.traveler.security.JwtUtils
import it.polito.wa2.traveler.services.TravelerServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

@SpringBootTest
class TravelerApplicationTests {

    @Autowired
    lateinit var jwtUtils: JwtUtils

    @Autowired
    lateinit var travelerServ: TravelerServiceImpl

    @Value("\${login.jwt.key}")
    lateinit var key: String

    @Value("\${login.jwt.expiration}")
    lateinit var expiration: String

    @Test
    fun testValidJwt() {
        val token = Jwts.builder()
            .setId("1")
            .setSubject("pippo")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration.toLong()))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key)))
            .compact()

        Assertions.assertTrue(jwtUtils.validateJwt(token))
    }

    @Test
    fun testValidUserDetailsDTO() {
        val token = Jwts.builder()
            .setId("1")
            .setSubject("pippo")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration.toLong()))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key)))
            .compact()

        val userDetailsDTO = jwtUtils.getDetailsJwt(token)

        Assertions.assertEquals("pippo", userDetailsDTO.username)
        Assertions.assertEquals("CUSTOMER", userDetailsDTO.roles)
    }

    @Test
    fun testGetMyProfile() {

        val token = Jwts.builder()
            .setId("1")
            .setSubject("pippo")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration.toLong()))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key)))
            .compact()
        val userDetails = jwtUtils.getDetailsJwt(token)

        val authorities = ArrayList<GrantedAuthority>()
        authorities.add(SimpleGrantedAuthority(userDetails.roles))
        val userAuth = UsernamePasswordAuthenticationToken(userDetails.username, null, authorities)

        userAuth?.also {
            SecurityContextHolder.getContext().authentication = it
        }

        Assertions.assertThrows(UserNotFoundException::class.java) { travelerServ.getMyProfile() }
    }

    @Test
    fun testUpdateMyProfile() {

        val token = Jwts.builder()
            //.setId("1")
            .setSubject("pippo")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration.toLong()))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key)))
            .compact()

        Assertions.assertThrows(InvalidJwtException::class.java) { travelerServ.updateMyProfile(token, UserDetailsRequest("", "", "", "")) }
    }

    @Test
    fun testGetMyTickets() {

        val token = Jwts.builder()
            .setId("1")
            .setSubject("pippo")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration.toLong()))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key)))
            .compact()
        val userDetails = jwtUtils.getDetailsJwt(token)

        val authorities = ArrayList<GrantedAuthority>()
        authorities.add(SimpleGrantedAuthority(userDetails.roles))
        val userAuth = UsernamePasswordAuthenticationToken(userDetails.username, null, authorities)

        userAuth?.also {
            SecurityContextHolder.getContext().authentication = it
        }

        Assertions.assertThrows(UserNotFoundException::class.java) { travelerServ.getTickets() }
    }

    @Test
    fun testBuyTickets() {

        val token = Jwts.builder()
            .setId("1")
            .setSubject("pippo")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration.toLong()))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key)))
            .compact()
        val userDetails = jwtUtils.getDetailsJwt(token)

        val authorities = ArrayList<GrantedAuthority>()
        authorities.add(SimpleGrantedAuthority(userDetails.roles))
        val userAuth = UsernamePasswordAuthenticationToken(userDetails.username, null, authorities)

        userAuth?.also {
            SecurityContextHolder.getContext().authentication = it
        }

        Assertions.assertThrows(UserNotFoundException::class.java) { travelerServ.buyTickets(TicketRequest("", "", "", emptyArray(), 3600, "")) }
    }

    @Test
    fun testAdminGetTravelers() {
        Assertions.assertThrows(UserNotFoundException::class.java) { travelerServ.getTravelerDetails(0) }
    }

    @Test
    fun testAdminGetTravelerTickets() {
        Assertions.assertThrows(UserNotFoundException::class.java) { travelerServ.getTravelerTickets(0) }
    }
}

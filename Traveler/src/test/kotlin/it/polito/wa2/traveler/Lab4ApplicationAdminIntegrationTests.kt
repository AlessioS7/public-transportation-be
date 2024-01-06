package it.polito.wa2.traveler

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.traveler.services.TravelerServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class Lab4ApplicationAdminIntegrationTests {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:latest")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }

    @LocalServerPort
    protected var port: Int = 0

    var baseUrl = ""

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var travelerServ: TravelerServiceImpl

    val headerName = "Authorization"

    val tokenPrefix = "Bearer "

    val expiration = 3600000

    val loginKey = "LZ0zXIt18phbMS3H96tVnTLan31T2FdjkwkHSY+6mY4"

    val ticketKey = "LZ0zXIt18phbMS3H96tVnTLan31T2FdjkwkHSY+6mY4"

    val httpHeaders = HttpHeaders()


    @BeforeEach
    fun setUp() {
        baseUrl = "http://localhost:$port"

        travelerServ.ticketRepository.deleteAll()
        travelerServ.userDetailsRepository.deleteAll()

        for(i in 1..3) {
            val jwt = Jwts.builder()
                .setId("$i")
                .setSubject("somename$i")
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() + expiration))
                .addClaims(mapOf("roles" to "CUSTOMER"))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(loginKey)))
                .compact()

            httpHeaders.set(headerName, tokenPrefix + jwt)

            val request1 = HttpEntity(
                UserDetailsRequest(
                    "somename$i",
                    "123 Main Street,New York, NY 10030",
                    "01-01-2000",
                    "123456789"
                ),
                httpHeaders
            )
            restTemplate.exchange<Unit>(
                "$baseUrl/my/profile", HttpMethod.PUT, request1
            )

            val request2 = HttpEntity(
                TicketRequest(
                    "buy_tickets",
                    "3",
                    "A,B,C",
                    validdays = emptyList(),
                    duration = 3600,
                    type = "Ordinal"
                ),
                httpHeaders
            )

            restTemplate.exchange<List<TicketResponse?>>(
                "$baseUrl/my/tickets", HttpMethod.POST, request2
            )
        }

        val jwt = Jwts.builder()
            .setId("4")
            .setSubject("admin")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "ADMIN"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(loginKey)))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)
    }

    @Test
    fun acceptProperGetTravelersRequest() {
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<List<String>>(
            "$baseUrl/admin/travelers", HttpMethod.GET, request
        )

        println(response.body?.size)
        println(response.statusCode)
        assert(response.statusCode == HttpStatus.OK && response.body?.size == 3)
    }


    @Test
    fun acceptProperGetTravelerDetailsRequest() {
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<UserDetailsResponse>(
            "$baseUrl/admin/traveler/1/profile", HttpMethod.GET, request
        )

        println(response.statusCode)
        println(response.body)

        assert(
            response.statusCode == HttpStatus.OK &&
            response.body?.name == "somename1" &&
            response.body?.address == "123 Main Street,New York, NY 10030" &&
            response.body?.dateOfBirth == "01-01-2000" &&
            response.body?.telephoneNumber == "123456789"
        )
    }

    @Test
    fun acceptProperGetTravelerTicketsRequest() {
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<List<TicketResponse>>(
            "$baseUrl/admin/traveler/1/tickets", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.OK && response.body?.size == 3)
    }

    @Test
    fun rejectWrongRolesGetTravelersRequest() {
        val jwt = Jwts.builder()
            .setId("4")
            .setSubject("admin")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(loginKey)))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<Any>(
            "$baseUrl/admin/travelers", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.FORBIDDEN)
    }

    @Test
    fun rejectWrongRolesGetTravelerDetailsRequest() {
        val jwt = Jwts.builder()
            .setId("4")
            .setSubject("admin")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(loginKey)))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<Any>(
            "$baseUrl/admin/traveler/1/profile", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.FORBIDDEN)
    }

    @Test
    fun rejectWrongRolesGetTravelerTicketsRequest() {
        val jwt = Jwts.builder()
            .setId("4")
            .setSubject("admin")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(loginKey)))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<Any>(
            "$baseUrl/admin/traveler/1/tickets", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.FORBIDDEN)
    }

    @Test
    fun rejectGetTravelerDetailsRequest() {

        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<Any>(
            "$baseUrl/admin/traveler/0/profile", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun rejectGetTravelerTicketsRequest() {
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<Any>(
            "$baseUrl/admin/traveler/5/tickets", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.NOT_FOUND)
    }
}
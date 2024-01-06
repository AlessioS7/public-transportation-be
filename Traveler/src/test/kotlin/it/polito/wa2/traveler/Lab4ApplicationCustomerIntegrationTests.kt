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
class Lab4ApplicationCustomerIntegrationTests {
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

        val jwt = Jwts.builder()
            .setId("3")
            .setSubject("somename")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(loginKey)))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)

        val request = HttpEntity(
            UserDetailsRequest(
                "somename",
                "123 Main Street,New York, NY 10030",
                "01-01-2000",
                "123456789"
            ),
            httpHeaders
        )
        val response = restTemplate.exchange<Unit>(
            "$baseUrl/my/profile", HttpMethod.PUT, request
        )
    }

    @Test
    fun acceptProperUpdateMyProfileRequest() {
        val request = HttpEntity(
            UserDetailsRequest(
                "somename2",
                "123 Main Street,New York, NY 10030",
                "01-01-2000",
                "123456789"
            ),
            httpHeaders
        )
        val response = restTemplate.exchange<Unit>(
            "$baseUrl/my/profile", HttpMethod.PUT, request
        )

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun acceptProperGetMyProfileRequest() {
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<UserDetailsResponse>(
            "$baseUrl/my/profile", HttpMethod.GET, request
        )

        assert(
            response.statusCode == HttpStatus.OK &&
            response.body?.name == "somename" &&
            response.body?.address == "123 Main Street,New York, NY 10030" &&
            response.body?.dateOfBirth == "01-01-2000" &&
            response.body?.telephoneNumber == "123456789"
        )
    }

    @Test
    fun acceptProperGetTicketsRequest() {
        val request = HttpEntity<Unit>(httpHeaders)
        val response = restTemplate.exchange<List<TicketResponse?>>(
            "$baseUrl/my/tickets", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.OK && response.body?.isEmpty() == true)
    }

    @Test
    fun acceptProperBuyTicketsRequest() {
        val request1 = HttpEntity(
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
        val response1 = restTemplate.exchange<List<TicketResponse?>>(
            "$baseUrl/my/tickets", HttpMethod.POST, request1
        )

        val request2 = HttpEntity<Unit>(httpHeaders)
        val response2 = restTemplate.exchange<List<TicketResponse?>>(
            "$baseUrl/my/tickets", HttpMethod.GET, request2
        )

        assert(
            response1.statusCode == HttpStatus.OK &&
            response2.statusCode == HttpStatus.OK &&
            response2.body?.size == 3
        )
    }

    @Test
    fun rejectTokenUpdateMyProfileRequest() {
        val jwt = Jwts.builder()
            .setId("3")
            .setSubject("somename")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("LZ0zXIt18phbMS3H96yVnTLan31T2FdjkwkHSY+6mY4")))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)
        val request = HttpEntity(
            UserDetailsRequest(
                "somename2",
                "123 Main Street,New York, NY 10030",
                "01-01-2000",
                "123456789"
            ),
            httpHeaders
        )
        val response = restTemplate.exchange<Unit>(
            "$baseUrl/my/profile", HttpMethod.PUT, request
        )

        assert(response.statusCode == HttpStatus.FORBIDDEN)
    }

    @Test
    fun rejectGetMyProfileRequest() {
        val jwt = Jwts.builder()
            .setId("3")
            .setSubject("not_somename")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("LZ0zXIt18phbMS3H96tVnTLan31T2FdjkwkHSY+6mY4")))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)
        val request = HttpEntity(
            UserDetailsRequest(
                "somename2",
                "123 Main Street,New York, NY 10030",
                "01-01-2000",
                "123456789"
            ),
            httpHeaders
        )
        val response = restTemplate.exchange<Unit>(
            "$baseUrl/my/profile", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun rejectGetMyTicketsRequest() {
        val jwt = Jwts.builder()
            .setId("3")
            .setSubject("not_somename")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("LZ0zXIt18phbMS3H96tVnTLan31T2FdjkwkHSY+6mY4")))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)
        val request = HttpEntity(
            UserDetailsRequest(
                "somename2",
                "123 Main Street,New York, NY 10030",
                "01-01-2000",
                "123456789"
            ),
            httpHeaders
        )
        val response = restTemplate.exchange<Unit>(
            "$baseUrl/my/tickets", HttpMethod.GET, request
        )

        assert(response.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun rejectPostTicketsRequest() {
        val jwt = Jwts.builder()
            .setId("3")
            .setSubject("not_somename")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .addClaims(mapOf("roles" to "CUSTOMER"))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("LZ0zXIt18phbMS3H96tVnTLan31T2FdjkwkHSY+6mY4")))
            .compact()

        httpHeaders.set(headerName, tokenPrefix + jwt)

        val request = HttpEntity(
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
        val response = restTemplate.exchange<List<TicketResponse?>>(
            "$baseUrl/my/tickets", HttpMethod.POST, request
        )

        assert(response.statusCode == HttpStatus.NOT_FOUND)
    }
}
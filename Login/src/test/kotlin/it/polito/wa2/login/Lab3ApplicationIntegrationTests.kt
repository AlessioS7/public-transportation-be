package it.polito.wa2.login

import it.polito.wa2.login.controllers.RegistrationResponse
import it.polito.wa2.login.dtos.ActivationDTO
import it.polito.wa2.login.dtos.UserDTO
import it.polito.wa2.login.services.LoginServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.lang.Thread.sleep

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class Lab3ApplicationIntegrationTests {
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

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var loginServ: LoginServiceImpl

    @BeforeEach
    fun setUp() {
        loginServ.activationRep.deleteAll()
        loginServ.userRep.deleteAll()
        sleep(1000) // needed to avoid sending too many requests in little time
    }

    @Test
    fun acceptProperRequest() {
        val baseUrl = "http://localhost:$port"
        val request1 = HttpEntity(UserDTO("somename", "Secret!Password1", "me@email.com"))
        val response1 = restTemplate.postForEntity<RegistrationResponse>(
            "$baseUrl/user/register", request1
        )

        val id = response1.body?.provisional_id
        val code = loginServ.activationRep.findById(id!!).get().code
        val request2 = HttpEntity(ActivationDTO(id, code))
        val response2 = restTemplate.postForEntity<String>(
            "$baseUrl/user/validate", request2
        )

        assert(response1.statusCode == HttpStatus.ACCEPTED && response2.statusCode == HttpStatus.CREATED)
    }

    @Test
    fun rejectEmptyUsername() {
        val baseUrl = "http://localhost:$port"
        val request = HttpEntity(UserDTO("", "Secret!Password1", "me@email.com"))
        val response = restTemplate.postForEntity<Unit>(
            "$baseUrl/user/register", request
        )

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun rejectEmptyPassword() {
        val baseUrl = "http://localhost:$port"
        val request1 = HttpEntity(UserDTO("somename", "", "me@email.com"))
        val response1 = restTemplate.postForEntity<Unit>(
            "$baseUrl/user/register", request1
        )

        assert(response1.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun rejectEmptyEmail() {
        val baseUrl = "http://localhost:$port"
        val request1 = HttpEntity(UserDTO("somename", "Secret!Password1", ""))
        val response1 = restTemplate.postForEntity<Unit>(
            "$baseUrl/user/register", request1
        )

        assert(response1.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun rejectWeakPassword() {
        val baseUrl = "http://localhost:$port"
        val request = HttpEntity(UserDTO("somename", "Secreword1", "me@email.com"))
        val response = restTemplate.postForEntity<Unit>(
            "$baseUrl/user/register", request
        )

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun rejectInvalidEmail() {
        val baseUrl = "http://localhost:$port"
        val request1 = HttpEntity(UserDTO("somename", "Secret!Password1", "meemailom"))
        val response1 = restTemplate.postForEntity<Unit>(
            "$baseUrl/user/register", request1
        )

        assert(response1.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun rejectNullActivationId() {
        val baseUrl = "http://localhost:$port"
        val request1 = HttpEntity(UserDTO("somename", "Secret!Password1", "me@email.com"))
        val response1 = restTemplate.postForEntity<RegistrationResponse>(
            "$baseUrl/user/register", request1
        )

        val id = response1.body?.provisional_id
        val code = loginServ.activationRep.findById(id!!).get().code
        val request2 = HttpEntity(ActivationDTO(null, code))
        val response2 = restTemplate.postForEntity<String>(
            "$baseUrl/user/validate", request2
        )

        assert(response2.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun rejectEmptyActivationCode() {
        val baseUrl = "http://localhost:$port"
        val request1 = HttpEntity(UserDTO("somename", "Secret!Password1", "me@email.com"))
        val response1 = restTemplate.postForEntity<RegistrationResponse>(
            "$baseUrl/user/register", request1
        )

        val id = response1.body?.provisional_id
        val request2 = HttpEntity(ActivationDTO(id, ""))
        val response2 = restTemplate.postForEntity<String>(
            "$baseUrl/user/validate", request2
        )

        assert(response2.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun rateLimitReached() {
        val baseUrl = "http://localhost:$port"
        val request = HttpEntity(UserDTO("", "", ""))

        for (i in 1..10) {
            restTemplate.postForEntity<RegistrationResponse>("$baseUrl/user/register", request)
        }

        val response = restTemplate.postForEntity<RegistrationResponse>(
            "$baseUrl/user/register", request
        )

        assert(response.statusCode == HttpStatus.TOO_MANY_REQUESTS)
    }
}
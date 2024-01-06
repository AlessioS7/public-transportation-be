package it.polito.wa2.login

import it.polito.wa2.login.dtos.ActivationDTO
import it.polito.wa2.login.dtos.UserDTO
import it.polito.wa2.login.dtos.toActivationDTO
import it.polito.wa2.login.dtos.toUserDTO
import it.polito.wa2.login.entities.Activation
import it.polito.wa2.login.entities.User
import it.polito.wa2.login.exceptions.DuplicatedUserException
import it.polito.wa2.login.exceptions.InvalidActivationCodeException
import it.polito.wa2.login.services.LoginServiceImpl
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.lang.Thread.sleep
import java.util.*

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class Lab3ApplicationUnitTests {
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

    @Autowired
    lateinit var loginServ: LoginServiceImpl

    @Test
    fun toUserDTOTest() {
        val user = User().apply {
            id = 1
            username = "un"
            password = "ps"
            email = "e"
            active = false
        }

        val userDTO = UserDTO(1, "un", "ps", "e", false)

        Assertions.assertTrue(user.toUserDTO() == userDTO)
    }

    @BeforeEach
    fun setUp() {
        loginServ.activationRep.deleteAll()
        loginServ.userRep.deleteAll()
    }

    @Test
    fun toActivationDTOTest() {
        val user = User().apply {
            id = 1
            username = "un"
            password = "ps"
            email = "e"
            active = false
        }

        val date = Date()
        val uuid = UUID(1L, 1L)

        val activation = Activation().apply {
            id = uuid
            code = "c"
            deadline = date
            this.user = user
        }

        val activationDTO = ActivationDTO(uuid, "c", 5, date, user)

        Assertions.assertTrue(activation.toActivationDTO() == activationDTO)
    }

    @Test
    fun registerTestSuccess() {
        val user = UserDTO("un", "ps", "me@email.com")
        val id = loginServ.register(user).provisional_id
        val activation = loginServ.activationRep.findById(id)

        Assertions.assertTrue(id == activation.get().id)
    }

    @Test
    fun registerTestFailure() {
        val user = UserDTO("un", "ps", "me@email.com")
        loginServ.register(user)

        Assertions.assertThrows(DuplicatedUserException::class.java) { loginServ.register(user) }
    }

    @Test
    fun validateTestSuccess() {
        assertDoesNotThrow {
            val user = UserDTO("un", "ps", "me@email.com")
            val id = loginServ.register(user).provisional_id
            val u = loginServ.activationRep.findById(id).get()
            loginServ.validate(ActivationDTO(id, u.code))
        }
    }

    @Test
    fun validateTestFailure() {
        val user = UserDTO("un", "ps", "me@email.com")
        val id = loginServ.register(user).provisional_id
        Assertions.assertThrows(InvalidActivationCodeException::class.java) {
            loginServ.validate(ActivationDTO(id, "111"))
        }
    }

    @Test
    fun validateCounterReaches0() {
        val user = UserDTO("un", "ps", "me@email.com")
        val id = loginServ.register(user).provisional_id
        val a = loginServ.activationRep.findById(id).get()

        a.counter = 1
        loginServ.activationRep.save(a)

        try {
            loginServ.validate(ActivationDTO(id, "111"))
        } catch (e: InvalidActivationCodeException) {
        }

        val a2 = loginServ.activationRep.findById(id)
        val u = loginServ.userRep.findById(a.user.id!!)

        Assertions.assertFalse(a2.isPresent || u.isPresent)
    }

    @Disabled // it takes too long
    @Test
    fun testScheduler() {
        loginServ.register(UserDTO("un", "ps", "me@email.com"))
        loginServ.register(UserDTO("un2", "ps2", "me2@email.com"))
        sleep(60*60*1000)
        Assertions.assertTrue(loginServ.activationRep.count() == 0L && loginServ.userRep.count() == 0L)
    }
}

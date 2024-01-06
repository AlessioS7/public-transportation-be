package it.polito.wa2.login.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.login.controllers.*
import it.polito.wa2.login.dtos.ActivationDTO
import it.polito.wa2.login.dtos.UserDTO
import it.polito.wa2.login.entities.Activation
import it.polito.wa2.login.entities.Role
import it.polito.wa2.login.entities.User
import it.polito.wa2.login.exceptions.*
import it.polito.wa2.login.passwordEncoder
import it.polito.wa2.login.repositories.ActivationRepository
import it.polito.wa2.login.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

const val ONE_HOUR: Long = 60*60*1000

@Service
@Transactional
class LoginServiceImpl : LoginService {
    @Autowired
    lateinit var userRep: UserRepository

    @Autowired
    lateinit var activationRep: ActivationRepository

    @Autowired
    lateinit var emailSer: EmailServiceImpl

    @Value("\${login.jwt.key}")
    lateinit var key: String

    @Value("\${login.jwt.expiration}")
    lateinit var expirationUser: String

    @Value("\${login.jwt.expiration.embedded}")
    lateinit var expirationEmbedded: String

    override fun register(user: UserDTO): RegistrationResponse {
        if (userRep.findByUsernameOrEmail(user.nickname, user.email).isNotEmpty()) {
            throw DuplicatedUserException()
        }

        val u = userRep.save(User().apply {
            username = user.nickname
            email = user.email
            password = passwordEncoder().encode(user.password)
        })

        val number = Random().nextInt(999999)

        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.HOUR, 1)

        val a = activationRep.save(Activation().apply {
            code = String.format("%06d", number)
            deadline = calendar.time
            this.user = u
        })

        emailSer.sendCode(a.code, a.deadline, u.email)

        return RegistrationResponse(a.id!!, u.email)
    }

    override fun validate(activation: ActivationDTO): ValidationResponse {
        val queryRes = activationRep.findById(activation.provisional_id!!)

        if (!queryRes.isPresent) {
            throw ActivationNotFoundException()
        }

        val a = queryRes.get()

        if (a.deadline.before(Date())) {
            activationRep.deleteById(activation.provisional_id)
            userRep.deleteById(a.user.id!!)

            throw ExpiredValidationException()
        }

        if (a.code != activation.activation_code) {
            a.counter --

            if (a.counter == 0) {
                activationRep.deleteById(activation.provisional_id)
                userRep.deleteById(a.user.id!!)
            } else {
                activationRep.save(a)
            }

            throw InvalidActivationCodeException()
        }

        val u = a.user
        u.active = true
        userRep.save(u)

        activationRep.deleteById(activation.provisional_id)

        return ValidationResponse(u.id!!, u.username, u.email)
    }

    override fun login(loginRequest: LoginRequest): String {
        val u = userRep.findFirstByUsername(loginRequest.nickname)
        if(!u.isPresent)
            throw InvalidCredentialException()

        if(!u.get().active)
            throw InvalidCredentialException()

        if(!passwordEncoder().matches(loginRequest.password, u.get().password))
            throw InvalidCredentialException()

        val expiration = if(u.get().role == Role.EMBEDDED) expirationEmbedded else expirationUser

        return Jwts.builder()
            .setId(u.get().id.toString())
            .setSubject(loginRequest.nickname)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration.toLong()))
            .addClaims(mapOf("roles" to u.get().role))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key)))
            .compact()
    }

    override fun enroll(enrollRequest: EnrollRequest) {
        if(userRep.findFirstByUsername(enrollRequest.nickname).isPresent)
            throw DuplicatedUserException()

        userRep.save(User().apply {
            id = null
            username = enrollRequest.nickname
            password = passwordEncoder().encode(enrollRequest.password)
            email = ""
            active = true
            role = if(enrollRequest.enroller) Role.ENROLLER else Role.ADMIN
        })
    }

    override fun newEmbeddedSystem(embeddedSystemRequest: EmbeddedSystemRequest) {
        if(userRep.findFirstByUsername(embeddedSystemRequest.embeddedId).isPresent)
            throw DuplicatedUserException()

        userRep.save(User().apply {
            id = null
            username = embeddedSystemRequest.embeddedId
            password = passwordEncoder().encode(embeddedSystemRequest.password)
            email = ""
            active = true
            role = Role.EMBEDDED
        })
    }


    @Scheduled(initialDelay = ONE_HOUR + (60 * 1000), fixedRate = ONE_HOUR)
    @Async
    fun pruneExpiredRegistrations() {
        activationRep.pruneExpiredActivations()
        userRep.pruneExpiredRegistrations()
    }
}

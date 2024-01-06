package it.polito.wa2.login.controllers

import it.polito.wa2.login.dtos.ActivationDTO
import it.polito.wa2.login.dtos.UserDTO
import it.polito.wa2.login.exceptions.ActivationException
import it.polito.wa2.login.exceptions.InvalidCredentialException
import it.polito.wa2.login.exceptions.LoginException
import it.polito.wa2.login.exceptions.RegistrationException
import it.polito.wa2.login.services.LoginServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@RestController
class UserController {
    @Autowired
    lateinit var lsi: LoginServiceImpl

    @PostMapping("/user/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun registerUser(@RequestBody @Valid u: UserDTO?, br: BindingResult): RegistrationResponse {
        if (br.hasErrors()) {
            throw RegistrationException()
        }

        return lsi.register(u!!)
    }

    @PostMapping("/user/validate")
    @ResponseStatus(HttpStatus.CREATED)
    fun validateUser(@RequestBody @Valid a: ActivationDTO?, br: BindingResult): ValidationResponse {
        if (br.hasErrors()) {
            throw ActivationException()
        }

        return lsi.validate(a!!)
    }

    @PostMapping("/user/login")
    @ResponseStatus(HttpStatus.OK)
    fun signIn(@RequestBody @Valid l: LoginRequest?, br: BindingResult): String{
        if (br.hasErrors()) {
            throw InvalidCredentialException()
        }

        return lsi.login(l!!)
    }

    @PostMapping("/admin/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ENROLLER')")
    fun enroll(@RequestBody @Valid er: EnrollRequest, br: BindingResult){
        if (br.hasErrors()) {
            throw RegistrationException()
        }

        lsi.enroll(er)
    }

    @PostMapping("/admin/embedded")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ENROLLER')")
    fun addEmbeddedSystem(@RequestBody @Valid er: EmbeddedSystemRequest, br: BindingResult){
        if (br.hasErrors()) {
            throw RegistrationException()
        }

        lsi.newEmbeddedSystem(er)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RegistrationException::class)
    fun invalidUserData(e: RegistrationException) { }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ActivationException::class)
    fun invalidActivationData(e: ActivationException) { }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(LoginException::class)
    fun wrongCredentials(e: LoginException) { }
}

data class RegistrationResponse(val provisional_id: UUID, val email: String)
data class ValidationResponse(val userId: Long, val nickname: String, val email: String)
data class LoginRequest(@field:NotEmpty val nickname: String, @field:NotEmpty val password: String)
data class EnrollRequest(@field:NotEmpty val nickname: String, @field:NotEmpty val password: String, @field:NotNull val enroller: Boolean)
data class EmbeddedSystemRequest(@field:NotEmpty val embeddedId: String, @field:NotEmpty val password: String)

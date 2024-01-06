package it.polito.wa2.login.services

import it.polito.wa2.login.controllers.*
import it.polito.wa2.login.dtos.ActivationDTO
import it.polito.wa2.login.dtos.UserDTO

interface LoginService {
    fun register(user: UserDTO): RegistrationResponse

    fun validate(activation: ActivationDTO): ValidationResponse

    fun login(loginRequest: LoginRequest): String

    fun enroll(enrollRequest: EnrollRequest)
    fun newEmbeddedSystem(embeddedSystemRequest: EmbeddedSystemRequest)
}

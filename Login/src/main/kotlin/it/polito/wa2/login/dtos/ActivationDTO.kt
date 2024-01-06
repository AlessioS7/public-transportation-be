package it.polito.wa2.login.dtos

import it.polito.wa2.login.entities.Activation
import it.polito.wa2.login.entities.User
import java.util.*
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

data class ActivationDTO(@field:NotNull val provisional_id: UUID?, @field:NotEmpty val activation_code: String) {

    private var counter = 5
    private var deadline = Date()
    var user: User? = null

    constructor(provisional_id: UUID?, activation_code: String, counter: Int, deadline: Date, user: User) : this(provisional_id,
        activation_code
    ) {
        this.counter = counter
        this.deadline = deadline
        this.user = user
    }
}

fun Activation.toActivationDTO() = ActivationDTO(id, code, counter, deadline, user)

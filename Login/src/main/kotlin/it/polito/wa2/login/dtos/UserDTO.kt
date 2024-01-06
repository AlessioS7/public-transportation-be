package it.polito.wa2.login.dtos

import it.polito.wa2.login.entities.User
import javax.validation.constraints.*

data class UserDTO(
    @field:NotEmpty val nickname: String,
    @field:NotEmpty
    @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$")
    val password: String,
    @field:NotEmpty @field:Email val email: String) {

    private var id: Long? = 0
    private var active = false

    constructor(id: Long?, nickname: String, password: String, email: String, active: Boolean)
            : this(nickname, password, email) {
        this.id = id
        this.active = active
    }

    override fun toString(): String =
        "UserDTO(id=$id, username=$nickname, password=$password, email=$email, active=$active)"
}

fun User.toUserDTO() = UserDTO(id, username, password, email, active)
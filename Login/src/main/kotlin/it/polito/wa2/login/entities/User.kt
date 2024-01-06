package it.polito.wa2.login.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = 0

    lateinit var username: String
    lateinit var password: String
    lateinit var email: String
    var active = false
    var role= Role.CUSTOMER
}

enum class Role { CUSTOMER, ADMIN, ENROLLER, EMBEDDED }

package it.polito.wa2.login.entities

import org.hibernate.annotations.GenericGenerator
import java.util.Date
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name = "activations")
class Activation {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    var id: UUID? = null

    var counter = 5
    lateinit var code: String

    @Temporal(TemporalType.TIMESTAMP)
    lateinit var deadline: Date

    @OneToOne
    lateinit var user: User
}
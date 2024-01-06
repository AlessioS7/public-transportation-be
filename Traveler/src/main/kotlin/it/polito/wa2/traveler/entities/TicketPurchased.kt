package it.polito.wa2.traveler.entities

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.*


@Entity
@Table(name = "ticket_purchased")
@EntityListeners(AuditingEntityListener::class)
class TicketPurchased {
    @Id
    lateinit var uuid: UUID

    lateinit var jwt: String

    @ManyToOne
    var user: UserDetails? = null

    @CreatedDate
    lateinit var date: Date
}

package it.polito.wa2.traveler.entities

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "transits")
@EntityListeners(AuditingEntityListener::class)
class Transit {
    @Id
    lateinit var uuid: UUID

    var ticketType: String = ""

    @CreatedDate
    lateinit var date: Date

    @CreatedBy
    lateinit var turnstile: String

    var userId: Long? = null
}


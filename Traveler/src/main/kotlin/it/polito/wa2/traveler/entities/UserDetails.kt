package it.polito.wa2.traveler.entities

import javax.persistence.*

@Entity
@Table(name = "user_details")
class UserDetails {
    @Id
    var id: Long? = 0

    lateinit var name: String
    lateinit var address: String

    @Column(name = "date_of_birth")
    lateinit var dateOfBirth: String

    @Column(name = "telephone_number")
    lateinit var telephoneNumber: String

    @OneToMany(mappedBy = "user")
    val purchasedTickets = mutableSetOf<TicketPurchased>()

}
package it.polito.wa2.traveler.repositories

import it.polito.wa2.traveler.dtos.TicketPurchasedDTO
import it.polito.wa2.traveler.entities.TicketPurchased
import it.polito.wa2.traveler.entities.UserDetails
import org.springframework.data.repository.CrudRepository
import java.util.*

interface TicketPurchasedRepository: CrudRepository<TicketPurchased, UUID> {

    fun findAllByUser(user: UserDetails): List<TicketPurchased>

    fun findByUuidAndUser(uuid: UUID, user: UserDetails): TicketPurchased?

    fun findFirstByUuid(uuid: UUID): TicketPurchased?
    fun findAllByDateBetween(from: Date, to: Date): List<TicketPurchased>
    fun findAllByDateBetweenAndUser(from: Date, to: Date, user: UserDetails): List<TicketPurchased>
    fun findAllByDateAfter(from: Date): List<TicketPurchased>
    fun findAllByDateAfterAndUser(from: Date, user: UserDetails): List<TicketPurchased>
    fun findAllByDateBefore(to: Date): List<TicketPurchased>
    fun findAllByDateBeforeAndUser(to: Date, user: UserDetails): List<TicketPurchased>

}

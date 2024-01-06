package it.polito.wa2.traveler.repositories

import it.polito.wa2.traveler.entities.TicketPurchased
import it.polito.wa2.traveler.entities.Transit
import it.polito.wa2.traveler.entities.UserDetails
import org.springframework.data.repository.CrudRepository
import java.util.*

interface TransitRepository: CrudRepository<Transit, UUID> {
    fun findAllByUserId(userId: Long): List<Transit>
    fun findAllByDateBetween(from: Date, to: Date): List<Transit>
    fun findAllByDateBetweenAndUserId(from: Date, to: Date, userId: Long): List<Transit>
    fun findAllByDateAfter(from: Date): List<Transit>
    fun findAllByDateAfterAndUserId(from: Date, userId: Long): List<Transit>
    fun findAllByDateBefore(to: Date): List<Transit>
    fun findAllByDateBeforeAndUserId(to: Date, userId: Long): List<Transit>
}

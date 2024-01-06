package it.polito.wa2.ticketcatalogue.repositories

import it.polito.wa2.ticketcatalogue.entities.Ticket
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketRepository: CoroutineCrudRepository<Ticket, String> {
}
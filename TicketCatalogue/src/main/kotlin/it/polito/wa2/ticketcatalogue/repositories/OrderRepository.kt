package it.polito.wa2.ticketcatalogue.repositories

import it.polito.wa2.ticketcatalogue.entities.Order
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Mono

interface OrderRepository: CoroutineCrudRepository<Order, ObjectId> {

    suspend fun findAllByUsername(username: String): Flow<Order>

    suspend fun findFirstByUsernameAndId(username: String, orderId: ObjectId): Mono<Order>

    suspend fun findFirstById(orderId: ObjectId): Mono<Order>
}
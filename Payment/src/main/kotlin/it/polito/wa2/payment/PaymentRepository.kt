package it.polito.wa2.payment

import it.polito.wa2.payment.entities.Transaction
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Mono

interface PaymentRepository: CoroutineCrudRepository<Transaction, ObjectId> {

    fun findAllByUserId(username: String): Flow<Transaction>

    fun findFirstByOrderId(id: String): Mono<Transaction>

}

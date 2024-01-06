package it.polito.wa2.ticketcatalogue.entities

import it.polito.wa2.ticketcatalogue.dto.OrderDTO
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("orders")
data class Order(
    @Id
    val id: ObjectId?,
    val ticketId: String,
    val username: String,
    val numOfTickets: Int,
    val status: String,
)

fun OrderDTO.toEntity() = Order(
    id = null,
    ticketId = ticketId,
    username = username,
    numOfTickets = numOfTickets,
    status = status
)

object OrderStatus {
    const val PENDING = "PENDING"
    const val CLOSED = "CLOSED"
    const val REJECTED = "REJECTED"
}
package it.polito.wa2.ticketcatalogue.dto

import it.polito.wa2.ticketcatalogue.entities.Order


data class OrderDTO(
    val orderId: String?,
    val ticketId: String,
    val username: String,
    val numOfTickets: Int,
    val status: String,
)

fun Order.toDTO() = OrderDTO(
    orderId = id.toString(),
    ticketId = ticketId,
    username = username,
    numOfTickets = numOfTickets,
    status = status
)
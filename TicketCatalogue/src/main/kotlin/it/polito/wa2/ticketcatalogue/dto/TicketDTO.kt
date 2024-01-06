package it.polito.wa2.ticketcatalogue.dto

import it.polito.wa2.ticketcatalogue.entities.Ticket

data class TicketDTO(
    val ticketId: String,
    val price: Double,
    val type: String,
    val validdays: List<Int>,
    val duration: Int,
    val zones: String,
    val minAge: Int?,
    val maxAge: Int?
)

fun Ticket.toDTO() = TicketDTO(
    ticketId = ticketId,
    price = price,
    type = type,
    validdays = validdays,
    duration = duration,
    zones = zones,
    minAge = minAge,
    maxAge = maxAge
)


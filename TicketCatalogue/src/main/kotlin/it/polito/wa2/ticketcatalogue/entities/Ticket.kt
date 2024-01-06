package it.polito.wa2.ticketcatalogue.entities

import it.polito.wa2.ticketcatalogue.dto.TicketDTO
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "tickets")
data class Ticket(
    @Id
    val ticketId: String,
    val price: Double,
    val type: String,
    val validdays: List<Int>,
    val duration: Int,
    val zones: String,
    val minAge: Int?,
    val maxAge: Int?
)

fun TicketDTO.toEntity() = Ticket(
    ticketId = ticketId,
    price = price,
    type = type,
    validdays = validdays,
    duration = duration,
    zones = zones,
    minAge = minAge,
    maxAge = maxAge
)
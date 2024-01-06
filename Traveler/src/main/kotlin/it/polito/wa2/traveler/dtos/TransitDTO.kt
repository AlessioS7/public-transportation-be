package it.polito.wa2.traveler.dtos

import it.polito.wa2.traveler.entities.Transit
import java.util.*

data class TransitDTO(
    val uuid: UUID,
    val date: Date,
    val turnstile: String,
    val userId: Long?,
    val ticketType: String
)

fun Transit.toDTO() = TransitDTO(
    uuid = uuid,
    date = date,
    turnstile = turnstile,
    userId = userId,
    ticketType = ticketType
)

package it.polito.wa2.traveler.dtos

import it.polito.wa2.traveler.entities.TicketPurchased
import java.util.*

data class TicketPurchasedDTO(
    val uuid: UUID,
    val date: Date,
    val jwt: String,
    val userId: Long?
)

fun TicketPurchased.toDTO(): TicketPurchasedDTO {
    return TicketPurchasedDTO(
        uuid = uuid,
        date = date,
        jwt = jwt,
        userId = user?.id
    )
}

package it.polito.wa2.traveler.services

import it.polito.wa2.traveler.*
import it.polito.wa2.traveler.dtos.TicketPurchasedDTO
import it.polito.wa2.traveler.dtos.TransitDTO
import java.awt.image.BufferedImage

interface TravelerService {
    fun getMyProfile(): UserDetailsResponse

    fun updateMyProfile(authToken: String, userDetails: UserDetailsRequest)

    fun getTickets(): List<TicketResponse?>

    fun getTicket(ticketID: String): BufferedImage?

    fun buyTickets(ticketRequest: TicketRequest): List<TicketResponse?>

    fun getTravelerNames(): List<String>

    fun getTravelerDetails( userId: Long): UserDetailsResponse

    fun getTravelerTickets(userId: Long): List<TicketResponse>
    fun registerTransit(transitRequest: TransitRequest)
    fun getPurchasesReport(from: String?, to: String?, userId: Long?): List<TicketPurchasedDTO>
    fun getTransitsReport(from: String?, to: String?, userId: Long?): List<TransitDTO>
}

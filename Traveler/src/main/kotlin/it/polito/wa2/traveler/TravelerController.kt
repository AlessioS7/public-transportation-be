package it.polito.wa2.traveler

import it.polito.wa2.traveler.dtos.TicketPurchasedDTO
import it.polito.wa2.traveler.dtos.TransitDTO
import it.polito.wa2.traveler.exceptions.InternalErrorException
import it.polito.wa2.traveler.exceptions.InvalidJwtException
import it.polito.wa2.traveler.exceptions.TicketNotFoundException
import it.polito.wa2.traveler.exceptions.UserNotFoundException
import it.polito.wa2.traveler.services.TravelerServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.BufferedImageHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.awt.image.BufferedImage
import java.text.ParseException
import java.util.Date
import javax.print.attribute.standard.Media

@RestController
class TravelerController {
    @Autowired
    lateinit var tsi: TravelerServiceImpl

    @GetMapping("/my/profile")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    fun myProfile(): UserDetailsResponse {
        return tsi.getMyProfile()
    }

    @PutMapping("/my/profile")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    fun updateMyProfile(
        @RequestHeader("Authorization") jwt: String,
        @RequestBody details: UserDetailsRequest
    ) {
        tsi.updateMyProfile(authToken = jwt, userDetails = details)
    }

    @GetMapping("/my/tickets")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    fun getTickets(): List<TicketResponse?> {
        return tsi.getTickets()
    }

    @GetMapping("/my/tickets/{ticketID}", produces = [MediaType.IMAGE_PNG_VALUE])
    @PreAuthorize("hasAuthority('CUSTOMER')")
    fun getTicket(@PathVariable ticketID: String): ResponseEntity<BufferedImage?> {
        return ResponseEntity(tsi.getTicket(ticketID), HttpStatus.OK)
    }

    @PostMapping("/my/tickets")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    fun addTickets(@RequestBody ticketRequest: TicketRequest): List<TicketResponse?> {
        return tsi.buyTickets(ticketRequest)
    }

    @GetMapping("/admin/travelers")
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    fun getTravelers(): List<String> {
        return tsi.getTravelerNames()
    }

    @GetMapping("/admin/traveler/{userID}/profile")
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    fun getTraveler(@PathVariable userID: Long): UserDetailsResponse {
        return tsi.getTravelerDetails(userID)
    }

    @GetMapping("/admin/traveler/{userID}/tickets")
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    fun getTravelerTickets(@PathVariable userID: Long): List<TicketResponse> {
        return tsi.getTravelerTickets(userID)
    }

    @PostMapping("/embedded/traveler/transits")
    @PreAuthorize("hasAuthority('EMBEDDED')")
    fun registerTransit(@RequestBody transitRequest: TransitRequest) {
        tsi.registerTransit(transitRequest)
    }

    @GetMapping("/admin/traveler/reports/purchases")
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    fun getPurchasesReport(
        @RequestParam(name = "from", required = false) from: String?,
        @RequestParam(name = "to", required = false) to: String?,
        @RequestParam(name = "userId", required = false) userId: Long?
    ): List<TicketPurchasedDTO> {
        return tsi.getPurchasesReport(from, to, userId)
    }

    @GetMapping("/admin/traveler/reports/transits")
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    fun getTransitsReport(
        @RequestParam(name = "from", required = false) from: String?,
        @RequestParam(name = "to", required = false) to: String?,
        @RequestParam(name = "userId", required = false) userId: Long?
    ): List<TransitDTO> {
        return tsi.getTransitsReport(from, to, userId)
    }



    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(InvalidJwtException::class)
    fun invalidJwt(e: InvalidJwtException) {
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalErrorException::class)
    fun internalError(e: InternalErrorException) {
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ParseException::class)
    fun invalidDate(e: ParseException) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException::class)
    fun userNotFound(e: UserNotFoundException) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(TicketNotFoundException::class)
    fun ticketNotFound(e: TicketNotFoundException) {
    }
}

data class UserDetailsRequest(
    val name: String,
    val address: String,
    val dateOfBirth: String,
    val telephoneNumber: String
)

data class UserDetailsResponse(
    val name: String,
    val address: String,
    val dateOfBirth: String,
    val telephoneNumber: String
)

data class TicketRequest(
    val cmd: String,
    val quantity: String,
    val zones: String,
    val validdays: List<Int>,
    val duration: Int,
    val type: String
)

data class TicketResponse(
    val sub: String,
    val iat: String,
    val exp: String,
    val zid: String,
    val jws: String,
    val nbf: String,
    val type: String
)

data class TransitRequest(
    val ticketID: String
)

package it.polito.wa2.ticketcatalogue

import it.polito.wa2.ticketcatalogue.dto.OrderDTO
import it.polito.wa2.ticketcatalogue.dto.TicketDTO
import it.polito.wa2.ticketcatalogue.exceptions.EntityNotFoundException
import it.polito.wa2.ticketcatalogue.exceptions.InvalidArgumentException
import it.polito.wa2.ticketcatalogue.exceptions.UnauthorizedException
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@RestController
class TicketCatalogueController {
    @Value("\${traveler.jwt.key}")
    lateinit var validationKey: String

    @Autowired
    lateinit var ticketCatalogueService: TicketCatalogueService

    @GetMapping("/tickets", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    suspend fun getTickets(): Flow<TicketDTO> {
        return ticketCatalogueService.getTickets()
    }

    @PostMapping("/admin/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    suspend fun addTicketToCatalogue(@RequestBody ticketDTO: TicketDTO?) {
        ticketCatalogueService.addTicketToCatalogue(ticketDTO)
    }

    @PutMapping("/admin/tickets")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    suspend fun editTicketCatalogue(@RequestBody ticketDTO: TicketDTO?) {
        ticketCatalogueService.editTicketCatalogue(ticketDTO)
    }

    @DeleteMapping("/admin/tickets/{ticketId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    suspend fun removeTicketFromCatalogue(@PathVariable ticketId: String?) {
        ticketCatalogueService.removeTicketFromCatalogue(ticketId)
    }

    @PostMapping("/shop", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('CUSTOMER')")
    suspend fun buyTicket(
        @RequestBody purchase: Purchase
    ): Flow<String> {
        return ticketCatalogueService.buyTicket(purchase)
    }

    @GetMapping("/orders", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('CUSTOMER')")
    suspend fun getMyOrders(): Flow<OrderDTO> {
        return ticketCatalogueService.getMyOrders()
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    suspend fun getMyOrder(@PathVariable orderId: String): OrderDTO? {

        return ticketCatalogueService.getMyOrder(orderId)
    }

    @GetMapping("/admin/orders", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    suspend fun getAllOrders(): Flow<OrderDTO> {
        return ticketCatalogueService.getAllOrders()
    }

    @GetMapping("/admin/orders/{userId}", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    suspend fun getUserOrders(@PathVariable userId: String): Flow<OrderDTO> {

        return ticketCatalogueService.getUserOrders(userId)
    }

    @GetMapping("/validation-key")
    @PreAuthorize("hasAuthority('EMBEDDED_SYSTEM')")
    suspend fun getValidationKey(): String {
        return validationKey
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidArgumentException::class)
    fun invalidArgument(e: InvalidArgumentException) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException::class)
    fun entityNotFound(e: EntityNotFoundException) {
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(e: UnauthorizedException) {
    }

}

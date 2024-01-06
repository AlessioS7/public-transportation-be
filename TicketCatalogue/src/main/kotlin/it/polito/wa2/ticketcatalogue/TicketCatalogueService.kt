package it.polito.wa2.ticketcatalogue

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.polito.wa2.ticketcatalogue.dto.OrderDTO
import it.polito.wa2.ticketcatalogue.dto.TicketDTO
import it.polito.wa2.ticketcatalogue.dto.toDTO
import it.polito.wa2.ticketcatalogue.entities.Order
import it.polito.wa2.ticketcatalogue.entities.OrderStatus
import it.polito.wa2.ticketcatalogue.entities.Ticket
import it.polito.wa2.ticketcatalogue.entities.toEntity
import it.polito.wa2.ticketcatalogue.exceptions.EntityNotFoundException
import it.polito.wa2.ticketcatalogue.exceptions.InvalidArgumentException
import it.polito.wa2.ticketcatalogue.repositories.OrderRepository
import it.polito.wa2.ticketcatalogue.repositories.TicketRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.publish
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.text.SimpleDateFormat
import java.util.*

@Service
class TicketCatalogueService {
    @Autowired
    private lateinit var ticketRepository: TicketRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var template: KafkaTemplate<String?, String?>
    //template: KafkaTemplate<String?, String?>

    val webClient = WebClient.create("http://localhost:8081")

    suspend fun getTickets(): Flow<TicketDTO> {
        return ticketRepository.findAll().map { it.toDTO() }
    }

    suspend fun addTicketToCatalogue(ticketDTO: TicketDTO?) {
        if (ticketDTO == null)
            throw InvalidArgumentException()
        ticketRepository.save(ticketDTO.toEntity())
    }

    suspend fun editTicketCatalogue(ticketDTO: TicketDTO?) {
        if (ticketDTO == null)
            throw InvalidArgumentException()
        if (!ticketRepository.existsById(ticketDTO.ticketId))
            throw EntityNotFoundException()
        ticketRepository.save(ticketDTO.toEntity())
    }

    suspend fun removeTicketFromCatalogue(ticketId: String?) {
        if (ticketId == null)
            throw InvalidArgumentException()
        val ticket = ticketRepository.findById(ticketId)
            ?: throw EntityNotFoundException()
        ticketRepository.delete(ticket)
    }

    suspend fun buyTicket(purchase: Purchase?): Flow<String> {
        if (purchase?.ticketId == null)
            throw InvalidArgumentException()

        val ticket = ticketRepository.findById(purchase.ticketId)
            ?: throw EntityNotFoundException()

        val securityContext = ReactiveSecurityContextHolder
            .getContext()
            .awaitFirstOrNull()

        val userInfo = webClient
            .get()
            .uri("my/profile")
            .header("Authorization", securityContext!!.authentication.credentials.toString())
            .accept(MediaType.APPLICATION_JSON)
            .exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.OK) {
                    response.bodyToMono(UserInfo::class.java)
                } else {
                    Mono.empty()
                }
            }.awaitFirstOrNull() ?: throw InvalidArgumentException()


        if (!checkConstraints(userInfo, ticket))
            throw InvalidArgumentException()


        val order = orderRepository.save(
            Order(
                id = null,
                ticketId = purchase.ticketId,
                username = securityContext!!.authentication.principal.toString(),
                numOfTickets = purchase.numberOfTickets,
                status = OrderStatus.PENDING
            )
        )

        val paymentRequest = PaymentRequest(
            billingInformation = purchase.paymentInfo,
            amount = ticket.price * order.numOfTickets.toDouble(),
            userId = securityContext.authentication.principal.toString(),
            orderId = order.id.toString(),
            userAuthToken = securityContext.authentication.credentials.toString()
        )

        val event = template.send("paymentReq", jacksonObjectMapper().writeValueAsString(paymentRequest))

        var ordersStatus = orderRepository.findById(order.id!!)?.status

        var counter = 0

        return flow {
            while (ordersStatus == OrderStatus.PENDING) {
                if (counter == 25) {
                    orderRepository.save(
                        Order(
                            id = order.id,
                            ticketId = order.ticketId,
                            username = order.username,
                            numOfTickets = order.numOfTickets,
                            status = OrderStatus.REJECTED
                        )
                    )
                    ordersStatus = OrderStatus.REJECTED
                    template.send("errorPaymentRes", order.id.toHexString())
                    break
                }
                this.emit(ordersStatus!!)
                delay(200L)
                ordersStatus = orderRepository.findById(order.id)?.status
                counter++
            }
            if (ordersStatus == OrderStatus.REJECTED) {
                this.emit(ordersStatus!!)
            } else {
                this.emit(order.id.toString())
            }
        }
    }


    suspend fun getMyOrders(): Flow<OrderDTO> {
        val securityContext = ReactiveSecurityContextHolder
            .getContext()
            .awaitFirstOrNull()

        val username = securityContext!!.authentication.principal.toString()

        return orderRepository.findAllByUsername(username).map { it.toDTO() }
    }


    suspend fun getMyOrder(orderId: String): OrderDTO? {
        val securityContext = ReactiveSecurityContextHolder
            .getContext()
            .awaitFirstOrNull()

        val username = securityContext!!.authentication.principal.toString()

        return orderRepository
            .findFirstByUsernameAndId(username, ObjectId(orderId)).awaitFirstOrNull()?.toDTO()

    }

    suspend fun getAllOrders(): Flow<OrderDTO> {

        return orderRepository.findAll().map { it.toDTO() }
    }

    suspend fun getUserOrders(username: String): Flow<OrderDTO> {

        return orderRepository.findAllByUsername(username).map { it.toDTO() }
    }

    @KafkaListener(id = "ticketCatalogueId", topics = ["paymentRes"])
    fun listen(value: String?) {
        if (value == null)
            throw Exception("Payment result message is null!")

        val paymentResult = jacksonObjectMapper().readValue<PaymentResult>(value)
        CoroutineScope(Dispatchers.IO).launch {
            val order = orderRepository.findFirstById(ObjectId(paymentResult.orderId)).awaitFirstOrNull()

            if (order?.status == OrderStatus.PENDING) {

                val status = if (paymentResult.status == "fail") OrderStatus.REJECTED else OrderStatus.CLOSED
                val ticketInfo = ticketRepository.findById(order.ticketId)
                if (ticketInfo != null) {
                    orderRepository.save(
                        Order(
                            id = order.id,
                            ticketId = order.ticketId,
                            username = order.username,
                            numOfTickets = order.numOfTickets,
                            status = status
                        )
                    )

                    if (status == OrderStatus.CLOSED) {
                        webClient
                            .post()
                            .uri("my/tickets")
                            .header("Authorization", paymentResult.userAuthToken)
                            .body(
                                BodyInserters.fromValue(
                                    mapOf(
                                        "cmd" to "buy_tickets",
                                        "quantity" to order.numOfTickets.toString(),
                                        "zones" to ticketInfo.zones,
                                        "validdays" to ticketInfo.validdays,
                                        "duration" to ticketInfo.duration,
                                        "type" to ticketInfo.type
                                    )
                                )
                            ).exchangeToMono { response ->
                                if (response.statusCode() == HttpStatus.OK) {
                                    response.bodyToMono(String::class.java)
                                } else {
                                    Mono.empty()
                                }
                            }.awaitFirstOrNull() ?: throw InvalidArgumentException()
                    }
                }
            } else {
                //è passato troppo tempo, chiediamo al payment di eliminare la transazione
                template.send("errorPaymentRes", paymentResult.orderId)
            }
        }
    }

}

data class Purchase(
    val numberOfTickets: Int,
    val ticketId: String,
    val paymentInfo: PaymentInfo
)

data class PaymentInfo(
    val ccn: String,        //Credit Card Number
    val expiration: String,
    val cvv: String,
    val cardHolder: String
)

data class PaymentRequest(
    val billingInformation: PaymentInfo,
    val amount: Double,
    val orderId: String,
    val userId: String,
    val userAuthToken: String
)

data class UserInfo(
    val name: String,
    val address: String,
    val dateOfBirth: String,
    val telephoneNumber: String
)

data class PaymentResult(
    val orderId: String,
    val status: String,
    val userAuthToken: String
)


fun checkConstraints(userInfo: UserInfo, ticket: Ticket): Boolean {
    var valid = true
    val formatter = SimpleDateFormat("yyyy-MM-dd")
    val birthDate = Calendar.getInstance()
    birthDate.time = formatter.parse(userInfo.dateOfBirth)
    val today = Calendar.getInstance()
    today.time = Date()

    //check age constraints
    if (ticket.minAge != null){
        var monthsInBetween = today.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH)
        monthsInBetween += 12 * (today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR))
        val monthFloat: Float = monthsInBetween / 12f

        if (monthFloat < ticket.minAge)
            valid = false
    }
    if (ticket.maxAge != null){
        var monthsInBetween = today.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH)
        monthsInBetween += 12 * (today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR))
        val monthFloat: Float = monthsInBetween / 12f

        if (monthFloat > ticket.maxAge)
            valid = false
    }

    //other check could be added

    return valid
}

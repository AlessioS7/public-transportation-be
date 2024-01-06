package it.polito.wa2.payment

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.polito.wa2.payment.dto.TransactionDTO
import it.polito.wa2.payment.dto.toDTO
import it.polito.wa2.payment.entities.Transaction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*
import kotlin.random.Random

@Service
class PaymentService {
    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var template: KafkaTemplate<String?, String?>

    suspend fun getAllTransactions(): Flow<TransactionDTO> {
        return paymentRepository.findAll().map { it.toDTO() }
    }

    suspend fun getMyTransaction(): Flow<TransactionDTO> {
        val securityContext = ReactiveSecurityContextHolder
            .getContext()
            .awaitFirstOrNull()

        val username = securityContext!!.authentication.principal.toString()

        return paymentRepository.findAllByUserId(username).map { it.toDTO() }
    }

    @KafkaListener(id = "paymentId", topics = ["paymentReq"])
    fun listen(value: String?) {
        if (value == null)
            throw Exception("Listener received a null value!")

        val paymentRequest = jacksonObjectMapper().readValue<PaymentRequest>(value)

        val result = makePayment()

        val paymentResult = PaymentResult(
            orderId = paymentRequest.orderId,
            status = result,
            userAuthToken = paymentRequest.userAuthToken
        )

        CoroutineScope(Dispatchers.IO).launch {
            paymentRepository.save(
                Transaction(
                    id = null,
                    orderId = paymentRequest.orderId,
                    userId = paymentRequest.userId,
                    amount = paymentRequest.amount,
                    date = Date()
                )
            )
        }

        runBlocking {
            delay(1000L)
        }
        template.send("paymentRes", jacksonObjectMapper().writeValueAsString(paymentResult))


    }

    @KafkaListener(id = "errorId", topics = ["errorPaymentRes"])
    fun listenFailure(value: String?) {
        if (value == null)
            throw Exception("Listener received a null value!")

        CoroutineScope(Dispatchers.IO).launch {
            val trans = paymentRepository.findFirstByOrderId(value).awaitFirstOrNull()
            if (trans != null) {
                paymentRepository.delete(trans)
            }
        }
    }
}

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

data class PaymentResult(
    val orderId: String,
    val status: String,
    val userAuthToken: String
)

fun makePayment(): String {
    if (Random.nextInt(0, 2) == 0)
        return "fail"
    return "success"
}

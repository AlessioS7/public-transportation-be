package it.polito.wa2.payment

import it.polito.wa2.payment.dto.TransactionDTO
import it.polito.wa2.payment.exceptions.InvalidJwtException
import it.polito.wa2.payment.exceptions.UnauthorizedException
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController


@RestController
class PaymentController {

    @Autowired
    lateinit var paymentService: PaymentService

    @GetMapping("/admin/transactions", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('ADMIN')" + "|| hasAuthority('ENROLLER')")
    suspend fun getAllTransactions(): Flow<TransactionDTO> {
        return paymentService.getAllTransactions()
    }

    @GetMapping("/transactions", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('CUSTOMER')")
    suspend fun getMyTransaction(): Flow<TransactionDTO> {
        return paymentService.getMyTransaction()
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(e: UnauthorizedException) { }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(InvalidJwtException::class)
    fun invalidJwt(e: InvalidJwtException) { }

}
package it.polito.wa2.payment.dto

import it.polito.wa2.payment.entities.Transaction
import org.bson.types.ObjectId
import java.util.*

data class TransactionDTO(
    val transactionId: String?,
    val orderId: String,
    val userId: String,
    val amount: Double,
    val date: Date
)

fun Transaction.toDTO() = TransactionDTO(
    transactionId = id.toString(),
    orderId = orderId,
    userId = userId,
    amount = amount,
    date = date
)


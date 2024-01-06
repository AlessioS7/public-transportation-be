package it.polito.wa2.payment.entities

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date

@Document
data class Transaction(
    @Id
    val id: ObjectId?,
    val orderId: String,
    val userId: String,
    val amount: Double,
    val date: Date
)
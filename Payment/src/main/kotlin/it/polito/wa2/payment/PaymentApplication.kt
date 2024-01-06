package it.polito.wa2.payment

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.KafkaListener

@SpringBootApplication
class PaymentApplication {
    @Bean
    fun topic() = NewTopic("paymentReq", 4, 1)

    @Bean
    fun errorTopic() = NewTopic("errorPaymentRes", 4, 1)
}

fun main(args: Array<String>) {
    runApplication<PaymentApplication>(*args)
}

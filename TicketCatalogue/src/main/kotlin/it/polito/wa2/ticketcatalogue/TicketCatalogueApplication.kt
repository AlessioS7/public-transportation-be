package it.polito.wa2.ticketcatalogue

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate

@SpringBootApplication
class TicketCatalogueApplication {
	@Bean
	fun topic() = NewTopic("paymentRes", 4, 1)
}

fun main(args: Array<String>) {
	runApplication<TicketCatalogueApplication>(*args)
}

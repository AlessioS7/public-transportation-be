package it.polito.wa2.traveler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class TravelerApplication

fun main(args: Array<String>) {
    runApplication<TravelerApplication>(*args)
}

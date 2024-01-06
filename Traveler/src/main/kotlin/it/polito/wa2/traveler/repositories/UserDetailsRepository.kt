package it.polito.wa2.traveler.repositories

import it.polito.wa2.traveler.entities.UserDetails
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface UserDetailsRepository: CrudRepository<UserDetails, Long> {
    @Query("SELECT name FROM UserDetails")
    fun getAllNames(): List<String>

    fun findFirstById(userId: Long): Optional<UserDetails>

    fun findByName(name: String): Optional<UserDetails>
}
package it.polito.wa2.login.repositories

import it.polito.wa2.login.entities.Activation
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface ActivationRepository : CrudRepository<Activation, UUID> {

    @Transactional
    @Modifying
    @Query("DELETE FROM Activation a WHERE a.deadline <= current_timestamp")
    fun pruneExpiredActivations()
}
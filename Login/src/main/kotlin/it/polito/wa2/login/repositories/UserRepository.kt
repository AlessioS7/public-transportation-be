package it.polito.wa2.login.repositories

import it.polito.wa2.login.entities.Role
import it.polito.wa2.login.entities.User
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface UserRepository: CrudRepository<User, Long> {

    fun findFirstByUsername(username: String): Optional<User>

    fun findFirstByRole(role: Role): Optional<User>

    fun findByUsernameOrEmail(username: String, email: String): List<User>

    @Transactional
    @Modifying
    @Query("DELETE FROM User u WHERE u.active = false AND u.id NOT IN (SELECT a.user.id FROM Activation a)")
    fun pruneExpiredRegistrations()
}

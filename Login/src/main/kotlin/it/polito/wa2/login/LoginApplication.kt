package it.polito.wa2.login

import it.polito.wa2.login.entities.Role
import it.polito.wa2.login.entities.User
import it.polito.wa2.login.repositories.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConditionalOnProperty(name = ["scheduler.enabled"], matchIfMissing = true)
class LoginApplication {

    @Bean
    fun CommandLineRunnerBean(userRepository: UserRepository): CommandLineRunner? {
        return CommandLineRunner { args ->
            loadEnroller(userRepository)
        }
    }

    fun loadEnroller(userRepository: UserRepository) {
        val user = userRepository.findFirstByRole(Role.ENROLLER)
        if (user.isEmpty) {
            val AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val rnd = Random()

            val sb: StringBuilder = StringBuilder(10)
            for (i in 0 until 10) {
                sb.append(AB[rnd.nextInt(AB.length)])
            }

            val newEnroller = User().apply {
                id = null
                username = "enroller"
                password = passwordEncoder().encode(sb.toString())
                email = ""
                active = true
                role = Role.ENROLLER
            }

            userRepository.save(newEnroller)
            println("Username: ${newEnroller.username}\nPassword: $sb")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<LoginApplication>(*args)
}

@Bean
fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder()
}

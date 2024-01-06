package it.polito.wa2.traveler.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.BufferedImageHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.awt.image.BufferedImage

@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class AppConfiguration(val securityProperties: SecurityProperties) {
    @Bean
    fun bCryptPasswordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun createImageHttpMessageConverter(): HttpMessageConverter<BufferedImage?>? {
        return BufferedImageHttpMessageConverter()
    }
}

package it.polito.wa2.login.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "login.jwt")
class SecurityProperties {

    @Value("\${login.token.prefix}")
    lateinit var tokenPrefix: String

    @Value("\${login.header.string}")
    lateinit var headerString: String
}


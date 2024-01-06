package it.polito.wa2.payment.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Configuration
class WebSecurityConfig {

    @Autowired
    private lateinit var securityContextRepository: SecurityContextRepository

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.httpBasic().disable()
        http.formLogin().disable()
        http.csrf().disable()
        http.logout().disable()

        //http.authenticationManager(this.authenticationManager);
        http.securityContextRepository(this.securityContextRepository)
        http.authorizeExchange().pathMatchers("/**").permitAll()
        http.authorizeExchange().anyExchange().authenticated()


        return http.build()
    }



}

package it.polito.wa2.login

import it.polito.wa2.login.config.SecurityProperties
import it.polito.wa2.login.interceptors.RateLimiterInterceptor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class AppConfig : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(RateLimiterInterceptor()).addPathPatterns("/user/**")
    }
}

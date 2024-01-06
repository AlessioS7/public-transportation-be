package it.polito.wa2.login.interceptors

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.time.Duration
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class RateLimiterInterceptor : HandlerInterceptorAdapter() {
    private val limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofSeconds(1)))
    private val tokenBucket = Bucket4j.builder().addLimit(limit).build()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val probe = tokenBucket.tryConsumeAndReturnRemaining(1)

        return if (probe.isConsumed) {
            true
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            false
        }
    }
}
package com.tranquilai.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class RateLimiterConfig {

    @Bean
    fun remoteAddrKeyResolver(): KeyResolver = KeyResolver { exchange ->
        Mono.justOrEmpty(exchange.request.remoteAddress?.address?.hostAddress)
            .defaultIfEmpty("unknown")
    }

    @Bean
    fun userOrRemoteAddrKeyResolver(): KeyResolver = KeyResolver { exchange ->
        Mono.justOrEmpty(exchange.request.headers.getFirst("X-User-Id"))
            .switchIfEmpty(Mono.justOrEmpty(exchange.request.remoteAddress?.address?.hostAddress))
            .defaultIfEmpty("unknown")
    }
}

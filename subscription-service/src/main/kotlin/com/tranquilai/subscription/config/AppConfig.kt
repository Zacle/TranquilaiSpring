package com.tranquilai.subscription.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

@Configuration
@EnableCaching
class AppConfig(
    @param:Value("\${app.entitlement-cache-ttl-seconds}") private val entitlementCacheTtlSeconds: Long,
    @param:Value("\${app.usage-cache-ttl-seconds}") private val usageCacheTtlSeconds: Long,
) {

    @Bean
    fun restTemplate(): RestTemplate {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5_000)
            setReadTimeout(10_000)
        }
        return RestTemplate(requestFactory)
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val serializer = RedisSerializationContext.SerializationPair.fromSerializer(
            GenericJackson2JsonRedisSerializer(),
        )
        val base = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(serializer)

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(base.entryTtl(Duration.ofSeconds(entitlementCacheTtlSeconds)))
            .withCacheConfiguration("entitlements", base.entryTtl(Duration.ofSeconds(entitlementCacheTtlSeconds)))
            .withCacheConfiguration("usage", base.entryTtl(Duration.ofSeconds(usageCacheTtlSeconds)))
            .build()
    }
}

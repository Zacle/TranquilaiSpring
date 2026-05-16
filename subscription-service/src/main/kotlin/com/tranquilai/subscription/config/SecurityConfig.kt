package com.tranquilai.subscription.config

import com.tranquilai.subscription.security.GatewayAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(private val gatewayAuthFilter: GatewayAuthFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    // Google Play real-time developer notifications are verified by purchase-token reconciliation.
                    .requestMatchers("/api/webhooks/**").permitAll()
                    .requestMatchers("/internal/**").hasRole("INTERNAL")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}

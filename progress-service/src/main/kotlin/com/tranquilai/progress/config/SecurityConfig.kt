package com.tranquilai.progress.config

import com.tranquilai.progress.security.GatewayAuthFilter
import org.springframework.beans.factory.annotation.Value
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
class SecurityConfig(
    private val gatewayAuthFilter: GatewayAuthFilter,
    @param:Value("\${app.internal-service-key}") private val internalServiceKey: String,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/internal/**").hasRole("INTERNAL")
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/prometheus").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(
                InternalServiceKeyFilter(internalServiceKey),
                GatewayAuthFilter::class.java,
            )

        return http.build()
    }
}

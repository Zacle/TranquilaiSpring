package com.tranquilai.auth.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthRabbitConfig {
    @Bean
    fun authEventsExchange(
        @Value("\${app.events.auth-exchange}") exchangeName: String,
    ): TopicExchange = TopicExchange(exchangeName, true, false)

    @Bean
    fun verificationEmailQueue(
        @Value("\${app.events.verification-email-queue}") queueName: String,
    ): Queue = QueueBuilder.durable(queueName).build()

    @Bean
    fun userVerifiedQueue(
        @Value("\${app.events.user-verified-queue}") queueName: String,
    ): Queue = QueueBuilder.durable(queueName).build()

    @Bean
    fun verificationEmailBinding(
        @Qualifier("verificationEmailQueue") verificationEmailQueue: Queue,
        authEventsExchange: TopicExchange,
        @Value("\${app.events.verification-email-routing-key}") routingKey: String,
    ): Binding = BindingBuilder.bind(verificationEmailQueue).to(authEventsExchange).with(routingKey)

    @Bean
    fun userVerifiedBinding(
        @Qualifier("userVerifiedQueue") userVerifiedQueue: Queue,
        authEventsExchange: TopicExchange,
        @Value("\${app.events.user-verified-routing-key}") routingKey: String,
    ): Binding = BindingBuilder.bind(userVerifiedQueue).to(authEventsExchange).with(routingKey)
}

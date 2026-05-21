package com.tranquilai.user.config

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
class UserRabbitConfig {
    @Bean
    fun authEventsExchange(
        @Value("\${app.events.auth-exchange}") exchangeName: String,
    ): TopicExchange = TopicExchange(exchangeName, true, false)

    @Bean
    fun userVerifiedQueue(
        @Value("\${app.events.user-verified-queue}") queueName: String,
    ): Queue = QueueBuilder.durable(queueName).build()

    @Bean
    fun userVerifiedBinding(
        @Qualifier("userVerifiedQueue") userVerifiedQueue: Queue,
        authEventsExchange: TopicExchange,
        @Value("\${app.events.user-verified-routing-key}") routingKey: String,
    ): Binding = BindingBuilder.bind(userVerifiedQueue).to(authEventsExchange).with(routingKey)
}

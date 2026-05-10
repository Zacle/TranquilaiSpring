package com.tranquilai.ai.messaging

import org.aopalliance.aop.Advice
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

object AiMessaging {
    const val Exchange = "tranquilai.ai.events"
    const val DeadLetterExchange = "tranquilai.ai.events.dlx"
    const val ChatPlanQueue = "ai.chat-plan-events"
    const val ChatProgressQueue = "ai.chat-progress-events"
    const val ChatPlanRoutingKey = "ai.chat.plan"
    const val ChatProgressRoutingKey = "ai.chat.progress"
}

@Configuration
class AiMessagingConfig {

    @Bean
    fun aiExchange() = DirectExchange(AiMessaging.Exchange, true, false)

    @Bean
    fun aiDeadLetterExchange() = DirectExchange(AiMessaging.DeadLetterExchange, true, false)

    @Bean
    fun aiChatPlanQueue() = durableQueue(AiMessaging.ChatPlanQueue)

    @Bean
    fun aiChatProgressQueue() = durableQueue(AiMessaging.ChatProgressQueue)

    @Bean
    fun aiChatPlanDeadLetterQueue() = Queue("${AiMessaging.ChatPlanQueue}.dlq", true)

    @Bean
    fun aiChatProgressDeadLetterQueue() = Queue("${AiMessaging.ChatProgressQueue}.dlq", true)

    @Bean
    fun aiChatPlanBinding(aiChatPlanQueue: Queue, aiExchange: DirectExchange): Binding =
        BindingBuilder.bind(aiChatPlanQueue).to(aiExchange).with(AiMessaging.ChatPlanRoutingKey)

    @Bean
    fun aiChatProgressBinding(aiChatProgressQueue: Queue, aiExchange: DirectExchange): Binding =
        BindingBuilder.bind(aiChatProgressQueue).to(aiExchange).with(AiMessaging.ChatProgressRoutingKey)

    @Bean
    fun aiChatPlanDeadLetterBinding(aiChatPlanDeadLetterQueue: Queue, aiDeadLetterExchange: DirectExchange): Binding =
        BindingBuilder.bind(aiChatPlanDeadLetterQueue).to(aiDeadLetterExchange).with(AiMessaging.ChatPlanRoutingKey)

    @Bean
    fun aiChatProgressDeadLetterBinding(aiChatProgressDeadLetterQueue: Queue, aiDeadLetterExchange: DirectExchange): Binding =
        BindingBuilder.bind(aiChatProgressDeadLetterQueue).to(aiDeadLetterExchange).with(AiMessaging.ChatProgressRoutingKey)

    @Bean
    fun aiMessageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun aiRabbitTemplate(connectionFactory: ConnectionFactory, aiMessageConverter: MessageConverter): RabbitTemplate =
        RabbitTemplate(connectionFactory).apply {
            messageConverter = aiMessageConverter
            setExchange(AiMessaging.Exchange)
            isChannelTransacted = true
        }

    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        aiMessageConverter: MessageConverter,
        aiRetryInterceptor: Advice,
    ): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setMessageConverter(aiMessageConverter)
            setAdviceChain(aiRetryInterceptor)
            setDefaultRequeueRejected(false)
        }

    @Bean
    fun aiRetryInterceptor(): Advice =
        RetryInterceptorBuilder.stateless()
            .maxAttempts(5)
            .backOffOptions(1_000, 2.0, 30_000)
            .recoverer(RejectAndDontRequeueRecoverer())
            .build()

    private fun durableQueue(name: String) = Queue(
        name,
        true,
        false,
        false,
        mapOf("x-dead-letter-exchange" to AiMessaging.DeadLetterExchange),
    )
}

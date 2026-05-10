package com.tranquilai.activity.messaging

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

object ActivityMessaging {
    const val Exchange = "tranquilai.activity.events"
    const val DeadLetterExchange = "tranquilai.activity.events.dlx"

    const val ProgressQueue = "activity.progress-events"
    const val PlanQueue = "activity.plan-events"
    const val AiMoodInsightQueue = "activity.ai-mood-insight-events"
    const val AiMoodInsightResultQueue = "activity.ai-mood-insight-result-events"
    const val AiJournalSummaryQueue = "activity.ai-journal-summary-events"
    const val AiJournalSummaryResultQueue = "activity.ai-journal-summary-result-events"

    const val ProgressRoutingKey = "activity.progress"
    const val PlanRoutingKey = "activity.plan"
    const val AiMoodInsightRoutingKey = "activity.ai.mood-insight"
    const val AiMoodInsightResultRoutingKey = "activity.ai.mood-insight.result"
    const val AiJournalSummaryRoutingKey = "activity.ai.journal-summary"
    const val AiJournalSummaryResultRoutingKey = "activity.ai.journal-summary.result"
}

@Configuration
class ActivityMessagingConfig {

    @Bean
    fun activityExchange() = DirectExchange(ActivityMessaging.Exchange, true, false)

    @Bean
    fun activityDeadLetterExchange() = DirectExchange(ActivityMessaging.DeadLetterExchange, true, false)

    @Bean
    fun activityProgressQueue() = durableQueue(ActivityMessaging.ProgressQueue)

    @Bean
    fun activityPlanQueue() = durableQueue(ActivityMessaging.PlanQueue)

    @Bean
    fun activityAiMoodInsightQueue() = durableQueue(ActivityMessaging.AiMoodInsightQueue)

    @Bean
    fun activityAiMoodInsightResultQueue() = durableQueue(ActivityMessaging.AiMoodInsightResultQueue)

    @Bean
    fun activityAiJournalSummaryQueue() = durableQueue(ActivityMessaging.AiJournalSummaryQueue)

    @Bean
    fun activityAiJournalSummaryResultQueue() = durableQueue(ActivityMessaging.AiJournalSummaryResultQueue)

    @Bean
    fun activityProgressDeadLetterQueue() = Queue("${ActivityMessaging.ProgressQueue}.dlq", true)

    @Bean
    fun activityPlanDeadLetterQueue() = Queue("${ActivityMessaging.PlanQueue}.dlq", true)

    @Bean
    fun activityAiMoodInsightDeadLetterQueue() = Queue("${ActivityMessaging.AiMoodInsightQueue}.dlq", true)

    @Bean
    fun activityAiMoodInsightResultDeadLetterQueue() = Queue("${ActivityMessaging.AiMoodInsightResultQueue}.dlq", true)

    @Bean
    fun activityAiJournalSummaryDeadLetterQueue() = Queue("${ActivityMessaging.AiJournalSummaryQueue}.dlq", true)

    @Bean
    fun activityAiJournalSummaryResultDeadLetterQueue() = Queue("${ActivityMessaging.AiJournalSummaryResultQueue}.dlq", true)

    @Bean
    fun activityProgressBinding(activityProgressQueue: Queue, activityExchange: DirectExchange): Binding =
        BindingBuilder.bind(activityProgressQueue).to(activityExchange).with(ActivityMessaging.ProgressRoutingKey)

    @Bean
    fun activityPlanBinding(activityPlanQueue: Queue, activityExchange: DirectExchange): Binding =
        BindingBuilder.bind(activityPlanQueue).to(activityExchange).with(ActivityMessaging.PlanRoutingKey)

    @Bean
    fun activityAiMoodInsightBinding(activityAiMoodInsightQueue: Queue, activityExchange: DirectExchange): Binding =
        BindingBuilder.bind(activityAiMoodInsightQueue).to(activityExchange).with(ActivityMessaging.AiMoodInsightRoutingKey)

    @Bean
    fun activityAiMoodInsightResultBinding(
        activityAiMoodInsightResultQueue: Queue,
        activityExchange: DirectExchange,
    ): Binding =
        BindingBuilder.bind(activityAiMoodInsightResultQueue)
            .to(activityExchange)
            .with(ActivityMessaging.AiMoodInsightResultRoutingKey)

    @Bean
    fun activityAiJournalSummaryBinding(activityAiJournalSummaryQueue: Queue, activityExchange: DirectExchange): Binding =
        BindingBuilder.bind(activityAiJournalSummaryQueue).to(activityExchange).with(ActivityMessaging.AiJournalSummaryRoutingKey)

    @Bean
    fun activityAiJournalSummaryResultBinding(
        activityAiJournalSummaryResultQueue: Queue,
        activityExchange: DirectExchange,
    ): Binding =
        BindingBuilder.bind(activityAiJournalSummaryResultQueue)
            .to(activityExchange)
            .with(ActivityMessaging.AiJournalSummaryResultRoutingKey)

    @Bean
    fun activityProgressDeadLetterBinding(
        activityProgressDeadLetterQueue: Queue,
        activityDeadLetterExchange: DirectExchange,
    ): Binding =
        BindingBuilder.bind(activityProgressDeadLetterQueue)
            .to(activityDeadLetterExchange)
            .with(ActivityMessaging.ProgressRoutingKey)

    @Bean
    fun activityPlanDeadLetterBinding(activityPlanDeadLetterQueue: Queue, activityDeadLetterExchange: DirectExchange): Binding =
        BindingBuilder.bind(activityPlanDeadLetterQueue).to(activityDeadLetterExchange).with(ActivityMessaging.PlanRoutingKey)

    @Bean
    fun activityAiMoodInsightDeadLetterBinding(
        activityAiMoodInsightDeadLetterQueue: Queue,
        activityDeadLetterExchange: DirectExchange,
    ): Binding =
        BindingBuilder.bind(activityAiMoodInsightDeadLetterQueue)
            .to(activityDeadLetterExchange)
            .with(ActivityMessaging.AiMoodInsightRoutingKey)

    @Bean
    fun activityAiMoodInsightResultDeadLetterBinding(
        activityAiMoodInsightResultDeadLetterQueue: Queue,
        activityDeadLetterExchange: DirectExchange,
    ): Binding =
        BindingBuilder.bind(activityAiMoodInsightResultDeadLetterQueue)
            .to(activityDeadLetterExchange)
            .with(ActivityMessaging.AiMoodInsightResultRoutingKey)

    @Bean
    fun activityAiJournalSummaryDeadLetterBinding(
        activityAiJournalSummaryDeadLetterQueue: Queue,
        activityDeadLetterExchange: DirectExchange,
    ): Binding =
        BindingBuilder.bind(activityAiJournalSummaryDeadLetterQueue)
            .to(activityDeadLetterExchange)
            .with(ActivityMessaging.AiJournalSummaryRoutingKey)

    @Bean
    fun activityAiJournalSummaryResultDeadLetterBinding(
        activityAiJournalSummaryResultDeadLetterQueue: Queue,
        activityDeadLetterExchange: DirectExchange,
    ): Binding =
        BindingBuilder.bind(activityAiJournalSummaryResultDeadLetterQueue)
            .to(activityDeadLetterExchange)
            .with(ActivityMessaging.AiJournalSummaryResultRoutingKey)

    @Bean
    fun activityMessageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, activityMessageConverter: MessageConverter): RabbitTemplate =
        RabbitTemplate(connectionFactory).apply {
            messageConverter = activityMessageConverter
            setExchange(ActivityMessaging.Exchange)
            isChannelTransacted = true
        }

    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        activityMessageConverter: MessageConverter,
        activityRetryInterceptor: Advice,
    ): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setMessageConverter(activityMessageConverter)
            setAdviceChain(activityRetryInterceptor)
            setDefaultRequeueRejected(false)
        }

    @Bean
    fun activityRetryInterceptor(): Advice =
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
        mapOf("x-dead-letter-exchange" to ActivityMessaging.DeadLetterExchange),
    )
}

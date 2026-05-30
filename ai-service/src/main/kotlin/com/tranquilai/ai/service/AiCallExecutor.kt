package com.tranquilai.ai.service

import jakarta.annotation.PreDestroy
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class AiCallExecutor(
    @param:Value("\${app.ai-call-timeout-ms:12000}")
    private val timeoutMs: Long = 12_000,
    private val meterRegistry: MeterRegistry? = null,
) {
    private val logger = LoggerFactory.getLogger(AiCallExecutor::class.java)
    private val executor: ExecutorService = Executors.newCachedThreadPool(AiThreadFactory)

    val timeoutDuration: Duration = Duration.ofMillis(timeoutMs.coerceAtLeast(1))

    fun <T> execute(
        operationName: String,
        fallback: () -> T,
        block: () -> T,
    ): T {
        val sample = meterRegistry?.let { Timer.start(it) }
        val future = executor.submit<T> { block() }
        return try {
            val result = future.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS)
            record(sample, operationName, "success")
            result
        } catch (_: TimeoutException) {
            future.cancel(true)
            logger.warn("AI operation timed out operation={} timeoutMs={}", operationName, timeoutDuration.toMillis())
            record(sample, operationName, "timeout")
            fallback()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("AI operation interrupted operation={}", operationName)
            record(sample, operationName, "interrupted")
            fallback()
        } catch (ex: ExecutionException) {
            logger.warn("AI operation failed operation={} error={}", operationName, ex.cause?.message ?: ex.message)
            record(sample, operationName, "error")
            fallback()
        } catch (ex: RuntimeException) {
            logger.warn("AI operation failed operation={} error={}", operationName, ex.message)
            record(sample, operationName, "error")
            fallback()
        }
    }

    private fun record(sample: Timer.Sample?, operationName: String, outcome: String) {
        if (meterRegistry == null || sample == null) return
        val tags = arrayOf("operation", operationName, "outcome", outcome)
        sample.stop(Timer.builder("tranquilai.ai.call.duration").tags(*tags).register(meterRegistry))
        meterRegistry.counter("tranquilai.ai.call.count", *tags).increment()
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    private object AiThreadFactory : ThreadFactory {
        override fun newThread(runnable: Runnable): Thread =
            Thread(runnable, "ai-call-timeout").apply { isDaemon = true }
    }
}

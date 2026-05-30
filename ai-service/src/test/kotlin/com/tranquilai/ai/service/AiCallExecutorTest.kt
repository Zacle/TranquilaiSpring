package com.tranquilai.ai.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AiCallExecutorTest {

    @Test
    fun `execute returns fallback when ai operation times out`() {
        val registry = SimpleMeterRegistry()
        val executor = AiCallExecutor(timeoutMs = 10, meterRegistry = registry)

        val response = try {
            executor.execute("slow test", fallback = { "fallback" }) {
                Thread.sleep(500)
                "late response"
            }
        } finally {
            executor.shutdown()
        }

        assertEquals("fallback", response)
        assertNotNull(
            registry.find("tranquilai.ai.call.duration")
                .tag("operation", "slow test")
                .tag("outcome", "timeout")
                .timer(),
        )
        assertEquals(
            1.0,
            registry.find("tranquilai.ai.call.count")
                .tag("operation", "slow test")
                .tag("outcome", "timeout")
                .counter()!!
                .count(),
        )
    }

    @Test
    fun `execute returns fallback when ai operation throws`() {
        val registry = SimpleMeterRegistry()
        val executor = AiCallExecutor(timeoutMs = 1_000, meterRegistry = registry)

        val response = try {
            executor.execute("failed test", fallback = { "fallback" }) {
                error("provider down")
            }
        } finally {
            executor.shutdown()
        }

        assertEquals("fallback", response)
        assertNotNull(
            registry.find("tranquilai.ai.call.duration")
                .tag("operation", "failed test")
                .tag("outcome", "error")
                .timer(),
        )
    }

    @Test
    fun `execute records success latency`() {
        val registry = SimpleMeterRegistry()
        val executor = AiCallExecutor(timeoutMs = 1_000, meterRegistry = registry)

        val response = try {
            executor.execute("success test", fallback = { "fallback" }) {
                "ok"
            }
        } finally {
            executor.shutdown()
        }

        assertEquals("ok", response)
        assertNotNull(
            registry.find("tranquilai.ai.call.duration")
                .tag("operation", "success test")
                .tag("outcome", "success")
                .timer(),
        )
    }
}

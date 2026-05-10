package com.tranquilai.plan.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.UUID

class UserServiceClientTest {

    private var server: HttpServer? = null
    private val mapper = ObjectMapper()

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun `getPlanContext sends internal key and maps successful response`() {
        val userId = UUID.randomUUID()
        var capturedKey: String? = null
        startServer { exchange ->
            capturedKey = exchange.requestHeaders.getFirst("X-Internal-Key")
            val response = mapper.writeValueAsString(
                PlanContextResponse(
                    userId = userId,
                    firstName = "Alex",
                    currentFeelingLevel = "CALM",
                    stressCauses = listOf("work"),
                    urgencyLevel = "LOW",
                    supportIntensity = "LIGHT",
                ),
            )
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }

        val context = UserServiceClient(baseUrl(), "secret").getPlanContext(userId)

        assertEquals("secret", capturedKey)
        assertEquals("Alex", context?.firstName)
        assertEquals(listOf("work"), context?.stressCauses)
    }

    @Test
    fun `getPlanContext returns null when downstream call fails`() {
        val userId = UUID.randomUUID()
        startServer { exchange ->
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }

        assertNull(UserServiceClient(baseUrl(), "secret").getPlanContext(userId))
    }

    private fun startServer(handler: (com.sun.net.httpserver.HttpExchange) -> Unit) {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/") { exchange -> handler(exchange) }
            start()
        }
    }

    private fun baseUrl(): String = "http://localhost:${server!!.address.port}"
}

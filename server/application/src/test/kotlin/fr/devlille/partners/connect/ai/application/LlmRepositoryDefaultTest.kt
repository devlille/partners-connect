package fr.devlille.partners.connect.ai.application

import fr.devlille.partners.connect.ai.FakeLlmGateway
import fr.devlille.partners.connect.ai.factories.createChatRequest
import fr.devlille.partners.connect.internal.infrastructure.api.ServiceUnavailableException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.net.ConnectException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LlmRepositoryDefaultTest {
    @Test
    fun `chat throws BadRequestException when prompt is blank`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway())
        assertFailsWith<BadRequestException> {
            repo.chat(createChatRequest(prompt = "   "))
        }
    }

    @Test
    fun `chat defaults to gemini-2 0-flash when model is null`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway(response = "hi"))
        val response = repo.chat(createChatRequest(prompt = "hello", model = null))
        assertEquals("gemini-2.0-flash", response.model)
        assertEquals("hi", response.response)
    }

    @Test
    fun `chat passes through caller-provided model`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway(response = "hi"))
        val response = repo.chat(createChatRequest(prompt = "hello", model = "gemini-2.0-flash-lite"))
        assertEquals("gemini-2.0-flash-lite", response.model)
    }

    @Test
    fun `chat maps ConnectException to ServiceUnavailableException`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway(throws = ConnectException("refused")))
        assertFailsWith<ServiceUnavailableException> {
            repo.chat(createChatRequest(prompt = "hello"))
        }
        Unit
    }

    @Test
    fun `chat maps ServerResponseException to ServiceUnavailableException`() = runBlocking {
        val response = mockk<HttpResponse>(relaxed = true)
        every { response.status } returns HttpStatusCode.InternalServerError
        val repo = LlmRepositoryDefault(
            FakeLlmGateway(throws = ServerResponseException(response, "boom")),
        )
        assertFailsWith<ServiceUnavailableException> {
            repo.chat(createChatRequest(prompt = "hello"))
        }
        Unit
    }

    @Test
    fun `chat maps ClientRequestException to ServiceUnavailableException`() = runBlocking {
        val response = mockk<HttpResponse>(relaxed = true)
        every { response.status } returns HttpStatusCode.Unauthorized
        val repo = LlmRepositoryDefault(
            FakeLlmGateway(throws = ClientRequestException(response, "no")),
        )
        assertFailsWith<ServiceUnavailableException> {
            repo.chat(createChatRequest(prompt = "hello"))
        }
        Unit
    }
}

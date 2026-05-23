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
    fun `chat defaults to gemma3 1b when model is null`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway(response = "hi"))
        val response = repo.chat(createChatRequest(prompt = "hello", model = null))
        assertEquals("gemma3:1b", response.model)
        assertEquals("hi", response.response)
    }

    @Test
    fun `chat passes through caller-provided model`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway(response = "hi"))
        val response = repo.chat(createChatRequest(prompt = "hello", model = "llama3.2:3b"))
        assertEquals("llama3.2:3b", response.model)
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

    @Test
    fun `listModels delegates to gateway`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway(models = listOf("gemma3:1b", "llama3.2:3b")))
        assertEquals(listOf("gemma3:1b", "llama3.2:3b"), repo.listModels())
    }

    @Test
    fun `listModels maps ConnectException to ServiceUnavailableException`() = runBlocking {
        val repo = LlmRepositoryDefault(FakeLlmGateway(throws = ConnectException("refused")))
        assertFailsWith<ServiceUnavailableException> {
            repo.listModels()
        }
        Unit
    }
}

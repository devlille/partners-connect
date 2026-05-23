package fr.devlille.partners.connect.ai.application

import fr.devlille.partners.connect.ai.domain.ChatRequest
import fr.devlille.partners.connect.ai.domain.ChatResponse
import fr.devlille.partners.connect.ai.domain.LlmGateway
import fr.devlille.partners.connect.ai.domain.LlmRepository
import fr.devlille.partners.connect.internal.infrastructure.api.ServiceUnavailableException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.server.plugins.BadRequestException
import java.net.ConnectException

private const val DEFAULT_MODEL = "gemma3:1b"

class LlmRepositoryDefault(
    private val gateway: LlmGateway,
) : LlmRepository {
    override suspend fun chat(request: ChatRequest): ChatResponse {
        if (request.prompt.isBlank()) throw BadRequestException("prompt must not be blank")
        val modelName = request.model ?: DEFAULT_MODEL
        val response = withOllamaErrorHandling {
            gateway.chat(request.prompt, request.system, modelName)
        }
        return ChatResponse(model = modelName, response = response)
    }

    override suspend fun listModels(): List<String> =
        withOllamaErrorHandling { gateway.listOllamaModels() }

    // Map upstream 4xx/5xx to 503 — partners-connect made the request, so the caller can't fix it.
    private suspend fun <T> withOllamaErrorHandling(block: suspend () -> T): T =
        try {
            block()
        } catch (e: ConnectException) {
            throw ServiceUnavailableException("LLM backend is unreachable", e)
        } catch (e: ConnectTimeoutException) {
            throw ServiceUnavailableException("LLM backend timed out", e)
        } catch (e: ServerResponseException) {
            throw ServiceUnavailableException("LLM backend returned ${e.response.status}", e)
        } catch (e: ClientRequestException) {
            throw ServiceUnavailableException("LLM backend returned ${e.response.status}", e)
        }
}

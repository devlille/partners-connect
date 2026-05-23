package fr.devlille.partners.connect.ai.infrastructure.gateways

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import fr.devlille.partners.connect.ai.domain.LlmGateway
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val DEFAULT_CONTEXT_LENGTH = 8192L

class OllamaLlmGateway(
    private val executor: SingleLLMPromptExecutor,
    private val http: HttpClient,
    private val ollamaBaseUrl: String,
) : LlmGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String {
        val model = LLModel(
            provider = LLMProvider.Ollama,
            id = modelName,
            capabilities = listOf(LLMCapability.Temperature),
            contextLength = DEFAULT_CONTEXT_LENGTH,
        )
        val chatPrompt = prompt("ai-chat") {
            system?.let { system(it) }
            user(userPrompt)
        }
        val messages = executor.execute(chatPrompt, model)
        return messages.joinToString("\n") { it.content }
    }

    override suspend fun listOllamaModels(): List<String> {
        val body = http.get("$ollamaBaseUrl/api/tags").bodyAsText()
        return json.decodeFromString<OllamaTagsResponse>(body).models.map { it.name }
    }

    @Serializable
    private data class OllamaTagsResponse(val models: List<OllamaTag> = emptyList())

    @Serializable
    private data class OllamaTag(val name: String)
}

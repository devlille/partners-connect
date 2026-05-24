package fr.devlille.partners.connect.ai.infrastructure.gateways

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import fr.devlille.partners.connect.ai.domain.LlmGateway

// Generous default. Gemini 2.0 Flash supports 1M tokens; this caps what Koog requests.
private const val DEFAULT_CONTEXT_LENGTH = 1_048_576L

class GeminiLlmGateway(
    private val executor: SingleLLMPromptExecutor,
) : LlmGateway {
    override suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String {
        val model = LLModel(
            provider = LLMProvider.Google,
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
}

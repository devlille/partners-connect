package fr.devlille.partners.connect.ai.infrastructure.gateways

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import fr.devlille.partners.connect.ai.domain.LlmGateway

class GeminiLlmGateway(
    private val executor: SingleLLMPromptExecutor,
) : LlmGateway {
    override suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String {
        val model = GoogleModels.models.firstOrNull { it.id == modelName }
            ?: error("Unsupported Gemini model: $modelName")
        val chatPrompt = prompt("ai-chat") {
            system?.let { system(it) }
            user(userPrompt)
        }
        val messages = executor.execute(chatPrompt, model)
        return messages.joinToString("\n") { it.content }
    }
}

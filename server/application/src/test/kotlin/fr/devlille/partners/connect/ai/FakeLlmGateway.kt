package fr.devlille.partners.connect.ai

import fr.devlille.partners.connect.ai.domain.LlmGateway

class FakeLlmGateway(
    private val response: String = "fake response",
    private val models: List<String> = listOf("gemma3:1b"),
    private val throws: Throwable? = null,
) : LlmGateway {
    override suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String = throws?.let { throw it } ?: response

    override suspend fun listOllamaModels(): List<String> =
        throws?.let { throw it } ?: models
}

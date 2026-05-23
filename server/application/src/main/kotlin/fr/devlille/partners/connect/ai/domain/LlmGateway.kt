package fr.devlille.partners.connect.ai.domain

interface LlmGateway {
    suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String

    suspend fun listOllamaModels(): List<String>
}

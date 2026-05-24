package fr.devlille.partners.connect.ai.domain

interface LlmRepository {
    suspend fun chat(request: ChatRequest): ChatResponse
}

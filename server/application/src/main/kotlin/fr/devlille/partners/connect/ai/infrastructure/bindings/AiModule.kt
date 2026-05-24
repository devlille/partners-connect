package fr.devlille.partners.connect.ai.infrastructure.bindings

import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import fr.devlille.partners.connect.ai.application.LlmRepositoryDefault
import fr.devlille.partners.connect.ai.domain.LlmGateway
import fr.devlille.partners.connect.ai.domain.LlmRepository
import fr.devlille.partners.connect.ai.infrastructure.gateways.GeminiLlmGateway
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import org.koin.dsl.module

val aiModule = module {
    single<LlmGateway> {
        val apiKey = SystemVarEnv.Llm.geminiApiKey
            ?: error("GEMINI_API_KEY environment variable is required for the AI gateway")
        GeminiLlmGateway(
            executor = SingleLLMPromptExecutor(GoogleLLMClient(apiKey = apiKey)),
        )
    }
    single<LlmRepository> { LlmRepositoryDefault(get()) }
}

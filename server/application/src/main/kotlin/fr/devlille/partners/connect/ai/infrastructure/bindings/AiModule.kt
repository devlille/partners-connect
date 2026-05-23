package fr.devlille.partners.connect.ai.infrastructure.bindings

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import fr.devlille.partners.connect.ai.application.LlmRepositoryDefault
import fr.devlille.partners.connect.ai.domain.LlmGateway
import fr.devlille.partners.connect.ai.domain.LlmRepository
import fr.devlille.partners.connect.ai.infrastructure.gateways.OllamaLlmGateway
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import io.ktor.client.HttpClient
import org.koin.dsl.module

val aiModule = module {
    single<LlmGateway> {
        val ollamaBaseUrl = SystemVarEnv.Llm.ollamaBaseUrl
        OllamaLlmGateway(
            executor = SingleLLMPromptExecutor(OllamaClient(baseUrl = ollamaBaseUrl)),
            http = get<HttpClient>(),
            ollamaBaseUrl = ollamaBaseUrl,
        )
    }
    single<LlmRepository> { LlmRepositoryDefault(get()) }
}

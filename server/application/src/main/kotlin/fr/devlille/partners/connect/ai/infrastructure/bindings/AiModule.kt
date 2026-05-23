package fr.devlille.partners.connect.ai.infrastructure.bindings

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import fr.devlille.partners.connect.ai.application.LlmRepositoryDefault
import fr.devlille.partners.connect.ai.domain.LlmGateway
import fr.devlille.partners.connect.ai.domain.LlmRepository
import fr.devlille.partners.connect.ai.infrastructure.gateways.OllamaLlmGateway
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

// Clever Cloud network-group hostnames contain underscores; JDK HttpClient rejects them
// per RFC 3986 (JDK-8266929). Both Koog and the /api/tags call need OkHttp.
val aiModule = module {
    single<LlmGateway> {
        val ollamaBaseUrl = SystemVarEnv.Llm.ollamaBaseUrl
        val ollamaHttp = HttpClient(OkHttp) { expectSuccess = true }
        OllamaLlmGateway(
            executor = SingleLLMPromptExecutor(
                OllamaClient(baseUrl = ollamaBaseUrl, baseClient = HttpClient(OkHttp)),
            ),
            http = ollamaHttp,
            ollamaBaseUrl = ollamaBaseUrl,
        )
    }
    single<LlmRepository> { LlmRepositoryDefault(get()) }
}

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

// Ollama is reached over Clever Cloud's network-group hostname, which contains underscores
// (e.g. app_<uuid>.m.ng_<uuid>.cc-ng.cloud). The JDK's java.net.http.HttpClient (used by the
// shared networkClientModule via ktor-client-java) rejects such URIs per RFC 3986
// (see JDK-8266929). OkHttp accepts them, so the AI module uses a dedicated OkHttp-backed
// client both for the direct /api/tags call and as Koog's baseClient.
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

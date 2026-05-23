package fr.devlille.partners.connect.ai.infrastructure.gateways

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.LLMChoice
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class OllamaLlmGatewayTest {
    @Test
    fun `listOllamaModels parses ollama tags response into model name list`() = runBlocking {
        val client = HttpClient(
            MockEngine { _ ->
                respond(
                    content = """{"models":[{"name":"gemma3:1b"},{"name":"llama3.2:3b"}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        val gateway = OllamaLlmGateway(
            executor = SingleLLMPromptExecutor(StubLLMClient()),
            http = client,
            ollamaBaseUrl = "https://ollama.test",
        )

        val models = gateway.listOllamaModels()

        assertEquals(listOf("gemma3:1b", "llama3.2:3b"), models)
    }

    @Test
    fun `listOllamaModels returns empty list when ollama has no models`() = runBlocking {
        val client = HttpClient(
            MockEngine { _ ->
                respond(
                    content = """{"models":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        val gateway = OllamaLlmGateway(
            executor = SingleLLMPromptExecutor(StubLLMClient()),
            http = client,
            ollamaBaseUrl = "https://ollama.test",
        )

        val models = gateway.listOllamaModels()

        assertEquals(emptyList(), models)
    }

    @Test
    fun `chat joins all response message contents with newlines`() = runBlocking {
        val responses = listOf(
            Message.Assistant(content = "Hello", metaInfo = ResponseMetaInfo(timestamp = Clock.System.now())),
            Message.Assistant(content = "world", metaInfo = ResponseMetaInfo(timestamp = Clock.System.now())),
        )
        val gateway = OllamaLlmGateway(
            executor = SingleLLMPromptExecutor(StubLLMClient(responses)),
            http = HttpClient(MockEngine { _ -> respond("") }),
            ollamaBaseUrl = "https://ollama.test",
        )

        val response = gateway.chat(userPrompt = "Hi", system = null, modelName = "gemma3:1b")

        assertEquals("Hello\nworld", response)
    }

    @Test
    fun `chat returns empty string when LLM returns no response messages`() = runBlocking {
        val gateway = OllamaLlmGateway(
            executor = SingleLLMPromptExecutor(StubLLMClient(emptyList())),
            http = HttpClient(MockEngine { _ -> respond("") }),
            ollamaBaseUrl = "https://ollama.test",
        )

        val response = gateway.chat(userPrompt = "Hi", system = "Be terse", modelName = "gemma3:1b")

        assertEquals("", response)
    }
}

private class StubLLMClient(
    private val responses: List<Message.Response> = listOf(
        Message.Assistant(content = "stub", metaInfo = ResponseMetaInfo(timestamp = Clock.System.now())),
    ),
) : LLMClient {
    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): List<Message.Response> = responses

    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): List<LLMChoice> = throw UnsupportedOperationException()

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        throw UnsupportedOperationException()

    override fun llmProvider(): LLMProvider = LLMProvider.Ollama
}

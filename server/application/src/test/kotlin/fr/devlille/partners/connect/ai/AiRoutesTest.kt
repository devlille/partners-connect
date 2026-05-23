package fr.devlille.partners.connect.ai

import fr.devlille.partners.connect.ai.domain.LlmGateway
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.net.ConnectException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AiRoutesTest {
    @Test
    fun `authenticated organiser hits chat and gets fake response`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                llmGateway = FakeLlmGateway(response = "end-to-end response"),
            )
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val chatResponse = client.post("/orgs/$orgId/ai/chat") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"prompt":"Hello","system":"Be terse"}""")
        }

        assertEquals(HttpStatusCode.OK, chatResponse.status)
        val body = Json.parseToJsonElement(chatResponse.bodyAsText()).jsonObject
        assertEquals("end-to-end response", body["response"]?.jsonPrimitive?.content)

        val modelsResponse = client.get("/orgs/$orgId/ai/models") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, modelsResponse.status)
        assertEquals(listOf("gemma3:1b"), Json.decodeFromString<List<String>>(modelsResponse.bodyAsText()))
    }

    @Test
    fun `chat returns 503 when LLM backend is unreachable`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                llmGateway = FakeLlmGateway(throws = ConnectException("connection refused")),
            )
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.post("/orgs/$orgId/ai/chat") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"prompt":"Hello"}""")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    @Test
    fun `models returns 503 when Ollama responds with 5xx`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                llmGateway = ResponseStatusThrowingGateway(HttpStatusCode.InternalServerError),
            )
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/ai/models") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    @Test
    fun `models returns 503 when Ollama responds with 4xx`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                llmGateway = ResponseStatusThrowingGateway(HttpStatusCode.Unauthorized),
            )
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/ai/models") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }
}

// Surfaces a real `ClientRequestException` / `ServerResponseException` for the configured status.
private class ResponseStatusThrowingGateway(
    private val status: HttpStatusCode,
) : LlmGateway {
    private val client = HttpClient(
        MockEngine { _ ->
            respond(
                content = "upstream error",
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
            )
        },
    ) {
        expectSuccess = true
    }

    override suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String {
        client.get("https://ollama.test/api/generate")
        error("MockEngine should have thrown before this point")
    }

    override suspend fun listOllamaModels(): List<String> {
        client.get("https://ollama.test/api/tags")
        error("MockEngine should have thrown before this point")
    }
}

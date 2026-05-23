package fr.devlille.partners.connect.ai.infrastructure.api

import fr.devlille.partners.connect.ai.FakeLlmGateway
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AiModelsRouteGetTest {
    @Test
    fun `GET models returns 200 with array of model names`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                llmGateway = FakeLlmGateway(models = listOf("gemma3:1b", "llama3.2:3b")),
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

        assertEquals(HttpStatusCode.OK, response.status)
        val models = Json.decodeFromString<List<String>>(response.bodyAsText())
        assertEquals(listOf("gemma3:1b", "llama3.2:3b"), models)
    }

    @Test
    fun `GET models returns 200 with empty array when Ollama has no models`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId, llmGateway = FakeLlmGateway(models = emptyList()))
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/ai/models") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), Json.decodeFromString<List<String>>(response.bodyAsText()))
    }

    @Test
    fun `GET models returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/ai/models")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET models returns 401 when user lacks org permission`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
            }
        }

        val response = client.get("/orgs/$orgId/ai/models") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

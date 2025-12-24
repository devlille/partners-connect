package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProvidersListEventRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET event by slug returns providers array when providers are attached`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val providerId1 = UUID.randomUUID()
        val providerId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedProvider(providerId1, orgId = orgId)
                insertMockedProvider(providerId2, orgId = orgId)
            }
        }

        // Attach providers to event using the API
        val providerIds = listOf(providerId1.toString(), providerId2.toString())
        val attachResponse = client.post("/orgs/$orgId/events/$eventId/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(providerIds))
        }
        assertEquals(HttpStatusCode.OK, attachResponse.status)

        // Now test that GET event returns the providers
        val response = client.get("/events/$eventId")
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val event = responseJson["event"]!!.jsonObject

        // Verify providers field exists and contains our providers
        assertTrue(event.containsKey("providers"))
        val providers = event["providers"]!!.jsonArray
        assertEquals(2, providers.size)

        // Verify provider details
        val providerNames = providers.map { it.jsonObject["name"]!!.toString().removeSurrounding("\"") }
        assertTrue(providerNames.contains("$providerId1"))
        assertTrue(providerNames.contains("$providerId2"))
    }

    @Test
    fun `POST is idempotent - attaching same providers twice doesn't cause errors`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val providerIds = listOf(providerId.toString())

        // First attachment
        val response1 = client.post("/orgs/$orgId/events/$eventId/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(providerIds))
        }

        assertEquals(HttpStatusCode.OK, response1.status)

        // Second attachment (should be idempotent)
        val response2 = client.post("/orgs/$orgId/events/$eventId/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(providerIds))
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        val attachedIds = Json.parseToJsonElement(response2.bodyAsText()).jsonArray
        assertEquals(1, attachedIds.size)
    }
}

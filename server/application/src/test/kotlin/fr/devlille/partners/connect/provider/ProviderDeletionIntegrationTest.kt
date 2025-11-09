package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for Scenario 3: Cascading provider deletion workflow.
 * Tests the complete workflow for provider deletion including event detachment requirements.
 */
class ProviderDeletionIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @Suppress("LongMethod") // Integration test requires comprehensive workflow validation
    fun `cascading provider deletion workflow works end-to-end`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val eventId1 = UUID.randomUUID()
        val eventSlug1 = "conference-2025"
        val eventId2 = UUID.randomUUID()
        val eventSlug2 = "workshop-2025"

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match mock auth
            insertMockedOrgaPermission(orgId, user, canEdit = true)

            // Create events for attachment testing
            insertMockedEventWithOrga(eventId1, organisation = org, slug = eventSlug1, name = "Conference 2025")
            insertMockedEventWithOrga(eventId2, organisation = org, slug = eventSlug2, name = "Workshop 2025")
        }

        // Step 1: Create provider for deletion testing
        val createProviderRequest = CreateProvider(
            name = "Provider To Delete",
            type = "test-provider",
        )

        val createResponse = client.post("/orgs/$orgSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createProviderRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdProvider = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val providerId = createdProvider["id"]?.jsonPrimitive?.content!!

        // Step 2: Attach provider to multiple events
        val attachRequest1 = listOf(providerId)
        val attachResponse1 = client.post("/orgs/$orgSlug/events/$eventSlug1/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(attachRequest1))
        }
        assertEquals(HttpStatusCode.OK, attachResponse1.status)

        val attachRequest2 = listOf(providerId)
        val attachResponse2 = client.post("/orgs/$orgSlug/events/$eventSlug2/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(attachRequest2))
        }
        assertEquals(HttpStatusCode.OK, attachResponse2.status)

        // Step 3: Attempt to delete provider while attached to events (should fail)
        val deleteResponse1 = client.delete("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.Conflict, deleteResponse1.status)
        val errorResponse = Json.parseToJsonElement(deleteResponse1.bodyAsText()).jsonObject
        val errorMessage = errorResponse["message"]?.jsonPrimitive?.content!!
        assertTrue(errorMessage.contains("attached to events"), "Error message should mention event attachments")

        // Step 4: Detach provider from first event
        val detachRequest1 = listOf(providerId)
        val detachResponse1 = client.delete("/orgs/$orgSlug/events/$eventSlug1/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(detachRequest1))
        }
        assertEquals(HttpStatusCode.OK, detachResponse1.status)

        // Step 5: Try deletion again (should still fail - attached to second event)
        val deleteResponse2 = client.delete("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.Conflict, deleteResponse2.status)

        // Step 6: Detach provider from second event
        val detachRequest2 = listOf(providerId)
        val detachResponse2 = client.delete("/orgs/$orgSlug/events/$eventSlug2/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(detachRequest2))
        }
        assertEquals(HttpStatusCode.OK, detachResponse2.status)

        // Step 7: Delete provider (should succeed now)
        val deleteResponse3 = client.delete("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse3.status)

        // Step 8: Verify provider is deleted (404 when trying to access)
        val getResponse = client.get("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, getResponse.status)

        // Step 9: Verify provider no longer appears in public listing
        val listResponse = client.get("/providers?org_slug=$orgSlug")
        assertEquals(HttpStatusCode.OK, listResponse.status)

        val listResult = Json.parseToJsonElement(listResponse.bodyAsText()).jsonObject
        val items = listResult["items"]?.jsonArray
        val hasDeletedProvider = items!!.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Provider To Delete" }
        assertTrue(!hasDeletedProvider, "Deleted provider should not appear in listings")
    }

    @Test
    fun `provider deletion works immediately for unattached providers`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com")
            insertMockedOrgaPermission(orgId, user, canEdit = true)

            // Create provider with no event attachments
            insertMockedProvider(providerId, organisation = org, name = "Unattached Provider")
        }

        // Delete unattached provider (should succeed immediately)
        val deleteResponse = client.delete("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify provider is deleted
        val getResponse = client.get("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }
}

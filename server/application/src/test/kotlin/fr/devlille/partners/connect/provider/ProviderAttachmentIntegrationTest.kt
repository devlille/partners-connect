package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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
 * Integration test for Scenario 2: Provider event attachment workflow.
 * Tests the complete end-to-end workflow for attaching/detaching providers to events.
 */
class ProviderAttachmentIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @Suppress("LongMethod") // Integration test requires comprehensive workflow validation
    fun `provider event attachment workflow works end-to-end`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val eventId1 = UUID.randomUUID()
        val eventSlug1 = "tech-conference-2025"
        val eventId2 = UUID.randomUUID()
        val eventSlug2 = "developer-meetup-2025"
        val providerId1 = UUID.randomUUID()
        val providerId2 = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match mock auth
            insertMockedOrgaPermission(orgId, user, canEdit = true)

            // Create events and providers
            insertMockedEventWithOrga(eventId1, organisation = org, slug = eventSlug1, name = "Tech Conference 2025")
            insertMockedEventWithOrga(eventId2, organisation = org, slug = eventSlug2, name = "Developer Meetup 2025")
            insertMockedProvider(providerId1, organisation = org, name = "Catering Service", type = "catering")
            insertMockedProvider(providerId2, organisation = org, name = "AV Equipment", type = "technical")
        }

        // Step 1: Attach single provider to event
        val singleAttachRequest = listOf(providerId1.toString())
        val attachResponse1 = client.post("/orgs/$orgSlug/events/$eventSlug1/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(singleAttachRequest))
        }

        assertEquals(HttpStatusCode.OK, attachResponse1.status)

        // Step 2: Verify provider appears in event provider listing
        val eventProvidersResponse1 = client.get("/orgs/$orgSlug/events/$eventSlug1/providers") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, eventProvidersResponse1.status)
        val eventProviders1 = Json.parseToJsonElement(eventProvidersResponse1.bodyAsText()).jsonObject
        val items1 = eventProviders1["items"]?.jsonArray
        assertTrue(items1!!.isNotEmpty())
        assertTrue(items1.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Catering Service" })

        // Step 3: Attach multiple providers to different event
        val bulkAttachRequest = listOf(providerId1.toString(), providerId2.toString())
        val attachResponse2 = client.post("/orgs/$orgSlug/events/$eventSlug2/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(bulkAttachRequest))
        }

        assertEquals(HttpStatusCode.OK, attachResponse2.status)

        // Step 4: Verify both providers appear in second event
        val eventProvidersResponse2 = client.get("/orgs/$orgSlug/events/$eventSlug2/providers") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, eventProvidersResponse2.status)
        val eventProviders2 = Json.parseToJsonElement(eventProvidersResponse2.bodyAsText()).jsonObject
        val items2 = eventProviders2["items"]?.jsonArray
        assertEquals(2, items2!!.size)
        assertTrue(items2.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Catering Service" })
        assertTrue(items2.any { it.jsonObject["name"]?.jsonPrimitive?.content == "AV Equipment" })

        // Step 5: Detach one provider from second event
        val partialDetachRequest = listOf(providerId2.toString())
        val detachResponse = client.delete("/orgs/$orgSlug/events/$eventSlug2/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(partialDetachRequest))
        }

        assertEquals(HttpStatusCode.OK, detachResponse.status)

        // Step 6: Verify only one provider remains in second event
        val eventProvidersResponse3 = client.get("/orgs/$orgSlug/events/$eventSlug2/providers") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, eventProvidersResponse3.status)
        val eventProviders3 = Json.parseToJsonElement(eventProvidersResponse3.bodyAsText()).jsonObject
        val items3 = eventProviders3["items"]?.jsonArray
        assertEquals(1, items3!!.size)
        assertTrue(items3.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Catering Service" })

        // Step 7: Verify first event still has its provider attached
        val finalEventProvidersResponse = client.get("/orgs/$orgSlug/events/$eventSlug1/providers") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, finalEventProvidersResponse.status)
        val finalEventProviders = Json.parseToJsonElement(finalEventProvidersResponse.bodyAsText()).jsonObject
        val finalItems = finalEventProviders["items"]?.jsonArray
        assertTrue(finalItems!!.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Catering Service" })
    }

    @Test
    fun `provider attachment validates organisation boundaries`() = testApplication {
        val orgId1 = UUID.randomUUID()
        val orgSlug1 = "org-1"
        val orgId2 = UUID.randomUUID()
        val orgSlug2 = "org-2"
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "cross-org-event"
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
            val org1 = insertMockedOrganisationEntity(orgId1, name = orgSlug1)
            val org2 = insertMockedOrganisationEntity(orgId2, name = orgSlug2)
            val user = insertMockedUser(userId, email = "john.doe@contact.com")
            insertMockedOrgaPermission(orgId1, user, canEdit = true)

            // Event belongs to org1, provider belongs to org2
            insertMockedEventWithOrga(eventId, organisation = org1, slug = eventSlug)
            insertMockedProvider(providerId, organisation = org2)
        }

        // Try to attach provider from org2 to event in org1 - should fail
        val attachRequest = listOf(providerId.toString())
        val response = client.post("/orgs/$orgSlug1/events/$eventSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(attachRequest))
        }

        // Should fail with appropriate error (404 or 403)
        assertTrue(response.status.value >= 400)
    }
}

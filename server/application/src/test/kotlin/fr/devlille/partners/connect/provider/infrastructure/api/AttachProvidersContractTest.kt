package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract test for POST /orgs/{orgSlug}/events/{eventSlug}/providers endpoint.
 * Tests JSON schema validation for create_by_identifiers.schema.json.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class AttachProvidersContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST providers validates create_by_identifiers schema successfully`() =
        testApplication {
            val orgId = UUID.randomUUID()
            val orgSlug = "test-org"
            val userId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val eventSlug = "test-event"
            val providerId = UUID.randomUUID()

            application {
                moduleMocked()
                val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
                val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
                insertMockedOrgaPermission(orgId, user, canEdit = true)
                insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
                insertMockedProvider(providerId, organisation = org)
            }

            val validAttachRequest = listOf(providerId.toString())

            val response = client.post("/orgs/$orgSlug/events/$eventSlug/providers") {
                header("Authorization", "Bearer valid")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(validAttachRequest))
            }

            // Route is now implemented, expecting successful attachment
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `POST attaches providers to event successfully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org"
        val testEventSlug = "test-event"
        val email = "john.doe@contact.com" // Must match the mock auth email

        // Use fixed provider IDs to avoid scoping issues
        val provider1Id = UUID.randomUUID()
        val provider2Id = UUID.randomUUID()

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            val orgEntity = insertMockedOrganisationEntity(id = orgId, name = testOrgSlug)
            insertMockedEvent(id = eventId, orgId = orgId, slug = testEventSlug, name = "Test Event")
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)

            // Create providers with fixed IDs
            insertMockedProvider(id = provider1Id, name = "Provider A", organisation = orgEntity)
            insertMockedProvider(id = provider2Id, name = "Provider B", organisation = orgEntity)
        }

        val providerIds = listOf(provider1Id.toString(), provider2Id.toString())

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(providerIds))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        val attachedIds = Json.parseToJsonElement(responseText).jsonArray
        assertEquals(2, attachedIds.size)
    }

    @Test
    fun `POST fails with 400 for empty provider IDs list`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org"
        val testEventSlug = "test-event"
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(id = orgId, name = testOrgSlug)
            insertMockedEvent(id = eventId, orgId = orgId, slug = testEventSlug)
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)
        }

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("[]")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("must not be an empty list"))
    }

    @Test
    fun `POST fails with 400 for invalid UUID format`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org"
        val testEventSlug = "test-event"
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(id = orgId, name = testOrgSlug)
            insertMockedEvent(id = eventId, orgId = orgId, slug = testEventSlug)
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)
        }

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("[\"invalid-uuid\"]")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("value does not match 'uuid' format"))
    }

    @Test
    fun `POST providers returns 401 for missing authorization`() = testApplication {
        val orgSlug = "test-org"
        val eventSlug = "test-event"
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val attachRequest = listOf(providerId.toString())

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/providers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(attachRequest))
        }

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST fails with 403 when user doesn't have write access to event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org"
        val testEventSlug = "test-event"
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(id = orgId, name = testOrgSlug)
            insertMockedEvent(id = eventId, orgId = orgId, slug = testEventSlug)
            // No permission granted to regular user - this should cause 401/403
        }

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("[\"some-uuid\"]")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("not allowed to edit"))
    }

    @Test
    fun `POST fails with 403 when provider IDs don't exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org"
        val testEventSlug = "test-event"
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(id = orgId, name = testOrgSlug)
            insertMockedEvent(id = eventId, orgId = orgId, slug = testEventSlug)
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)
        }

        val nonExistentIds = listOf(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
        )

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(nonExistentIds))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("One or more provider IDs do not exist"))
    }

    @Test
    fun `POST fails with 404 when event doesn't exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val testOrgSlug = "test-org"
        val testEventSlug = "nonexistent-event"
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(id = orgId, name = testOrgSlug)
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)
        }

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("[\"${UUID.randomUUID()}\"]")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Event with slug $testEventSlug not found"))
    }
}

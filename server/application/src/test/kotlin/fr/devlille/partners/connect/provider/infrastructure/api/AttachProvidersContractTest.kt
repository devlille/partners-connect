package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for POST /orgs/{orgSlug}/events/{eventSlug}/providers endpoint.
 * Tests JSON schema validation for create_by_identifiers.schema.json.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class AttachProvidersContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST orgs orgSlug events eventSlug providers validates create_by_identifiers schema successfully`() =
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
    fun `POST orgs orgSlug events eventSlug providers accepts multiple provider IDs`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val providerId1 = UUID.randomUUID()
        val providerId2 = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedProvider(providerId1, organisation = org)
            insertMockedProvider(providerId2, organisation = org)
        }

        val bulkAttachRequest = listOf(providerId1.toString(), providerId2.toString())

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(bulkAttachRequest))
        }

        // Route is now implemented, expecting successful bulk attachment
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST orgs orgSlug events eventSlug providers returns 401 for missing authorization`() = testApplication {
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
    fun `POST orgs orgSlug events eventSlug providers returns 400 for invalid request body`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val eventSlug = "test-event"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        // Invalid request - empty array violates minItems: 1
        val invalidAttachRequest = emptyList<String>()

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(invalidAttachRequest))
        }

        // Should return 400 for schema validation error
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}

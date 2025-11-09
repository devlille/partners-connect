package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.UpdateProvider
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for PUT /orgs/{orgSlug}/providers/{id} endpoint.
 * Tests JSON schema validation for update_provider.schema.json.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class UpdateProviderContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT orgs orgSlug providers id validates update_provider schema successfully`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            insertMockedProvider(providerId, organisation = org)
        }

        val validUpdateRequest = UpdateProvider(
            name = "Updated Provider Name",
            phone = "+33987654321",
        )

        val response = client.put("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), validUpdateRequest))
        }

        // Route is now implemented, expecting successful update
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT orgs orgSlug providers id accepts partial updates`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            insertMockedProvider(providerId, organisation = org)
        }

        // Partial update - only name field
        val partialUpdateRequest = UpdateProvider(name = "New Name Only")

        val response = client.put("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), partialUpdateRequest))
        }

        // Route is now implemented, expecting successful update
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT orgs orgSlug providers id returns 401 for missing authorization`() = testApplication {
        val orgSlug = "test-org"
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.put("/orgs/$orgSlug/providers/$providerId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), UpdateProvider()))
        }

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT orgs orgSlug providers id returns 404 for non-existent provider`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val nonExistentProviderId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        val validUpdateRequest = UpdateProvider(name = "Updated Name")

        val response = client.put("/orgs/$orgSlug/providers/$nonExistentProviderId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), validUpdateRequest))
        }

        // Route is implemented, but provider doesn't exist
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}

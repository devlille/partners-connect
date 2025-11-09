package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for POST /orgs/{orgSlug}/providers endpoint.
 * Tests JSON schema validation for create_provider.schema.json.
 * Validates that the endpoint correctly handles valid and invalid requests.
 */
class CreateProviderContractTest {
    @Test
    fun `POST orgs orgSlug providers validates create_provider schema successfully`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com")
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        val validRequest = CreateProvider(
            name = "Test Catering Service",
            type = "catering",
            website = "https://test-catering.com",
            phone = "+33123456789",
            email = "contact@test-catering.com",
        )

        val response = client.post("/orgs/$orgSlug/providers") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CreateProvider.serializer(), validRequest))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST orgs orgSlug providers returns 400 for invalid request body`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com")
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        // Invalid request - missing required fields
        val invalidRequest = mapOf(
            "website" to "https://test.com",
            // Missing required name and type fields
        )

        val response = client.post("/orgs/$orgSlug/providers") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(invalidRequest))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST orgs orgSlug providers returns 401 for missing authorization`() = testApplication {
        val orgSlug = "test-org"

        application {
            moduleMocked()
        }

        val validRequest = CreateProvider(
            name = "Test Provider",
            type = "Technology",
            website = "https://testprovider.com",
            phone = "+33123456789",
            email = "contact@testprovider.com",
        )

        val response = client.post("/orgs/$orgSlug/providers") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CreateProvider.serializer(), validRequest))
        }

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

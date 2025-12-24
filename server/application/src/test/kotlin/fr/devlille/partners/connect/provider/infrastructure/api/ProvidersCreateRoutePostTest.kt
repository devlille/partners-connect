package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.CreateProvider
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
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract test for POST /orgs/{orgSlug}/providers endpoint.
 * Tests JSON schema validation for create_provider.schema.json.
 * Validates that the endpoint correctly handles valid and invalid requests.
 */
class ProvidersCreateRoutePostTest {
    @Test
    fun `POST providers validates create_provider schema successfully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val validRequest = CreateProvider(
            name = UUID.randomUUID().toString(),
            type = "catering",
            website = "https://test-catering.com",
            phone = "+33123456789",
            email = "${UUID.randomUUID()}@test-catering.com",
        )

        val response = client.post("/orgs/$orgId/providers") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CreateProvider.serializer(), validRequest))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST providers returns 400 for invalid request body`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        // Invalid request - missing required fields
        val invalidRequest = mapOf(
            "website" to "https://test.com",
            // Missing required name and type fields
        )

        val response = client.post("/orgs/$orgId/providers") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(invalidRequest))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST providers returns 401 for missing authorization`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val validRequest = CreateProvider(
            name = "Test Provider",
            type = "Technology",
            website = "https://testprovider.com",
            phone = "+33123456789",
            email = "contact@testprovider.com",
        )

        val response = client.post("/orgs/$orgId/providers") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CreateProvider.serializer(), validRequest))
        }

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST providers fails with 401 when user has no organizer permissions`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val providerInput = CreateProvider(
            name = "Test Provider",
            type = "Technology",
            website = "https://testprovider.com",
            phone = "+33123456789",
            email = "contact@testprovider.com",
        )

        val response = client.post("/orgs/$orgId/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateProvider.serializer(), providerInput))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("not allowed to edit"))
    }
}
